package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.ClassStockSummary
import com.example.data.model.ExecutiveEntity
import com.example.data.model.ExecutiveStockSummary
import com.example.data.model.InventoryLocation
import com.example.data.model.InventoryTagEntity
import com.example.data.model.LoggedInUser
import com.example.data.model.MovementWithProof
import com.example.data.model.OverallStockMetrics
import com.example.data.model.ProofDocumentEntity
import com.example.data.model.ProofType
import com.example.data.model.UserRole
import com.example.data.repository.InventoryRepository
import com.example.data.repository.TransactionExecutionResult
import com.example.data.repository.TransactionValidationResult
import com.example.ocr.ExtractedTagCandidate
import com.example.ocr.OcrProcessingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class AppScreen {
    DASHBOARD,
    WORKFLOW,
    EXECUTIVES,
    SEARCH,
    PROOFS,
    REPORTS
}

enum class WorkflowStep {
    SELECT_DOCUMENT,
    OCR_PROCESSING,
    VERIFICATION,
    PREVIEW_SUMMARY,
    SUCCESS_RESULT
}

data class WorkflowUiState(
    val proofType: ProofType = ProofType.CENTRAL_RECEIVED,
    val currentStep: WorkflowStep = WorkflowStep.SELECT_DOCUMENT,
    val documentUri: Uri? = null,
    val documentBitmap: Bitmap? = null,
    val documentFilename: String = "proof_document.jpg",
    val documentStorageKey: String = "",
    val documentFileSize: Long = 1024L,
    val candidates: List<ExtractedTagCandidate> = emptyList(),
    val selectedExecutiveId: String? = "EXEC-01",
    val selectedLocationForExisting: InventoryLocation = InventoryLocation.MASTER,
    val validationResult: TransactionValidationResult? = null,
    val executionResult: TransactionExecutionResult? = null,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val rawOcrText: String? = null,
    val detectedBank: String? = null,
    val detectedVehicle: String? = null,
    val detectedSerialsList: List<String> = emptyList(),
    val ocrLinesCount: Int = 0
)

class InventoryViewModel(application: Application) : AndroidViewModel(application) {

    val db = AppDatabase.getDatabase(application, viewModelScope)
    val repository = InventoryRepository(db)

    // Global navigation
    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Authentication State
    private val _currentUser = MutableStateFlow<LoggedInUser?>(null)
    val currentUser: StateFlow<LoggedInUser?> = _currentUser.asStateFlow()

    fun loginAsAdmin(username: String, password: String): Boolean {
        if (username.trim().equals("admin", ignoreCase = true) || username.isNotBlank()) {
            _currentUser.value = LoggedInUser(
                role = UserRole.ADMIN,
                userId = "admin",
                displayName = "System Administrator",
                region = "Central Logistics HQ"
            )
            _currentScreen.value = AppScreen.DASHBOARD
            return true
        }
        return false
    }

    fun loginAsUser(execIdOrName: String, password: String): Boolean {
        if (execIdOrName.isBlank()) return false
        val clean = execIdOrName.trim()
        val allExecs = allExecutives.value
        val matched = allExecs.find { 
            it.executiveId.equals(clean, ignoreCase = true) || 
            it.executiveName.contains(clean, ignoreCase = true) 
        }
        val id = matched?.executiveId ?: (if (clean.uppercase().startsWith("EXEC")) clean.uppercase() else "EXEC-01")
        val name = matched?.executiveName ?: clean
        val region = matched?.region ?: "Regional Territory"

        _currentUser.value = LoggedInUser(
            role = UserRole.USER,
            userId = id,
            displayName = name,
            region = region
        )
        return true
    }

    fun logout() {
        _currentUser.value = null
        _currentScreen.value = AppScreen.DASHBOARD
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    // Repository Flows
    val metrics: StateFlow<OverallStockMetrics> = repository.overallMetrics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OverallStockMetrics())

    val classSummaries: StateFlow<List<ClassStockSummary>> = repository.classStockSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val executiveSummaries: StateFlow<List<ExecutiveStockSummary>> = repository.executiveStockSummaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeExecutives: StateFlow<List<ExecutiveEntity>> = repository.activeExecutives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExecutives: StateFlow<List<ExecutiveEntity>> = repository.allExecutives
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentMovements: StateFlow<List<MovementWithProof>> = repository.recentMovements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProofs: StateFlow<List<ProofDocumentEntity>> = repository.allProofs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val discoveredClasses: StateFlow<List<String>> = repository.getDiscoveredClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Workflow State
    private val _workflowState = MutableStateFlow(WorkflowUiState())
    val workflowState: StateFlow<WorkflowUiState> = _workflowState.asStateFlow()

    // Selected Tag for Details Sheet
    private val _selectedTagSerial = MutableStateFlow<String?>(null)
    val selectedTagSerial: StateFlow<String?> = _selectedTagSerial.asStateFlow()

    // Selected Executive for Details Sheet
    private val _selectedExecutiveId = MutableStateFlow<String?>(null)
    val selectedExecutiveId: StateFlow<String?> = _selectedExecutiveId.asStateFlow()

    // Selected Proof for Preview Dialog
    private val _selectedProof = MutableStateFlow<ProofDocumentEntity?>(null)
    val selectedProof: StateFlow<ProofDocumentEntity?> = _selectedProof.asStateFlow()

    // Search & Filter State
    val searchQuery = MutableStateFlow("")
    val searchLocation = MutableStateFlow("")
    val searchClass = MutableStateFlow("")
    val searchExecutive = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val searchResults: StateFlow<List<InventoryTagEntity>> = combine(
        searchQuery,
        searchLocation,
        searchClass,
        searchExecutive
    ) { query: String, loc: String, cls: String, exec: String ->
        listOf(query, loc, cls, exec)
    }.flatMapLatest { params: List<String> ->
        repository.searchTags(params[0], params[1], params[2], params[3])
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tag Detail Flows
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTagEntity: StateFlow<InventoryTagEntity?> = _selectedTagSerial
        .flatMapLatest { serial: String? ->
            if (serial != null) db.inventoryTagDao().getTagBySerial(serial)
            else flowOf(null)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedTagHistory: StateFlow<List<MovementWithProof>> = _selectedTagSerial
        .flatMapLatest { serial: String? ->
            if (serial != null) db.stockMovementLedgerDao().getMovementsForTag(serial)
            else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTagForDetails(serial: String?) {
        _selectedTagSerial.value = serial
    }

    fun selectExecutiveForDetails(execId: String?) {
        _selectedExecutiveId.value = execId
    }

    fun selectProofById(proofId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val proof = db.proofDocumentDao().getProofByIdSync(proofId)
            _selectedProof.value = proof
        }
    }

    fun selectProofForPreview(proof: ProofDocumentEntity?) {
        _selectedProof.value = proof
    }

    fun startWorkflow(proofType: ProofType) {
        _workflowState.value = WorkflowUiState(
            proofType = proofType,
            currentStep = WorkflowStep.SELECT_DOCUMENT,
            selectedExecutiveId = activeExecutives.value.firstOrNull()?.executiveId ?: "EXEC-01"
        )
        _currentScreen.value = AppScreen.WORKFLOW
    }

    fun cancelWorkflow() {
        _workflowState.value = WorkflowUiState()
        _currentScreen.value = AppScreen.DASHBOARD
    }

    fun onDocumentSelected(uri: Uri?, bitmap: Bitmap?, filename: String, rawTextContent: String? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _workflowState.value = _workflowState.value.copy(
                currentStep = WorkflowStep.OCR_PROCESSING,
                documentUri = uri,
                documentBitmap = bitmap,
                documentFilename = filename,
                documentStorageKey = "vault_${System.currentTimeMillis()}_$filename",
                isProcessing = true,
                errorMessage = null
            )

            // Process FASTag document with Google ML Kit Text Recognition
            val ocrResult = if (rawTextContent != null) {
                OcrProcessingEngine.parseOcrDetailed(rawTextContent)
            } else if (bitmap != null) {
                OcrProcessingEngine.processBitmapDetailed(bitmap)
            } else if (uri != null) {
                OcrProcessingEngine.processImageDetailed(getApplication(), uri)
            } else {
                com.example.ocr.FastagOcrResult()
            }

            val finalCandidates = if (ocrResult.candidates.isEmpty()) {
                // Provide a default editable candidate so admin can enter values easily
                listOf(
                    ExtractedTagCandidate(
                        subagentId = ocrResult.detectedSubagentId ?: "SA-1001",
                        subagentName = ocrResult.detectedSubagentName ?: "Executive Agent",
                        tagClass = ocrResult.detectedClass ?: "Class 12",
                        isRange = false,
                        serialNumber = ocrResult.primarySerial ?: ""
                    )
                )
            } else {
                ocrResult.candidates
            }

            _workflowState.value = _workflowState.value.copy(
                currentStep = WorkflowStep.VERIFICATION,
                candidates = finalCandidates,
                rawOcrText = ocrResult.rawText,
                detectedBank = ocrResult.detectedBank,
                detectedVehicle = ocrResult.detectedVehicleNumber,
                detectedSerialsList = ocrResult.detectedSerials,
                ocrLinesCount = ocrResult.linesCount,
                isProcessing = false
            )

            revalidateCandidates()
        }
    }

    fun updateCandidate(id: String, transform: (ExtractedTagCandidate) -> ExtractedTagCandidate) {
        val updated = _workflowState.value.candidates.map {
            if (it.id == id) {
                val c = transform(it)
                c.updateCalculatedQuantity()
                c
            } else it
        }
        _workflowState.value = _workflowState.value.copy(candidates = updated)
        revalidateCandidates()
    }

    fun addCandidateRow() {
        val current = _workflowState.value.candidates
        val last = current.lastOrNull()
        val newRow = ExtractedTagCandidate(
            subagentId = last?.subagentId ?: "SA-1001",
            subagentName = last?.subagentName ?: "Executive Agent",
            tagClass = last?.tagClass ?: "Class 12",
            isRange = false,
            serialNumber = ""
        )
        newRow.updateCalculatedQuantity()
        _workflowState.value = _workflowState.value.copy(candidates = current + newRow)
        revalidateCandidates()
    }

    fun removeCandidateRow(id: String) {
        val updated = _workflowState.value.candidates.filterNot { it.id == id }
        _workflowState.value = _workflowState.value.copy(candidates = updated)
        revalidateCandidates()
    }

    fun setSelectedExecutiveId(id: String) {
        _workflowState.value = _workflowState.value.copy(selectedExecutiveId = id)
        revalidateCandidates()
    }

    fun setSelectedLocationForExisting(location: InventoryLocation) {
        _workflowState.value = _workflowState.value.copy(selectedLocationForExisting = location)
        revalidateCandidates()
    }

    private fun revalidateCandidates() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _workflowState.value
            val res = repository.validateCandidates(
                proofType = state.proofType,
                candidates = state.candidates,
                selectedExecutiveId = state.selectedExecutiveId,
                selectedLocationForExisting = state.selectedLocationForExisting
            )
            _workflowState.value = _workflowState.value.copy(validationResult = res)
        }
    }

    fun proceedToSummary() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _workflowState.value
            val res = repository.validateCandidates(
                proofType = state.proofType,
                candidates = state.candidates,
                selectedExecutiveId = state.selectedExecutiveId,
                selectedLocationForExisting = state.selectedLocationForExisting
            )
            if (!res.isValid) {
                _workflowState.value = _workflowState.value.copy(
                    validationResult = res,
                    errorMessage = res.errorMessage ?: "Please fix verification errors before confirming."
                )
            } else {
                _workflowState.value = _workflowState.value.copy(
                    validationResult = res,
                    currentStep = WorkflowStep.PREVIEW_SUMMARY,
                    errorMessage = null
                )
            }
        }
    }

    fun backToVerification() {
        _workflowState.value = _workflowState.value.copy(
            currentStep = WorkflowStep.VERIFICATION,
            errorMessage = null
        )
    }

    fun confirmAndExecuteTransaction() {
        viewModelScope.launch(Dispatchers.IO) {
            _workflowState.value = _workflowState.value.copy(isProcessing = true, errorMessage = null)
            val state = _workflowState.value
            val plan = state.validationResult
            if (plan == null || !plan.isValid) {
                _workflowState.value = _workflowState.value.copy(
                    isProcessing = false,
                    errorMessage = "Cannot commit an invalid transaction plan."
                )
                return@launch
            }

            val result = repository.executeTransaction(
                proofType = state.proofType,
                validationPlan = plan,
                selectedExecutiveId = state.selectedExecutiveId,
                selectedLocationForExisting = state.selectedLocationForExisting,
                proofFilename = state.documentFilename,
                proofStorageKey = state.documentStorageKey,
                fileSize = state.documentFileSize
            )

            if (result.success) {
                _workflowState.value = _workflowState.value.copy(
                    isProcessing = false,
                    currentStep = WorkflowStep.SUCCESS_RESULT,
                    executionResult = result,
                    errorMessage = null
                )
            } else {
                _workflowState.value = _workflowState.value.copy(
                    isProcessing = false,
                    errorMessage = result.message
                )
            }
        }
    }

    fun dismissSuccess() {
        _workflowState.value = WorkflowUiState()
        _currentScreen.value = AppScreen.DASHBOARD
    }

    // Subagent / Executive Management
    fun addExecutive(id: String, name: String, region: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.addExecutive(id, name, region)
            result.fold(
                onSuccess = { onResult(true, "Executive '$id' created successfully.") },
                onFailure = { onResult(false, it.message ?: "Failed to add executive.") }
            )
        }
    }

    fun toggleExecutiveActive(execId: String, currentActive: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleExecutiveActive(execId, !currentActive)
        }
    }

    fun updateExecutive(entity: ExecutiveEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateExecutive(entity)
        }
    }

    fun updateExecutiveDetails(
        execId: String,
        name: String,
        region: String,
        active: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.updateExecutiveDetails(execId, name, region, active)
            result.fold(
                onSuccess = { onResult(true, "Subagent details updated successfully.") },
                onFailure = { onResult(false, it.message ?: "Failed to update subagent.") }
            )
        }
    }

    fun clearAllInventoryData(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.clearAllInventoryData()
            result.fold(
                onSuccess = { onResult(true, "Inventory database cleared. Ready for fresh operations.") },
                onFailure = { onResult(false, it.message ?: "Failed to clear inventory.") }
            )
        }
    }

    fun submitUserTagImage(
        bitmap: Bitmap?,
        uri: Uri?,
        subagentId: String,
        subagentName: String,
        tagSerial: String,
        tagClass: String,
        notes: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val timestamp = System.currentTimeMillis()
                val filename = "tag_upload_${subagentId}_$timestamp.jpg"
                val proofsDir = File(context.filesDir, "proofs").apply { mkdirs() }
                val targetFile = File(proofsDir, filename)

                if (bitmap != null) {
                    FileOutputStream(targetFile).use { out ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }
                } else if (uri != null) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                val result = repository.submitUserTagImage(
                    filename = filename,
                    storageKey = targetFile.absolutePath,
                    subagentId = subagentId,
                    subagentName = subagentName,
                    tagSerial = tagSerial,
                    tagClass = tagClass,
                    notes = notes
                )

                result.fold(
                    onSuccess = { proofId ->
                        onResult(true, "Tag image uploaded successfully (Proof #$proofId) and recorded in Proof Vault.")
                    },
                    onFailure = {
                        onResult(false, it.message ?: "Failed to record tag image.")
                    }
                )
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Error saving tag image.")
            }
        }
    }
}
