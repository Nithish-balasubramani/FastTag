package com.example.ocr

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object OcrProcessingEngine {

    suspend fun processImage(context: Context, imageUri: Uri): List<ExtractedTagCandidate> {
        return processImageDetailed(context, imageUri).candidates
    }

    suspend fun processBitmap(bitmap: Bitmap): List<ExtractedTagCandidate> {
        return processBitmapDetailed(bitmap).candidates
    }

    suspend fun processImageDetailed(context: Context, imageUri: Uri): FastagOcrResult {
        return try {
            val inputImage = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            if (visionText != null && visionText.text.isNotBlank()) {
                val blocksCount = visionText.textBlocks.size
                val linesCount = visionText.textBlocks.sumOf { it.lines.size }
                parseOcrDetailed(visionText.text, blocksCount, linesCount)
            } else {
                FastagOcrResult(
                    isSuccess = false,
                    errorMessage = "No legible text recognized in this FASTag document image."
                )
            }
        } catch (e: Exception) {
            FastagOcrResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "ML Kit Text Recognition failed"
            )
        }
    }

    suspend fun processBitmapDetailed(bitmap: Bitmap): FastagOcrResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = suspendCancellableCoroutine { cont ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { cont.resume(it) }
                    .addOnFailureListener { cont.resume(null) }
            }
            if (visionText != null && visionText.text.isNotBlank()) {
                val blocksCount = visionText.textBlocks.size
                val linesCount = visionText.textBlocks.sumOf { it.lines.size }
                parseOcrDetailed(visionText.text, blocksCount, linesCount)
            } else {
                FastagOcrResult(
                    isSuccess = false,
                    errorMessage = "No legible text recognized in this FASTag document image."
                )
            }
        } catch (e: Exception) {
            FastagOcrResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "ML Kit Text Recognition failed"
            )
        }
    }

    /**
     * Backward-compatible parse of raw OCR text lines returning candidate tags or ranges.
     */
    fun parseOcrText(rawText: String): List<ExtractedTagCandidate> {
        return parseOcrDetailed(rawText).candidates
    }

    /**
     * Comprehensive analysis of text detected by ML Kit from FASTag documents.
     * Extracts tabular candidates, standalone serials, barcode numbers, vehicle registration,
     * NPCI vehicle classes, issuer banks, and subagent tags.
     */
    fun parseOcrDetailed(rawText: String, textBlocksCount: Int = 0, linesCount: Int = 0): FastagOcrResult {
        val candidates = mutableListOf<ExtractedTagCandidate>()
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val effectiveLinesCount = if (linesCount > 0) linesCount else lines.size

        // Detect Issuer Bank
        val detectedBank = extractIssuerBank(rawText)

        // Detect Vehicle Registration Number (VRN)
        val detectedVrn = extractVehicleNumber(rawText)

        // Detect Tag Class
        val detectedClass = extractClassFromText(rawText)

        // Detect Subagent / Executive ID & Name
        val detectedSubagentId = extractSubagentId(rawText)
        val detectedSubagentName = extractName(rawText)

        // Detect Date
        val detectedDate = extractDate(rawText)

        // Detect All Serials in document
        val allSerials = extractAllSerials(rawText)

        // Detect Ranges in document
        val detectedRanges = extractAllRanges(rawText)

        // Ignore known header rows when evaluating table/manifest rows
        val contentLines = lines.filterNot { line ->
            val lower = line.lowercase()
            (lower.contains("subagent") || lower.contains("sub-agent") || lower.contains("agent")) &&
                    (lower.contains("serial") || lower.contains("class") || lower.contains("name"))
        }

        // 1. Try row-by-row candidate parsing (e.g. manifests, tables, delimited slips)
        for (line in contentLines) {
            val candidate = parseSingleLine(line)
            if (candidate != null) {
                candidates.add(candidate)
            }
        }

        // 2. If row-by-row parsing didn't find candidates, but ranges were detected:
        if (candidates.isEmpty() && detectedRanges.isNotEmpty()) {
            for (range in detectedRanges) {
                val candidate = ExtractedTagCandidate(
                    subagentId = detectedSubagentId ?: "SA-1001",
                    subagentName = detectedSubagentName ?: "Field Executive",
                    tagClass = detectedClass,
                    isRange = true,
                    fromSerial = range.first,
                    toSerial = range.second
                )
                candidate.updateCalculatedQuantity()
                candidates.add(candidate)
            }
        }

        // 3. If still empty, but single/multiple serial numbers or barcode values were found:
        if (candidates.isEmpty() && allSerials.isNotEmpty()) {
            // For a single tag sticker photo, add the first detected serial
            val primary = allSerials.first()
            val candidate = ExtractedTagCandidate(
                subagentId = detectedSubagentId ?: "SA-1001",
                subagentName = detectedSubagentName ?: "Partner Agent",
                tagClass = detectedClass,
                isRange = false,
                serialNumber = primary
            )
            candidate.updateCalculatedQuantity()
            candidates.add(candidate)
        }

        val primarySerial = candidates.firstOrNull()?.let {
            if (it.isRange) it.fromSerial else it.serialNumber
        } ?: allSerials.firstOrNull()

        return FastagOcrResult(
            rawText = rawText,
            candidates = candidates,
            detectedSerials = allSerials,
            primarySerial = primarySerial,
            detectedClass = detectedClass,
            detectedVehicleNumber = detectedVrn,
            detectedBank = detectedBank,
            detectedSubagentId = detectedSubagentId,
            detectedSubagentName = detectedSubagentName,
            detectedDate = detectedDate,
            detectedRanges = detectedRanges,
            textBlocksCount = textBlocksCount,
            linesCount = effectiveLinesCount,
            isSuccess = candidates.isNotEmpty() || allSerials.isNotEmpty() || rawText.isNotBlank()
        )
    }

    private fun parseSingleLine(line: String): ExtractedTagCandidate? {
        // Delimiter split: pipe, tab, or comma
        val tokens = if (line.contains("|")) {
            line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
        } else if (line.contains("\t")) {
            line.split("\t").map { it.trim() }.filter { it.isNotEmpty() }
        } else if (line.contains(",")) {
            line.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            // Space delimited
            line.split(Regex("\\s{2,}")).map { it.trim() }.filter { it.isNotEmpty() }
        }

        // Pattern 1: 5 tokens Range format: "SA001 | Kumar | Class 12 | 100001 | 100050"
        if (tokens.size >= 5) {
            val subId = tokens[0]
            val subName = tokens[1]
            val tagClass = cleanClass(tokens[2])
            val from = cleanSerial(tokens[3])
            val to = cleanSerial(tokens[4])

            val candidate = ExtractedTagCandidate(
                subagentId = subId,
                subagentName = subName,
                tagClass = tagClass,
                isRange = true,
                fromSerial = from,
                toSerial = to
            )
            candidate.updateCalculatedQuantity()
            return candidate
        }

        // Pattern 2: 4 columns: could be single serial or range with arrow/hyphen in 4th col
        if (tokens.size == 4) {
            val subId = tokens[0]
            val subName = tokens[1]
            val tagClass = cleanClass(tokens[2])
            val serialPart = tokens[3]

            val rangeMatch = Regex("([A-Za-z0-9_-]+)\\s*(?:->|to|–|-)\\s*([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE).find(serialPart)
            if (rangeMatch != null) {
                val candidate = ExtractedTagCandidate(
                    subagentId = subId,
                    subagentName = subName,
                    tagClass = tagClass,
                    isRange = true,
                    fromSerial = cleanSerial(rangeMatch.groupValues[1]),
                    toSerial = cleanSerial(rangeMatch.groupValues[2])
                )
                candidate.updateCalculatedQuantity()
                return candidate
            } else {
                val candidate = ExtractedTagCandidate(
                    subagentId = subId,
                    subagentName = subName,
                    tagClass = tagClass,
                    isRange = false,
                    serialNumber = cleanSerial(serialPart)
                )
                candidate.updateCalculatedQuantity()
                return candidate
            }
        }

        // Pattern 3: Unstructured line with range regex search
        val rangeRegex = Regex("([A-Za-z0-9_-]{4,})\\s*(?:->|to|–|-)\\s*([A-Za-z0-9_-]{4,})", RegexOption.IGNORE_CASE)
        val rangeMatch = rangeRegex.find(line)
        if (rangeMatch != null) {
            val from = cleanSerial(rangeMatch.groupValues[1])
            val to = cleanSerial(rangeMatch.groupValues[2])
            val remaining = line.replace(rangeMatch.value, "").trim()
            val detectedClass = extractClassFromText(remaining)
            val subId = extractSubagentId(remaining) ?: "SA-1001"
            val subName = extractName(remaining) ?: "Executive Partner"

            val candidate = ExtractedTagCandidate(
                subagentId = subId,
                subagentName = subName,
                tagClass = detectedClass,
                isRange = true,
                fromSerial = from,
                toSerial = to
            )
            candidate.updateCalculatedQuantity()
            return candidate
        }

        // Check if there is an individual serial number or barcode format
        val serialRegex = Regex("\\b([A-Z]{2,}\\d{4,}|\\d{6,})\\b")
        val serialMatch = serialRegex.find(line)
        if (serialMatch != null && !isLikelyDateOrPhone(serialMatch.value, line)) {
            val serial = cleanSerial(serialMatch.value)
            val remaining = line.replace(serialMatch.value, "").trim()
            val detectedClass = extractClassFromText(remaining)
            val subId = extractSubagentId(remaining) ?: "SA-1001"
            val subName = extractName(remaining) ?: "Partner Agent"

            val candidate = ExtractedTagCandidate(
                subagentId = subId,
                subagentName = subName,
                tagClass = detectedClass,
                isRange = false,
                serialNumber = serial
            )
            candidate.updateCalculatedQuantity()
            return candidate
        }

        return null
    }

    private fun cleanClass(raw: String): String {
        val trimmed = raw.trim()
        val lower = trimmed.lowercase()
        return when {
            lower.contains("class 12") || lower.contains("vc12") || lower.contains("vc 12") -> "Class 12"
            lower.contains("class 7") || lower.contains("vc7") || lower.contains("vc 7") -> "Class 7"
            lower.contains("class 4") || lower.contains("vc4") || lower.contains("vc 4") -> "Class 4"
            lower.contains("class 5") || lower.contains("vc5") || lower.contains("vc 5") -> "Class 5"
            lower.contains("class 16") || lower.contains("vc16") || lower.contains("vc 16") -> "Class 16"
            lower.contains("commercial") -> "Commercial"
            else -> if (trimmed.startsWith("Class", ignoreCase = true)) trimmed else "Class $trimmed"
        }
    }

    private fun cleanSerial(raw: String): String {
        return raw.trim().replace(Regex("[^A-Za-z0-9_-]"), "")
    }

    fun extractClassFromText(text: String): String {
        val lower = text.lowercase()
        return when {
            Regex("\\b(vc\\s*4|class\\s*4|class\\s*04|car|jeep|van)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "Class 4"
            Regex("\\b(vc\\s*5|class\\s*5|lcv|mini\\s*bus)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "Class 5"
            Regex("\\b(vc\\s*7|class\\s*7|bus\\s*2\\s*axle|truck\\s*2\\s*axle)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "Class 7"
            Regex("\\b(vc\\s*12|class\\s*12|3\\s*axle|4\\s*axle|heavy)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "Class 12"
            Regex("\\b(vc\\s*16|class\\s*16|multi\\s*axle|oversized|osv)\\b", RegexOption.IGNORE_CASE).containsMatchIn(text) -> "Class 16"
            lower.contains("commercial") -> "Commercial"
            else -> {
                val match = Regex("Class\\s*\\d+", RegexOption.IGNORE_CASE).find(text)
                match?.value?.replace(Regex("\\s+"), " ")?.capitalizeWords() ?: "Class 4"
            }
        }
    }

    fun extractSubagentId(text: String): String? {
        val match = Regex("\\b(EXEC-?\\d+|SA-?\\d+)\\b", RegexOption.IGNORE_CASE).find(text)
        return match?.value?.uppercase()
    }

    fun extractName(text: String): String? {
        // Look for "Name: Kumar" or "Agent: Rajesh"
        val namedMatch = Regex("(?:Name|Agent|Executive|Subagent)\\s*[:#-]?\\s*([A-Za-z]{3,}(?:\\s+[A-Za-z]{3,})?)", RegexOption.IGNORE_CASE).find(text)
        if (namedMatch != null) {
            val n = namedMatch.groupValues[1].trim()
            if (!n.equals("Agent", true) && !n.equals("Executive", true)) {
                return n
            }
        }
        val words = text.split(Regex("[\\s|,-]+")).filter { it.length > 2 && it.all { ch -> ch.isLetter() } }
        val filteredWords = words.filterNot {
            val l = it.lowercase()
            l in setOf("fastag", "npci", "netc", "class", "serial", "bank", "tag", "india", "toll", "manifest", "courier", "date", "invoice")
        }
        return if (filteredWords.isNotEmpty()) filteredWords.take(2).joinToString(" ") else null
    }

    /**
     * Extracts all valid FASTag serials or barcode identifiers from OCR text.
     * Handles NPCI 16-digit/24-digit barcodes, NETC codes, prefixed serials, and numeric IDs.
     */
    fun extractAllSerials(text: String): List<String> {
        val serials = linkedSetOf<String>()

        // 1. FASTag standard 16-24 digit numeric EPC or barcode
        val npciRegex = Regex("\\b([0-9]{12,24})\\b")
        npciRegex.findAll(text).forEach { m ->
            val v = m.value
            if (!isLikelyDateOrPhone(v, text)) {
                serials.add(v)
            }
        }

        // 2. Barcode with hyphen or prefix: e.g. 607417-001-123456
        val hyphenatedRegex = Regex("\\b([0-9]{6}-[0-9]{3}-[0-9]{6,10})\\b")
        hyphenatedRegex.findAll(text).forEach { m ->
            serials.add(m.value)
        }

        // 3. NETC or FT prefixed tags: e.g. NETC607417001001, FT100234, TAG99210
        val prefixedRegex = Regex("\\b((?:NETC|FT|TAG|NPCI)[A-Z0-9]{4,20})\\b", RegexOption.IGNORE_CASE)
        prefixedRegex.findAll(text).forEach { m ->
            serials.add(m.value.uppercase())
        }

        // 4. Sequential 6-digit numeric IDs (common in manifests and batch allocations)
        val shortSeqRegex = Regex("\\b([1-9][0-9]{5,7})\\b")
        shortSeqRegex.findAll(text).forEach { m ->
            val v = m.value
            if (!isLikelyDateOrPhone(v, text)) {
                serials.add(v)
            }
        }

        return serials.toList()
    }

    /**
     * Extracts ranges defined with arrows, hyphens, or 'to' keywords.
     */
    fun extractAllRanges(text: String): List<Pair<String, String>> {
        val ranges = mutableListOf<Pair<String, String>>()
        val regex1 = Regex("([A-Za-z0-9_-]{4,})\\s*(?:->|–|to|-)\\s*([A-Za-z0-9_-]{4,})", RegexOption.IGNORE_CASE)
        regex1.findAll(text).forEach { m ->
            val from = cleanSerial(m.groupValues[1])
            val to = cleanSerial(m.groupValues[2])
            // Check they aren't vehicle numbers or dates
            if (from.length >= 4 && to.length >= 4 && !from.contains("/") && !to.contains("/")) {
                ranges.add(from to to)
            }
        }

        val regex2 = Regex("From\\s*[:#]?\\s*([A-Za-z0-9_-]{4,})\\s*To\\s*[:#]?\\s*([A-Za-z0-9_-]{4,})", RegexOption.IGNORE_CASE)
        regex2.findAll(text).forEach { m ->
            val from = cleanSerial(m.groupValues[1])
            val to = cleanSerial(m.groupValues[2])
            ranges.add(from to to)
        }

        return ranges.distinct()
    }

    /**
     * Extracts Indian vehicle registration numbers (VRN)
     * Patterns like DL 01 AB 1234, MH12CD5678, KA 05 MN 9021, HR-26-DQ-5555
     */
    fun extractVehicleNumber(text: String): String? {
        val vrnRegex = Regex("\\b([A-Z]{2}[ -]?[0-9]{1,2}[ -]?[A-Z]{1,3}[ -]?[0-9]{4})\\b", RegexOption.IGNORE_CASE)
        val match = vrnRegex.find(text)
        return match?.value?.uppercase()?.replace(Regex("\\s+"), " ")
    }

    /**
     * Extracts Issuer Bank Name
     */
    fun extractIssuerBank(text: String): String? {
        val banks = listOf(
            "ICICI Bank" to listOf("icici"),
            "State Bank of India (SBI)" to listOf("sbi", "state bank of india"),
            "Paytm Payments Bank" to listOf("paytm"),
            "HDFC Bank" to listOf("hdfc"),
            "Axis Bank" to listOf("axis"),
            "IDFC FIRST Bank" to listOf("idfc", "idfc first"),
            "Kotak Mahindra Bank" to listOf("kotak"),
            "Airtel Payments Bank" to listOf("airtel"),
            "Bank of Baroda" to listOf("baroda", "bob"),
            "IndusInd Bank" to listOf("indusind"),
            "Federal Bank" to listOf("federal bank")
        )

        val lower = text.lowercase()
        for ((bankName, keywords) in banks) {
            if (keywords.any { lower.contains(it) }) {
                return bankName
            }
        }
        return if (lower.contains("netc") || lower.contains("npci")) "NPCI NETC FASTag" else null
    }

    fun extractDate(text: String): String? {
        val dateRegex = Regex("\\b([0-3]?[0-9][/-][0-1]?[0-9][/-]20[2-3][0-9])\\b")
        return dateRegex.find(text)?.value
    }

    private fun isLikelyDateOrPhone(v: String, fullText: String): Boolean {
        if (v.length == 10 && (v.startsWith("9") || v.startsWith("8") || v.startsWith("7") || v.startsWith("6"))) {
            val idx = fullText.indexOf(v)
            if (idx > 0) {
                val prefix = fullText.substring((idx - 8).coerceAtLeast(0), idx).lowercase()
                if (prefix.contains("ph") || prefix.contains("tel") || prefix.contains("mob") || prefix.contains("+91")) {
                    return true
                }
            }
        }
        if (v.length == 6 && v.startsWith("1100") || v.startsWith("4000") || v.startsWith("5600")) {
            val idx = fullText.indexOf(v)
            if (idx > 0) {
                val prefix = fullText.substring((idx - 8).coerceAtLeast(0), idx).lowercase()
                if (prefix.contains("pin") || prefix.contains("zip")) {
                    return true
                }
            }
        }
        return false
    }

    private fun String.capitalizeWords(): String {
        return split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
