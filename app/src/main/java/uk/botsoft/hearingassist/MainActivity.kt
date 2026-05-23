package uk.botsoft.hearingassist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import uk.botsoft.hearingassist.data.AppSettings
import uk.botsoft.hearingassist.maintenance.ErrorReporter
import uk.botsoft.hearingassist.ui.HearingAssistApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ErrorReporter(applicationContext).installCrashHandler(AppSettings().maintenanceSettings.productId)
        setContent {
            HearingAssistApp()
        }
    }
}
