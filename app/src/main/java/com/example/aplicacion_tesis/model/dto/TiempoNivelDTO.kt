package com.example.aplicacion_tesis.model.dto

import com.google.gson.annotations.SerializedName

/** Un ítem por nivel de dificultad del ejercicio (N1..N4). */
data class TiempoNivelItemDTO(
    @SerializedName("nivelEjercicio")   val nivelEjercicio:  Int    = 0,
    @SerializedName("nombreNivel")      val nombreNivel:     String = "",
    @SerializedName("promedioSeg")      val promedioSeg:     Float  = 0f,
    @SerializedName("promedioFormato")  val promedioFormato: String = "",
    @SerializedName("totalRespuestas")  val totalRespuestas: Int    = 0,
    @SerializedName("tasaAcierto")      val tasaAcierto:     Float  = 0f
)

/** Envoltorio de la respuesta GET /progreso/tiempo_por_nivel */
data class TiempoNivelResponse(
    @SerializedName("status")  val status:  Boolean                = false,
    @SerializedName("mensaje") val mensaje: String?                = null,
    @SerializedName("niveles") val niveles: List<TiempoNivelItemDTO> = emptyList()
)