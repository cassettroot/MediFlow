package com.example.mediflow

import android.content.Context
import android.os.Bundle
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mediflow.ui.theme.MediFlowTheme
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
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
                else -> isSystemInDarkTheme()
            }

            MediFlowTheme(
                darkTheme = isDarkTheme,
                appPrimaryColor = Color(appPrimaryColorHex)
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

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("¡Bienvenido a MediFlow!", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        Spacer(Modifier.height(16.dp))
        Text("Por favor, ingresa tus datos para comenzar.", color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Tu nombre") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = edad,
            onValueChange = { if (it.all { char -> char.isDigit() }) edad = it },
            label = { Text("Tu edad") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(20.dp)
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { if (nombre.isNotBlank() && edad.isNotBlank()) onRegister(nombre, edad.toInt()) },
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
    themeSetting: String,
    appColor: Long,
    onThemeChange: (String) -> Unit,
    onColorChange: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(Screen.Inicio) }
    val listaMedicamentos = remember { mutableStateListOf<Medicamento>() }
    val historial = remember { mutableStateListOf<RegistroToma>() }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AppBottomBar(selectedTab) { selectedTab = it }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                Screen.Inicio -> MisTomasScreen(
                    userProfile,
                    listaMedicamentos,
                    historial,
                    onNavigateToAgregar = { selectedTab = Screen.Agregar }
                )
                Screen.Agregar -> AgregarMedicamentoScreen(
                    onMedicamentoGuardado = { nuevo ->
                        listaMedicamentos.add(nuevo)
                        selectedTab = Screen.Inicio
                    }
                )
                Screen.Historial -> HistorialScreen(listaMedicamentos, historial)
                Screen.Ajustes -> AjustesScreen(
                    themeSetting = themeSetting,
                    appColor = appColor,
                    onThemeChange = onThemeChange,
                    onColorChange = onColorChange
                )
            }
        }
    }
}

@Composable
fun AppBottomBar(selectedTab: Screen, onTabSelected: (Screen) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == Screen.Inicio,
            onClick = { onTabSelected(Screen.Inicio) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") }
        )
        NavigationBarItem(
            selected = selectedTab == Screen.Agregar,
            onClick = { onTabSelected(Screen.Agregar) },
            icon = { Icon(Icons.Default.Add, contentDescription = "Agregar") },
            label = { Text("Agregar") }
        )
        NavigationBarItem(
            selected = selectedTab == Screen.Historial,
            onClick = { onTabSelected(Screen.Historial) },
            icon = { Icon(Icons.Default.History, contentDescription = "Historial") },
            label = { Text("Historial") }
        )
        NavigationBarItem(
            selected = selectedTab == Screen.Ajustes,
            onClick = { onTabSelected(Screen.Ajustes) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
            label = { Text("Ajustes") }
        )
    }
}

// --- PANTALLA: MIS TOMAS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MisTomasScreen(
    user: UserProfile,
    lista: MutableList<Medicamento>,
    historial: MutableList<RegistroToma>,
    onNavigateToAgregar: () -> Unit
) {
    var medicamentoSeleccionado by remember { mutableStateOf<Medicamento?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showSheet by remember { mutableStateOf(false) }

    val saludo = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
        in 6..12 -> "Buenos días"
        in 13..20 -> "Buenas tardes"
        else -> "Buenas noches"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$saludo,", fontSize = 18.sp, color = Color.Gray)
            Text(user.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        }

        if (lista.isEmpty()) {
            EmptyState(onNavigateToAgregar)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lista) { med ->
                    MedicamentoCard(
                        med = med,
                        onClick = {
                            medicamentoSeleccionado = med
                            showSheet = true
                        },
                        onRegistrar = { isEarly ->
                            val index = lista.indexOf(med)
                            if (index != -1) {
                                val now = System.currentTimeMillis()
                                val updated = med.copy(
                                    dosisActual = med.dosisActual + 1,
                                    stockRestante = med.stockRestante - 1,
                                    lastTakenTimestamp = now
                                )
                                lista[index] = updated
                                
                                val sdf = SimpleDateFormat("HH:mm - dd MMM", Locale.getDefault())
                                historial.add(0, RegistroToma(
                                    medicamentoId = med.id,
                                    nombreMedicamento = med.nombre,
                                    fechaHora = sdf.format(Date(now)),
                                    colorHex = med.colorHex,
                                    isEarly = isEarly
                                ))
                            }
                        },
                        onDelete = {
                            lista.remove(med)
                        }
                    )
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

    if (showSheet && medicamentoSeleccionado != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState
        ) {
            MedicamentoDetalleContent(medicamentoSeleccionado!!)
        }
    }
}

@Composable
fun MedicamentoCard(
    med: Medicamento,
    onClick: () -> Unit,
    onRegistrar: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showEarlyWarningDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isLowStock = med.stockRestante <= 5

    // Lógica de cuenta regresiva TOTAL
    val totalMilis = med.duracionDias.toLong() * 24 * 60 * 60 * 1000
    val finTimestamp = med.timestampInicio + totalMilis
    val restanteMilis = (finTimestamp - System.currentTimeMillis()).coerceAtLeast(0)
    val diasRestantes = restanteMilis / (24 * 60 * 60 * 1000)
    val horasRestantes = (restanteMilis / (60 * 60 * 1000)) % 24

    // Lógica de cuenta regresiva SIGUIENTE TOMA
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .then(
                if (isLowStock) Modifier.border(2.dp, Color.Red, RoundedCornerShape(20.dp))
                else Modifier
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
                    onClick = { 
                        if (timeToNext > 0) {
                            showEarlyWarningDialog = true
                        } else {
                            showConfirmDialog = true
                        }
                    },
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
            confirmButton = {
                TextButton(onClick = { onRegistrar(false); showConfirmDialog = false }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Cancelar") }
            }
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
                Spacer(Modifier.width(8.dp))
                Text("Toma Anticipada")
            }},
            text = { Text("Tomaste este medicamento hace solo ${diffHrs}h ${remainingMins}min.\n\nSe recomienda esperar a que el contador llegue a cero para mantener la eficacia del tratamiento.\n\n¿Deseas registrarla de todos modos?") },
            confirmButton = {
                TextButton(onClick = { onRegistrar(true); showEarlyWarningDialog = false }) { Text("Registrar de todos modos", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showEarlyWarningDialog = false }) { Text("Esperar (Recomendado)") }
            }
        )
    }

    if (showDeleteDialog) {
        val dosisFaltantes = med.dosisTotal - med.dosisActual
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Tratamiento", color = Color.Red) },
            text = { Text("¿Estás seguro? Te faltaban $dosisFaltantes dosis para terminar.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) { Text("Eliminar", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
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
        DetailItem(label = "Frecuencia", value = "Cada ${med.frecuenciaHoras} horas")
        DetailItem(label = "Duración", value = "${med.duracionDias} días de tratamiento")
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
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("💊", fontSize = 80.sp)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onNavigateToAgregar, shape = RoundedCornerShape(50.dp)) {
            Text("Configurar primer medicamento", fontWeight = FontWeight.Bold)
        }
    }
}

// --- PANTALLA: AGREGAR ---

@Composable
fun AgregarMedicamentoScreen(onMedicamentoGuardado: (Medicamento) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var duracionDias by remember { mutableStateOf("") }
    var frecuenciaHoras by remember { mutableStateOf("8") }
    var stockInicial by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(0xFF6200EE) }
    var descripcion by remember { mutableStateOf("") }
    var isCustomFrecuencia by remember { mutableStateOf(false) }

    val colors = listOf(0xFFF44336, 0xFFE91E63, 0xFF9C27B0, 0xFF2196F3, 0xFF4CAF50, 0xFFFFC107, 0xFF6200EE, 0xFF009688)
    val frecuenciasPredefinidas = listOf("4", "6", "8", "12", "24")

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Text("Nuevo Tratamiento", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp) }
        
        item {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del fármaco (ej. Paracetamol)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
        }
        
        item {
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = duracionDias,
                    onValueChange = { if (it.all { c -> c.isDigit() }) duracionDias = it },
                    label = { Text("Duración (días)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(20.dp)
                )
                OutlinedTextField(
                    value = stockInicial,
                    onValueChange = { if (it.all { c -> c.isDigit() }) stockInicial = it },
                    label = { Text("Stock inicial") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        item {
            Text("Cada cuántas horas:", fontWeight = FontWeight.Medium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                frecuenciasPredefinidas.forEach { hrs ->
                    FilterChip(
                        selected = frecuenciaHoras == hrs && !isCustomFrecuencia,
                        onClick = { frecuenciaHoras = hrs; isCustomFrecuencia = false },
                        label = { Text("${hrs}h") }
                    )
                }
                FilterChip(
                    selected = isCustomFrecuencia,
                    onClick = { isCustomFrecuencia = true },
                    label = { Text("Otro") }
                )
            }
            if (isCustomFrecuencia) {
                OutlinedTextField(
                    value = frecuenciaHoras,
                    onValueChange = { if (it.all { c -> c.isDigit() }) frecuenciaHoras = it },
                    label = { Text("Horas personalizadas") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        item {
            Text("Selecciona un color identificativo:", fontWeight = FontWeight.Medium)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(100.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(colors) { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(color), CircleShape)
                            .border(width = if (selectedColor == color) 3.dp else 0.dp, color = if (selectedColor == color) Color.Black else Color.Transparent, shape = CircleShape)
                            .clickable { selectedColor = color }
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val dDias = duracionDias.toIntOrNull() ?: 0
                    val fHoras = frecuenciaHoras.toIntOrNull() ?: 1
                    val sInicial = stockInicial.toIntOrNull() ?: 0
                    if (nombre.isNotBlank() && dDias > 0 && sInicial > 0) {
                        val dosisPorDia = 24 / fHoras
                        val totalDosis = dDias * dosisPorDia
                        val m = Medicamento(
                            nombre = nombre,
                            dosisTotal = totalDosis,
                            stockInicial = sInicial,
                            stockRestante = sInicial,
                            colorHex = selectedColor,
                            descripcion = descripcion,
                            duracionDias = dDias,
                            frecuenciaHoras = fHoras
                        )
                        onMedicamentoGuardado(m)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(selectedColor))
            ) {
                Text("Guardar Medicamento", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}

// --- PANTALLA: HISTORIAL ---

@Composable
fun HistorialScreen(listaMedicamentos: List<Medicamento>, historial: List<RegistroToma>) {
    var selectedTab by remember { mutableIntStateOf(0) }
    
    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text("Tomadas", modifier = Modifier.padding(16.dp))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text("Pendientes", modifier = Modifier.padding(16.dp))
            }
        }

        if (selectedTab == 0) {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(historial) { registro ->
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color(registro.colorHex).copy(alpha = 0.05f))) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (registro.isEarly) {
                                Icon(Icons.Default.Help, contentDescription = "Antes de tiempo", tint = Color(0xFFFF9800))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(registro.colorHex))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(registro.nombreMedicamento, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(registro.fechaHora, fontSize = 12.sp, color = Color.Gray)
                                    if (registro.isEarly) {
                                        Spacer(Modifier.width(8.dp))
                                        Text("(Antes de tiempo)", fontSize = 11.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listaMedicamentos.filter { it.dosisActual < it.dosisTotal }) { med ->
                    val dosisRestantes = med.dosisTotal - med.dosisActual
                    val diasRestantes = (dosisRestantes * med.frecuenciaHoras) / 24.0
                    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(med.colorHex))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(med.nombre, fontWeight = FontWeight.Bold)
                                Text("Restan $dosisRestantes pastillas (~${String.format("%.1f", diasRestantes)} días)", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PANTALLA: AJUSTES ---

@Composable
fun AjustesScreen(
    themeSetting: String,
    appColor: Long,
    onThemeChange: (String) -> Unit,
    onColorChange: (Long) -> Unit
) {
    val options = listOf("Sistema", "Claro", "Oscuro")
    val appColors = listOf(0xFF6200EE, 0xFF009688, 0xFFE91E63, 0xFF2196F3, 0xFF4CAF50, 0xFFFF9800)

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Configuración", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        Spacer(Modifier.height(32.dp))
        
        Text("Tema de la aplicación", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = themeSetting == option,
                    onClick = { onThemeChange(option) },
                    label = { Text(option) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Color de acento", fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            appColors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .background(Color(color), CircleShape)
                        .border(width = if (appColor == color) 3.dp else 0.dp, color = MaterialTheme.colorScheme.onSurface, shape = CircleShape)
                        .clickable { onColorChange(color) }
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text("MediFlow v1.1", fontWeight = FontWeight.Medium, color = Color.Gray)
    }
}
