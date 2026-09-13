package com.example

import com.example.ocr.OcrProcessingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrProcessingEngineTest {

    @Test
    fun testPhysicalFastagStickerParsing() {
        val stickerText = """
            NPCI NETC FASTag
            IDFC FIRST BANK
            Class 4
            DL01AB1234
            6074170010045678
            Date: 12/09/2026
        """.trimIndent()

        val result = OcrProcessingEngine.parseOcrDetailed(stickerText)
        assertTrue(result.isSuccess)
        assertEquals("Class 4", result.detectedClass)
        assertEquals("IDFC FIRST Bank", result.detectedBank)
        assertEquals("DL01AB1234", result.detectedVehicleNumber)
        assertTrue(result.detectedSerials.contains("6074170010045678"))
        assertEquals("6074170010045678", result.primarySerial)
    }

    @Test
    fun testDispatchManifestRangeParsing() {
        val manifestText = """
            CENTRAL INVENTORY DISPATCH MANIFEST
            Subagent ID | Subagent Name | Tag Class | From Serial | To Serial
            SA001 | Rajesh Kumar | Class 12 | 100001 | 100050
        """.trimIndent()

        val result = OcrProcessingEngine.parseOcrDetailed(manifestText)
        assertTrue(result.isSuccess)
        assertEquals(1, result.candidates.size)
        val candidate = result.candidates[0]
        assertTrue(candidate.isRange)
        assertEquals("100001", candidate.fromSerial)
        assertEquals("100050", candidate.toSerial)
        assertEquals(50, candidate.calculatedQuantity)
        assertEquals("Class 12", candidate.tagClass)
    }

    @Test
    fun testArrowRangeParsing() {
        val arrowText = """
            DELIVERY NOTE
            Item: FASTag Toll Tags
            Range: 200001 -> 200025
            Vehicle: MH12DE1433
            Class: Class 7
        """.trimIndent()

        val result = OcrProcessingEngine.parseOcrDetailed(arrowText)
        assertTrue(result.isSuccess)
        assertEquals("Class 7", result.detectedClass)
        assertEquals("MH12DE1433", result.detectedVehicleNumber)
        assertTrue(result.detectedRanges.isNotEmpty())
        assertEquals("200001", result.detectedRanges[0].first)
        assertEquals("200025", result.detectedRanges[0].second)
    }

    @Test
    fun testExtractionOfMultipleSerials() {
        val sheetText = """
            FASTAG BARCODE SHEET
            Tag 1: 6074170010011111
            Tag 2: 6074170010012222
            Tag 3: 6074170010013333
        """.trimIndent()

        val result = OcrProcessingEngine.parseOcrDetailed(sheetText)
        assertTrue(result.isSuccess)
        assertEquals(3, result.detectedSerials.size)
        assertTrue(result.detectedSerials.contains("6074170010011111"))
        assertTrue(result.detectedSerials.contains("6074170010012222"))
        assertTrue(result.detectedSerials.contains("6074170010013333"))
    }
}
