package compose.broadcasts.sample

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import compose.broadcasts.rememberBatteryLevel
import compose.broadcasts.rememberCurrentInputMethod
import compose.broadcasts.rememberCurrentTimeMillis
import compose.broadcasts.rememberIsAirplaneModeOn
import compose.broadcasts.rememberIsCharging
import compose.broadcasts.rememberIsHeadsetConnected
import compose.broadcasts.rememberIsScreenOn
import compose.broadcasts.rememberPackageInfo
import compose.broadcasts.rememberSystemLocale
import compose.broadcasts.sample.ui.theme.ComposeBroadcastsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : ComponentActivity() {
    private val localeReceiver = LocaleReceiver()
    private val packageInfoReceiver = PackageInfoReceiver()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ComposeBroadcastsTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = { Text("Compose Broadcasts") },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                ) { bodyPadding ->
                    Column(
                        modifier = Modifier
                            .padding(bodyPadding)
                            .padding(horizontal = 24.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Box(Modifier.padding(top = 16.dp))

                        // Airplane Mode
                        Text(
                            text = "Airplane mode is " +
                                    if (rememberIsAirplaneModeOn().value) "ON" else "OFF" +
                                            " (try toggling it!)",
                        )

                        // Battery Level
                        Text("Current battery level is ${rememberBatteryLevel().value}")

                        // Is Charging (or NOT)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Text(
                                text = "Device is ${if (rememberIsCharging().value) "" else "NOT "}" +
                                        "charging"
                            )
                        }

                        // Package Info
                        Text(
                            "Package Info for the app you just interacted with " +
                                    "(Try installing, uninstalling, or updating an app " +
                                    "[make sure to change its version number when updating]): " +
                                    rememberPackageInfo(packageInfoReceiver).value
                        )

                        // Current System Time Millis (updated every minute with the system)
                        Text(
                            "Time (updated every minute with system): " +
                                    convertMillisToTimeString(rememberCurrentTimeMillis().value)
                        )

                        // Current System Local
                        Text(
                            "Current System Locale: " +
                                    rememberSystemLocale(localeReceiver).value.toLanguageTag()
                        )

                        // Screen ON/OFF state
                        val isScreenOn by rememberIsScreenOn()
                        val screenOnOffList = rememberSaveable(
                            saver = Saver(
                                save = { list -> list.joinToString(", ") },
                                restore = { listString ->
                                    SnapshotStateList<String>().apply {
                                        addAll(listString.split(", "))
                                    }
                                },
                            )
                        ) { mutableStateListOf<String>() }

                        // Making sure to not add same state repeatedly in the list
                        LaunchedEffect(isScreenOn) {
                            val lastItemWasOnAndStateIsOn = isScreenOn
                                    && screenOnOffList.lastOrNull()?.contains("ON") == true

                            val lastItemWasOffAndStateIsOff = !isScreenOn
                                    && screenOnOffList.lastOrNull()?.contains("OFF") == true

                            val lastItemMatchesState =
                                lastItemWasOnAndStateIsOn || lastItemWasOffAndStateIsOff

                            if (!lastItemMatchesState) {
                                screenOnOffList.add(
                                    "${if (isScreenOn) "ON" else "OFF"} at " +
                                            convertMillisToTimeString(
                                                System.currentTimeMillis(),
                                                showSeconds = true,
                                            )
                                )
                            }
                        }

                        Text(
                            "Screen On/Off events (try turning screen off and on multiple times):\n" +
                                    screenOnOffList.joinToString("\n")
                        )

                        // Headset/Earphone Connection Status
                        Text(
                            "Is an earphone/headset connected?: " +
                                    rememberIsHeadsetConnected().value
                        )

                        // Current Input Method
                        val currentInputMethod by rememberCurrentInputMethod()
                        currentInputMethod?.let { inputMethod ->
                            Image(
                                modifier = Modifier.size(64.dp),
                                bitmap = inputMethod.appIcon.toBitmap().asImageBitmap(),
                                contentDescription = null
                            )
                        }
                        Text(
                            "Current input method (you can click on the TextField " +
                                    "below to open the keyboard and be able to switch to " +
                                    "a different input method): " +
                                    currentInputMethod
                        )
                        TextField(
                            value = "",
                            onValueChange = {},
                            placeholder = { Text("Click to open keyboard") },
                        )

                        Box(Modifier.padding(bottom = 16.dp))
                    }
                }
            }
        }
    }
}

private fun convertMillisToTimeString(millis: Long, showSeconds: Boolean = false): String {
    // Get the system's default time zone and locale
    val timeZone = TimeZone.getDefault()
    val locale = Locale.getDefault()

    // Create a SimpleDateFormat for hh:mm a (12-hour format with AM/PM)
    val dateFormat = SimpleDateFormat(if (showSeconds) "hh:mm:ss a" else "hh:mm a", locale)
    dateFormat.timeZone = timeZone

    // Convert milliseconds to a Date object
    val date = Date(millis)

    // Return formatted time string with AM/PM
    return dateFormat.format(date)
}
