package com.example.ocr

data class FastagOcrResult(
    val rawText: String = "",
    val candidates: List<ExtractedTagCandidate> = emptyList(),
    val detectedSerials: List<String> = emptyList(),
    val primarySerial: String? = null,
    val detectedClass: String? = null,
    val detectedVehicleNumber: String? = null,
    val detectedBank: String? = null,
    val detectedSubagentId: String? = null,
    val detectedSubagentName: String? = null,
    val detectedDate: String? = null,
    val detectedRanges: List<Pair<String, String>> = emptyList(),
    val textBlocksCount: Int = 0,
    val linesCount: Int = 0,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)
