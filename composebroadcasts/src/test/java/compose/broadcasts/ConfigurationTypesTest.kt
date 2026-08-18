package compose.broadcasts

import android.content.Intent
import android.location.LocationManager
import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfigurationTypesTest {
    @Test
    fun `predefined actions expose their Android intent values`() {
        assertEquals(
            Intent.ACTION_AIRPLANE_MODE_CHANGED,
            CBIntentAction.AirplaneModeChanged.rawValue
        )
        assertEquals(Intent.ACTION_PACKAGE_ADDED, CBIntentAction.PackageAdded.rawValue)
        assertEquals(Intent.ACTION_INPUT_METHOD_CHANGED, CBIntentAction.InputMethodChanged.rawValue)
        assertEquals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED, CBIntentAction.PowerSaveModeChanged.rawValue)
        assertEquals(LocationManager.MODE_CHANGED_ACTION, CBIntentAction.LocationModeChanged.rawValue)
    }

    @Test
    fun `custom action data type and scheme retain their supplied values`() {
        val filter = CBIntentFilter(
            action = CBIntentAction.Custom("example.ACTION_SYNC"),
            dataType = CBIntentDataType.Custom("application/example"),
            dataScheme = CBIntentDataScheme.Custom("example"),
        )

        assertEquals("example.ACTION_SYNC", filter.action.rawValue)
        assertEquals("application/example", filter.dataType?.rawValue)
        assertEquals("example", filter.dataScheme?.rawValue)
    }

    @Test
    fun `library tags are recognized while arbitrary user tags are allowed`() {
        assertTrue(CBConstants.entries.all { it.value.isComposeBroadcastsTag })
        assertFalse("my-custom-receiver".isComposeBroadcastsTag)
    }
}
