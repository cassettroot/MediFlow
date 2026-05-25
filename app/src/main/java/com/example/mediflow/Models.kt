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
    val dosisTotal: Int,
    val dosisActual: Int = 0,
    val stockInicial: Int,
    val stockRestante: Int,
    val colorHex: Long,
    val descripcion: String = "",
    val duracionDias: Int,
    val frecuenciaHoras: Int,
    val timestampInicio: Long = System.currentTimeMillis(),
    val lastTakenTimestamp: Long? = null
)

data class RegistroToma(
    val id: UUID = UUID.randomUUID(),
    val medicamentoId: UUID,
    val nombreMedicamento: String,
    val fechaHora: String,
    val colorHex: Long,
    val isEarly: Boolean = false
)
