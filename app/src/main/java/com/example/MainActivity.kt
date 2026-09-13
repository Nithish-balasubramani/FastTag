package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AppDatabase
import com.example.data.model.ProofType
import com.example.data.model.UserRole
import com.example.ui.components.AcceptanceTestDialog
import com.example.ui.components.ExecutiveDetailDialog
import com.example.ui.components.ProofViewerDialog
import com.example.ui.components.TagDetailDialog
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.InventoryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: InventoryViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                val metrics by viewModel.metrics.collectAsStateWithLifecycle()
                val classSummaries by viewModel.classSummaries.collectAsStateWithLifecycle()
                val executiveSummaries by viewModel.executiveSummaries.collectAsStateWithLifecycle()
                val activeExecutives by viewModel.activeExecutives.collectAsStateWithLifecycle()
                val recentMovements by viewModel.recentMovements.collectAsStateWithLifecycle()
                val allProofs by viewModel.allProofs.collectAsStateWithLifecycle()
                val discoveredClasses by viewModel.discoveredClasses.collectAsStateWithLifecycle()

                val workflowState by viewModel.workflowState.collectAsStateWithLifecycle()
                val selectedTagEntity by viewModel.selectedTagEntity.collectAsStateWithLifecycle()
                val selectedTagHistory by viewModel.selectedTagHistory.collectAsStateWithLifecycle()
                val selectedProof by viewModel.selectedProof.collectAsStateWithLifecycle()
                val selectedExecutiveId by viewModel.selectedExecutiveId.collectAsStateWithLifecycle()

                val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
                val searchLocation by viewModel.searchLocation.collectAsStateWithLifecycle()
                val searchClass by viewModel.searchClass.collectAsStateWithLifecycle()
                val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
                val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

                var showAcceptanceTests by remember { mutableStateOf(false) }
                var showWorkflowDropdown by remember { mutableStateOf(false) }

                // Authentication Routing
                if (currentUser == null) {
                    LoginScreen(
                        activeExecutives = activeExecutives,
                        onLoginAsAdmin = { username, password ->
                            viewModel.loginAsAdmin(username, password)
                        },
                        onLoginAsUser = { execIdOrName, password ->
                            viewModel.loginAsUser(execIdOrName, password)
                        }
                    )
                } else if (currentUser?.role == UserRole.USER) {
                    UserUploadTagScreen(
                        user = currentUser!!,
                        allProofs = allProofs,
                        onLogout = { viewModel.logout() },
                        onSubmitTagImage = { bmp, uri, subId, subName, serial, cls, notes, cb ->
                            viewModel.submitUserTagImage(bmp, uri, subId, subName, serial, cls, notes, cb)
                        }
                    )
                } else {
                    // Administrator Portal - Complete Access
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "FASTag Admin Portal",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "ADMIN",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${currentUser?.displayName ?: "Administrator"} • Central Logistics",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                },
                                actions = {
                                    // Acceptance Test Runner button (Evaluator verification of Tests A-G)
                                    IconButton(
                                        onClick = { showAcceptanceTests = true },
                                        modifier = Modifier.testTag("btn_open_acceptance_suite")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FactCheck,
                                            contentDescription = "Test Suite",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Quick Workflow Action Menu
                                    Box {
                                        IconButton(
                                            onClick = { showWorkflowDropdown = true },
                                            modifier = Modifier.testTag("btn_workflow_menu")
                                        ) {
                                            Icon(Icons.Default.AddCircleOutline, contentDescription = "New Operation")
                                        }

                                        DropdownMenu(
                                            expanded = showWorkflowDropdown,
                                            onDismissRequest = { showWorkflowDropdown = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Upload Existing Stock") },
                                                leadingIcon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                                                onClick = {
                                                    showWorkflowDropdown = false
                                                    viewModel.startWorkflow(ProofType.EXISTING_STOCK)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Central Stock Received") },
                                                leadingIcon = { Icon(Icons.Default.MoveToInbox, contentDescription = null) },
                                                onClick = {
                                                    showWorkflowDropdown = false
                                                    viewModel.startWorkflow(ProofType.CENTRAL_RECEIVED)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Courier to Executive") },
                                                leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null) },
                                                onClick = {
                                                    showWorkflowDropdown = false
                                                    viewModel.startWorkflow(ProofType.COURIER_TO_EXECUTIVE)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Tag Assigned") },
                                                leadingIcon = { Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null) },
                                                onClick = {
                                                    showWorkflowDropdown = false
                                                    viewModel.startWorkflow(ProofType.TAG_ASSIGNED)
                                                }
                                            )
                                        }
                                    }

                                    // Logout Button
                                    IconButton(
                                        onClick = { viewModel.logout() },
                                        modifier = Modifier.testTag("btn_admin_logout")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ExitToApp,
                                            contentDescription = "Logout",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                        },
                        bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.DASHBOARD,
                                onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                                label = { Text("Dashboard", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_dashboard")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.WORKFLOW,
                                onClick = { viewModel.navigateTo(AppScreen.WORKFLOW) },
                                icon = { Icon(Icons.Default.SwapHoriz, contentDescription = "Operations") },
                                label = { Text("Operations", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_workflow")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.EXECUTIVES,
                                onClick = { viewModel.navigateTo(AppScreen.EXECUTIVES) },
                                icon = { Icon(Icons.Default.People, contentDescription = "Subagents") },
                                label = { Text("Subagents", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_executives")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SEARCH,
                                onClick = { viewModel.navigateTo(AppScreen.SEARCH) },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                                label = { Text("Search", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_search")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.PROOFS,
                                onClick = { viewModel.navigateTo(AppScreen.PROOFS) },
                                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Proofs") },
                                label = { Text("Proofs", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_proofs")
                            )
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.REPORTS,
                                onClick = { viewModel.navigateTo(AppScreen.REPORTS) },
                                icon = { Icon(Icons.Default.Assessment, contentDescription = "Reports") },
                                label = { Text("Reports", fontSize = 11.sp) },
                                modifier = Modifier.testTag("nav_reports")
                            )
                        }
                    },
                    floatingActionButton = {
                        if (currentScreen == AppScreen.DASHBOARD) {
                            ExtendedFloatingActionButton(
                                onClick = { viewModel.startWorkflow(ProofType.CENTRAL_RECEIVED) },
                                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                                text = { Text("OCR Manifest") },
                                modifier = Modifier.testTag("fab_ocr_manifest"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (currentScreen) {
                            AppScreen.DASHBOARD -> {
                                DashboardScreen(
                                    metrics = metrics,
                                    executiveSummaries = executiveSummaries,
                                    classSummaries = classSummaries,
                                    recentMovements = recentMovements,
                                    onStartWorkflow = { viewModel.startWorkflow(it) },
                                    onViewExecutive = { viewModel.selectExecutiveForDetails(it) },
                                    onViewTag = { viewModel.selectTagForDetails(it) },
                                    onViewProof = { viewModel.selectProofById(it) }
                                )
                            }

                            AppScreen.WORKFLOW -> {
                                WorkflowScreen(
                                    state = workflowState,
                                    activeExecutives = activeExecutives,
                                    onSelectDocument = { uri, bitmap, filename, rawText ->
                                        viewModel.onDocumentSelected(uri, bitmap, filename, rawText)
                                    },
                                    onUpdateCandidate = { id, transform ->
                                        viewModel.updateCandidate(id, transform)
                                    },
                                    onAddCandidate = { viewModel.addCandidateRow() },
                                    onRemoveCandidate = { viewModel.removeCandidateRow(it) },
                                    onSetExecutive = { viewModel.setSelectedExecutiveId(it) },
                                    onSetLocationForExisting = { viewModel.setSelectedLocationForExisting(it) },
                                    onProceedToSummary = { viewModel.proceedToSummary() },
                                    onBackToVerification = { viewModel.backToVerification() },
                                    onConfirmTransaction = { viewModel.confirmAndExecuteTransaction() },
                                    onCancel = { viewModel.cancelWorkflow() },
                                    onDismissSuccess = { viewModel.dismissSuccess() }
                                )
                            }

                            AppScreen.EXECUTIVES -> {
                                ExecutivesScreen(
                                    executives = executiveSummaries,
                                    onToggleActive = { id, active -> viewModel.toggleExecutiveActive(id, active) },
                                    onAddExecutive = { id, name, region, cb -> viewModel.addExecutive(id, name, region, cb) },
                                    onEditExecutiveDetails = { id, name, region, active, cb ->
                                        viewModel.updateExecutiveDetails(id, name, region, active, cb)
                                    },
                                    onViewExecutiveDetails = { viewModel.selectExecutiveForDetails(it) },
                                    onClearInventoryData = { cb ->
                                        viewModel.clearAllInventoryData(cb)
                                    }
                                )
                            }

                            AppScreen.SEARCH -> {
                                TagSearchScreen(
                                    searchQuery = searchQuery,
                                    onQueryChange = { viewModel.searchQuery.value = it },
                                    selectedLocation = searchLocation,
                                    onLocationChange = { viewModel.searchLocation.value = it },
                                    selectedClass = searchClass,
                                    onClassChange = { viewModel.searchClass.value = it },
                                    discoveredClasses = discoveredClasses,
                                    searchResults = searchResults,
                                    onSelectTag = { viewModel.selectTagForDetails(it) }
                                )
                            }

                            AppScreen.PROOFS -> {
                                ProofViewerScreen(
                                    proofs = allProofs,
                                    onSelectProof = { viewModel.selectProofForPreview(it) }
                                )
                            }

                            AppScreen.REPORTS -> {
                                ReportsScreen(
                                    metrics = metrics,
                                    classSummaries = classSummaries,
                                    executiveSummaries = executiveSummaries
                                )
                            }
                        }
                    }
                }

                // Proof Viewer Dialog
                selectedProof?.let { proof ->
                    ProofViewerDialog(
                        proof = proof,
                        onDismiss = { viewModel.selectProofForPreview(null) }
                    )
                }

                // Tag Details Sheet
                selectedTagEntity?.let { tag ->
                    TagDetailDialog(
                        tag = tag,
                        movements = selectedTagHistory,
                        onDismiss = { viewModel.selectTagForDetails(null) },
                        onViewProof = { viewModel.selectProofById(it) }
                    )
                }

                // Executive Details Sheet
                selectedExecutiveId?.let { execId ->
                    ExecutiveDetailDialog(
                        executiveId = execId,
                        db = viewModel.db,
                        onDismiss = { viewModel.selectExecutiveForDetails(null) },
                        onViewProof = { viewModel.selectProofById(it) }
                    )
                }

                // Acceptance Test Runner Dialog
                if (showAcceptanceTests) {
                    AcceptanceTestDialog(
                        repository = viewModel.repository,
                        onDismiss = { showAcceptanceTests = false }
                    )
                }
            }
        }
    }
}
}
