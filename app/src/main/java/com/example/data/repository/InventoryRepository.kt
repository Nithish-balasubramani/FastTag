package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.AppDatabase
import com.example.data.model.ClassCountRaw
import com.example.data.model.ClassStockSummary
import com.example.data.model.ExecutiveClassCountRaw
import com.example.data.model.ExecutiveEntity
import com.example.data.model.ExecutiveStockSummary
import com.example.data.model.InventoryLocation
import com.example.data.model.InventoryTagEntity
import com.example.data.model.MovementWithProof
import com.example.data.model.OverallStockMetrics
import com.example.data.model.ProofDocumentEntity
import com.example.data.model.ProofType
import com.example.data.model.StockMovementLedgerEntity
import com.example.ocr.ExtractedTagCandidate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class TagItemPlan(
    val serial: String,
    val tagClass: String,
    val subagentId: String,
    val subagentName: String
)

data class TransactionValidationResult(
    val isValid: Boolean,
    val totalCount: Int,
    val classBreakdown: Map<String, Int>,
    val tags: List<TagItemPlan>,
    val errorMessage: String? = null
)

data class TransactionExecutionResult(
    val success: Boolean,
    val message: String,
    val affectedCount: Int = 0,
    val proofId: Long = 0L
)

class InventoryRepository(private val db: AppDatabase) {

    private val executiveDao = db.executiveDao()
    private val tagDao = db.inventoryTagDao()
    private val proofDao = db.proofDocumentDao()
    private val ledgerDao = db.stockMovementLedgerDao()

    // Reactive streams
    val allExecutives: Flow<List<ExecutiveEntity>> = executiveDao.getAllExecutives()
    val activeExecutives: Flow<List<ExecutiveEntity>> = executiveDao.getActiveExecutives()
    val allTags: Flow<List<InventoryTagEntity>> = tagDao.getAllTags()
    val allProofs: Flow<List<ProofDocumentEntity>> = proofDao.getAllProofs()
    val recentMovements: Flow<List<MovementWithProof>> = ledgerDao.getRecentMovements(50)

    // Stock metrics dynamically calculated from tag records
    val overallMetrics: Flow<OverallStockMetrics> = combine(
        tagDao.countByLocation(InventoryLocation.CENTRAL.name),
        tagDao.countByLocation(InventoryLocation.MASTER.name),
        tagDao.countByLocation(InventoryLocation.EXECUTIVE.name),
        tagDao.countByLocation(InventoryLocation.ASSIGNED.name),
        tagDao.countTotal()
    ) { central, master, executive, assigned, total ->
        OverallStockMetrics(
            centralStock = central,
            masterStock = master,
            executiveStock = executive,
            assignedStock = assigned,
            totalActive = total
        )
    }

    // Dynamic class-wise summary discovered from DB
    val classStockSummaries: Flow<List<ClassStockSummary>> = tagDao.getClassCounts().map { rawList ->
        val classes = rawList.map { it.tag_class }.distinct().sorted()
        classes.map { cls ->
            val central = rawList.find { it.tag_class == cls && it.current_location == InventoryLocation.CENTRAL.name }?.count ?: 0
            val master = rawList.find { it.tag_class == cls && it.current_location == InventoryLocation.MASTER.name }?.count ?: 0
            val exec = rawList.find { it.tag_class == cls && it.current_location == InventoryLocation.EXECUTIVE.name }?.count ?: 0
            val assigned = rawList.find { it.tag_class == cls && it.current_location == InventoryLocation.ASSIGNED.name }?.count ?: 0
            val total = central + master + exec + assigned
            ClassStockSummary(
                tagClass = cls,
                centralCount = central,
                masterCount = master,
                executiveCount = exec,
                assignedCount = assigned,
                totalCount = total
            )
        }
    }

    // Executive stock summaries
    val executiveStockSummaries: Flow<List<ExecutiveStockSummary>> = allExecutives.combine(allTags) { execs, tags ->
        execs.map { exec ->
            val count = tags.count { it.currentExecutiveId == exec.executiveId && it.currentLocation == InventoryLocation.EXECUTIVE.name }
            ExecutiveStockSummary(
                executiveId = exec.executiveId,
                executiveName = exec.executiveName,
                region = exec.region,
                active = exec.active,
                currentStock = count
            )
        }
    }

    fun getExecutive(execId: String): Flow<ExecutiveEntity?> = executiveDao.getExecutiveByExecId(execId)
    fun getExecutiveClassCounts(execId: String): Flow<List<ExecutiveClassCountRaw>> = tagDao.getExecutiveClassCounts(execId)
    fun getExecutiveTags(execId: String): Flow<List<InventoryTagEntity>> = tagDao.getTagsByExecutive(execId)
    fun getExecutiveMovements(execId: String): Flow<List<MovementWithProof>> = ledgerDao.getMovementsForExecutive(execId)

    fun getTagBySerial(serial: String): Flow<InventoryTagEntity?> = tagDao.getTagBySerial(serial)
    fun getTagMovements(serial: String): Flow<List<MovementWithProof>> = ledgerDao.getMovementsForTag(serial)

    fun searchTags(query: String, location: String, tagClass: String, execId: String): Flow<List<InventoryTagEntity>> {
        return tagDao.searchTags(query.trim(), location, tagClass, execId)
    }

    fun getDiscoveredClasses(): Flow<List<String>> = tagDao.getAllDiscoveredClasses()

    suspend fun addExecutive(id: String, name: String, region: String): Result<Long> {
        val existing = executiveDao.getExecutiveByExecIdSync(id.trim().uppercase())
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Executive ID '${id.trim().uppercase()}' already exists!"))
        }
        val entity = ExecutiveEntity(
            executiveId = id.trim().uppercase(),
            executiveName = name.trim(),
            region = region.trim(),
            active = true
        )
        return Result.success(executiveDao.insertExecutive(entity))
    }

    suspend fun updateExecutive(entity: ExecutiveEntity): Int {
        return executiveDao.updateExecutive(entity.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateExecutiveDetails(execId: String, name: String, region: String, active: Boolean): Result<Unit> = runCatching {
        val updated = executiveDao.updateExecutiveDetails(
            execId = execId.trim().uppercase(),
            name = name.trim(),
            region = region.trim(),
            active = active
        )
        if (updated <= 0) {
            throw IllegalArgumentException("Executive '$execId' not found.")
        }
    }

    suspend fun toggleExecutiveActive(execId: String, active: Boolean): Int {
        return executiveDao.setExecutiveActive(execId, active)
    }

    suspend fun clearAllInventoryData(): Result<Unit> = runCatching {
        db.withTransaction {
            tagDao.deleteAllTags()
            ledgerDao.deleteAllMovements()
            proofDao.deleteAllProofs()
        }
    }

    suspend fun submitUserTagImage(
        filename: String,
        storageKey: String,
        subagentId: String,
        subagentName: String,
        tagSerial: String,
        tagClass: String,
        notes: String
    ): Result<Long> = runCatching {
        val now = System.currentTimeMillis()
        val desc = if (notes.isNotBlank()) " | $notes" else ""
        val proof = ProofDocumentEntity(
            originalFilename = filename,
            storageObjectKey = storageKey,
            proofType = "USER_TAG_UPLOAD",
            uploadedAt = now,
            uploadedBy = "$subagentName ($subagentId)$desc"
        )
        val proofId = proofDao.insertProof(proof)

        // If tagSerial is provided and not blank, also record an audit ledger entry or link
        if (tagSerial.isNotBlank()) {
            val cleanSerial = tagSerial.trim().uppercase()
            val existing = tagDao.getTagBySerialSync(cleanSerial)
            if (existing != null) {
                ledgerDao.insertMovement(
                    StockMovementLedgerEntity(
                        tagId = existing.id,
                        movementType = "USER_IMAGE_UPLOAD",
                        previousLocation = existing.currentLocation,
                        newLocation = existing.currentLocation,
                        executiveId = subagentId,
                        subagentId = subagentId,
                        subagentName = subagentName,
                        tagClass = existing.tagClass,
                        tagSerialNumber = cleanSerial,
                        proofDocumentId = proofId,
                        createdAt = now,
                        createdBy = subagentName
                    )
                )
            }
        }
        proofId
    }

    /**
     * Pre-validate candidate tags and serial ranges before showing confirmation.
     */
    suspend fun validateCandidates(
        proofType: ProofType,
        candidates: List<ExtractedTagCandidate>,
        selectedExecutiveId: String?,
        selectedLocationForExisting: InventoryLocation?
    ): TransactionValidationResult {
        if (candidates.isEmpty()) {
            return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "No tags detected or provided for processing")
        }

        // Expand all ranges
        val expandedTags = mutableListOf<TagItemPlan>()
        for (cand in candidates) {
            if (!cand.isSelected) continue

            if (cand.subagentId.isBlank() || cand.subagentName.isBlank()) {
                return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "Subagent ID and Subagent Name are required for all rows")
            }
            if (cand.tagClass.isBlank()) {
                return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "Tag Class is required for all rows")
            }

            if (!cand.isRange) {
                if (cand.serialNumber.isBlank()) {
                    return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "Serial number cannot be blank")
                }
                expandedTags.add(
                    TagItemPlan(
                        serial = cand.serialNumber.trim(),
                        tagClass = cand.tagClass.trim(),
                        subagentId = cand.subagentId.trim(),
                        subagentName = cand.subagentName.trim()
                    )
                )
            } else {
                val expansion = SerialRangeHelper.expandRange(cand.fromSerial, cand.toSerial)
                if (!expansion.isValid) {
                    return TransactionValidationResult(false, 0, emptyMap(), emptyList(), expansion.errorMessage ?: "Invalid serial range")
                }
                for (s in expansion.serials) {
                    expandedTags.add(
                        TagItemPlan(
                            serial = s,
                            tagClass = cand.tagClass.trim(),
                            subagentId = cand.subagentId.trim(),
                            subagentName = cand.subagentName.trim()
                        )
                    )
                }
            }
        }

        if (expandedTags.isEmpty()) {
            return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "No active tags selected for processing")
        }

        // Check internal duplicates inside batch
        val seenSerials = HashSet<String>()
        for (item in expandedTags) {
            if (!seenSerials.add(item.serial)) {
                return TransactionValidationResult(
                    false, expandedTags.size, emptyMap(), emptyList(),
                    "DUPLICATE FOUND IN BATCH: Tag '${item.serial}' appears multiple times in this document/range."
                )
            }
        }

        // Check database constraints based on operation
        val serialsList = expandedTags.map { it.serial }
        val existingTags = tagDao.getTagsBySerialsSync(serialsList)
        val existingMap = existingTags.associateBy { it.tagSerialNumber }

        when (proofType) {
            ProofType.EXISTING_STOCK -> {
                if (selectedLocationForExisting == null) {
                    return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "Initial location (MASTER, EXECUTIVE, ASSIGNED) must be selected")
                }
                if (selectedLocationForExisting == InventoryLocation.EXECUTIVE) {
                    if (selectedExecutiveId.isNullOrBlank()) {
                        return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "An Executive must be selected when importing existing stock into EXECUTIVE location")
                    }
                    val exec = executiveDao.getExecutiveByExecIdSync(selectedExecutiveId)
                    if (exec == null || !exec.active) {
                        return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "Selected Executive '$selectedExecutiveId' is invalid or inactive")
                    }
                }
                // Check if any serial already exists in database
                for (item in expandedTags) {
                    if (existingMap.containsKey(item.serial)) {
                        return TransactionValidationResult(
                            false, expandedTags.size, emptyMap(), emptyList(),
                            "DUPLICATE FOUND: Tag '${item.serial}' already exists in database!"
                        )
                    }
                }
            }

            ProofType.CENTRAL_RECEIVED -> {
                // Central received: CENTRAL -> MASTER
                // Check if tags exist and are already in MASTER or further down
                for (item in expandedTags) {
                    val existing = existingMap[item.serial]
                    if (existing != null && existing.currentLocation != InventoryLocation.CENTRAL.name) {
                        return TransactionValidationResult(
                            false, expandedTags.size, emptyMap(), emptyList(),
                            "DUPLICATE FOUND: Tag '${item.serial}' is already in ${existing.currentLocation}! Cannot receive into MASTER."
                        )
                    }
                }
            }

            ProofType.COURIER_TO_EXECUTIVE -> {
                if (selectedExecutiveId.isNullOrBlank()) {
                    return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "Please select an active Executive for Courier dispatch")
                }
                val exec = executiveDao.getExecutiveByExecIdSync(selectedExecutiveId)
                if (exec == null || !exec.active) {
                    return TransactionValidationResult(false, 0, emptyMap(), emptyList(), "Selected Executive '$selectedExecutiveId' is inactive or not found")
                }

                // EVERY tag MUST be currently in MASTER
                for (item in expandedTags) {
                    val existing = existingMap[item.serial]
                    if (existing == null) {
                        return TransactionValidationResult(
                            false, expandedTags.size, emptyMap(), emptyList(),
                            "VALIDATION FAILED: Tag '${item.serial}' not found in inventory! All tags must be in MASTER."
                        )
                    }
                    if (existing.currentLocation != InventoryLocation.MASTER.name) {
                        return TransactionValidationResult(
                            false, expandedTags.size, emptyMap(), emptyList(),
                            "VALIDATION FAILED: Tag '${item.serial}' is currently in '${existing.currentLocation}', NOT in MASTER. Entire transaction rejected."
                        )
                    }
                }
            }

            ProofType.TAG_ASSIGNED -> {
                // For every tag: Current location MUST be EXECUTIVE
                for (item in expandedTags) {
                    val existing = existingMap[item.serial]
                    if (existing == null) {
                        return TransactionValidationResult(
                            false, expandedTags.size, emptyMap(), emptyList(),
                            "VALIDATION FAILED: Tag '${item.serial}' not found in inventory!"
                        )
                    }
                    if (existing.currentLocation != InventoryLocation.EXECUTIVE.name) {
                        return TransactionValidationResult(
                            false, expandedTags.size, emptyMap(), emptyList(),
                            "VALIDATION FAILED: Tag '${item.serial}' is in '${existing.currentLocation}', NOT in EXECUTIVE. Entire transaction rejected."
                        )
                    }
                }
            }
        }

        // Class breakdown
        val breakdown = expandedTags.groupingBy { it.tagClass }.eachCount()
        return TransactionValidationResult(
            isValid = true,
            totalCount = expandedTags.size,
            classBreakdown = breakdown,
            tags = expandedTags
        )
    }

    /**
     * Executes the atomic Room database transaction after admin confirmation.
     */
    suspend fun executeTransaction(
        proofType: ProofType,
        validationPlan: TransactionValidationResult,
        selectedExecutiveId: String?,
        selectedLocationForExisting: InventoryLocation?,
        proofFilename: String,
        proofStorageKey: String,
        fileSize: Long
    ): TransactionExecutionResult {
        if (!validationPlan.isValid || validationPlan.tags.isEmpty()) {
            return TransactionExecutionResult(false, validationPlan.errorMessage ?: "Invalid transaction plan")
        }

        return try {
            db.withTransaction {
                // Re-verify under transaction lock
                val serials = validationPlan.tags.map { it.serial }
                val existingList = tagDao.getTagsBySerialsSync(serials)
                val existingMap = existingList.associateBy { it.tagSerialNumber }

                // 1. Create Proof Document
                val proofEntity = ProofDocumentEntity(
                    proofType = proofType.name,
                    storageProvider = "LOCAL_SECURE_STORAGE",
                    storageObjectKey = proofStorageKey,
                    originalFilename = proofFilename,
                    mimeType = "image/jpeg",
                    fileSize = fileSize,
                    uploadedAt = System.currentTimeMillis(),
                    uploadedBy = "ADMIN",
                    createdAt = System.currentTimeMillis()
                )
                val proofId = proofDao.insertProof(proofEntity)

                val tagsToInsert = mutableListOf<InventoryTagEntity>()
                val tagsToUpdate = mutableListOf<InventoryTagEntity>()
                val ledgerEntries = mutableListOf<StockMovementLedgerEntity>()
                val now = System.currentTimeMillis()

                when (proofType) {
                    ProofType.EXISTING_STOCK -> {
                        val targetLoc = selectedLocationForExisting ?: InventoryLocation.MASTER
                        val execId = if (targetLoc == InventoryLocation.EXECUTIVE) selectedExecutiveId else null

                        for (item in validationPlan.tags) {
                            if (existingMap.containsKey(item.serial)) {
                                throw IllegalStateException("Tag '${item.serial}' already exists in database during commit!")
                            }
                            val newTag = InventoryTagEntity(
                                tagSerialNumber = item.serial,
                                tagClass = item.tagClass,
                                subagentId = item.subagentId,
                                subagentName = item.subagentName,
                                currentLocation = targetLoc.name,
                                currentExecutiveId = execId,
                                status = if (targetLoc == InventoryLocation.ASSIGNED) "ASSIGNED" else "ACTIVE",
                                createdAt = now,
                                updatedAt = now
                            )
                            tagsToInsert.add(newTag)
                        }

                        val insertedIds = tagDao.insertTags(tagsToInsert)
                        for (i in validationPlan.tags.indices) {
                            val tagId = insertedIds.getOrElse(i) { 0L }
                            val item = validationPlan.tags[i]
                            ledgerEntries.add(
                                StockMovementLedgerEntity(
                                    tagId = tagId,
                                    movementType = "EXISTING_STOCK",
                                    previousLocation = null,
                                    newLocation = targetLoc.name,
                                    executiveId = execId,
                                    subagentId = item.subagentId,
                                    subagentName = item.subagentName,
                                    tagClass = item.tagClass,
                                    tagSerialNumber = item.serial,
                                    proofDocumentId = proofId,
                                    createdAt = now,
                                    createdBy = "ADMIN"
                                )
                            )
                        }
                    }

                    ProofType.CENTRAL_RECEIVED -> {
                        for (item in validationPlan.tags) {
                            val existing = existingMap[item.serial]
                            if (existing == null) {
                                val newTag = InventoryTagEntity(
                                    tagSerialNumber = item.serial,
                                    tagClass = item.tagClass,
                                    subagentId = item.subagentId,
                                    subagentName = item.subagentName,
                                    currentLocation = InventoryLocation.MASTER.name,
                                    currentExecutiveId = null,
                                    status = "ACTIVE",
                                    createdAt = now,
                                    updatedAt = now
                                )
                                tagsToInsert.add(newTag)
                            } else {
                                if (existing.currentLocation != InventoryLocation.CENTRAL.name) {
                                    throw IllegalStateException("Tag '${item.serial}' is not in CENTRAL!")
                                }
                                val updated = existing.copy(
                                    currentLocation = InventoryLocation.MASTER.name,
                                    currentExecutiveId = null,
                                    updatedAt = now
                                )
                                tagsToUpdate.add(updated)
                            }
                        }

                        val insertedIds = if (tagsToInsert.isNotEmpty()) tagDao.insertTags(tagsToInsert) else emptyList()
                        if (tagsToUpdate.isNotEmpty()) tagDao.updateTags(tagsToUpdate)

                        var insertIdx = 0
                        for (item in validationPlan.tags) {
                            val existing = existingMap[item.serial]
                            val tagId = existing?.id ?: insertedIds.getOrElse(insertIdx++) { 0L }
                            ledgerEntries.add(
                                StockMovementLedgerEntity(
                                    tagId = tagId,
                                    movementType = "CENTRAL_RECEIVED",
                                    previousLocation = InventoryLocation.CENTRAL.name,
                                    newLocation = InventoryLocation.MASTER.name,
                                    executiveId = null,
                                    subagentId = item.subagentId,
                                    subagentName = item.subagentName,
                                    tagClass = item.tagClass,
                                    tagSerialNumber = item.serial,
                                    proofDocumentId = proofId,
                                    createdAt = now,
                                    createdBy = "ADMIN"
                                )
                            )
                        }
                    }

                    ProofType.COURIER_TO_EXECUTIVE -> {
                        for (item in validationPlan.tags) {
                            val existing = existingMap[item.serial]
                                ?: throw IllegalStateException("Tag '${item.serial}' not found during commit!")

                            if (existing.currentLocation != InventoryLocation.MASTER.name) {
                                throw IllegalStateException("Tag '${item.serial}' is no longer in MASTER!")
                            }

                            val updated = existing.copy(
                                currentLocation = InventoryLocation.EXECUTIVE.name,
                                currentExecutiveId = selectedExecutiveId,
                                subagentId = item.subagentId,
                                subagentName = item.subagentName,
                                updatedAt = now
                            )
                            tagsToUpdate.add(updated)

                            ledgerEntries.add(
                                StockMovementLedgerEntity(
                                    tagId = existing.id,
                                    movementType = "COURIER_TO_EXECUTIVE",
                                    previousLocation = InventoryLocation.MASTER.name,
                                    newLocation = InventoryLocation.EXECUTIVE.name,
                                    executiveId = selectedExecutiveId,
                                    subagentId = item.subagentId,
                                    subagentName = item.subagentName,
                                    tagClass = existing.tagClass,
                                    tagSerialNumber = existing.tagSerialNumber,
                                    proofDocumentId = proofId,
                                    createdAt = now,
                                    createdBy = "ADMIN"
                                )
                            )
                        }
                        tagDao.updateTags(tagsToUpdate)
                    }

                    ProofType.TAG_ASSIGNED -> {
                        for (item in validationPlan.tags) {
                            val existing = existingMap[item.serial]
                                ?: throw IllegalStateException("Tag '${item.serial}' not found during commit!")

                            if (existing.currentLocation != InventoryLocation.EXECUTIVE.name) {
                                throw IllegalStateException("Tag '${item.serial}' is no longer with an Executive!")
                            }

                            val holdingExecId = existing.currentExecutiveId

                            val updated = existing.copy(
                                currentLocation = InventoryLocation.ASSIGNED.name,
                                status = "ASSIGNED",
                                updatedAt = now
                            )
                            tagsToUpdate.add(updated)

                            ledgerEntries.add(
                                StockMovementLedgerEntity(
                                    tagId = existing.id,
                                    movementType = "TAG_ASSIGNED",
                                    previousLocation = InventoryLocation.EXECUTIVE.name,
                                    newLocation = InventoryLocation.ASSIGNED.name,
                                    executiveId = holdingExecId,
                                    subagentId = existing.subagentId,
                                    subagentName = existing.subagentName,
                                    tagClass = existing.tagClass,
                                    tagSerialNumber = existing.tagSerialNumber,
                                    proofDocumentId = proofId,
                                    createdAt = now,
                                    createdBy = "ADMIN"
                                )
                            )
                        }
                        tagDao.updateTags(tagsToUpdate)
                    }
                }

                ledgerDao.insertMovements(ledgerEntries)

                TransactionExecutionResult(
                    success = true,
                    message = "Successfully processed ${validationPlan.totalCount} tags for ${proofType.displayName}.",
                    affectedCount = validationPlan.totalCount,
                    proofId = proofId
                )
            }
        } catch (e: Exception) {
            TransactionExecutionResult(
                success = false,
                message = "Transaction aborted: ${e.message ?: "Database rollback executed."}"
            )
        }
    }
}
