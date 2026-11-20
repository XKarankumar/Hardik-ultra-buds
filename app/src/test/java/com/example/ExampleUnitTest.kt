package com.example

import com.example.model.PresetCatalog
import com.example.update.AppUpdateManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun semVerComparison_handlesMajorMinorPatch() {
        assertTrue(AppUpdateManager.compareSemVer("1.0.1", "1.0.0") > 0)
        assertTrue(AppUpdateManager.compareSemVer("1.2.0", "1.1.9") > 0)
        assertEquals(0, AppUpdateManager.compareSemVer("1.0.0", "1.0.0"))
        assertTrue(AppUpdateManager.compareSemVer("1.0.0", "1.0.1") < 0)
    }

    @Test
    fun presetCatalog_hasAllNinePresetsWithTenBands() {
        assertEquals(9, PresetCatalog.builtInPresets.size)
        assertEquals(10, PresetCatalog.freqLabels.size)
        PresetCatalog.builtInPresets.forEach { preset ->
            assertEquals("Preset ${preset.name} must have 10 EQ bands", 10, preset.bandsDb.size)
            assertTrue(preset.loudnessGainDb in 0f..10f)
        }
    }
}
