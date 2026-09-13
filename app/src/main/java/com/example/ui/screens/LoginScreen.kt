package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExecutiveEntity
import com.example.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    activeExecutives: List<ExecutiveEntity>,
    onLoginAsAdmin: (String, String) -> Boolean,
    onLoginAsUser: (String, String) -> Boolean
) {
    var selectedRole by remember { mutableStateOf(UserRole.USER) }
    var username by remember { mutableStateOf("EXEC-01") }
    var password by remember { mutableStateOf("user123") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current

    // Synchronize defaults when toggling role
    fun selectRole(role: UserRole) {
        selectedRole = role
        errorMessage = null
        if (role == UserRole.ADMIN) {
            username = "admin"
            password = "admin123"
        } else {
            val firstExec = activeExecutives.firstOrNull()?.executiveId ?: "EXEC-01"
            username = firstExec
            password = "user123"
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF07213A),
                        Color(0xFF0F3B5F),
                        Color(0xFF065F6F)
                    )
                )
            )
            .testTag("login_screen")
    ) {
        val isWide = maxWidth >= 840.dp

        if (isWide) {
            // Tablet / Desktop side-by-side layout matching reference
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Feature Showcase
                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .padding(end = 32.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    LoginHeaderAndFeatureTiles()
                }

                // Right Floating Auth Card
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AuthCard(
                        selectedRole = selectedRole,
                        username = username,
                        password = password,
                        passwordVisible = passwordVisible,
                        errorMessage = errorMessage,
                        activeExecutives = activeExecutives,
                        onRoleSelected = { selectRole(it) },
                        onUsernameChange = { username = it; errorMessage = null },
                        onPasswordChange = { password = it; errorMessage = null },
                        onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                        onSubmit = {
                            focusManager.clearFocus()
                            if (selectedRole == UserRole.ADMIN) {
                                val success = onLoginAsAdmin(username, password)
                                if (!success) errorMessage = "Invalid administrator credentials."
                            } else {
                                val success = onLoginAsUser(username, password)
                                if (!success) errorMessage = "Invalid subagent credentials or inactive account."
                            }
                        }
                    )
                }
            }
        } else {
            // Mobile Vertical Flow
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LoginHeaderAndFeatureTiles(compact = true)

                Spacer(modifier = Modifier.height(24.dp))

                AuthCard(
                    selectedRole = selectedRole,
                    username = username,
                    password = password,
                    passwordVisible = passwordVisible,
                    errorMessage = errorMessage,
                    activeExecutives = activeExecutives,
                    onRoleSelected = { selectRole(it) },
                    onUsernameChange = { username = it; errorMessage = null },
                    onPasswordChange = { password = it; errorMessage = null },
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    onSubmit = {
                        focusManager.clearFocus()
                        if (selectedRole == UserRole.ADMIN) {
                            val success = onLoginAsAdmin(username, password)
                            if (!success) errorMessage = "Invalid administrator credentials."
                        } else {
                            val success = onLoginAsUser(username, password)
                            if (!success) errorMessage = "Invalid subagent credentials or inactive account."
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun LoginHeaderAndFeatureTiles(compact: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (compact) Alignment.CenterHorizontally else Alignment.Start
    ) {
        // App Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.15f),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = Color(0xFF64FFDA),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "FASTag CONNECT LOGISTICS",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to FASTag CONNECT",
            color = Color.White,
            fontSize = if (compact) 24.sp else 32.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = if (compact) TextAlign.Center else TextAlign.Start
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Digital inventory register & immutable movement ledger for RFID and FASTag tracking across central hubs and field agents.",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = if (compact) TextAlign.Center else TextAlign.Start
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4 Feature highlight tiles
        val features = listOf(
            Triple(Icons.Default.Inventory2, "Multi-tier Stock Tracking", "Real-time visibility: Central, Master, Executive & Vehicle"),
            Triple(Icons.Default.DocumentScanner, "Instant Tag Photo & OCR", "Capture tags, waybills & assignment slips with camera"),
            Triple(Icons.Default.Security, "Dual-Role Governance", "Field agent tag upload portal & complete admin control"),
            Triple(Icons.Default.ReceiptLong, "Immutable Audit Ledger", "Tamper-proof audit logs with cryptographic proof links")
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            features.forEach { (icon, title, desc) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF00B4D8).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = Color(0xFF64FFDA), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text(desc, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthCard(
    selectedRole: UserRole,
    username: String,
    password: String,
    passwordVisible: Boolean,
    errorMessage: String?,
    activeExecutives: List<ExecutiveEntity>,
    onRoleSelected: (UserRole) -> Unit,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .testTag("auth_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card Brand Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E3A8A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "FASTag CONNECT",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "SECURE AUTHENTICATION PORTAL",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Segmented Role Switcher (matching screenshot)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {
                    // Field Agent / User Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedRole == UserRole.USER) Color(0xFF0D9488)
                                else Color.Transparent
                            )
                            .clickable { onRoleSelected(UserRole.USER) }
                            .testTag("tab_login_user"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = if (selectedRole == UserRole.USER) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Field Agent",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedRole == UserRole.USER) Color.White else Color(0xFF475569)
                            )
                        }
                    }

                    // Administrator Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selectedRole == UserRole.ADMIN) Color(0xFF1E3A8A)
                                else Color.Transparent
                            )
                            .clickable { onRoleSelected(UserRole.ADMIN) }
                            .testTag("tab_login_admin"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = if (selectedRole == UserRole.ADMIN) Color.White else Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Administrator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (selectedRole == UserRole.ADMIN) Color.White else Color(0xFF475569)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Role Explanation Hint
            Surface(
                color = if (selectedRole == UserRole.ADMIN) Color(0xFFEFF6FF) else Color(0xFFF0FDF4),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selectedRole == UserRole.ADMIN) Icons.Default.VerifiedUser else Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        tint = if (selectedRole == UserRole.ADMIN) Color(0xFF1E40AF) else Color(0xFF15803D),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedRole == UserRole.ADMIN)
                            "Full access: Stock operations, subagent editing, ledger & reports."
                        else
                            "Agent access: Dedicated photo capture & upload of FASTag images.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selectedRole == UserRole.ADMIN) Color(0xFF1E40AF) else Color(0xFF15803D),
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Username / ID Field
            OutlinedTextField(
                value = username,
                onValueChange = onUsernameChange,
                label = { Text(if (selectedRole == UserRole.ADMIN) "Admin Username" else "Subagent / Executive ID") },
                placeholder = { Text(if (selectedRole == UserRole.ADMIN) "e.g. admin" else "e.g. EXEC-01") },
                leadingIcon = {
                    Icon(
                        imageVector = if (selectedRole == UserRole.ADMIN) Icons.Default.Badge else Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF475569)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (selectedRole == UserRole.ADMIN) Color(0xFF1E3A8A) else Color(0xFF0D9488),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_username")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                leadingIcon = {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF475569))
                },
                trailingIcon = {
                    IconButton(onClick = onTogglePasswordVisibility) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = Color(0xFF64748B)
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (selectedRole == UserRole.ADMIN) Color(0xFF1E3A8A) else Color(0xFF0D9488),
                    unfocusedBorderColor = Color(0xFFCBD5E1),
                    focusedContainerColor = Color(0xFFF8FAFC),
                    unfocusedContainerColor = Color(0xFFF8FAFC)
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_password")
            )

            // Quick Fill chips
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Select:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )

                if (selectedRole == UserRole.ADMIN) {
                    SuggestionChip(
                        onClick = {
                            onUsernameChange("admin")
                            onPasswordChange("admin123")
                        },
                        label = { Text("⚡ Admin (admin123)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("chip_quick_admin")
                    )
                } else {
                    SuggestionChip(
                        onClick = {
                            val first = activeExecutives.firstOrNull()?.executiveId ?: "EXEC-01"
                            onUsernameChange(first)
                            onPasswordChange("user123")
                        },
                        label = { Text("⚡ Agent EXEC-01", fontSize = 11.sp) },
                        modifier = Modifier.testTag("chip_quick_user")
                    )
                }
            }

            // Error Display
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                errorMessage?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Main Submit Button
            Button(
                onClick = onSubmit,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedRole == UserRole.ADMIN) Color(0xFF1E3A8A) else Color(0xFF0D9488)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("btn_submit_login")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Login,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedRole == UserRole.ADMIN) "Login as Administrator" else "Login as Field Agent",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Protected by Role-Based Access Control System",
                fontSize = 10.5.sp,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
