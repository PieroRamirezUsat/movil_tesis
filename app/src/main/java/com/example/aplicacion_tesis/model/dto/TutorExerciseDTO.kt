package com.example.aplicacion_tesis.model.dto

import com.google.gson.annotations.SerializedName

data class TutorExerciseOptionDTO(
    val idOpcion: Int,
    val letra:    String,
    val texto:    String
)

data class TutorExerciseDTO(
    val status:                      Boolean,
    // ⚠️ La API envía "bloqueado":true cuando el docente no registró diagnóstico.
    //    Sin @SerializedName Gson no mapea el campo → el check en TutorFragment nunca dispara.
    @SerializedName("bloqueado")
    val bloqueadoSinDiagnostico:     Boolean? = null,
    val sinEjercicios:               Boolean? = null,
    val idEjercicio:                 Int?     = null,
    val idCompetencia:               Int?     = null,
    val enunciado:                   String?  = null,
    val imagenUrl:                   String?  = null,
    val opciones:                    List<TutorExerciseOptionDTO>? = null,
    val pista:                       String?  = null,
    val mensaje:                     String?  = null,
    val requiereDesarrollo:          Boolean? = null,
    val modo:                        String?  = null,
    val nivelEstudianteCompetencia:  String?  = null,
    @SerializedName("esVerificacion")
    val esVerificacion:              Boolean? = null
)

// ✅ Detalle fresco de un ejercicio (GET /ejercicios/{id}) — usado para
//    refrescar la imagen del ejercicio en pantalla al volver a la pestaña
data class EjercicioDetalleDataDTO(
    @SerializedName("idEjercicio") val idEjercicio: Int,
    @SerializedName("imagenUrl")   val imagenUrl:   String? = null
)

data class EjercicioDetalleResponse(
    val status: Boolean,
    val data:   EjercicioDetalleDataDTO? = null
)

// ✅ Material sugerido cuando el estudiante falla (asignado por el docente)
data class MaterialSugeridoDTO(
    @SerializedName("idMaterial")     val idMaterial:     Int,
    @SerializedName("titulo")         val titulo:         String,
    @SerializedName("tipo")           val tipo:           String,
    @SerializedName("url")            val url:            String,
    @SerializedName("tiempoEstimado") val tiempoEstimado: Int = 300
)

// ✅ URLs de búsqueda generadas por el tutor (YouTube · Web · PDF)
data class RecursosAdicionalesDTO(
    @SerializedName("youtubeUrl") val youtubeUrl: String,
    @SerializedName("webUrl")     val webUrl:     String,
    @SerializedName("pdfUrl")     val pdfUrl:     String,
    @SerializedName("query")      val query:      String
)

// ✅ Respuesta del tutor al responder
data class RespuestaTutorDTO(
    @SerializedName("correcta")            val correcta:            Boolean,
    @SerializedName("mostrarPista")        val mostrarPista:        Boolean                = false,
    @SerializedName("mensaje")             val mensaje:             String?                = null,
    @SerializedName("nuevoAjuste")         val nuevoAjuste:         String?                = null,
    @SerializedName("idRespuesta")         val idRespuesta:         Int?                   = null,
    @SerializedName("modo")                val modo:                String?                = null,
    @SerializedName("nivelMLCompetencia")  val nivelMLCompetencia:  String?                = null,
    @SerializedName("nivelCompetenciaInt") val nivelCompetenciaInt: Int?                   = null,
    @SerializedName("nivelGlobal")         val nivelGlobal:         String?                = null,
    @SerializedName("materialSugerido")    val materialSugerido:    MaterialSugeridoDTO?   = null,
    @SerializedName("recursosAdicionales") val recursosAdicionales: RecursosAdicionalesDTO? = null,
    @SerializedName("docenteAlertado")     val docenteAlertado:     Boolean?                = null
)
