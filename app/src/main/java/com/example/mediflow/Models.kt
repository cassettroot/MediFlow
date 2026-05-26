package com.example.mediflow

import java.util.UUID

data class UserProfile(
    val nombre: String = "",
    val edad: Int = 0,
    val isRegistered: Boolean = false
)

data class Medicamento(
    val id: UUID = UUID.randomUUID(),
    val nombre: String,
    var dosisTotal: Int,
    var dosisActual: Int = 0,
    val stockInicial: Int,
    var stockRestante: Int,
    var colorHex: Long,
    var descripcion: String = "",
    val duracionDias: Int,
    val frecuenciaHoras: Int,
    val timestampInicio: Long = System.currentTimeMillis(),
    var lastTakenTimestamp: Long? = null,
    val grupo: String? = null,
    val grupoColorHex: Long? = null,
    val horaFija: String? = null // Para "Desayuno", "Comida", "Cena" o "Hora exacta"
)

data class RegistroToma(
    val id: UUID = UUID.randomUUID(),
    val medicamentoId: UUID,
    val nombreMedicamento: String,
    val fechaHora: String,
    val colorHex: Long,
    val isEarly: Boolean = false,
    val grupoNombre: String? = null
)

data class Nota(
    val id: UUID = UUID.randomUUID(),
    val texto: String,
    val fechaHora: String,
    val medicamentoNombre: String? = null,
    val grupoNombre: String? = null,
    val categoria: String // "Medicamento", "Tratamiento", "Otro"
)

data class MedicamentoHistorico(
    val id: UUID = UUID.randomUUID(),
    val nombre: String,
    val fechaInicio: String,
    val dosisTomadas: Int,
    val duracionDias: Int,
    val grupo: String? = null,
    val notas: MutableList<Nota> = mutableListOf()
)
