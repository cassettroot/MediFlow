package com.example.mediflow

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.app.TimePickerDialog
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.mediflow.ui.theme.MediFlowTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.os.Vibrator
import android.os.VibrationEffect
import android.media.RingtoneManager
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            
            // Notification Permission Request
            var hasNotificationPermission by remember {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                } else mutableStateOf(true)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission(),
                onResult = { isGranted -> hasNotificationPermission = isGranted }
            )

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            val prefs = remember { context.getSharedPreferences("mediflow_prefs", Context.MODE_PRIVATE) }
            
            var userProfile by remember {
                mutableStateOf(UserProfile(
                    nombre = prefs.getString("user_name", "") ?: "",
                    edad = prefs.getInt("user_age", 0),
                    isRegistered = prefs.getBoolean("is_registered", false)
                ))
            }

            var themeSetting by remember { mutableStateOf(prefs.getString("theme_mode", "Sistema") ?: "Sistema") }
            var appPrimaryColorHex by remember { mutableLongStateOf(prefs.getLong("app_color", 0xFF6200EE)) }

            val isDarkTheme = when (themeSetting) {
                "Oscuro" -> true
                "Claro" -> false
                "B y N" -> isSystemInDarkTheme()
                else -> isSystemInDarkTheme()
            }

            val finalPrimaryColor = if (themeSetting == "B y N") {
                if (isDarkTheme) Color.White else Color.Black
            } else {
                Color(appPrimaryColorHex)
            }

            MediFlowTheme(
                darkTheme = isDarkTheme,
                appPrimaryColor = finalPrimaryColor,
                isPureBnW = themeSetting == "B y N"
            ) {
                if (!userProfile.isRegistered) {
                    UserRegistrationScreen { nombre, edad ->
                        userProfile = UserProfile(nombre, edad, true)
                        prefs.edit().apply {
                            putString("user_name", nombre)
                            putInt("user_age", edad)
                            putBoolean("is_registered", true)
                            apply()
                        }
                    }
                } else {
                    MediFlowApp(
                        userProfile = userProfile,
                        onUpdateProfile = { updated ->
                            userProfile = updated
                            prefs.edit().apply {
                                putString("user_name", updated.nombre)
                                putInt("user_age", updated.edad)
                                apply()
                            }
                        },
                        themeSetting = themeSetting,
                        appColor = appPrimaryColorHex,
                        onThemeChange = { 
                            themeSetting = it
                            prefs.edit().putString("theme_mode", it).apply()
                        },
                        onColorChange = {
                            appPrimaryColorHex = it
                            prefs.edit().putLong("app_color", it).apply()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun UserRegistrationScreen(onRegister: (String, Int) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡Bienvenido a MediFlow!", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        Spacer(Modifier.height(16.dp))
        Text("Por favor, ingresa tus datos para comenzar.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = nombre,
            onValueChange = { 
                if (it.all { char -> char.isLetter() || char.isWhitespace() }) nombre = it 
            },
            label = { Text("Tu nombre (solo letras)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            isError = showError != null && nombre.isBlank(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = edad,
            onValueChange = { if (it.all { char -> char.isDigit() }) edad = it },
            label = { Text("Tu edad") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(20.dp),
            isError = showError != null && (edad.isBlank() || (edad.toIntOrNull() ?: 0) > 100),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        if (showError != null) {
            Text(showError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { 
                val ageInt = edad.toIntOrNull() ?: 0
                when {
                    nombre.isBlank() -> showError = "Por favor, ingresa tu nombre."
                    !nombre.all { it.isLetter() || it.isWhitespace() } -> showError = "El nombre no puede contener caracteres especiales."
                    edad.isBlank() -> showError = "Por favor, ingresa tu edad."
                    ageInt > 100 -> showError = "La edad no puede ser mayor a 100 años."
                    ageInt <= 0 -> showError = "Por favor, ingresa una edad válida."
                    else -> onRegister(nombre, ageInt)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(50.dp)
        ) {
            Text("Comenzar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

enum class Screen {
    Inicio, Agregar, Historial, Ajustes
}

@Composable
fun MediFlowApp(
    userProfile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    themeSetting: String,
    appColor: Long,
    onThemeChange: (String) -> Unit,
    onColorChange: (Long) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mediflow_prefs", Context.MODE_PRIVATE) }
    val gson = remember { Gson() }

    val listaMedicamentos = remember {
        val json = prefs.getString("lista_medicamentos", null)
        val list = if (json != null) {
            val type = object : TypeToken<MutableList<Medicamento>>() {}.type
            gson.fromJson<MutableList<Medicamento>>(json, type)
        } else mutableListOf<Medicamento>()
        mutableStateListOf<Medicamento>().apply { addAll(list) }
    }
    
    fun safeUpdateMed(updated: Medicamento) {
        val i = listaMedicamentos.indexOfFirst { it.id == updated.id }
        if (i != -1) {
            listaMedicamentos.removeAt(i)
            listaMedicamentos.add(i, updated)
        }
    }

    val historial = remember {
        val json = prefs.getString("historial_tomas", null)
        val list = if (json != null) {
            val type = object : TypeToken<MutableList<RegistroToma>>() {}.type
            gson.fromJson<MutableList<RegistroToma>>(json, type)
        } else mutableListOf<RegistroToma>()
        mutableStateListOf<RegistroToma>().apply { addAll(list) }
    }

    val historialMedico = remember {
        val json = prefs.getString("historial_medico", null)
        val list = if (json != null) {
            val type = object : TypeToken<MutableList<MedicamentoHistorico>>() {}.type
            gson.fromJson<MutableList<MedicamentoHistorico>>(json, type)
        } else mutableListOf<MedicamentoHistorico>()
        mutableStateListOf<MedicamentoHistorico>().apply { addAll(list) }
    }

    val gruposGuardados = remember {
        val json = prefs.getString("grupos_guardados", null)
        val list = if (json != null) {
            val type = object : TypeToken<MutableList<Triple<String, Long, Long>>>() {}.type // Nombre, Color, GroupColor
            gson.fromJson<MutableList<Triple<String, Long, Long>>>(json, type)
        } else mutableListOf()
        mutableStateListOf<Triple<String, Long, Long>>().apply { addAll(list) }
    }

    val todasLasNotas = remember {
        val json = prefs.getString("todas_las_notas", null)
        val list = if (json != null) {
            val type = object : TypeToken<MutableList<Nota>>() {}.type
            gson.fromJson<MutableList<Nota>>(json, type)
        } else mutableListOf<Nota>()
        mutableStateListOf<Nota>().apply { addAll(list) }
    }

    LaunchedEffect(listaMedicamentos.toList()) {
        prefs.edit().putString("lista_medicamentos", gson.toJson(listaMedicamentos.toList())).apply()
    }
    LaunchedEffect(historial.toList()) {
        prefs.edit().putString("historial_tomas", gson.toJson(historial.toList())).apply()
    }
    LaunchedEffect(historialMedico.toList()) {
        prefs.edit().putString("historial_medico", gson.toJson(historialMedico.toList())).apply()
    }
    LaunchedEffect(gruposGuardados.toList()) {
        prefs.edit().putString("grupos_guardados", gson.toJson(gruposGuardados.toList())).apply()
    }
    LaunchedEffect(todasLasNotas.toList()) {
        prefs.edit().putString("todas_las_notas", gson.toJson(todasLasNotas.toList())).apply()
    }

    var selectedTab by remember { mutableStateOf(Screen.Inicio) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(selectedTab, appColor) { selectedTab = it }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                Screen.Inicio -> MisTomasScreen(
                    userProfile,
                    listaMedicamentos,
                    historial,
                    historialMedico,
                    todasLasNotas,
                    onNavigateToAgregar = { selectedTab = Screen.Agregar },
                    onUpdateMedicamento = { 
                        safeUpdateMed(it)
                        AlarmScheduler.scheduleAlarms(context, it)
                    }
                )
                Screen.Agregar -> AgregarMedicamentoScreen(
                    listaExistente = listaMedicamentos.toList(),
                    gruposExistentes = gruposGuardados.toList(),
                    onMedicamentoGuardado = { nuevo, registrarToma, gNombre, gColor, mColor ->
                        listaMedicamentos.add(nuevo)
                        AlarmScheduler.scheduleAlarms(context, nuevo)
                        if (registrarToma) {
                           // Lógica para registrar primera toma
                           val now = System.currentTimeMillis()
                           val updated = nuevo.copy(
                                dosisActual = 1,
                                stockRestante = nuevo.stockRestante - 1,
                                lastTakenTimestamp = now
                           )
                           safeUpdateMed(updated)
                           AlarmScheduler.scheduleAlarms(context, updated)
                           val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
                           historial.add(0, RegistroToma(
                               medicamentoId = nuevo.id,
                               nombreMedicamento = nuevo.nombre,
                               fechaHora = sdf.format(Date(now)),
                               colorHex = nuevo.colorHex,
                               isEarly = false,
                               grupoNombre = nuevo.grupo,
                               timestamp = now
                           ))
                        }
                        if (gNombre != null && !gruposGuardados.any { it.first == gNombre }) {
                            gruposGuardados.add(Triple(gNombre, mColor, gColor ?: 0xFF009688))
                        }
                        selectedTab = Screen.Inicio
                    }
                )
                Screen.Historial -> HistorialScreen(
                    listaMedicamentos = listaMedicamentos, 
                    historial = historial,
                    onUpdateMedicamento = { safeUpdateMed(it) }
                )
                Screen.Ajustes -> AjustesScreen(
                    userProfile = userProfile,
                    onUpdateProfile = onUpdateProfile,
                    themeSetting = themeSetting,
                    appColor = appColor,
                    onThemeChange = onThemeChange,
                    onColorChange = onColorChange,
                    historialMedico = historialMedico,
                    listaMedicamentos = listaMedicamentos,
                    historial = historial,
                    todasLasNotas = todasLasNotas,
                    onUpdateMedicamento = { 
                        safeUpdateMed(it)
                        AlarmScheduler.scheduleAlarms(context, it)
                    },
                    onNavigateToInicio = { selectedTab = Screen.Inicio }
                )
            }
        }
    }
}

@Composable
fun AppBottomBar(selectedTab: Screen, appColor: Long, onTabSelected: (Screen) -> Unit) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val highlightColor = Color(appColor).copy(alpha = 0.1f)
    
    Column {
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = backgroundColor,
            tonalElevation = 0.dp
        ) {
            NavigationBarItem(
                selected = selectedTab == Screen.Inicio,
                onClick = { onTabSelected(Screen.Inicio) },
                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                label = { Text("Inicio") },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = highlightColor
                )
            )
            NavigationBarItem(
                selected = selectedTab == Screen.Agregar,
                onClick = { onTabSelected(Screen.Agregar) },
                icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
                label = { Text("Agregar") },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = highlightColor
                )
            )
            NavigationBarItem(
                selected = selectedTab == Screen.Historial,
                onClick = { onTabSelected(Screen.Historial) },
                icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
                label = { Text("Historial") },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = highlightColor
                )
            )
            NavigationBarItem(
                selected = selectedTab == Screen.Ajustes,
                onClick = { onTabSelected(Screen.Ajustes) },
                icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                label = { Text("Ajustes") },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = highlightColor
                )
            )
        }
    }
}

// --- PANTALLA: MIS TOMAS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTomasScreen(
    user: UserProfile,
    lista: MutableList<Medicamento>,
    historial: MutableList<RegistroToma>,
    historialMedico: MutableList<MedicamentoHistorico>,
    todasLasNotas: MutableList<Nota>,
    onNavigateToAgregar: () -> Unit,
    onUpdateMedicamento: (Medicamento) -> Unit
) {
    var medicamentoSeleccionado by remember { mutableStateOf<Medicamento?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val saludo = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 6..12 -> "Buenos días"
        in 13..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNoteDialog = true }) {
                Icon(Icons.Default.NoteAdd, contentDescription = "Notas")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("$saludo,", fontSize = 18.sp, color = Color.Gray)
                Text(user.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
            }

            if (lista.isEmpty()) {
                EmptyState(onNavigateToAgregar)
            } else {
                val grouped = lista.groupBy { it.grupo }
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    grouped.forEach { (grupo, meds) ->
                        item {
                            val colorBorde = meds.firstOrNull()?.grupoColorHex?.let { Color(it) } ?: Color.Transparent
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(if (grupo != null) Modifier.border(2.dp, colorBorde, RoundedCornerShape(20.dp)).padding(8.dp) else Modifier)
                            ) {
                                if (grupo != null) {
                                    Text(grupo, fontWeight = FontWeight.Bold, color = colorBorde, modifier = Modifier.padding(bottom = 8.dp, start = 8.dp))
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    meds.forEach { med ->
                                        MedicamentoCard(
                                            med = med,
                                            onClick = {
                                                medicamentoSeleccionado = med
                                                showSheet = true
                                            },
                                            onRegistrar = { isEarly ->
                                                val now = System.currentTimeMillis()
                                                val updated = med.copy(
                                                    dosisActual = med.dosisActual + 1,
                                                    stockRestante = med.stockRestante - 1,
                                                    lastTakenTimestamp = now
                                                )
                                                onUpdateMedicamento(updated)

                                                if (updated.dosisActual >= updated.dosisTotal) {
                                                    val sdfFecha = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                                    historialMedico.add(MedicamentoHistorico(
                                                        nombre = updated.nombre,
                                                        fechaInicio = sdfFecha.format(Date(updated.timestampInicio)),
                                                        dosisTomadas = updated.dosisActual,
                                                        duracionDias = updated.duracionDias,
                                                        grupo = updated.grupo
                                                    ))
                                                }
                                                
                                                val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
                                                historial.add(0, RegistroToma(
                                                    medicamentoId = med.id,
                                                    nombreMedicamento = med.nombre,
                                                    fechaHora = sdf.format(Date(now)),
                                                    colorHex = med.colorHex,
                                                    isEarly = isEarly,
                                                    grupoNombre = med.grupo,
                                                    timestamp = now
                                                ))
                                            },
                                            onUndo = {
                                                if (med.dosisActual > 0) {
                                                    val previousTakes = historial.filter { it.medicamentoId == med.id && !it.isUndoRecord }
                                                    if (previousTakes.isNotEmpty()) {
                                                        val lastToma = previousTakes.first()
                                                        val newLastTakenTimestamp = if (previousTakes.size > 1) previousTakes[1].timestamp else null
                                                        
                                                        val updated = med.copy(
                                                            dosisActual = (med.dosisActual - 1).coerceAtLeast(0),
                                                            stockRestante = med.stockRestante + 1,
                                                            lastTakenTimestamp = newLastTakenTimestamp
                                                        )
                                                        onUpdateMedicamento(updated)

                                                        // Remover del historial médico si ya se había terminado
                                                        if (med.dosisActual >= med.dosisTotal) {
                                                            historialMedico.removeAll { it.nombre == med.nombre && it.grupo == med.grupo }
                                                        }

                                                        // Registrar corrección en historial
                                                        val now = System.currentTimeMillis()
                                                        val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
                                                        historial.add(0, RegistroToma(
                                                            medicamentoId = med.id,
                                                            nombreMedicamento = "CORRECCIÓN: ${med.nombre}",
                                                            fechaHora = sdf.format(Date(now)),
                                                            colorHex = med.colorHex,
                                                            isEarly = false,
                                                            grupoNombre = med.grupo,
                                                            timestamp = now,
                                                            isUndoRecord = true
                                                        ))
                                                    }
                                                }
                                            },
                                            onDelete = {
                                                lista.remove(med)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        var showFinished by remember { mutableStateOf(false) }
                        if (historialMedico.isNotEmpty()) {
                            Column {
                                ListItem(
                                    headlineContent = { Text("Tratamientos Terminados (${historialMedico.size})", fontWeight = FontWeight.Bold) },
                                    trailingContent = { 
                                        IconButton(onClick = { showFinished = !showFinished }) {
                                            Icon(if (showFinished) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                        }
                                    },
                                    modifier = Modifier.clickable { showFinished = !showFinished }
                                )
                                if (showFinished) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(8.dp)) {
                                        historialMedico.forEach { h ->
                                            Card(modifier = Modifier.fillMaxWidth()) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Text(h.nombre, fontWeight = FontWeight.Bold)
                                                    Text("Finalizado: ${h.fechaInicio}", fontSize = 12.sp, color = Color.Gray)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Button(
                            onClick = onNavigateToAgregar,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("+ Agregar medicamento extra")
                        }
                    }
                }
            }
        }
    }

    if (showSheet && medicamentoSeleccionado != null) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }, sheetState = sheetState) {
            MedicamentoDetalleContent(medicamentoSeleccionado!!)
        }
    }

    if (showNoteDialog) {
        AddNoteDialog(
            lista = lista,
            onDismiss = { showNoteDialog = false },
            onSaveNote = { categoria, medId, grupoNombre, texto ->
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val med = lista.find { it.id == medId }
                
                val nota = Nota(
                    texto = texto,
                    fechaHora = sdf.format(Date()),
                    medicamentoNombre = med?.nombre,
                    grupoNombre = grupoNombre,
                    categoria = categoria
                )
                
                todasLasNotas.add(0, nota)
                
                // También vincular al historial médico si es posible
                if (categoria == "Medicamento" && med != null) {
                    val hist = historialMedico.find { it.nombre == med.nombre && it.grupo == med.grupo }
                    hist?.notas?.add(nota)
                } else if (categoria == "Tratamiento" && grupoNombre != null) {
                    historialMedico.filter { it.grupo == grupoNombre }.forEach { it.notas.add(nota) }
                }

                val destino = when(categoria) {
                    "Tratamiento" -> "Historial Médico -> Notas -> Tratamientos"
                    "Medicamento" -> "Historial Médico -> Notas -> Medicamentos"
                    else -> "Historial Médico -> Notas -> General"
                }

                scope.launch {
                    snackbarHostState.showSnackbar("Nota guardada en: $destino")
                }
                showNoteDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteDialog(lista: List<Medicamento>, onDismiss: () -> Unit, onSaveNote: (String, UUID?, String?, String) -> Unit) {
    var selectedCategory by remember { mutableStateOf("Medicamento") }
    var selectedMedId by remember { mutableStateOf<UUID?>(null) }
    var selectedGroup by remember { mutableStateOf<String?>(null) }
    var noteText by remember { mutableStateOf("") }

    // Reset selection when category changes
    LaunchedEffect(selectedCategory) {
        selectedMedId = null
        selectedGroup = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.NoteAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("¿Qué deseas anotar?", style = MaterialTheme.typography.titleLarge)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Modified selection bar logic for better design
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val categories = listOf("Medicamento", "Tratamiento", "Otro")
                    categories.forEachIndexed { index, cat ->
                        SegmentedButton(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = categories.size),
                            icon = {}
                        ) {
                            Text(cat, fontSize = 12.sp)
                        }
                    }
                }

                val showSelectionList = (selectedCategory == "Medicamento" && selectedMedId == null) || 
                                       (selectedCategory == "Tratamiento" && selectedGroup == null)

                if (showSelectionList) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        when (selectedCategory) {
                            "Medicamento" -> {
                                if (lista.isEmpty()) {
                                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text("No hay medicamentos activos.", color = Color.Gray, fontSize = 14.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.padding(4.dp)) {
                                        items(lista) { med ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedMedId = med.id }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(Modifier.size(12.dp).background(Color(med.colorHex), CircleShape))
                                                Spacer(Modifier.width(12.dp))
                                                Column {
                                                    Text(med.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    med.grupo?.let { Text(it, fontSize = 11.sp, color = Color.Gray) }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            "Tratamiento" -> {
                                val grupos = lista.mapNotNull { it.grupo }.distinct()
                                if (grupos.isEmpty()) {
                                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text("No hay tratamientos activos.", color = Color.Gray, fontSize = 14.sp)
                                    }
                                } else {
                                    LazyColumn(modifier = Modifier.padding(4.dp)) {
                                        items(grupos) { g ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { selectedGroup = g }
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(12.dp))
                                                Text(g, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Show note input area
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (selectedCategory != "Otro") {
                            val selectedName = if (selectedCategory == "Medicamento") {
                                lista.find { it.id == selectedMedId }?.nombre ?: ""
                            } else {
                                selectedGroup ?: ""
                            }
                            
                            AssistChip(
                                onClick = { 
                                    selectedMedId = null
                                    selectedGroup = null
                                },
                                label = { Text(selectedName, fontWeight = FontWeight.Bold) },
                                leadingIcon = { 
                                    if (selectedCategory == "Medicamento") {
                                        val color = lista.find { it.id == selectedMedId }?.colorHex ?: 0xFF000000
                                        Box(Modifier.size(12.dp).background(Color(color), CircleShape))
                                    } else {
                                        Icon(Icons.Default.Label, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = "Cambiar", modifier = Modifier.size(16.dp)) }
                            )
                        } else {
                            Text("Anota cualquier síntoma o detalle general aquí debajo.", fontSize = 13.sp, color = Color.Gray)
                        }

                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Escribe tus notas...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            minLines = 3,
                            maxLines = 5
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isReady = when (selectedCategory) {
                "Medicamento" -> selectedMedId != null && noteText.isNotBlank()
                "Tratamiento" -> selectedGroup != null && noteText.isNotBlank()
                "Otro" -> noteText.isNotBlank()
                else -> false
            }
            Button(
                onClick = { 
                    if (isReady) {
                        onSaveNote(selectedCategory, selectedMedId, selectedGroup, noteText)
                    }
                },
                enabled = isReady,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50.dp)
            ) { 
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar Nota") 
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Cancelar") }
        }
    )
}

@Composable
fun MedicamentoCard(
    med: Medicamento,
    onClick: () -> Unit,
    onRegistrar: (Boolean) -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showEarlyWarningDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUndoDialog by remember { mutableStateOf(false) }
    val isLowStock = med.stockRestante <= 5

    val ticks = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while(true) {
            delay(1000)
            ticks.longValue = System.currentTimeMillis()
        }
    }
    
    val timeToNext = med.lastTakenTimestamp?.let {
        val nextMilis = it + (med.frecuenciaHoras.toLong() * 60 * 60 * 1000)
        (nextMilis - ticks.longValue).coerceAtLeast(0)
    } ?: 0L

    val nextHrs = timeToNext / (60 * 60 * 1000)
    val nextMins = (timeToNext / (60 * 1000)) % 60
    val nextSecs = (timeToNext / 1000) % 60

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.then(
            if (isLowStock) Modifier.border(2.dp, Color.Red, RoundedCornerShape(20.dp)) else Modifier
        ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(med.colorHex).copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(40.dp).background(Color(med.colorHex), CircleShape))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1.0f)) {
                    Text(med.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    if (med.dosisActual >= med.dosisTotal) {
                        Text("¡TRATAMIENTO TERMINADO!", color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    } else {
                        Text("Siguiente en: ${String.format("%02d:%02d:%02d", nextHrs, nextMins, nextSecs)}", 
                            fontWeight = FontWeight.Bold, color = if(timeToNext == 0L) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = { showUndoDialog = true }, enabled = med.dosisActual > 0) {
                    Icon(Icons.Default.Undo, contentDescription = "Deshacer", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { med.dosisActual.toFloat() / med.dosisTotal.toFloat() },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Color(med.colorHex),
                trackColor = Color(med.colorHex).copy(alpha = 0.2f),
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Stock: ${med.stockRestante}", color = if (isLowStock) Color.Red else Color.Unspecified, fontWeight = if (isLowStock) FontWeight.Bold else FontWeight.Normal)
                    Text("Dosis: ${med.dosisActual}/${med.dosisTotal}", fontSize = 14.sp)
                }
                Button(
                    onClick = { if (timeToNext > 0) showEarlyWarningDialog = true else showConfirmDialog = true },
                    enabled = med.stockRestante > 0 && med.dosisActual < med.dosisTotal,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(med.colorHex)),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Text("✓ Registrar", fontWeight = FontWeight.Medium)
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirmar Toma") },
            text = { Text("¿Deseas registrar una dosis de ${med.nombre}?") },
            confirmButton = { TextButton(onClick = { onRegistrar(false); showConfirmDialog = false }) { Text("Confirmar") } },
            dismissButton = { TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showEarlyWarningDialog) {
        val lastTaken = med.lastTakenTimestamp ?: 0L
        val diffMilis = System.currentTimeMillis() - lastTaken
        val diffMins = diffMilis / (60 * 1000)
        val diffHrs = diffMins / 60
        val remainingMins = diffMins % 60
        AlertDialog(
            onDismissRequest = { showEarlyWarningDialog = false },
            title = { Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9800))
                Spacer(Modifier.width(8.dp)); Text("Toma Anticipada")
            }},
            text = { Text("Tomaste este medicamento hace solo ${diffHrs}h ${remainingMins}min.\n\n¿Deseas registrarla de todos modos?") },
            confirmButton = { TextButton(onClick = { onRegistrar(true); showEarlyWarningDialog = false }) { Text("Registrar", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showEarlyWarningDialog = false }) { Text("Esperar") } }
        )
    }

    if (showDeleteDialog) {
        val dosisFaltantes = med.dosisTotal - med.dosisActual
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Tratamiento", color = Color.Red) },
            text = { Text("¿Estás seguro? Te faltaban $dosisFaltantes dosis.") },
            confirmButton = { TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Eliminar", color = Color.Red) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showUndoDialog) {
        AlertDialog(
            onDismissRequest = { showUndoDialog = false },
            title = { Text("Deshacer última toma") },
            text = { 
                Column {
                    Text("Esta acción revertirá los siguientes cambios:")
                    Spacer(Modifier.height(8.dp))
                    Text("• El stock aumentará en 1 unidad.")
                    Text("• El contador de dosis disminuirá.")
                    Text("• El tiempo de la siguiente toma se recalculará.")
                    Spacer(Modifier.height(16.dp))
                    Text("¿Deseas confirmar la modificación de la última toma?", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = { 
                TextButton(onClick = { onUndo(); showUndoDialog = false }) { 
                    Text("Confirmar", fontWeight = FontWeight.Bold) 
                } 
            },
            dismissButton = { 
                TextButton(onClick = { showUndoDialog = false }) { Text("Cancelar") } 
            }
        )
    }
}

@Composable
fun MedicamentoDetalleContent(med: Medicamento) {
    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().padding(bottom = 32.dp)) {
        Box(modifier = Modifier.size(60.dp).background(Color(med.colorHex), CircleShape).align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        Text(med.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
        DetailItem(label = "Descripción", value = med.descripcion.ifBlank { "Sin descripción" })
        med.grupo?.let { DetailItem(label = "Grupo", value = it) }
        DetailItem(label = "Frecuencia", value = "Cada ${med.frecuenciaHoras} horas")
        DetailItem(label = "Duración", value = "${med.duracionDias} días")
        DetailItem(label = "Progreso", value = "${med.dosisActual} de ${med.dosisTotal} dosis")
        DetailItem(label = "Stock", value = "${med.stockRestante} unidades restantes")
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(label, fontWeight = FontWeight.Medium, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontSize = 18.sp)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
    }
}

@Composable
fun EmptyState(onNavigateToAgregar: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("💊", fontSize = 80.sp); Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToAgregar, shape = RoundedCornerShape(50.dp)) { Text("Configurar primer medicamento", fontWeight = FontWeight.Bold) }
    }
}

// --- PANTALLA: AGREGAR ---

@Composable
fun AgregarMedicamentoScreen(
    listaExistente: List<Medicamento>, 
    gruposExistentes: List<Triple<String, Long, Long>>, // Nombre, Color, GroupColor
    onMedicamentoGuardado: (Medicamento, Boolean, String?, Long?, Long) -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var duracionDias by remember { mutableStateOf("1") }
    var frecuenciaHoras by remember { mutableStateOf("8") }
    var stockInicial by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    
    var isGroupEnabled by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var registrarPrimeraToma by remember { mutableStateOf(false) }

    var isCustomDuration by remember { mutableStateOf(false) }
    
    // Nueva lógica de selección de tiempo
    var frequencyMode by remember { mutableStateOf(true) } // true: Frecuencia, false: Momento Recomendado
    var isCustomFrecuencia by remember { mutableStateOf(false) }
    var horaFijaSeleccionada by remember { mutableStateOf<String?>(null) }
    
    var showExistenteDialog by remember { mutableStateOf(false) }
    var showGrupoDialog by remember { mutableStateOf(false) }

    // Alertas de validación
    var alertMessage by remember { mutableStateOf<String?>(null) }
    var showConfirmStockDialog by remember { mutableStateOf<Int?>(null) }
    var showConfirmDurationDialog by remember { mutableStateOf<Int?>(null) }
    var pendingMedToSave by remember { mutableStateOf<Medicamento?>(null) }

    // Colores ampliados y filtrado
    val allColors = remember {
        listOf(
            0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 
            0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50, 
            0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 
            0xFFFF5722, 0xFF795548, 0xFF9E9E92, 0xFF607D8B, 0xFF000000,
            0xFF1A237E, 0xFF004D40, 0xFF33691E, 0xFFF57F17, 0xFFE65100,
            0xFF3E2723, 0xFF212121, 0xFF4A148C, 0xFF880E4F, 0xFFB71C1C
        )
    }
    
    val usedColors = remember(listaExistente, gruposExistentes) {
        val colors = listaExistente.map { it.colorHex }.toMutableSet()
        colors.addAll(listaExistente.mapNotNull { it.grupoColorHex })
        colors.addAll(gruposExistentes.map { it.third })
        colors
    }

    var selectedColor by remember(usedColors) { 
        mutableLongStateOf(allColors.firstOrNull { it !in usedColors } ?: allColors.first()) 
    }
    
    val availableColors = remember(usedColors, selectedColor) {
        allColors.filter { it !in usedColors || it == selectedColor }
    }

    val availableGroupColors = remember(usedColors) {
        allColors.filter { it !in usedColors }.take(10).ifEmpty { allColors.take(10) }
    }
    var groupColor by remember(availableGroupColors) { 
        mutableLongStateOf(availableGroupColors.first())
    }

    val duracionesPredefinidas = listOf("1" to "Un día", "3" to "Tres días", "7" to "Una semana", "30" to "Un mes", "365" to "Diario")
    val frecuenciasPredefinidas = listOf("4", "6", "8", "12", "24")
    val momentosEspeciales = listOf("Desayuno (7am)", "Comida (2pm)", "Cena (10pm)", "Hora exacta")

    LazyColumn(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("Nuevo Tratamiento", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp) }
        
        item {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = nombre, onValueChange = { nombre = it }, label = { Text("Nombre del fármaco") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp))
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showExistenteDialog = true }) { Icon(Icons.Default.Search, contentDescription = "Previos") }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("¿Agrupar en tratamiento?", fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Switch(checked = isGroupEnabled, onCheckedChange = { isGroupEnabled = it })
            }
        }

        if (isGroupEnabled) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = groupName, 
                        onValueChange = { groupName = it }, 
                        label = { Text("Nombre del Grupo/Tratamiento") }, 
                        modifier = Modifier.weight(1f), 
                        shape = RoundedCornerShape(20.dp),
                        leadingIcon = { Icon(Icons.Default.Label, contentDescription = null) }
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = { showGrupoDialog = true }) { Icon(Icons.Default.List, contentDescription = "Grupos") }
                }
            }
            item {
                Text("Color del Grupo (disponibles):", fontWeight = FontWeight.Medium)
                LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.height(100.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableGroupColors) { color ->
                        Box(modifier = Modifier.size(36.dp).background(Color(color), CircleShape).border(width = if (groupColor == color) 3.dp else 0.dp, color = Color.Black, shape = CircleShape).clickable { groupColor = color })
                    }
                }
            }
        }

        item {
            OutlinedTextField(value = descripcion, onValueChange = { descripcion = it }, label = { Text("Descripción (opcional)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp))
        }

        item {
            Text("Duración del tratamiento:", fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Primera fila de opciones rápidas
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val part1 = duracionesPredefinidas.take(3)
                    part1.forEach { (dias, label) ->
                        FilterChip(
                            selected = duracionDias == dias && !isCustomDuration,
                            onClick = { duracionDias = dias; isCustomDuration = false },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                // Segunda fila de opciones rápidas + Personalizado
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val part2 = duracionesPredefinidas.drop(3)
                    part2.forEach { (dias, label) ->
                        FilterChip(
                            selected = duracionDias == dias && !isCustomDuration,
                            onClick = { duracionDias = dias; isCustomDuration = false },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    FilterChip(
                        selected = isCustomDuration,
                        onClick = { isCustomDuration = true },
                        label = { Text("Personalizado", fontSize = 12.sp) },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
            if (isCustomDuration) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = duracionDias,
                    onValueChange = { if (it.all { c -> c.isDigit() }) duracionDias = it },
                    label = { Text("Número de días") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(20.dp),
                    trailingIcon = { Text("días ", color = Color.Gray) }
                )
            }
        }

        item {
            OutlinedTextField(value = stockInicial, onValueChange = { if (it.all { c -> c.isDigit() }) stockInicial = it }, label = { Text("Stock inicial (pastillas)") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(20.dp))
        }

        item {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { frequencyMode = true },
                        modifier = Modifier.weight(1f),
                        colors = if (frequencyMode) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("Frecuencia") }
                    Button(
                        onClick = { frequencyMode = false },
                        modifier = Modifier.weight(1f),
                        colors = if (!frequencyMode) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors(),
                        shape = RoundedCornerShape(20.dp)
                    ) { Text("Momento") }
                }
                
                Spacer(Modifier.height(12.dp))
                
                if (frequencyMode) {
                    Text("Cada cuántas horas:", fontWeight = FontWeight.Medium)
                    LazyVerticalGrid(columns = GridCells.Adaptive(75.dp), modifier = Modifier.height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(frecuenciasPredefinidas) { hrs ->
                            FilterChip(selected = frecuenciaHoras == hrs && !isCustomFrecuencia, onClick = { frecuenciaHoras = hrs; isCustomFrecuencia = false; horaFijaSeleccionada = null }, label = { Text("${hrs}h") })
                        }
                        item { FilterChip(selected = isCustomFrecuencia, onClick = { isCustomFrecuencia = true; horaFijaSeleccionada = null }, label = { Text("Otro") }) }
                    }
                    if (isCustomFrecuencia) {
                        OutlinedTextField(value = frecuenciaHoras, onValueChange = { if (it.all { c -> c.isDigit() }) frecuenciaHoras = it }, label = { Text("Horas") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(20.dp))
                    }
                } else {
                    val context = LocalContext.current
                    Text("Momento recomendado:", fontWeight = FontWeight.Medium)
                    LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), modifier = Modifier.height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(momentosEspeciales) { momento ->
                            FilterChip(
                                selected = if (momento == "Hora exacta") horaFijaSeleccionada?.startsWith("Hora:") == true else horaFijaSeleccionada == momento, 
                                onClick = { 
                                    if (momento == "Hora exacta") {
                                        val cal = Calendar.getInstance()
                                        TimePickerDialog(context, { _, h, m ->
                                            horaFijaSeleccionada = String.format(Locale.getDefault(), "Hora: %02d:%02d", h, m)
                                            isCustomFrecuencia = false
                                            frecuenciaHoras = "24"
                                        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
                                    } else {
                                        horaFijaSeleccionada = momento
                                        isCustomFrecuencia = false
                                        frecuenciaHoras = "24" 
                                    }
                                }, 
                                label = { Text(if (momento == "Hora exacta" && horaFijaSeleccionada?.startsWith("Hora:") == true) horaFijaSeleccionada!! else momento) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Color del Medicamento (disponibles):", fontWeight = FontWeight.Medium)
            LazyVerticalGrid(columns = GridCells.Fixed(5), modifier = Modifier.height(150.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableColors) { color ->
                    Box(modifier = Modifier.size(36.dp).background(Color(color), CircleShape).border(width = if (selectedColor == color) 3.dp else 0.dp, color = Color.Black, shape = CircleShape).clickable { selectedColor = color })
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = registrarPrimeraToma, onCheckedChange = { registrarPrimeraToma = it })
                Text("Registrar primera toma ahora")
            }
        }

        item {
            Button(
                onClick = {
                    val dDias = duracionDias.toIntOrNull() ?: 0
                    val fHoras = frecuenciaHoras.toIntOrNull() ?: 1
                    val sInicial = stockInicial.toIntOrNull() ?: 0
                    
                    val missingParams = mutableListOf<String>()
                    if (nombre.isBlank()) missingParams.add("Nombre")
                    if (stockInicial.isBlank()) missingParams.add("Stock")
                    if (duracionDias.isBlank()) missingParams.add("Duración")
                    
                    if (missingParams.isNotEmpty()) {
                        alertMessage = "Por favor, completa los siguientes campos: ${missingParams.joinToString(", ")}"
                        return@Button
                    }

                    if (!nombre.all { it.isLetterOrDigit() || it.isWhitespace() }) {
                        alertMessage = "El nombre no puede contener caracteres especiales."
                        return@Button
                    }

                    if (listaExistente.any { it.nombre.equals(nombre, true) }) {
                        alertMessage = "Este medicamento ya está registrado y activo. Ve a Ajustes para editarlo si es necesario."
                        return@Button
                    }

                    if (sInicial <= 0) {
                        alertMessage = "El stock inicial debe ser mayor a 0."
                        return@Button
                    }

                    if (dDias <= 0) {
                        alertMessage = "La duración debe ser mayor a 0 días."
                        return@Button
                    }

                    if (fHoras <= 0 || fHoras > 24) {
                        alertMessage = "La frecuencia debe ser entre 1 y 24 horas."
                        return@Button
                    }

                    val m = Medicamento(
                        nombre = nombre,
                        dosisTotal = dDias * (24 / fHoras),
                        stockInicial = sInicial,
                        stockRestante = sInicial,
                        colorHex = selectedColor,
                        descripcion = descripcion,
                        duracionDias = dDias,
                        frecuenciaHoras = fHoras,
                        grupo = if (isGroupEnabled) groupName.ifBlank { null } else null,
                        grupoColorHex = if (isGroupEnabled) groupColor else null,
                        horaFija = if (!frequencyMode) horaFijaSeleccionada else null
                    )

                    pendingMedToSave = m
                    
                    if (sInicial >= 299) {
                        showConfirmStockDialog = sInicial
                    } else if (dDias > 90) {
                        showConfirmDurationDialog = dDias
                    } else {
                        onMedicamentoGuardado(m, registrarPrimeraToma, m.grupo, m.grupoColorHex, m.colorHex)
                        pendingMedToSave = null
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColor))
            ) { Text("Guardar", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        }
    }

    if (alertMessage != null) {
        AlertDialog(
            onDismissRequest = { alertMessage = null },
            title = { Text("Parámetros a editar") },
            text = { Text(alertMessage!!) },
            confirmButton = { TextButton(onClick = { alertMessage = null }) { Text("Corregir") } }
        )
    }

    if (showConfirmStockDialog != null && pendingMedToSave != null) {
        AlertDialog(
            onDismissRequest = { showConfirmStockDialog = null },
            title = { Text("Confirmar Stock") },
            text = { Text("Has ingresado un stock de $showConfirmStockDialog piezas. ¿Deseas confirmar este slot y stock elevado?") },
            confirmButton = { 
                TextButton(onClick = { 
                    val m = pendingMedToSave!!
                    if (m.duracionDias > 90) {
                        showConfirmDurationDialog = m.duracionDias
                    } else {
                        onMedicamentoGuardado(m, registrarPrimeraToma, m.grupo, m.grupoColorHex, m.colorHex)
                        pendingMedToSave = null
                    }
                    showConfirmStockDialog = null 
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showConfirmStockDialog = null }) { Text("Cancelar") } }
        )
    }

    if (showConfirmDurationDialog != null && pendingMedToSave != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDurationDialog = null },
            title = { Text("Tratamiento Prolongado") },
            text = { Text("La duración de $showConfirmDurationDialog días supera un tratamiento normal. ¿Deseas guardarlo de todos modos?") },
            confirmButton = { 
                TextButton(onClick = { 
                    val m = pendingMedToSave!!
                    onMedicamentoGuardado(m, registrarPrimeraToma, m.grupo, m.grupoColorHex, m.colorHex)
                    pendingMedToSave = null
                    showConfirmDurationDialog = null 
                }) { Text("Confirmar") }
            },
            dismissButton = { TextButton(onClick = { showConfirmDurationDialog = null }) { Text("Cancelar") } }
        )
    }

    if (showExistenteDialog) {
        AlertDialog(onDismissRequest = { showExistenteDialog = false }, title = { Text("Medicamentos previos") }, text = {
            val unique = (listaExistente.map { it.nombre }).distinct()
            if (unique.isEmpty()) {
                Text("No hay medicamentos previos guardados.", color = Color.Gray)
            } else {
                LazyColumn { items(unique) { name -> 
                    Text(name, modifier = Modifier.fillMaxWidth().clickable { 
                        nombre = name
                        listaExistente.find { it.nombre == name }?.let { descripcion = it.descripcion; selectedColor = it.colorHex }
                        showExistenteDialog = false
                    }.padding(12.dp))
                }}
            }
        }, confirmButton = {})
    }

    if (showGrupoDialog) {
        AlertDialog(onDismissRequest = { showGrupoDialog = false }, title = { Text("Grupos guardados") }, text = {
            if (gruposExistentes.isEmpty()) {
                Text("No hay grupos guardados.", color = Color.Gray)
            } else {
                LazyColumn { items(gruposExistentes) { (gName, mColor, gColor) ->
                    Text(gName, modifier = Modifier.fillMaxWidth().clickable {
                        groupName = gName
                        groupColor = gColor
                        selectedColor = mColor
                        showGrupoDialog = false
                    }.padding(12.dp))
                }}
            }
        }, confirmButton = {})
    }
}

// --- PANTALLA: HISTORIAL ---

@Composable
fun HistorialScreen(listaMedicamentos: List<Medicamento>, historial: List<RegistroToma>, onUpdateMedicamento: (Medicamento) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    // Estados para las alertas de stock
    var showDialog1 by remember { mutableStateOf<Medicamento?>(null) }
    var showDialog2 by remember { mutableStateOf<Medicamento?>(null) }
    var showDialog3 by remember { mutableStateOf<Medicamento?>(null) }
    var pendingAmount by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Tomadas", modifier = Modifier.padding(16.dp)) }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Pendientes", modifier = Modifier.padding(16.dp)) }
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }) { Text("Stock", modifier = Modifier.padding(16.dp)) }
        }
        when (selectedTab) {
            0 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (historial.isEmpty()) { item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Sin registros de tomas", color = Color.Gray) } } }
                items(historial) { registro ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = if(registro.isUndoRecord) Color.Gray.copy(alpha = 0.1f) else Color(registro.colorHex).copy(alpha = 0.05f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            val icon = when {
                                registro.isUndoRecord -> Icons.Default.History
                                registro.isEarly -> Icons.Default.Help
                                else -> Icons.Default.CheckCircle
                            }
                            val tint = when {
                                registro.isUndoRecord -> Color.Gray
                                registro.isEarly -> Color(0xFFFF9800)
                                else -> Color(registro.colorHex)
                            }
                            Icon(icon, null, tint = tint)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(registro.nombreMedicamento, fontWeight = FontWeight.Bold, color = if(registro.isUndoRecord) Color.Gray else Color.Unspecified)
                                Row {
                                    Text(registro.fechaHora, fontSize = 12.sp, color = Color.Gray)
                                    if (registro.isEarly && !registro.isUndoRecord) Text(" (Antes)", fontSize = 11.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                    if (registro.isUndoRecord) Text(" (Toma Modificada)", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            1 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val pendientes = listaMedicamentos.filter { it.dosisActual < it.dosisTotal }
                if (pendientes.isEmpty()) { item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Sin dosis pendientes", color = Color.Gray) } } }
                items(pendientes) { med ->
                    val rest = med.dosisTotal - med.dosisActual
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, null, tint = Color(med.colorHex))
                            Spacer(Modifier.width(12.dp))
                            Column { Text(med.nombre, fontWeight = FontWeight.Bold); Text("Restan $rest dosis", fontSize = 12.sp, color = Color.Gray) }
                        }
                    }
                }
            }
            2 -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (listaMedicamentos.isEmpty()) { item { Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { Text("Sin medicamentos configurados", color = Color.Gray) } } }
                items(listaMedicamentos) { med ->
                    var showEditStock by remember { mutableStateOf(false) }; var amountText by remember { mutableStateOf("") }
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Inventory, null, tint = Color(med.colorHex))
                                Spacer(Modifier.width(12.dp)); Text(med.nombre, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f))
                                IconButton(onClick = { showEditStock = !showEditStock }) { Icon(Icons.Default.Edit, null, tint = Color(med.colorHex)) }
                            }
                            Text("Stock actual: ${med.stockRestante}", fontWeight = FontWeight.Medium)
                            if (showEditStock) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                                    OutlinedTextField(
                                        value = amountText, 
                                        onValueChange = { if (it.isEmpty() || it == "-" || it.all { c -> c.isDigit() || c == '-' }) amountText = it }, 
                                        label = { Text("Nueva cantidad (+ o -)") }, 
                                        modifier = Modifier.weight(1f), 
                                        shape = RoundedCornerShape(15.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Button(onClick = {
                                        val c = amountText.toIntOrNull() ?: 0
                                        if (c != 0) { 
                                            pendingAmount = c
                                            showDialog1 = med
                                            amountText = ""
                                            showEditStock = false
                                        }
                                    }) { Text("Modificar") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Lógica de diálogos secuenciales
    if (showDialog1 != null) {
        AlertDialog(
            onDismissRequest = { showDialog1 = null },
            title = { Text("Aviso de Seguridad") },
            text = { Text("Modificar este campo puede cambiar y afectar cualquier tratamiento activo o futuro.") },
            confirmButton = { TextButton(onClick = { 
                val med = showDialog1; showDialog1 = null; showDialog2 = med 
            }) { Text("Aceptar") } }
        )
    }

    if (showDialog2 != null) {
        AlertDialog(
            onDismissRequest = { showDialog2 = null },
            title = { Text("Confirmar Cambio") },
            text = { Text("¿Está seguro de este cambio? Puede afectar los tratamientos.") },
            confirmButton = { 
                TextButton(onClick = { 
                    val med = showDialog2!!
                    showDialog2 = null
                    if (pendingAmount > 400) {
                        showDialog3 = med
                    } else {
                        onUpdateMedicamento(med.copy(stockRestante = (med.stockRestante + pendingAmount).coerceAtLeast(0)))
                    }
                }) { Text("Sí") }
            },
            dismissButton = { TextButton(onClick = { showDialog2 = null }) { Text("No") } }
        )
    }

    if (showDialog3 != null) {
        AlertDialog(
            onDismissRequest = { showDialog3 = null },
            title = { Text("Stock Elevado") },
            text = { Text("Este campo supera el stock normal en un tratamiento médico normal. ¿Confirmar?") },
            confirmButton = { 
                TextButton(onClick = { 
                    val med = showDialog3!!
                    onUpdateMedicamento(med.copy(stockRestante = (med.stockRestante + pendingAmount).coerceAtLeast(0)))
                    showDialog3 = null
                }) { Text("Sí") }
            },
            dismissButton = { TextButton(onClick = { showDialog3 = null }) { Text("No") } }
        )
    }
}

// --- PANTALLA: AJUSTES ---

@Composable
fun AjustesScreen(
    userProfile: UserProfile,
    onUpdateProfile: (UserProfile) -> Unit,
    themeSetting: String,
    appColor: Long,
    onThemeChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    historialMedico: MutableList<MedicamentoHistorico>,
    listaMedicamentos: MutableList<Medicamento>,
    historial: MutableList<RegistroToma>,
    todasLasNotas: MutableList<Nota>,
    onUpdateMedicamento: (Medicamento) -> Unit,
    onNavigateToInicio: () -> Unit
) {
    var subMenu by remember { mutableStateOf<String?>(null) }
    var secretClickCount by remember { mutableIntStateOf(0) }
    var isTestMenuVisible by remember { mutableStateOf(false) }

    when (subMenu) {
        "Perfil" -> PerfilSubScreen(userProfile, onUpdateProfile) { subMenu = null }
        "Tema" -> TemaSubScreen(themeSetting, appColor, onThemeChange, onColorChange) { subMenu = null }
        "Notificaciones" -> NotificacionesSubScreen { subMenu = null }
        "Test" -> TestSubScreen { subMenu = null }
        "HistorialMedico" -> HistorialMedicoSubScreen(historialMedico, listaMedicamentos, historial, todasLasNotas, onUpdateMedicamento, onNavigateToInicio) { subMenu = null }
        else -> Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("Configuración", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp); Spacer(Modifier.height(32.dp))
            AjusteItem("Perfil", "Nombre, Edad", Icons.Default.Person) { subMenu = "Perfil" }
            AjusteItem("Tema y Color", "Visualización de la app", Icons.Default.Palette) { subMenu = "Tema" }
            AjusteItem("Notificaciones", "Sonido, Vibración y Recordatorios", Icons.Default.Notifications) { subMenu = "Notificaciones" }
            AjusteItem("Historial Médico", "Tratamientos previos y notas", Icons.Default.MedicalServices) { subMenu = "HistorialMedico" }
            
            if (isTestMenuVisible) {
                AjusteItem("Test de Funciones", "Prueba sonido y vibración", Icons.Default.BugReport) { subMenu = "Test" }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "MediFlow v1.5", 
                color = Color.Gray, 
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        secretClickCount++
                        if (secretClickCount >= 4) {
                            isTestMenuVisible = true
                        }
                    }
            )
        }
    }
}

@Composable
fun AjusteItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    ListItem(headlineContent = { Text(title, fontWeight = FontWeight.Bold) }, supportingContent = { Text(subtitle) }, leadingContent = { Icon(icon, null) }, modifier = Modifier.clickable { onClick() })
    HorizontalDivider()
}

@Composable
fun PerfilSubScreen(user: UserProfile, onUpdate: (UserProfile) -> Unit, onBack: () -> Unit) {
    var name by remember { mutableStateOf(user.nombre) }
    var age by remember { mutableStateOf(user.edad.toString()) }
    var showError by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Perfil", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
        }
        Spacer(Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { 
                if (it.all { char -> char.isLetter() || char.isWhitespace() }) name = it 
            },
            label = { Text("Nombre (solo letras)") },
            modifier = Modifier.fillMaxWidth(),
            isError = showError != null && name.isBlank()
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = age,
            onValueChange = { if (it.all { char -> char.isDigit() }) age = it },
            label = { Text("Edad") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = showError != null && (age.isBlank() || (age.toIntOrNull() ?: 0) > 100)
        )
        
        if (showError != null) {
            Text(showError!!, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { 
                val ageInt = age.toIntOrNull() ?: 0
                when {
                    name.isBlank() -> showError = "El nombre no puede estar vacío."
                    !name.all { it.isLetter() || it.isWhitespace() } -> showError = "El nombre no puede contener caracteres especiales."
                    age.isBlank() -> showError = "La edad no puede estar vacía."
                    ageInt > 100 -> showError = "La edad no puede ser mayor a 100 años."
                    ageInt <= 0 -> showError = "Ingresa una edad válida."
                    else -> {
                        onUpdate(user.copy(nombre = name, edad = ageInt))
                        onBack()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar Cambios") }
    }
}

@Composable
fun NotificacionesSubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("mediflow_prefs", Context.MODE_PRIVATE) }
    
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("notif_sound", true)) }
    var vibrateEnabled by remember { mutableStateOf(prefs.getBoolean("notif_vibrate", true)) }
    var reminderTime by remember { mutableIntStateOf(prefs.getInt("reminder_time", 15)) }
    
    val reminderOptions = listOf(5, 10, 15, 30)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Notificaciones", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
        }
        Spacer(Modifier.height(24.dp))

        ListItem(
            headlineContent = { Text("Sonido de alarma") },
            trailingContent = {
                Switch(checked = soundEnabled, onCheckedChange = { 
                    soundEnabled = it
                    prefs.edit().putBoolean("notif_sound", it).apply()
                })
            }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Vibración") },
            trailingContent = {
                Switch(checked = vibrateEnabled, onCheckedChange = { 
                    vibrateEnabled = it
                    prefs.edit().putBoolean("notif_vibrate", it).apply()
                })
            }
        )
        HorizontalDivider()

        Spacer(Modifier.height(16.dp))
        Text("Recordatorio previo:", fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reminderOptions.forEach { mins ->
                FilterChip(
                    selected = reminderTime == mins,
                    onClick = { 
                        reminderTime = mins
                        prefs.edit().putInt("reminder_time", mins).apply()
                    },
                    label = { Text("${mins} min") }
                )
            }
        }
    }
}

@Composable
fun TestSubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Test de Funciones", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    putExtra("MED_NAME", "Test Medicamento")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(alarmIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Alarm, null)
            Spacer(Modifier.width(8.dp))
            Text("Probar Pantalla de Alarma")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("MED_NAME", "Test Recordatorio")
                    putExtra("IS_REMINDER", true)
                    putExtra("TIME_LEFT", "15:00")
                }
                context.sendBroadcast(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Notifications, null)
            Spacer(Modifier.width(8.dp))
            Text("Probar Notificación (Tiempo restante)")
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 1000, 500, 1000), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 1000, 500, 1000), -1)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Vibration, null)
            Spacer(Modifier.width(8.dp))
            Text("Probar Vibración (Fuerte)")
        }
        
        Spacer(Modifier.height(32.dp))
        Text("Nota: Estos tests simulan el comportamiento real de la aplicación.", 
            fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
fun TemaSubScreen(current: String, color: Long, onTheme: (String) -> Unit, onColor: (Long) -> Unit, onBack: () -> Unit) {
    val opts = listOf("Sistema", "Claro", "Oscuro", "B y N")
    val colors = listOf(
        // Brillantes / Vibrantes
        0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5, 
        0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50, 
        0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 
        0xFFFF5722,
        // Oscuros / Profundos
        0xFFB71C1C, 0xFF880E4F, 0xFF4A148C, 0xFF311B92, 0xFF1A237E,
        0xFF0D47A1, 0xFF01579B, 0xFF006064, 0xFF004D40, 0xFF1B5E20,
        0xFF33691E, 0xFF827717, 0xFFF57F17, 0xFFFF6F00, 0xFFE65100,
        0xFF3E2723, 0xFF212121, 0xFF263238
    )
    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { 
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
            Text("Tema y Color", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
        }
        Spacer(Modifier.height(24.dp))
        Text("Tema:", fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { 
            opts.forEach { FilterChip(selected = current == it, onClick = { onTheme(it) }, label = { Text(it, fontSize = 12.sp) }) } 
        }
        Spacer(Modifier.height(24.dp))
        Text("Color de acento:", fontWeight = FontWeight.Medium) 

        if (current == "B y N") {
            Text("Personalización de color deshabilitada en modo B y N", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(250.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colors) { c ->
                    val isSelected = color == c
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(
                                width = if (isSelected) 4.dp else 0.dp,
                                color = MaterialTheme.colorScheme.onSurface,
                                shape = CircleShape
                            )
                            .clickable { onColor(c) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = if (Color(c).luminance() > 0.5) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistorialMedicoSubScreen(
    hist: MutableList<MedicamentoHistorico>, 
    activeMeds: MutableList<Medicamento>, 
    historialTomas: List<RegistroToma>,
    todasLasNotas: List<Nota>,
    onUpdate: (Medicamento) -> Unit,
    onNavigateToInicio: () -> Unit,
    onBack: () -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    var selectedTreatment by remember { mutableStateOf<String?>(null) }
    var showEmptyWindow by remember { mutableStateOf(false) }

    val isEmpty = (activeMeds.isEmpty() && hist.isEmpty() && todasLasNotas.isEmpty())

    if (showEmptyWindow || (isEmpty && selectedTreatment == null)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Sin datos", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onNavigateToInicio() }) { Text("Regresar al Inicio") }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) { 
            IconButton(onClick = { if (selectedTreatment != null) selectedTreatment = null else onBack() }) { 
                Icon(Icons.Default.ArrowBack, null) 
            }
            Text(if (selectedTreatment != null) "Detalle: $selectedTreatment" else "Historial Médico", fontWeight = FontWeight.Bold, fontSize = 20.sp) 
        }

        if (selectedTreatment == null) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }) { Text("Tratamientos", modifier = Modifier.padding(16.dp)) }
                Tab(selected = tab == 1, onClick = { tab = 1 }) { Text("Pastillas", modifier = Modifier.padding(16.dp)) }
                Tab(selected = tab == 2, onClick = { tab = 2 }) { Text("Notas", modifier = Modifier.padding(16.dp)) }
            }
            
            when(tab) {
                0 -> {
                    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item { Text("Activos", fontWeight = FontWeight.Bold) }
                        val activeGroups = activeMeds.groupBy { it.grupo ?: "Individuales" }
                        if (activeGroups.isEmpty()) { item { Text("Sin tratamientos activos", fontSize = 12.sp, color = Color.Gray) } }
                        activeGroups.forEach { (grupo, meds) ->
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedTreatment = grupo }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(grupo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text("${meds.size} medicamentos activos", fontSize = 12.sp)
                                        }
                                        IconButton(onClick = { 
                                            activeMeds.removeAll { it.grupo == grupo || (grupo == "Individuales" && it.grupo == null) }
                                        }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                    }
                                }
                            }
                        }

                        item { Spacer(Modifier.height(16.dp)); Text("Terminados / Previos", fontWeight = FontWeight.Bold) }
                        val histGroups = hist.groupBy { it.grupo ?: "Finalizados" }
                        if (histGroups.isEmpty()) {
                            item { Text("Sin tratamientos terminados", fontSize = 12.sp, color = Color.Gray) }
                        }
                        histGroups.forEach { (grupo, items) ->
                            item {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Row(modifier = Modifier.fillMaxWidth().clickable { selectedTreatment = grupo }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(grupo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            Text("${items.size} registros finalizados", fontSize = 12.sp)
                                        }
                                        IconButton(onClick = { 
                                            hist.removeAll { it.grupo == grupo || (grupo == "Finalizados" && it.grupo == null) }
                                        }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (activeMeds.isEmpty()) { item { Text("Sin pastillas configuradas", color = Color.Gray) } }
                        items(activeMeds) { med ->
                            var name by remember { mutableStateOf(med.nombre) }; var desc by remember { mutableStateOf(med.descripcion) }; var edit by remember { mutableStateOf(false) }
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    if (edit) {
                                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
                                        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Descripción") })
                                        Button(onClick = { onUpdate(med.copy(nombre = name, descripcion = desc)); edit = false }) { Text("Guardar") }
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(med.nombre, fontWeight = FontWeight.Bold)
                                            Spacer(Modifier.weight(1f))
                                            IconButton(onClick = { edit = true }) { Icon(Icons.Default.Edit, null) }
                                            IconButton(onClick = { activeMeds.remove(med) }) { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                                        }
                                        Text(med.descripcion)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    var noteSubTab by remember { mutableStateOf("General") }
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("General", "Pastillas", "Tratamientos").forEach { ncat ->
                                FilterChip(
                                    selected = noteSubTab == ncat,
                                    onClick = { noteSubTab = ncat },
                                    label = { Text(ncat) }
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        val filteredNotes = when(noteSubTab) {
                            "General" -> todasLasNotas.filter { it.categoria == "Otro" }
                            "Pastillas" -> todasLasNotas.filter { it.categoria == "Medicamento" }
                            else -> todasLasNotas.filter { it.categoria == "Tratamiento" }
                        }
                        
                        if (filteredNotes.isEmpty()) {
                            Text("Sin datos", color = Color.Gray, modifier = Modifier.padding(16.dp))
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(filteredNotes) { nota ->
                                    Card(modifier = Modifier.fillMaxWidth()) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(nota.fechaHora, fontSize = 10.sp, color = Color.Gray)
                                            Text(nota.texto, fontWeight = FontWeight.Medium)
                                            if (nota.medicamentoNombre != null) Text("Medicamento: ${nota.medicamentoNombre}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            if (nota.grupoNombre != null) Text("Tratamiento: ${nota.grupoNombre}", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Detalle del Tratamiento
            val currentGroup = selectedTreatment
            val medsInGroup = activeMeds.filter { it.grupo == currentGroup || (currentGroup == "Individuales" && it.grupo == null) }
            val histInGroup = hist.filter { it.grupo == currentGroup || (currentGroup == "Finalizados" && it.grupo == null) }
            val tomasInGroup = historialTomas.filter { it.grupoNombre == currentGroup || (currentGroup == "Individuales" && it.grupoNombre == null) }
            val notasInGroup = todasLasNotas.filter { it.grupoNombre == currentGroup && it.categoria == "Tratamiento" }

            LazyColumn(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("Medicamentos", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                items(medsInGroup) { med ->
                    var isEditing by remember { mutableStateOf(false) }
                    var editNombre by remember { mutableStateOf(med.nombre) }
                    var editDesc by remember { mutableStateOf(med.descripcion) }
                    var editFrecuencia by remember { mutableStateOf(med.frecuenciaHoras.toString()) }
                    var editGrupo by remember { mutableStateOf(med.grupo ?: "") }

                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (isEditing) {
                                OutlinedTextField(value = editNombre, onValueChange = { editNombre = it }, label = { Text("Nombre") })
                                OutlinedTextField(value = editDesc, onValueChange = { editDesc = it }, label = { Text("Descripción") })
                                OutlinedTextField(
                                    value = editFrecuencia, 
                                    onValueChange = { if (it.all { c -> c.isDigit() }) editFrecuencia = it }, 
                                    label = { Text("Cada cuántas horas") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                OutlinedTextField(value = editGrupo, onValueChange = { editGrupo = it }, label = { Text("Grupo / Tratamiento") })
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                                    Button(onClick = {
                                        val freq = editFrecuencia.toIntOrNull() ?: med.frecuenciaHoras
                                        if (freq > 0 && freq <= 24) {
                                            onUpdate(med.copy(
                                                nombre = editNombre,
                                                descripcion = editDesc,
                                                frecuenciaHoras = freq,
                                                grupo = editGrupo.ifBlank { null }
                                            ))
                                            isEditing = false
                                        }
                                    }) { Text("Guardar") }
                                    TextButton(onClick = { isEditing = false }) { Text("Cancelar") }
                                }
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("• ${med.nombre}: ${med.descripcion}", fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { isEditing = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                items(histInGroup) { h -> Text("• ${h.nombre} (Terminado)", fontSize = 14.sp, color = Color.Gray) }

                item { HorizontalDivider(); Text("Notas / Síntomas del Tratamiento", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                if (notasInGroup.isEmpty()) { item { Text("Sin notas registradas", color = Color.Gray, fontSize = 12.sp) } }
                items(notasInGroup) { nota ->
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(nota.fechaHora, fontSize = 10.sp, color = Color.Gray)
                            Text(nota.texto, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                item { HorizontalDivider(); Text("Línea del tiempo de tomas", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                if (tomasInGroup.isEmpty()) { item { Text("Sin tomas registradas", color = Color.Gray, fontSize = 12.sp) } }
                items(tomasInGroup) { toma ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).background(Color(toma.colorHex), CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(toma.nombreMedicamento, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(toma.fechaHora, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}
