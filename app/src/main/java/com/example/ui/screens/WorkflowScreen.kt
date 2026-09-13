package com.example.ui.screens

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ExecutiveEntity
import com.example.data.model.InventoryLocation
import com.example.data.model.ProofType
import com.example.ocr.ExtractedTagCandidate
import com.example.ocr.SampleDocumentFixtures
import com.example.ui.theme.*
import com.example.ui.viewmodel.WorkflowStep
import com.example.ui.viewmodel.WorkflowUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowScreen(
    state: WorkflowUiState,
    activeExecutives: List<ExecutiveEntity>,
    onSelectDocument: (Uri?, Bitmap?, String, String?) -> Unit,
    onUpdateCandidate: (String, (ExtractedTagCandidate) -> ExtractedTagCandidate) -> Unit,
    onAddCandidate: () -> Unit,
    onRemoveCandidate: (String) -> Unit,
    onSetExecutive: (String) -> Unit,
    onSetLocationForExisting: (InventoryLocation) -> Unit,
    onProceedToSummary: () -> Unit,
    onBackToVerification: () -> Unit,
    onConfirmTransaction: () -> Unit,
    onCancel: () -> Unit,
    onDismissSuccess: () -> Unit
) {
    val context = LocalContext.current
    var permissionErrorMessage by remember { mutableStateOf<String?>(null) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            onSelectDocument(null, bitmap, "camera_capture_${System.currentTimeMillis()}.jpg", null)
        }
    }

    // Camera permission request launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                permissionErrorMessage = "Camera launch error: ${e.localizedMessage ?: "Unable to open camera"}"
            }
        } else {
            permissionErrorMessage = "Camera permission was not granted. Please allow camera access, or use Gallery / Sample manifests below."
        }
    }

    val launchCameraSafely = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                permissionErrorMessage = "Camera launch error: ${e.localizedMessage ?: "Unable to open camera"}"
            }
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    // Photo picker launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectDocument(uri, null, "gallery_proof_${System.currentTimeMillis()}.jpg", null)
        }
    }

    val launchGallerySafely = {
        try {
            galleryLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (e: Exception) {
            permissionErrorMessage = "Gallery error: ${e.localizedMessage ?: "Unable to open gallery"}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("workflow_screen")
    ) {
        // Workflow Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = state.proofType.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (state.proofType) {
                                ProofType.EXISTING_STOCK -> "Step: Initialize Baseline Inventory"
                                ProofType.CENTRAL_RECEIVED -> "Route: CENTRAL → MASTER"
                                ProofType.COURIER_TO_EXECUTIVE -> "Route: MASTER → EXECUTIVE"
                                ProofType.TAG_ASSIGNED -> "Route: EXECUTIVE → ASSIGNED"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when (state.currentStep) {
                            WorkflowStep.SELECT_DOCUMENT -> "1. Proof"
                            WorkflowStep.OCR_PROCESSING -> "2. OCR"
                            WorkflowStep.VERIFICATION -> "3. Verify"
                            WorkflowStep.PREVIEW_SUMMARY -> "4. Confirm"
                            WorkflowStep.SUCCESS_RESULT -> "Done"
                        },
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Error message banner if any
        if (state.errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = state.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Camera / Gallery error or permission banner
        if (permissionErrorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = permissionErrorMessage ?: "",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { permissionErrorMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Step Routing Content
        when (state.currentStep) {
            WorkflowStep.SELECT_DOCUMENT -> {
                SelectDocumentStep(
                    proofType = state.proofType,
                    activeExecutives = activeExecutives,
                    selectedExecutiveId = state.selectedExecutiveId,
                    selectedLocationForExisting = state.selectedLocationForExisting,
                    onSetExecutive = onSetExecutive,
                    onSetLocationForExisting = onSetLocationForExisting,
                    onCaptureCamera = launchCameraSafely,
                    onPickGallery = launchGallerySafely,
                    onSelectFixture = { fixture ->
                        onSelectDocument(null, null, fixture.defaultFilename, fixture.rawText)
                    }
                )
            }

            WorkflowStep.OCR_PROCESSING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Extracting FASTag Manifest...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Detecting rows, serial numbers, and range definitions with Google ML Kit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            WorkflowStep.VERIFICATION -> {
                VerificationStep(
                    state = state,
                    activeExecutives = activeExecutives,
                    onUpdateCandidate = onUpdateCandidate,
                    onAddCandidate = onAddCandidate,
                    onRemoveCandidate = onRemoveCandidate,
                    onSetExecutive = onSetExecutive,
                    onSetLocationForExisting = onSetLocationForExisting,
                    onProceedToSummary = onProceedToSummary,
                    onRetake = { state.documentFilename.let { } }
                )
            }

            WorkflowStep.PREVIEW_SUMMARY -> {
                PreviewSummaryStep(
                    state = state,
                    onBack = onBackToVerification,
                    onConfirm = onConfirmTransaction
                )
            }

            WorkflowStep.SUCCESS_RESULT -> {
                SuccessResultStep(
                    state = state,
                    onDismiss = onDismissSuccess
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectDocumentStep(
    proofType: ProofType,
    activeExecutives: List<ExecutiveEntity>,
    selectedExecutiveId: String?,
    selectedLocationForExisting: InventoryLocation,
    onSetExecutive: (String) -> Unit,
    onSetLocationForExisting: (InventoryLocation) -> Unit,
    onCaptureCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onSelectFixture: (com.example.ocr.SampleReceiptFixture) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "STEP 1: UPLOAD PROOF DOCUMENT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Every inventory-changing operation requires verifiable proof (waybill, dispatch note, or assignment slip).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Executive selector for Courier to Executive
        if (proofType == ProofType.COURIER_TO_EXECUTIVE) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SELECT RECIPIENT EXECUTIVE *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            activeExecutives.forEach { exec ->
                                val isSelected = exec.executiveId == selectedExecutiveId
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetExecutive(exec.executiveId) },
                                    label = { Text("${exec.executiveId} (${exec.executiveName})") },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }
        }

        // Initial Location selector for Existing Stock
        if (proofType == ProofType.EXISTING_STOCK) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "SELECT INITIAL BASELINE LOCATION *",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(InventoryLocation.MASTER, InventoryLocation.EXECUTIVE, InventoryLocation.ASSIGNED).forEach { loc ->
                                val isSelected = loc == selectedLocationForExisting
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onSetLocationForExisting(loc) },
                                    label = { Text(loc.name) }
                                )
                            }
                        }

                        if (selectedLocationForExisting == InventoryLocation.EXECUTIVE) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Select Executive:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                activeExecutives.take(5).forEach { exec ->
                                    val isSelected = exec.executiveId == selectedExecutiveId
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSetExecutive(exec.executiveId) },
                                        label = { Text(exec.executiveId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Capture/Upload Buttons
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "DOCUMENT CAPTURE",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onCaptureCamera,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_capture_camera"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera")
                        }

                        OutlinedButton(
                            onClick = onPickGallery,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_pick_gallery"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gallery")
                        }
                    }
                }
            }
        }

        // Direct Manual / Guide Section
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "REAL-TIME DOCUMENT OCR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Capture or select an image of your physical invoice, courier slip, or FASTag barcode sheet. Text and barcode sequences are extracted automatically.",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VerificationStep(
    state: WorkflowUiState,
    activeExecutives: List<ExecutiveEntity>,
    onUpdateCandidate: (String, (ExtractedTagCandidate) -> ExtractedTagCandidate) -> Unit,
    onAddCandidate: () -> Unit,
    onRemoveCandidate: (String) -> Unit,
    onSetExecutive: (String) -> Unit,
    onSetLocationForExisting: (InventoryLocation) -> Unit,
    onProceedToSummary: () -> Unit,
    onRetake: () -> Unit
) {
    val totalCalculated = remember(state.candidates) {
        state.candidates.filter { it.isSelected }.sumOf { it.calculatedQuantity }
    }

    val hasAnyError = remember(state.candidates, state.validationResult) {
        state.candidates.any { it.hasError } || (state.validationResult?.isValid == false)
    }
    var showRawOcrText by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Document info & warning
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = state.documentFilename,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "ML Kit OCR extraction completed. Admin verification required.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$totalCalculated Tags",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // ML Kit Text Recognition Card
        if (state.rawOcrText != null || state.detectedBank != null || state.detectedVehicle != null) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Google ML Kit Text Recognition",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${state.ocrLinesCount} lines parsed",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (!state.detectedBank.isNullOrBlank()) {
                            Text("Issuer Bank: ${state.detectedBank}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (!state.detectedVehicle.isNullOrBlank()) {
                            Text("Detected Vehicle: ${state.detectedVehicle}", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }

                        if (!state.rawOcrText.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showRawOcrText = !showRawOcrText },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (showRawOcrText) "Hide Raw Extracted Text" else "View Raw OCR Extracted Text",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Icon(
                                    imageVector = if (showRawOcrText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            AnimatedVisibility(visible = showRawOcrText) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = state.rawOcrText!!,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Admin verification warning
        item {
            Surface(
                color = Color(0xFFFEF3C7),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircleOutline,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OCR outputs are suggestions only. Verify all fields before confirmation.",
                        color = Color(0xFF92400E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Candidate edit cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "EXTRACTED TAG ROWS (${state.candidates.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextButton(onClick = onAddCandidate) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Row", fontSize = 12.sp)
                }
            }
        }

        itemsIndexed(state.candidates) { index, candidate ->
            CandidateItemCard(
                index = index + 1,
                candidate = candidate,
                onUpdate = { transform -> onUpdateCandidate(candidate.id, transform) },
                onDelete = { onRemoveCandidate(candidate.id) }
            )
        }

        // Action Buttons
        item {
            Spacer(modifier = Modifier.height(8.dp))

            if (state.validationResult?.isValid == false) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.validationResult.errorMessage ?: "Validation error found.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = onProceedToSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_proceed_summary"),
                shape = RoundedCornerShape(10.dp),
                enabled = !hasAnyError && totalCalculated > 0
            ) {
                Text("Confirm & Review Summary ($totalCalculated Tags)")
            }
        }
    }
}

@Composable
fun CandidateItemCard(
    index: Int,
    candidate: ExtractedTagCandidate,
    onUpdate: ((ExtractedTagCandidate) -> ExtractedTagCandidate) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (candidate.hasError) Color(0xFFFFF1F2) else MaterialTheme.colorScheme.surface
        ),
        border = if (candidate.hasError) {
            androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Slate200)
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#$index",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (candidate.isRange) BrandPurple.copy(alpha = 0.12f) else BrandTeal.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (candidate.isRange) "SERIAL RANGE" else "INDIVIDUAL SERIAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (candidate.isRange) BrandPurple else BrandTeal,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Qty: ${candidate.calculatedQuantity}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subagent ID & Name
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = candidate.subagentId,
                    onValueChange = { onUpdate { c -> c.copy(subagentId = it) } },
                    label = { Text("Subagent ID", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = candidate.subagentName,
                    onValueChange = { onUpdate { c -> c.copy(subagentName = it) } },
                    label = { Text("Subagent Name", fontSize = 11.sp) },
                    modifier = Modifier.weight(1.4f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tag Class & Toggle Range
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = candidate.tagClass,
                    onValueChange = { onUpdate { c -> c.copy(tagClass = it) } },
                    label = { Text("Tag Class", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = candidate.isRange,
                        onCheckedChange = { isRange -> onUpdate { c -> c.copy(isRange = isRange) } }
                    )
                    Text("Is Range?", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Serials
            if (candidate.isRange) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = candidate.fromSerial,
                        onValueChange = { onUpdate { c -> c.copy(fromSerial = it) } },
                        label = { Text("From Serial", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = candidate.toSerial,
                        onValueChange = { onUpdate { c -> c.copy(toSerial = it) } },
                        label = { Text("To Serial", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            } else {
                OutlinedTextField(
                    value = candidate.serialNumber,
                    onValueChange = { onUpdate { c -> c.copy(serialNumber = it) } },
                    label = { Text("Tag Serial Number", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            if (candidate.hasError && candidate.errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⚠ ${candidate.errorMessage}",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun PreviewSummaryStep(
    state: WorkflowUiState,
    onBack: () -> Unit,
    onConfirm: () -> Unit
) {
    val plan = state.validationResult

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "STEP 4: TRANSACTION SUMMARY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )

            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "MOVEMENT CONFIRMATION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val routeText = when (state.proofType) {
                        ProofType.EXISTING_STOCK -> "INITIALIZE → ${state.selectedLocationForExisting.name}"
                        ProofType.CENTRAL_RECEIVED -> "CENTRAL → MASTER"
                        ProofType.COURIER_TO_EXECUTIVE -> "MASTER → ${state.selectedExecutiveId}"
                        ProofType.TAG_ASSIGNED -> "EXECUTIVE → ASSIGNED"
                    }

                    Surface(
                        color = BrandBlueLight,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = routeText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = BrandBluePrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Tags to Process:", fontSize = 13.sp)
                        Text("${plan?.totalCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Proof Document:", fontSize = 13.sp)
                        Text(state.documentFilename, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }

                    Divider(modifier = Modifier.padding(vertical = 12.dp))

                    Text(
                        text = "CLASS BREAKDOWN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    plan?.classBreakdown?.forEach { (cls, count) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(cls, fontSize = 12.sp)
                            Text("$count tags", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Surface(
                color = Color(0xFFF1F5F9),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Slate700,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Atomic execution guaranteed: If any individual tag is invalid, all changes rollback.",
                        fontSize = 11.sp,
                        color = Slate700
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("btn_confirm_transaction"),
                shape = RoundedCornerShape(10.dp),
                enabled = !state.isProcessing
            ) {
                if (state.isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Executing Transaction...")
                } else {
                    Text("Confirm & Commit Transaction")
                }
            }

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Back to Verification")
            }
        }
    }
}

@Composable
fun SuccessResultStep(
    state: WorkflowUiState,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandEmerald.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = BrandEmerald,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Transaction Committed",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = state.executionResult?.message ?: "Inventory updated successfully.",
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider()

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Affected Tags:", fontSize = 12.sp)
                    Text("${state.executionResult?.affectedCount ?: 0}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Proof Document ID:", fontSize = 12.sp)
                    Text("#${state.executionResult?.proofId ?: 0}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Audit Ledger:", fontSize = 12.sp)
                    Text("Permanent & Immutable", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandEmerald)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_return_dashboard"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Return to Dashboard")
                }
            }
        }
    }
}
