package com.example.ocr

import java.util.UUID

enum class CandidateType {
    INDIVIDUAL,
    RANGE
}

data class ExtractedTagCandidate(
    val id: String = UUID.randomUUID().toString(),
    var subagentId: String = "",
    var subagentName: String = "",
    var tagClass: String = "Class 12",
    var isRange: Boolean = false,
    var serialNumber: String = "",
    var fromSerial: String = "",
    var toSerial: String = "",
    var calculatedQuantity: Int = 1,
    var confidenceScore: Float = 0.95f,
    var hasError: Boolean = false,
    var errorMessage: String? = null,
    var isSelected: Boolean = true
) {
    fun updateCalculatedQuantity() {
        if (!isRange) {
            calculatedQuantity = if (serialNumber.isNotBlank()) 1 else 0
            hasError = serialNumber.isBlank()
            errorMessage = if (hasError) "Serial number is required" else null
        } else {
            val result = com.example.data.repository.SerialRangeHelper.expandRange(fromSerial, toSerial)
            calculatedQuantity = result.quantity
            hasError = !result.isValid
            errorMessage = result.errorMessage
        }
    }
}
