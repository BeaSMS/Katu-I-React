package com.katui.offline

import com.katui.offline.util.MedicalUnitConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class MedicalUnitConverterTest {

    @Test
    fun testGlucoseNormalization() {
        val mmolValue = 5.0
        val normalized = MedicalUnitConverter.normalize(mmolValue, "mmol/L", "Glicose")
        assertEquals(90.08, normalized, 0.1)
    }

    @Test
    fun testHemoglobinNormalization() {
        val gLValue = 140.0
        val normalized = MedicalUnitConverter.normalize(gLValue, "g/L", "Hemoglobina")
        assertEquals(14.0, normalized, 0.1)
    }

    @Test
    fun testParseReferenceRange() {
        val rangeStr = "70 - 99"
        val range = MedicalUnitConverter.parseReferenceRange(rangeStr)
        assertEquals(70.0, range?.first ?: 0.0, 0.01)
        assertEquals(99.0, range?.second ?: 0.0, 0.01)
    }

    @Test
    fun testParseReferenceRangeLess() {
        val rangeStr = "< 150"
        val range = MedicalUnitConverter.parseReferenceRange(rangeStr)
        assertEquals(0.0, range?.first ?: -1.0, 0.01)
        assertEquals(150.0, range?.second ?: 0.0, 0.01)
    }
}
