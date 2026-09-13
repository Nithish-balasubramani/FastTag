package com.example.data.repository

data class RangeExpansionResult(
    val isValid: Boolean,
    val quantity: Int,
    val serials: List<String>,
    val errorMessage: String? = null
)

object SerialRangeHelper {
    private const val MAX_RANGE_LIMIT = 2000

    /**
     * Safely parses and expands a sequential range from `fromSerial` to `toSerial`.
     * Supports purely numeric serials (e.g., "100001" to "100050")
     * and structured alphanumeric serials with common prefix and numeric suffix (e.g., "TAG100001" to "TAG100050").
     */
    fun expandRange(fromRaw: String, toRaw: String): RangeExpansionResult {
        val from = fromRaw.trim()
        val to = toRaw.trim()

        if (from.isEmpty() || to.isEmpty()) {
            return RangeExpansionResult(false, 0, emptyList(), "From and To serials cannot be empty")
        }

        if (from == to) {
            return RangeExpansionResult(true, 1, listOf(from))
        }

        // Pure numeric case
        if (from.all { it.isDigit() } && to.all { it.isDigit() }) {
            val fromNum = from.toLongOrNull()
            val toNum = to.toLongOrNull()

            if (fromNum == null || toNum == null) {
                return RangeExpansionResult(false, 0, emptyList(), "Numeric serial numbers exceed valid range limits")
            }

            if (fromNum > toNum) {
                return RangeExpansionResult(false, 0, emptyList(), "From serial ($from) must be less than or equal to To serial ($to)")
            }

            val qty = (toNum - fromNum + 1).toInt()
            if (qty > MAX_RANGE_LIMIT) {
                return RangeExpansionResult(false, 0, emptyList(), "Range exceeds maximum allowable limit of $MAX_RANGE_LIMIT tags (Found: $qty)")
            }

            val padLength = from.length
            val list = ArrayList<String>(qty)
            for (i in fromNum..toNum) {
                list.add(i.toString().padStart(padLength, '0'))
            }
            return RangeExpansionResult(true, qty, list)
        }

        // Alphanumeric structured case with prefix + numeric suffix
        val fromMatch = Regex("^([A-Za-z_-]+)(\\d+)$").find(from)
        val toMatch = Regex("^([A-Za-z_-]+)(\\d+)$").find(to)

        if (fromMatch != null && toMatch != null) {
            val prefixFrom = fromMatch.groupValues[1]
            val suffixFrom = fromMatch.groupValues[2]
            val prefixTo = toMatch.groupValues[1]
            val suffixTo = toMatch.groupValues[2]

            if (prefixFrom.uppercase() != prefixTo.uppercase()) {
                return RangeExpansionResult(false, 0, emptyList(), "Prefix mismatch: '$prefixFrom' vs '$prefixTo'")
            }

            val fromNum = suffixFrom.toLongOrNull()
            val toNum = suffixTo.toLongOrNull()

            if (fromNum == null || toNum == null) {
                return RangeExpansionResult(false, 0, emptyList(), "Invalid numeric suffix in structured serial")
            }

            if (fromNum > toNum) {
                return RangeExpansionResult(false, 0, emptyList(), "From serial number ($suffixFrom) must be <= To serial number ($suffixTo)")
            }

            val qty = (toNum - fromNum + 1).toInt()
            if (qty > MAX_RANGE_LIMIT) {
                return RangeExpansionResult(false, 0, emptyList(), "Range size ($qty) exceeds maximum allowable limit of $MAX_RANGE_LIMIT tags")
            }

            val padLength = suffixFrom.length
            val list = ArrayList<String>(qty)
            for (i in fromNum..toNum) {
                list.add(prefixFrom + i.toString().padStart(padLength, '0'))
            }
            return RangeExpansionResult(true, qty, list)
        }

        return RangeExpansionResult(false, 0, emptyList(), "Serial format cannot be automatically incremented. Please enter individual serials or correct format.")
    }
}
