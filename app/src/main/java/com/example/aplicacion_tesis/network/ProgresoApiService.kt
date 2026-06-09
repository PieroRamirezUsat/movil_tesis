package com.example.aplicacion_tesis.network

import com.example.aplicacion_tesis.model.dto.ProgresoHistorialResponse
import com.example.aplicacion_tesis.model.dto.ProgresoPorCompetenciaDTO
import com.example.aplicacion_tesis.model.dto.ProgresoResumenDTO
import com.example.aplicacion_tesis.model.dto.TiempoNivelResponse
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.aplicacion_tesis.model.dto.ChartResponse

interface ProgresoApiService {

    @GET("progreso/resumen")
    suspend fun getResumen(
        @Query("idEstudiante") idEstudiante: Int
    ): ProgresoResumenDTO

    @GET("progreso/por_competencia")
    suspend fun getPorCompetencia(
        @Query("idEstudiante") idEstudiante: Int
    ): ProgresoPorCompetenciaDTO

    @GET("progreso/historial")
    suspend fun getHistorial(
        @Query("idEstudiante") idEstudiante: Int,
        @Query("limite")       limite: Int = 5,
        @Query("offset")       offset: Int = 0
    ): ProgresoHistorialResponse

    @GET("progreso/chart")
    suspend fun getChart(
        @Query("idEstudiante") idEstudiante: Int
    ): ChartResponse

    /** Tiempo promedio de respuesta y tasa de acierto por nivel de dificultad */
    @GET("progreso/tiempo_por_nivel")
    suspend fun getTiempoPorNivel(
        @Query("idEstudiante") idEstudiante: Int
    ): TiempoNivelResponse
}