package com.example.aplicacion_tesis.network

import com.example.aplicacion_tesis.model.dto.AperturaMaterialRequest
import com.example.aplicacion_tesis.model.dto.EjercicioDetalleResponse
import com.example.aplicacion_tesis.model.dto.EvaluacionActivaResponse
import com.example.aplicacion_tesis.model.dto.FinalizarEvaluacionResponse
import com.example.aplicacion_tesis.model.dto.TutorAnswerRequest
import com.example.aplicacion_tesis.model.dto.TutorExerciseDTO
import com.example.aplicacion_tesis.model.dto.UploadResponseDTO
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*
import com.example.aplicacion_tesis.model.dto.RespuestaTutorDTO
interface TutorApiService {

    // ✅ Sin / al inicio — Retrofit usa BASE_URL correctamente
    @GET("tutor/ejercicio_siguiente")
    suspend fun getNextExercise(
        @Query("idEstudiante")       idEstudiante: Int,
        @Query("idDominio")          idDominio: Int?,
        @Query("ajuste")             ajuste: String?,
        @Query("modo")               modo: String = "repaso",
        @Query("idEvaluacion")       idEvaluacion: Int? = null,
        @Query("postRefuerzo")       postRefuerzo: Boolean? = null,
        @Query("idEjercicioFallado") idEjercicioFallado: Int? = null
    ): TutorExerciseDTO

    @POST("tutor/responder")
    suspend fun sendAnswer(
        @Body request: TutorAnswerRequest
    ): RespuestaTutorDTO

    @Multipart
    @POST("tutor/subir_desarrollo")
    suspend fun uploadDevelopment(
        @Part("idRespuesta") idRespuesta: RequestBody,
        @Part archivo: MultipartBody.Part
    ): UploadResponseDTO

    @GET("tutor/evaluacion/activa")
    suspend fun getEvaluacionActiva(
        @Query("idEstudiante") idEstudiante: Int
    ): EvaluacionActivaResponse

    @POST("tutor/evaluacion/finalizar")
    suspend fun finalizarEvaluacion(
        @Body body: Map<String, Int>
    ): FinalizarEvaluacionResponse

    @POST("tutor/material/abrir")
    suspend fun registrarAperturaMaterial(
        @Body body: AperturaMaterialRequest
    ): retrofit2.Response<Unit>

    // Detalle fresco de un ejercicio (para refrescar imagen/datos del que
    // está en pantalla sin cambiar de ejercicio)
    @GET("ejercicios/{id}")
    suspend fun getEjercicioDetalle(
        @Path("id") idEjercicio: Int
    ): EjercicioDetalleResponse
}