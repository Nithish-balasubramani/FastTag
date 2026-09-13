package com.example.ui.screens

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.LoggedInUser
import com.example.data.model.ProofDocumentEntity
import com.example.ocr.FastagOcrResult
import com.example.ocr.OcrProcessingEngine
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserUploadTagScreen(
    user: LoggedInUser,
    allProofs: List<ProofDocumentEntity>,
    onLogout: () -> Unit,
    onSubmitTagImage: (Bitmap?, Uri?, String, String, String, String, String, (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }

    var tagSerial by remember { mutableStateOf("") }
    var tagClass by remember { mutableStateOf("Class 4") }
    var notes by remember { mutableStateOf("") }
    var isOcrRunning by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var selectedProofForPreview by remember { mutableStateOf<ProofDocumentEntity?>(null) }
    var ocrResult by remember { mutableStateOf<FastagOcrResult?>(null) }
    var showRawOcrDialog by remember { mutableStateOf(false) }

    fun processOcrForBitmap(bitmap: Bitmap) {
        coroutineScope.launch {
            isOcrRunning = true
            val result = OcrProcessingEngine.processBitmapDetailed(bitmap)
            ocrResult = result
            isOcrRunning = false
            if (!result.primarySerial.isNullOrBlank()) {
                tagSerial = result.primarySerial
            }
            if (!result.detectedClass.isNullOrBlank()) {
                tagClass = result.detectedClass
            }
            if (!result.detectedVehicleNumber.isNullOrBlank() && notes.isBlank()) {
                notes = "Vehicle: ${result.detectedVehicleNumber}"
            }
        }
    }

    fun processOcrForUri(uri: Uri) {
        coroutineScope.launch {
            isOcrRunning = true
            val result = OcrProcessingEngine.processImageDetailed(context, uri)
            ocrResult = result
            isOcrRunning = false
            if (!result.primarySerial.isNullOrBlank()) {
                tagSerial = result.primarySerial
            }
            if (!result.detectedClass.isNullOrBlank()) {
                tagClass = result.detectedClass
            }
            if (!result.detectedVehicleNumber.isNullOrBlank() && notes.isBlank()) {
                notes = "Vehicle: ${result.detectedVehicleNumber}"
            }
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
            selectedUri = null
            statusMessage = null
            processOcrForBitmap(bitmap)
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
                statusMessage = false to "Camera launch error: ${e.localizedMessage}"
            }
        } else {
            statusMessage = false to "Camera permission denied. You can still pick an image from your gallery."
        }
    }

    val launchCamera = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            try {
                cameraLauncher.launch(null)
            } catch (e: Exception) {
                statusMessage = false to "Camera error: ${e.localizedMessage}"
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
            selectedUri = uri
            selectedBitmap = null
            statusMessage = null
            processOcrForUri(uri)
        }
    }

    val myUploads = remember(allProofs, user.userId) {
        allProofs.filter {
            it.uploadedBy.contains(user.userId, ignoreCase = true) ||
            it.uploadedBy.contains(user.displayName, ignoreCase = true) ||
            it.proofType == "USER_TAG_UPLOAD"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FASTag Agent Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "${user.displayName} (${user.userId}) • ${user.region}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D9488).copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = "FIELD AGENT",
                            color = Color(0xFF0D9488),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("btn_user_logout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Instruction
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "UPLOAD TAG PROOF PHOTO",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Capture or pick a clear photo of the RFID/FASTag barcode or installation slip. Submitted images are permanently recorded in the immutable audit vault.",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Capture Actions
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "IMAGE CAPTURE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = launchCamera,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_user_take_photo"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Take Photo", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("btn_user_pick_gallery"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Gallery", fontWeight = FontWeight.Bold)
                            }
                        }

                        // Selected Image Preview
                        if (selectedBitmap != null || selectedUri != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.05f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selectedBitmap != null) {
                                    Image(
                                        bitmap = selectedBitmap!!.asImageBitmap(),
                                        contentDescription = "Selected Tag",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else if (selectedUri != null) {
                                    AsyncImage(
                                        model = selectedUri,
                                        contentDescription = "Selected Tag",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Retake / Clear Button
                                IconButton(
                                    onClick = {
                                        selectedBitmap = null
                                        selectedUri = null
                                        ocrResult = null
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear image",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (isOcrRunning) {
                                    Surface(
                                        color = Color.Black.copy(alpha = 0.7f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            CircularProgressIndicator(
                                                color = Color(0xFF64FFDA),
                                                strokeWidth = 2.dp,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Processing with Google ML Kit...",
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ML Kit Text Recognition Card
            if (ocrResult != null) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_mlkit_ocr_results")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "ML KIT TEXT RECOGNITION",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Surface(
                                    color = if (ocrResult!!.isSuccess) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (ocrResult!!.isSuccess) "Recognized (${ocrResult!!.linesCount} lines)" else "No Text Found",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ocrResult!!.isSuccess) Color(0xFF15803D) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            // Issuer Bank
                            if (!ocrResult!!.detectedBank.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Issuer / Bank: ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(ocrResult!!.detectedBank!!, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }

                            // Detected Vehicle Number
                            if (!ocrResult!!.detectedVehicleNumber.isNullOrBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Detected Vehicle: ", fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(ocrResult!!.detectedVehicleNumber!!, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    }
                                    TextButton(
                                        onClick = {
                                            val v = ocrResult!!.detectedVehicleNumber!!
                                            notes = if (notes.isBlank()) "Vehicle: $v" else "$notes | Vehicle: $v"
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Add to Notes", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Detected Serials
                            if (ocrResult!!.detectedSerials.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "Detected Tag Serials (tap to fill input):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ocrResult!!.detectedSerials.forEach { serial ->
                                            val isCurrent = tagSerial == serial
                                            FilterChip(
                                                selected = isCurrent,
                                                onClick = { tagSerial = serial },
                                                label = { Text(serial, fontSize = 11.sp, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal) },
                                                leadingIcon = if (isCurrent) {
                                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                } else null
                                            )
                                        }
                                    }
                                }
                            }

                            // Detected Ranges
                            if (ocrResult!!.detectedRanges.isNotEmpty()) {
                                Column {
                                    Text(
                                        text = "Detected Range(s):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ocrResult!!.detectedRanges.forEach { range ->
                                            AssistChip(
                                                onClick = { tagSerial = range.first },
                                                label = { Text("${range.first} → ${range.second}", fontSize = 11.sp) },
                                                leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Actions
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (ocrResult!!.rawText.isNotBlank()) {
                                    TextButton(
                                        onClick = { showRawOcrDialog = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("View Raw OCR Text", fontSize = 11.5.sp)
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                TextButton(
                                    onClick = {
                                        if (selectedBitmap != null) {
                                            processOcrForBitmap(selectedBitmap!!)
                                        } else if (selectedUri != null) {
                                            processOcrForUri(selectedUri!!)
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Re-scan with ML Kit", fontSize = 11.5.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Tag Details Form
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "TAG IDENTIFICATION & DETAILS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )

                        OutlinedTextField(
                            value = tagSerial,
                            onValueChange = { tagSerial = it.uppercase() },
                            label = { Text("Tag Serial Number *") },
                            placeholder = { Text("e.g. 100001 or TAG-99210") },
                            leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_user_tag_serial")
                        )

                        // Class selection row
                        Column {
                            Text(
                                text = "Tag Class",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("Class 4", "Class 7", "Class 12", "Commercial").forEach { cls ->
                                    FilterChip(
                                        selected = tagClass == cls,
                                        onClick = { tagClass = cls },
                                        label = { Text(cls, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Installation / Vehicle Notes") },
                            placeholder = { Text("e.g. Vehicle # MH-12-AB-3456, Toll Plaza A") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_user_notes")
                        )

                        // Status / error banner
                        AnimatedVisibility(visible = statusMessage != null) {
                            statusMessage?.let { (isSuccess, text) ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSuccess) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.errorContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (isSuccess) Color(0xFF15803D) else MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = text,
                                            color = if (isSuccess) Color(0xFF15803D) else MaterialTheme.colorScheme.onErrorContainer,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                if (selectedBitmap == null && selectedUri == null) {
                                    statusMessage = false to "Please take a photo or choose an image first."
                                    return@Button
                                }
                                if (tagSerial.isBlank()) {
                                    statusMessage = false to "Please enter or scan the Tag Serial Number."
                                    return@Button
                                }

                                isSubmitting = true
                                statusMessage = null
                                onSubmitTagImage(
                                    selectedBitmap,
                                    selectedUri,
                                    user.userId,
                                    user.displayName,
                                    tagSerial,
                                    tagClass,
                                    notes
                                ) { success, msg ->
                                    isSubmitting = false
                                    statusMessage = success to msg
                                    if (success) {
                                        selectedBitmap = null
                                        selectedUri = null
                                        tagSerial = ""
                                        notes = ""
                                    }
                                }
                            },
                            enabled = !isSubmitting && (selectedBitmap != null || selectedUri != null),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_user_submit_proof")
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Submit Tag Image to Proof Vault", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // User's Upload History
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MY RECENT SUBMISSIONS (${myUploads.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (myUploads.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No tag photos uploaded yet.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Use the camera above to capture FASTags installed on customer vehicles.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
                items(myUploads) { proof ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedProofForPreview = proof }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0D9488).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = null,
                                    tint = Color(0xFF0D9488),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = proof.originalFilename,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Uploaded by: ${proof.uploadedBy}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                    text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(proof.uploadedAt)),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFDCFCE7)
                            ) {
                                Text(
                                    text = "RECORDED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Preview dialog for clicked proof
        selectedProofForPreview?.let { proof ->
            AlertDialog(
                onDismissRequest = { selectedProofForPreview = null },
                title = { Text(proof.originalFilename, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val file = File(proof.storageObjectKey)
                        if (file.exists()) {
                            AsyncImage(
                                model = file,
                                contentDescription = "Proof image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Image stored in proof vault", fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Uploaded by: ${proof.uploadedBy}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Uploaded at: ${SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(proof.uploadedAt))}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedProofForPreview = null }) {
                        Text("Close")
                    }
                }
            )
        }

        // Full Raw OCR Text Dialog
        if (showRawOcrDialog && ocrResult != null) {
            AlertDialog(
                onDismissRequest = { showRawOcrDialog = false },
                icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("ML Kit Recognized Text", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${ocrResult!!.linesCount} line(s) recognized across ${ocrResult!!.textBlocksCount} text block(s):",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                        ) {
                            Text(
                                text = ocrResult!!.rawText.ifBlank { "No text recognized." },
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showRawOcrDialog = false }) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        notes = if (notes.isBlank()) ocrResult!!.rawText.take(120) else "$notes | ${ocrResult!!.rawText.take(120)}"
                        showRawOcrDialog = false
                    }) {
                        Text("Append to Notes")
                    }
                }
            )
        }
    }
}
