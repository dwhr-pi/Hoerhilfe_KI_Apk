package uk.botsoft.hearingassist.audio

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.random.Random
import uk.botsoft.hearingassist.data.EarSide
import uk.botsoft.hearingassist.data.HearingTestResult

data class HearingTestUiState(
    val isRunning: Boolean = false,
    val status: String = "",
    val canReact: Boolean = false,
    val currentFrequencyHz: Int? = null,
    val currentLevelPercent: Int? = null,
    val currentEar: EarSide? = null,
    val completedUnits: Int = 0,
    val totalUnits: Int = 64,
    val lastReactionMs: Long? = null,
    val result: HearingTestResult = HearingTestResult(),
)

class AdaptiveHearingTestEngine(
    private val player: HearingTestPlayer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val random = Random(System.currentTimeMillis())
    private val frequencies = listOf(
        125,
        250,
        500,
        750,
        1000,
        1250,
        1500,
        1750,
        2000,
        2250,
        2500,
        2750,
        3000,
        3250,
        3500,
        3750,
        4000,
        4250,
        4500,
        5000,
        5250,
        5500,
        5750,
        6000,
        6250,
        6500,
        6750,
        7000,
        7250,
        7500,
        7750,
        8000,
    )
    private val ears = listOf(EarSide.Left, EarSide.Right)
    private val levelSteps = listOf(0.88f, 0.72f, 0.58f, 0.46f, 0.34f, 0.24f, 0.17f, 0.11f, 0.07f)
    private val levelDbMap = listOf(60, 52, 44, 36, 28, 22, 16, 10, 5)
    private val baselineReactionTimesMs = mutableListOf<Long>()
    private val frequencyStates = buildMap {
        ears.forEach { ear ->
            frequencies.forEach { frequency ->
                put(ear to frequency, FrequencyState(ear, frequency))
            }
        }
    }

    @Volatile
    private var running = false

    private var activeTrial: ActiveTrial? = null
    private var testJob: Job? = null
    private var stateCallback: (HearingTestUiState) -> Unit = {}
    private var completionCallback: (HearingTestResult) -> Unit = {}

    fun start(
        onStateChanged: (HearingTestUiState) -> Unit,
        onCompleted: (HearingTestResult) -> Unit,
    ) {
        stop()
        running = true
        stateCallback = onStateChanged
        completionCallback = onCompleted
        baselineReactionTimesMs.clear()
        frequencyStates.values.forEach { it.reset() }

        emitState(
            HearingTestUiState(
                isRunning = true,
                status = "Hörtest gestartet",
                totalUnits = frequencyStates.size,
            ),
        )

        testJob = scope.launch {
            while (running && frequencyStates.values.any { !it.isComplete }) {
                val nextState = selectNextFrequency() ?: break
                val delayMs = random.nextLong(1_000L, 2_200L)
                emitStateSnapshot(status = "Nächster Ton wird vorbereitet")
                delay(delayMs)
                val heard = runTrial(nextState)
                applyTrialResult(nextState, heard)
            }

            if (running) {
                val result = buildResult()
                running = false
                emitState(
                    HearingTestUiState(
                        isRunning = false,
                        status = "Hörtest abgeschlossen. Bitte empfohlenes Profil speichern.",
                        completedUnits = frequencyStates.size,
                        totalUnits = frequencyStates.size,
                        result = result,
                    ),
                )
                withContext(Dispatchers.Main) {
                    completionCallback(result)
                }
            }
        }
    }

    fun registerReaction() {
        val trial = activeTrial ?: return
        if (!running) return
        if (!trial.response.isCompleted) {
            val reactionMs = System.currentTimeMillis() - trial.startedAtMs
            trial.response.complete(reactionMs.coerceAtLeast(0))
        }
    }

    fun stop() {
        running = false
        activeTrial?.response?.complete(null)
        activeTrial = null
        testJob?.cancel()
        testJob = null
        scope.coroutineContext.cancelChildren()
        emitState(
            HearingTestUiState(
                isRunning = false,
                status = "Hörtest gestoppt",
                completedUnits = frequencyStates.values.count { it.isComplete },
                totalUnits = frequencyStates.size,
                result = buildResult(),
            ),
        )
    }

    private suspend fun runTrial(state: FrequencyState): Boolean {
        val boostedStep = (state.currentStep - state.pendingReplayBoost).coerceAtLeast(0)
        val levelIndex = boostedStep.coerceIn(levelSteps.indices)
        val level = levelSteps[levelIndex]

        emitStateSnapshot(
            status = "Ton bei ${state.frequencyHz} Hz auf ${state.ear.label()} wird abgespielt",
            canReact = true,
            currentFrequency = state.frequencyHz,
            currentLevel = (level * 100).roundToInt(),
            currentEar = state.ear,
            lastReactionMs = null,
        )

        val response = CompletableDeferred<Long?>()
        activeTrial = ActiveTrial(
            ear = state.ear,
            frequencyHz = state.frequencyHz,
            levelIndex = levelIndex,
            startedAtMs = System.currentTimeMillis(),
            response = response,
        )

        withContext(Dispatchers.Main) {
            player.playTone(
                frequencyHz = state.frequencyHz,
                level = level,
                durationMs = 700,
                earSide = state.ear,
            )
        }

        val reactionMs = kotlinx.coroutines.withTimeoutOrNull(RESPONSE_WINDOW_MS) {
            response.await()
        }

        val baseline = baselineReactionTimesMs.average().takeIf { !it.isNaN() } ?: 0.0
        val reactionTooFast = reactionMs != null && (reactionMs < MIN_VALID_REACTION_MS || (baseline > 0.0 && reactionMs < baseline * 0.42))
        val reactionTooSlow = reactionMs != null && (reactionMs > MAX_VALID_REACTION_MS || (baseline > 0.0 && reactionMs > baseline * 2.35))
        val validReaction = reactionMs != null && !reactionTooFast && !reactionTooSlow

        activeTrial = null
        emitStateSnapshot(
            canReact = false,
            currentFrequency = state.frequencyHz,
            currentLevel = (level * 100).roundToInt(),
            currentEar = state.ear,
            lastReactionMs = reactionMs,
            status = if (validReaction) {
                "Ton bei ${state.frequencyHz} Hz auf ${state.ear.label()} erkannt nach ${reactionMs} ms"
            } else if (reactionTooFast) {
                "Klick bei ${state.frequencyHz} Hz auf ${state.ear.label()} war zu schnell. Ton wird später wiederholt."
            } else if (reactionTooSlow) {
                "Klick bei ${state.frequencyHz} Hz auf ${state.ear.label()} war zu spät. Ton wird später wiederholt."
            } else {
                "Kein sicherer Klick bei ${state.frequencyHz} Hz auf ${state.ear.label()}"
            },
        )

        state.totalTrials++
        if (validReaction) {
            val validReactionMs = reactionMs ?: 0L
            state.reactionTimesMs += validReactionMs
            state.heardDbCandidates += levelDbMap[levelIndex]
            if (levelIndex <= 2 && baselineReactionTimesMs.size < 8) {
                baselineReactionTimesMs += validReactionMs
            }
            state.pendingReplayCount = 0
            state.pendingReplayBoost = 0
        } else {
            state.pendingReplayCount = (state.pendingReplayCount + 1).coerceAtMost(3)
            state.pendingReplayBoost = if (reactionTooFast) 0 else 1
        }
        return validReaction
    }

    private fun applyTrialResult(state: FrequencyState, heard: Boolean) {
        val previousDirection = state.lastDirection
        if (heard) {
            state.lastDirection = Direction.Heard
            if (previousDirection == Direction.Missed) state.reversals++
            state.currentStep = when {
                state.currentStep <= 1 -> (state.currentStep + 2).coerceAtMost(levelSteps.lastIndex)
                else -> (state.currentStep + 1).coerceAtMost(levelSteps.lastIndex)
            }
        } else {
            state.lastDirection = Direction.Missed
            if (previousDirection == Direction.Heard) state.reversals++
            state.currentStep = (state.currentStep - 1).coerceAtLeast(0)
        }

        if (state.pendingReplayCount == 0 && state.totalTrials >= 4 && state.reversals >= 2) {
            state.isComplete = true
        }
        if (state.pendingReplayCount == 0 && state.totalTrials >= 6) {
            state.isComplete = true
        }

        emitStateSnapshot(
            completedUnits = frequencyStates.values.count { it.isComplete },
            result = buildResult(),
        )
    }

    private fun selectNextFrequency(): FrequencyState? {
        val candidates = frequencyStates.values.filter { !it.isComplete }
        val replayCandidates = candidates.filter { it.pendingReplayCount > 0 }
        return when {
            replayCandidates.isNotEmpty() -> replayCandidates.shuffled(random).firstOrNull()
            else -> candidates.shuffled(random).firstOrNull()
        }
    }

    private fun buildResult(): HearingTestResult {
        val left = mutableMapOf<Int, Int>()
        val right = mutableMapOf<Int, Int>()
        frequencyStates.values.forEach { state ->
            if (state.heardDbCandidates.isEmpty()) return@forEach
            when (state.ear) {
                EarSide.Left -> left[state.frequencyHz] = state.estimatedThresholdDb()
                EarSide.Right -> right[state.frequencyHz] = state.estimatedThresholdDb()
            }
        }
        return HearingTestResult(
            leftThresholdByFrequency = left.toSortedMap(),
            rightThresholdByFrequency = right.toSortedMap(),
        )
    }

    private fun emitStateSnapshot(
        status: String? = null,
        canReact: Boolean? = null,
        currentFrequency: Int? = null,
        currentLevel: Int? = null,
        currentEar: EarSide? = null,
        completedUnits: Int? = null,
        lastReactionMs: Long? = null,
        result: HearingTestResult? = null,
    ) {
        emitState(
            HearingTestUiState(
                isRunning = running,
                status = status ?: "",
                canReact = canReact ?: false,
                currentFrequencyHz = currentFrequency,
                currentLevelPercent = currentLevel,
                currentEar = currentEar,
                completedUnits = completedUnits ?: frequencyStates.values.count { it.isComplete },
                totalUnits = frequencyStates.size,
                lastReactionMs = lastReactionMs,
                result = result ?: buildResult(),
            ),
        )
    }

    private fun emitState(state: HearingTestUiState) {
        scope.launch(Dispatchers.Main) {
            stateCallback(state)
        }
    }

    private data class ActiveTrial(
        val ear: EarSide,
        val frequencyHz: Int,
        val levelIndex: Int,
        val startedAtMs: Long,
        val response: CompletableDeferred<Long?>,
    )

    private class FrequencyState(
        val ear: EarSide,
        val frequencyHz: Int,
    ) {
        var currentStep: Int = 1
        var totalTrials: Int = 0
        var reversals: Int = 0
        var isComplete: Boolean = false
        var pendingReplayCount: Int = 0
        var pendingReplayBoost: Int = 0
        var lastDirection: Direction? = null
        val heardDbCandidates = mutableListOf<Int>()
        val reactionTimesMs = mutableListOf<Long>()

        fun reset() {
            currentStep = 1
            totalTrials = 0
            reversals = 0
            isComplete = false
            pendingReplayCount = 0
            pendingReplayBoost = 0
            lastDirection = null
            heardDbCandidates.clear()
            reactionTimesMs.clear()
        }

        fun estimatedThresholdDb(): Int {
            val tail = heardDbCandidates.takeLast(3)
            return if (tail.isEmpty()) 60 else tail.average().roundToInt()
        }
    }

    private enum class Direction {
        Heard,
        Missed,
    }

    private fun EarSide.label(): String = when (this) {
        EarSide.Left -> "links"
        EarSide.Right -> "rechts"
    }

    private companion object {
        const val RESPONSE_WINDOW_MS = 2_000L
        const val MIN_VALID_REACTION_MS = 140L
        const val MAX_VALID_REACTION_MS = 1_700L
    }
}
