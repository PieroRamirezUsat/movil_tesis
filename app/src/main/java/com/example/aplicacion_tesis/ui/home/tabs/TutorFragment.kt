package com.example.aplicacion_tesis.ui.home.tabs

import android.app.Activity
import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.bumptech.glide.Glide
import com.example.aplicacion_tesis.R
import com.example.aplicacion_tesis.model.dto.AperturaMaterialRequest
import com.example.aplicacion_tesis.model.dto.HistorialMaterialRequest
import com.example.aplicacion_tesis.model.dto.MaterialSugeridoDTO
import com.example.aplicacion_tesis.model.dto.RecursosAdicionalesDTO
import com.example.aplicacion_tesis.model.dto.TutorAnswerRequest
import com.example.aplicacion_tesis.model.dto.TutorExerciseDTO
import com.example.aplicacion_tesis.network.RetrofitClient
import com.example.aplicacion_tesis.network.TokenStore
import com.example.aplicacion_tesis.ui.home.ProgressEvents
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.viewpager2.widget.ViewPager2
import com.example.aplicacion_tesis.ui.components.DonutChartView
import java.io.File
import java.io.FileOutputStream

class TutorFragment : Fragment() {

    companion object {
        var selectedDominioId: Int? = null
        private const val REQ_PICK_FILE    = 5001
        private const val PREFS_NAME       = "tutor_prefs"
        private const val KEY_ENUNCIADO    = "ejercicio_enunciado"
        private const val KEY_IMAGEN_URL   = "ejercicio_imagen_url"
        private const val KEY_ID_EJERCICIO = "ejercicio_id"
        private const val KEY_ID_COMP      = "ejercicio_id_comp"
        private const val KEY_PISTA         = "ejercicio_pista"
        private const val KEY_PISTA_VISIBLE = "pista_visible"
        private const val KEY_MODO          = "ejercicio_modo"
        private const val KEY_LAST_FILE    = "last_file_uri"
        private const val KEY_SONIDO       = "sonido_activado"

        var ejercicioGuardado: TutorExerciseDTO? = null
        var modoGuardado:      String            = "repaso"
        var pistaGuardada:     String?           = null
    }

    private var idEstudiante:        Int?    = null
    private var modoActual:          String  = "repaso"
    private var idEvaluacionActiva:  Int?    = null
    private var evaluacionEnCurso:   Boolean = false
    private var correctasEvaluacion: Int     = 0
    private var totalEvaluacion:     Int     = 0
    private var hayEvaluacionActiva: Boolean = false

    private lateinit var tvTituloTutor:           TextView
    private lateinit var tvEnunciado:             TextView
    private lateinit var imgProblema:             ImageView
    private lateinit var rgAlternativas:          RadioGroup
    private lateinit var rbA:                     RadioButton
    private lateinit var rbB:                     RadioButton
    private lateinit var rbC:                     RadioButton
    private lateinit var rbD:                     RadioButton
    private lateinit var rbE:                     RadioButton
    private lateinit var cardFeedback:            View
    private lateinit var tvFeedbackTitle:         TextView
    private lateinit var tvFeedback:              TextView
    private lateinit var btnSubirFoto:            MaterialButton
    private lateinit var btnEnviar:               MaterialButton
    private lateinit var btnModoRepaso:           TextView
    private lateinit var btnModoEvaluacion:       TextView
    private lateinit var tvBadgeModo:             TextView
    private lateinit var cardEvaluacionActiva:    LinearLayout
    private lateinit var tvEvaluacionTitulo:      TextView
    private lateinit var tvEvaluacionDescripcion: TextView
    private lateinit var btnIniciarEvaluacion:    MaterialButton
    private lateinit var cardResultadoEvaluacion: LinearLayout
    private lateinit var tvResultadoCorrectas:    TextView
    private lateinit var tvResultadoPuntaje:      TextView
    private lateinit var tvNivelResultado:        TextView
    private lateinit var donutResultado:          DonutChartView
    private lateinit var confettiView:            com.example.aplicacion_tesis.ui.components.ConfettiView
    private lateinit var cardEjercicio:           LinearLayout
    private lateinit var tvNivelActual:           TextView
    private lateinit var tvCompetenciaTag:        TextView
    private lateinit var btnSonido:               FloatingActionButton
    private var sonidoActivado: Boolean = true

    private var currentExercise:              TutorExerciseDTO? = null
    private var attempts:                     Int     = 0
    private var lastAjuste:                   String? = null
    private var startTimeMillis:              Long    = 0L
    private var usoPistaActual:               Boolean = false
    private var lastRespuestaId:              Int?    = null
    private var selectedFileUri:              Uri?    = null
    private var desarrolloSubidoOk:           Boolean = false
    private var pistaActual:                  String? = null
    private var ejercicioCargadoUnaVez:       Boolean = false
    private var nivelMLPendiente:             String? = null
    private var nivelActualCached:            String? = null
    private var idEjercicioDesarrolloSubido:  Int?    = null
    private var repasoExerciseBeforeEval:     TutorExerciseDTO? = null
    private var modoVerificacion:             Boolean           = false
    private val competenciasAlertadas = mutableSetOf<Int>()
    private var rachaActual:                  Int               = 0
    private lateinit var tvRachaBadge:        TextView
    private lateinit var viewProgressBar:     View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_tutor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupModoSelector()
        modoActual = modoGuardado
        actualizarBadgeModo()

        // ✅ Ocultar respuestas y botones por defecto — se muestran cuando hay ejercicio
        rgAlternativas.visibility = View.GONE
        btnSubirFoto.visibility   = View.GONE
        btnEnviar.visibility      = View.GONE
        tvTituloTutor.text        = "Práctica de Álgebra"

        btnSubirFoto.setOnClickListener { abrirSelectorArchivo() }
        btnEnviar.setOnClickListener { verificarRespuesta() }

        sonidoActivado = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_SONIDO, true)
        actualizarIconoSonido()
        btnSonido.setOnClickListener {
            sonidoActivado = !sonidoActivado
            requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_SONIDO, sonidoActivado).apply()
            actualizarIconoSonido()
        }

        if (idEstudiante != null && idEstudiante!! > 0) return
        obtenerIdEstudianteDesdeBackend()
    }

    override fun onResume() {
        super.onResume()
        val stored = TokenStore.studentId
        if (stored != null && stored > 0 && idEstudiante == null) {
            idEstudiante = stored
            verificarEvaluacionActivaSilencioso()
        }
        // Si la carga anterior no dejó ejercicio en pantalla (p. ej. bloqueo
        // por diagnóstico pendiente), reintenta al volver a esta pestaña —
        // ViewPager2 dispara onResume en cada cambio de página.
        if (idEstudiante != null && !ejercicioCargadoUnaVez) {
            iniciarTutor()
        }
        // El ejercicio en pantalla se conserva a propósito (para no perder el
        // trabajo del alumno), pero sus datos pueden haber cambiado en el
        // servidor (p. ej. el docente le agregó una imagen). Al volver a la
        // pestaña se re-consulta y se refresca la imagen si cambió.
        refrescarImagenEjercicioActual()
    }

    private fun refrescarImagenEjercicioActual() {
        val actual = currentExercise ?: return
        val idEj   = actual.idEjercicio ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val det = RetrofitClient.tutorApi.getEjercicioDetalle(idEj)
                val urlNueva = det.data?.imagenUrl
                if (det.status && det.data?.idEjercicio == idEj &&
                    urlNueva != actual.imagenUrl) {
                    val actualizado = actual.copy(imagenUrl = urlNueva)
                    currentExercise   = actualizado
                    ejercicioGuardado = actualizado
                    cargarImagenConGlide(urlNueva)
                }
            } catch (_: Exception) {
                // Sin conexión o error: se mantiene la imagen actual
            }
        }
    }

    private fun corregirUrlParaDispositivo(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            // Solo reescribir URLs del entorno de desarrollo (emulador:
            // localhost → 10.0.2.2). Las URLs externas (Cloudinary, Railway)
            // deben pasar INTACTAS: antes se les cambiaba el host por el de
            // la API y las imágenes daban 404 en producción.
            val hostUrl = Uri.parse(url).host ?: return url
            val esLocal = hostUrl == "localhost" || hostUrl == "127.0.0.1" || hostUrl == "10.0.2.2"
            if (!esLocal) return url
            val baseUri    = Uri.parse(RetrofitClient.BASE_URL.trimEnd('/'))
            val hostBase   = baseUri.host ?: return url
            val puertoBase = baseUri.port
            val esquema    = baseUri.scheme ?: "http"
            val path       = Uri.parse(url).path ?: return url
            if (puertoBase > 0) "$esquema://$hostBase:$puertoBase$path"
            else "$esquema://$hostBase$path"
        } catch (_: Exception) { url }
    }

    private fun cargarImagenConGlide(url: String?) {
        val urlFinal = corregirUrlParaDispositivo(url)
        android.util.Log.d("IMAGEN_TUTOR", "URL original: $url | corregida: $urlFinal")
        if (urlFinal.isNullOrBlank()) {
            imgProblema.visibility = View.GONE
            return
        }
        imgProblema.visibility = View.VISIBLE
        Glide.with(requireContext())
            .load(urlFinal)
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.DATA)
            .skipMemoryCache(false)
            .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.e("IMAGEN_TUTOR", "❌ Falló: $urlFinal — ${e?.message}")
                    imgProblema.visibility = View.GONE
                    return false
                }
                override fun onResourceReady(
                    resource: android.graphics.drawable.Drawable,
                    model: Any,
                    target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                    dataSource: com.bumptech.glide.load.DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    android.util.Log.d("IMAGEN_TUTOR", "✅ Cargada: $urlFinal")
                    imgProblema.visibility = View.VISIBLE
                    return false
                }
            })
            .into(imgProblema)
    }

    private fun bindViews(view: View) {
        tvTituloTutor            = view.findViewById(R.id.tvTituloTutor)
        tvEnunciado              = view.findViewById(R.id.tvEnunciado)
        imgProblema              = view.findViewById(R.id.imgProblema)
        rgAlternativas           = view.findViewById(R.id.rgAlternativas)
        rbA                      = view.findViewById(R.id.rbA)
        rbB                      = view.findViewById(R.id.rbB)
        rbC                      = view.findViewById(R.id.rbC)
        rbD                      = view.findViewById(R.id.rbD)
        rbE                      = view.findViewById(R.id.rbE)
        cardFeedback             = view.findViewById(R.id.cardFeedback)
        tvFeedbackTitle          = view.findViewById(R.id.tvFeedbackTitle)
        tvFeedback               = view.findViewById(R.id.tvFeedback)
        btnSubirFoto             = view.findViewById(R.id.btnSubirFoto)
        btnEnviar                = view.findViewById(R.id.btnEnviar)
        btnModoRepaso            = view.findViewById(R.id.btnModoRepaso)
        btnModoEvaluacion        = view.findViewById(R.id.btnModoEvaluacion)
        tvBadgeModo              = view.findViewById(R.id.tvBadgeModo)
        cardEvaluacionActiva     = view.findViewById(R.id.cardEvaluacionActiva)
        tvEvaluacionTitulo       = view.findViewById(R.id.tvEvaluacionTitulo)
        tvEvaluacionDescripcion  = view.findViewById(R.id.tvEvaluacionDescripcion)
        btnIniciarEvaluacion     = view.findViewById(R.id.btnIniciarEvaluacion)
        cardResultadoEvaluacion  = view.findViewById(R.id.cardResultadoEvaluacion)
        tvResultadoCorrectas     = view.findViewById(R.id.tvResultadoCorrectas)
        tvResultadoPuntaje       = view.findViewById(R.id.tvResultadoPuntaje)
        tvNivelResultado         = view.findViewById(R.id.tvNivelResultado)
        donutResultado           = view.findViewById(R.id.donutResultado)
        cardEjercicio            = view.findViewById(R.id.cardEjercicio)
        tvCompetenciaTag         = view.findViewById(R.id.tvCompetenciaTag)
        tvNivelActual            = try {
            view.findViewById(R.id.tvNivelActual)
        } catch (e: Exception) { TextView(requireContext()) }
        btnSonido                = view.findViewById(R.id.btnSonido)
        tvRachaBadge             = view.findViewById(R.id.tvRachaBadge)
        viewProgressBar          = view.findViewById(R.id.viewProgressBar)
        confettiView             = view.findViewById(R.id.confettiView)
    }

    private fun setupModoSelector() {
        btnModoRepaso.setOnClickListener {
            if (modoActual != "repaso") {
                // Bloquear cambio si hay una evaluación en curso
                if (evaluacionEnCurso) {
                    mostrarDialogoSalirEvaluacion()
                    return@setOnClickListener
                }

                val eraEvaluacionEnCurso = evaluacionEnCurso
                val ejercicioPreEval     = ejercicioGuardado

                modoActual        = "repaso"
                modoGuardado      = "repaso"
                evaluacionEnCurso = false
                actualizarBadgeModo()

                cardEvaluacionActiva.visibility    = View.GONE
                cardResultadoEvaluacion.visibility = View.GONE
                cardFeedback.visibility            = View.GONE
                cardEjercicio.visibility           = View.VISIBLE
                rgAlternativas.visibility          = View.VISIBLE
                btnSubirFoto.visibility            = View.VISIBLE
                btnEnviar.visibility               = View.VISIBLE

                if (!eraEvaluacionEnCurso && ejercicioPreEval != null) {
                    // Solo miró el tab de evaluación sin iniciar → restaurar el ejercicio anterior
                    ejercicioCargadoUnaVez = true
                    currentExercise        = ejercicioPreEval
                    pistaActual            = pistaGuardada
                    restaurarUIDesdeEjercicio(ejercicioPreEval)
                } else {
                    // La evaluación fue iniciada (o no había ejercicio) → cargar uno nuevo
                    ejercicioGuardado      = null
                    currentExercise        = null
                    ejercicioCargadoUnaVez = false
                    limpiarPrefs()
                    rgAlternativas.clearCheck()
                    listOf(rbA, rbB, rbC, rbD, rbE).forEach { it.visibility = View.GONE }
                    cargarNuevoEjercicio(null)
                }
            }
        }

        btnModoEvaluacion.setOnClickListener {
            // Guardar ejercicio de repaso antes de cambiar modo
            if (modoActual == "repaso" && currentExercise != null) {
                repasoExerciseBeforeEval = currentExercise
            }
            if (!hayEvaluacionActiva) {
                val idEst = idEstudiante ?: run {
                    Toast.makeText(requireContext(),
                        "No hay evaluación activa. Tu docente debe activarla desde el sistema.",
                        Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.tutorApi.getEvaluacionActiva(idEst)
                        hayEvaluacionActiva = resp.hayEvaluacion && resp.evaluacion != null && !resp.yaCompleto
                        if (hayEvaluacionActiva && resp.evaluacion != null) {
                            idEvaluacionActiva              = resp.evaluacion.idEvaluacion
                            modoActual                      = "evaluacion"
                            modoGuardado                    = "evaluacion"
                            actualizarBadgeModo()
                            cardEjercicio.visibility        = View.GONE
                            rgAlternativas.visibility       = View.GONE
                            btnSubirFoto.visibility         = View.GONE
                            btnEnviar.visibility            = View.GONE
                            tvCompetenciaTag.visibility     = View.GONE
                            cardFeedback.visibility         = View.GONE
                            cardEvaluacionActiva.visibility = View.VISIBLE
                            tvEvaluacionTitulo.text         = resp.evaluacion.titulo
                            tvEvaluacionDescripcion.text    = resp.evaluacion.descripcion
                                ?: "Tu docente ha activado una evaluación."
                        } else {
                            Toast.makeText(requireContext(),
                                "No hay evaluación activa. Tu docente debe activarla desde el sistema.",
                                Toast.LENGTH_LONG).show()
                        }
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(),
                            "No hay evaluación activa. Tu docente debe activarla desde el sistema.",
                            Toast.LENGTH_LONG).show()
                    }
                }
                return@setOnClickListener
            }
            if (modoActual != "evaluacion") {
                modoActual   = "evaluacion"
                modoGuardado = "evaluacion"
                actualizarBadgeModo()
                cardFeedback.visibility         = View.GONE
                // ✅ Ocultar ejercicio y botones hasta que inicie
                cardEjercicio.visibility        = View.GONE
                btnSubirFoto.visibility         = View.GONE
                btnEnviar.visibility            = View.GONE
                cardEvaluacionActiva.visibility = View.VISIBLE
                // ✅ Ocultar respuestas y botones hasta que inicie
                rgAlternativas.visibility  = View.GONE
                btnSubirFoto.visibility    = View.GONE
                btnEnviar.visibility       = View.GONE
                cardEjercicio.visibility   = View.GONE
            }
        }

        btnIniciarEvaluacion.setOnClickListener {
            cardEvaluacionActiva.visibility = View.GONE
            // ✅ Mostrar todo al iniciar
            rgAlternativas.visibility       = View.VISIBLE
            btnSubirFoto.visibility         = View.VISIBLE
            btnEnviar.visibility            = View.VISIBLE
            cardEjercicio.visibility        = View.VISIBLE
            btnSubirFoto.visibility         = View.VISIBLE
            btnEnviar.visibility            = View.VISIBLE
            evaluacionEnCurso               = true
            correctasEvaluacion             = 0
            totalEvaluacion                 = 0
            ejercicioGuardado               = null
            limpiarPrefs()
            cargarNuevoEjercicio(null)
        }
    }

    private fun actualizarBadgeModo() {
        val colorBlanco  = android.graphics.Color.WHITE
        val colorPrimary = resources.getColor(R.color.ai_primary, null)
        val colorMuted   = resources.getColor(R.color.ai_text_muted, null)
        if (modoActual == "repaso") {
            btnModoRepaso.setTextColor(colorBlanco)
            btnModoRepaso.setBackgroundResource(R.drawable.bg_tab_selected_left)
            btnModoEvaluacion.setTextColor(colorMuted)
            btnModoEvaluacion.setBackgroundResource(R.drawable.bg_tab_selected_right)
            tvBadgeModo.text = "Modo Repaso — Las pistas están disponibles"
            tvBadgeModo.setTextColor(colorPrimary)
        } else {
            btnModoEvaluacion.setTextColor(colorBlanco)
            btnModoEvaluacion.setBackgroundResource(R.drawable.bg_tab_selected_right_active)
            btnModoRepaso.setTextColor(colorMuted)
            btnModoRepaso.setBackgroundResource(R.drawable.bg_tab_selected_left_inactive)
            tvBadgeModo.text = "Modo Evaluación — Sin pistas disponibles"
            tvBadgeModo.setTextColor(resources.getColor(R.color.ai_accent, null))
        }
    }

    private fun verificarEvaluacionActivaSilencioso() {
        val idEst = idEstudiante ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val resp = RetrofitClient.tutorApi.getEvaluacionActiva(idEst)
                hayEvaluacionActiva = resp.hayEvaluacion && resp.evaluacion != null && !resp.yaCompleto
                if (hayEvaluacionActiva && resp.evaluacion != null) {
                    idEvaluacionActiva = resp.evaluacion.idEvaluacion
                    // ✅ Solo guardamos datos, NO cambiamos modo — el alumno decide cuándo entrar
                    tvEvaluacionTitulo.text      = resp.evaluacion.titulo
                    tvEvaluacionDescripcion.text = resp.evaluacion.descripcion
                        ?: "Tu docente ha activado una evaluación. No habrá pistas."
                    // Resaltar botón evaluación para avisar que hay una activa
                    btnModoEvaluacion.setBackgroundResource(R.drawable.bg_tab_selected_right_active)
                } else {
                    hayEvaluacionActiva = false
                    idEvaluacionActiva  = null
                }
            } catch (_: Exception) {
                hayEvaluacionActiva = false
                idEvaluacionActiva  = null
            }
        }
    }

    private fun cargarNivelInicial(idEst: Int) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val progresoResp    = RetrofitClient.progresoApi.getPorCompetencia(idEst)
                val temas           = progresoResp.temas ?: emptyList()
                val nivelesConDatos = temas.mapNotNull { it.nivelActual }.filter { it > 0 }
                if (nivelesConDatos.isNotEmpty()) {
                    val promedio = nivelesConDatos.average()
                    val nivelML  = when {
                        promedio >= 5 -> "alto"
                        promedio >= 3 -> "medio"
                        else          -> "bajo"
                    }
                    nivelActualCached = nivelML
                    mostrarNivelEnUI(nivelML)
                }
            } catch (_: Exception) {}
        }
    }

    private fun obtenerIdEstudianteDesdeBackend() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val storedStudentId = TokenStore.studentId
                if (storedStudentId != null && storedStudentId > 0) {
                    idEstudiante = storedStudentId
                    cargarNivelInicial(storedStudentId)
                    iniciarTutor()
                    return@launch
                }
                val idUsuario = TokenStore.userId
                if (idUsuario == null || idUsuario <= 0) {
                    mostrarMensajeSinEstudiante("No se encontró sesión. Vuelve a iniciar sesión.")
                    return@launch
                }
                val response = RetrofitClient.estudianteApi.getEstudiantePorUsuario(idUsuario)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.status && body.data != null) {
                        idEstudiante = body.data.idEstudiante
                        TokenStore.setStudentId(idEstudiante)
                        cargarNivelInicial(body.data.idEstudiante)
                        iniciarTutor()
                    } else {
                        mostrarMensajeSinEstudiante("Tu docente aún no te ha registrado.")
                    }
                } else {
                    mostrarMensajeSinEstudiante("Error al obtener datos. Intenta más tarde.")
                }
            } catch (e: Exception) {
                mostrarMensajeSinEstudiante("No se pudo conectar. Revisa tu conexión.")
            }
        }
    }

    private fun iniciarTutor() {
        val idEst = idEstudiante ?: return
        if (ejercicioCargadoUnaVez) return

        val guardado = ejercicioGuardado
        if (guardado != null && modoGuardado == "repaso") {
            // ✅ FIX 3 — Solo restaurar ejercicio guardado si es modo repaso
            ejercicioCargadoUnaVez = true
            modoActual             = modoGuardado
            pistaActual            = pistaGuardada
            actualizarBadgeModo()
            currentExercise = guardado
            restaurarUIDesdeEjercicio(guardado)
            return
        }

        val prefs       = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enunciado   = prefs.getString(KEY_ENUNCIADO, null)
        val idEjercicio = prefs.getInt(KEY_ID_EJERCICIO, -1)
        val imagenUrl   = prefs.getString(KEY_IMAGEN_URL, null)
        val pistaPrefs  = prefs.getString(KEY_PISTA, null)
        val modoPrefs   = prefs.getString(KEY_MODO, "repaso") ?: "repaso"

        // ✅ FIX 3 — Si prefs tiene modo evaluacion, verificar primero si sigue activa
        if (modoPrefs == "evaluacion") {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val evalResp = RetrofitClient.tutorApi.getEvaluacionActiva(idEst)
                    hayEvaluacionActiva = evalResp.hayEvaluacion &&
                            evalResp.evaluacion != null && !evalResp.yaCompleto

                    if (hayEvaluacionActiva && evalResp.evaluacion != null) {
                        idEvaluacionActiva = evalResp.evaluacion.idEvaluacion
                        modoActual         = "evaluacion"
                        modoGuardado       = "evaluacion"
                        actualizarBadgeModo()
                        cardEjercicio.visibility        = View.GONE
                        cardEvaluacionActiva.visibility = View.VISIBLE
                        tvEvaluacionTitulo.text         = evalResp.evaluacion.titulo
                        tvEvaluacionDescripcion.text    = evalResp.evaluacion.descripcion
                            ?: "Tu docente activó una evaluación. No habrá pistas."
                        ejercicioCargadoUnaVez = true
                    } else {
                        // Evaluación ya no activa → cambiar a repaso
                        modoActual   = "repaso"
                        modoGuardado = "repaso"
                        limpiarPrefs()
                        actualizarBadgeModo()
                        ejercicioCargadoUnaVez = true
                        cargarNuevoEjercicio(null)
                    }
                } catch (_: Exception) {
                    modoActual   = "repaso"
                    modoGuardado = "repaso"
                    limpiarPrefs()
                    actualizarBadgeModo()
                    ejercicioCargadoUnaVez = true
                    cargarNuevoEjercicio(null)
                }
            }
            return
        }

        if (enunciado != null && idEjercicio > 0 && modoPrefs == "repaso") {
            ejercicioCargadoUnaVez = true
            modoActual             = modoPrefs
            modoGuardado           = modoPrefs
            pistaActual            = pistaPrefs
            actualizarBadgeModo()

            tvTituloTutor.text = "Práctica de Álgebra"
            tvEnunciado.text   = enunciado
            cargarImagenConGlide(imagenUrl)

            // ✅ Mostrar botones aunque vengan de cache
            rgAlternativas.visibility          = View.VISIBLE
            btnSubirFoto.visibility            = View.VISIBLE
            btnEnviar.visibility               = View.VISIBLE
            btnEnviar.isEnabled                = false
            btnSubirFoto.isEnabled             = true
            cardEjercicio.visibility           = View.VISIBLE
            cardFeedback.visibility            = View.GONE
            cardResultadoEvaluacion.visibility = View.GONE
            tvNivelActual.visibility           = View.GONE
            nivelActualCached?.let { mostrarNivelEnUI(it) }

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val dto = RetrofitClient.tutorApi.getNextExercise(
                        idEstudiante = idEst,
                        idDominio    = selectedDominioId,
                        ajuste       = null,
                        modo         = modoActual,
                        idEvaluacion = if (modoActual == "evaluacion") idEvaluacionActiva else null
                    )
                    if (dto.idEjercicio == idEjercicio && dto.modo == modoPrefs) {
                        currentExercise   = dto
                        ejercicioGuardado = dto
                        val botones = listOf(rbA, rbB, rbC, rbD, rbE)
                        botones.forEach { it.visibility = View.GONE }
                        dto.opciones?.forEachIndexed { index, opcion ->
                            if (index < botones.size) {
                                botones[index].apply {
                                    visibility = View.VISIBLE
                                    text       = "${opcion.letra}) ${opcion.texto}"
                                    tag        = opcion.idOpcion
                                }
                            }
                        }
                        mostrarCompetenciaTag(dto.idCompetencia)
                        // Actualizar nivel desde API (fuente autoritativa por competencia)
                        val nivelApi = dto.nivelEstudianteCompetencia
                        if (nivelApi != null) {
                            nivelActualCached = nivelApi
                            mostrarNivelEnUI(nivelApi)
                        } else {
                            nivelActualCached?.let { mostrarNivelEnUI(it) }
                        }
                        // Reinicia el cronómetro: el ejercicio recién se muestra ahora,
                        // aunque venga restaurado desde SharedPreferences (ver comentario
                        // en restaurarUIDesdeEjercicio sobre el mismo bug).
                        startTimeMillis           = System.currentTimeMillis()
                        btnEnviar.isEnabled       = true
                        rgAlternativas.visibility = View.VISIBLE
                        btnSubirFoto.visibility   = View.VISIBLE
                        btnEnviar.visibility      = View.VISIBLE
                        cargarImagenConGlide(dto.imagenUrl)
                    } else {
                        limpiarPrefs()
                        cargarNuevoEjercicio(null)
                    }
                } catch (_: Exception) {
                    tvEnunciado.text    = enunciado
                    btnEnviar.isEnabled = false
                }
            }
            return
        }

        // ✅ Sin cache → siempre repaso, verificar evaluación solo para habilitar tab
        ejercicioCargadoUnaVez = true
        modoActual   = "repaso"
        modoGuardado = "repaso"
        actualizarBadgeModo()
        cargarNuevoEjercicio(null)

        // Verificar si hay evaluación en paralelo (solo para habilitar el botón)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val evalResp = RetrofitClient.tutorApi.getEvaluacionActiva(idEst)
                hayEvaluacionActiva = evalResp.hayEvaluacion &&
                        evalResp.evaluacion != null && !evalResp.yaCompleto
                if (hayEvaluacionActiva && evalResp.evaluacion != null) {
                    idEvaluacionActiva           = evalResp.evaluacion.idEvaluacion
                    tvEvaluacionTitulo.text      = evalResp.evaluacion.titulo
                    tvEvaluacionDescripcion.text = evalResp.evaluacion.descripcion
                        ?: "Tu docente activó una evaluación. No habrá pistas."
                }
            } catch (_: Exception) { }
        }
    }

    private fun restaurarUIDesdeEjercicio(dto: TutorExerciseDTO) {
        // El cronómetro se reinicia aquí porque el estudiante ve el ejercicio
        // recién ahora (venía de caché/proceso anterior); sin esto, tiempo_respuesta
        // se calculaba contra startTimeMillis=0 y mandaba el epoch actual como
        // "segundos de respuesta" (~56 años), inflando los promedios del backend.
        startTimeMillis = System.currentTimeMillis()
        tvTituloTutor.text = if (modoActual == "evaluacion")
            "Evaluación de Álgebra" else "Práctica de Álgebra"
        tvEnunciado.text   = dto.enunciado ?: ""
        cargarImagenConGlide(dto.imagenUrl)

        val botones = listOf(rbA, rbB, rbC, rbD, rbE)
        botones.forEach { it.visibility = View.GONE }
        dto.opciones?.forEachIndexed { index, opcion ->
            if (index < botones.size) {
                botones[index].apply {
                    visibility = View.VISIBLE
                    text       = "${opcion.letra}) ${opcion.texto}"
                    tag        = opcion.idOpcion
                }
            }
        }
        mostrarCompetenciaTag(dto.idCompetencia)
        cardEjercicio.visibility           = View.VISIBLE
        cardEvaluacionActiva.visibility    = View.GONE
        cardFeedback.visibility            = View.GONE
        cardResultadoEvaluacion.visibility = View.GONE
        btnEnviar.isEnabled                = true
        btnSubirFoto.isEnabled             = true
        rgAlternativas.visibility          = View.VISIBLE
        btnSubirFoto.visibility            = View.VISIBLE
        btnEnviar.visibility               = View.VISIBLE
        val nivelRest = dto.nivelEstudianteCompetencia ?: nivelActualCached
        if (nivelRest != null) {
            nivelActualCached = nivelRest
            mostrarNivelEnUI(nivelRest)
        }
        // Restaurar pista si estaba visible antes de cerrar sesión
        val pistaVis = requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PISTA_VISIBLE, false)
        val pistaTexto = pistaActual?.takeIf { it.isNotBlank() }
        if (pistaVis && pistaTexto != null && modoActual == "repaso") {
            cardFeedback.visibility = View.VISIBLE
            tvFeedbackTitle.text    = "Pista"
            tvFeedback.text         = pistaTexto
        }
    }

    private fun mostrarMensajeSinEstudiante(mensaje: String) {
        tvTituloTutor.text  = "Módulo Tutor"
        tvEnunciado.text    = mensaje
        tvCompetenciaTag.visibility = View.GONE
        listOf(rbA, rbB, rbC, rbD, rbE).forEach { it.visibility = View.GONE }
        btnEnviar.isEnabled     = false
        btnSubirFoto.isEnabled  = false
        cardFeedback.visibility = View.GONE
    }

    private fun cargarNuevoEjercicio(ajuste: String?) {
        val idEst = idEstudiante ?: return

        modoVerificacion            = false
        actualizarBadgeModo()
        attempts                    = 0
        usoPistaActual              = false
        lastAjuste                  = ajuste
        selectedFileUri             = null
        desarrolloSubidoOk          = false
        lastRespuestaId             = null
        pistaActual                 = null
        nivelMLPendiente            = null
        idEjercicioDesarrolloSubido = null

        cardFeedback.visibility            = View.GONE
        cardResultadoEvaluacion.visibility = View.GONE
        cardEjercicio.visibility           = View.VISIBLE
        tvFeedback.text                    = ""
        rgAlternativas.clearCheck()
        listOf(rbA, rbB, rbC, rbD, rbE).forEach { it.visibility = View.GONE }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val dto = RetrofitClient.tutorApi.getNextExercise(
                    idEstudiante = idEst,
                    idDominio    = selectedDominioId,
                    ajuste       = ajuste,
                    modo         = modoActual,
                    idEvaluacion = if (modoActual == "evaluacion") idEvaluacionActiva else null  // ✅
                )
                if (dto.bloqueadoSinDiagnostico == true) {
                    mostrarMensajeSinEstudiante(dto.mensaje ?: "Tu docente aún no registró tu diagnóstico.")
                    // Permite reintentar automáticamente al volver a esta
                    // pestaña: cuando el docente registre el diagnóstico en
                    // la web, el alumno se desbloquea sin reiniciar la app.
                    ejercicioCargadoUnaVez = false
                    return@launch
                }
                if (!dto.status || dto.sinEjercicios == true || dto.opciones.isNullOrEmpty()) {
                    if (modoActual == "evaluacion" && evaluacionEnCurso) finalizarEvaluacion()
                    else mostrarFinDeEjercicios(dto.mensaje)
                    return@launch
                }
                currentExercise = dto
                bindExerciseToUI(dto)
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Error al cargar ejercicio: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun mostrarFinDeEjercicios(mensaje: String?) {
        tvTituloTutor.text  = "Módulo Tutor"
        tvEnunciado.text    = mensaje ?: "¡Completaste todos los ejercicios disponibles!"
        tvCompetenciaTag.visibility = View.GONE
        listOf(rbA, rbB, rbC, rbD, rbE).forEach { it.visibility = View.GONE }
        btnEnviar.isEnabled     = false
        btnSubirFoto.isEnabled  = false
        cardFeedback.visibility = View.GONE
        limpiarPrefs()
    }

    private fun bindExerciseToUI(dto: TutorExerciseDTO) {
        ejercicioGuardado = dto
        modoGuardado      = modoActual
        guardarEjercicioEnPrefs(dto)

        tvTituloTutor.text = when {
            modoActual == "evaluacion" -> "Evaluación de Álgebra"
            modoVerificacion           -> "Comprueba lo aprendido"
            else                       -> "Práctica de Álgebra"
        }
        tvEnunciado.text   = dto.enunciado ?: ""
        cargarImagenConGlide(dto.imagenUrl)

        val botones = listOf(rbA, rbB, rbC, rbD, rbE)
        botones.forEach { it.visibility = View.GONE }
        dto.opciones?.forEachIndexed { index, opcion ->
            if (index < botones.size) {
                botones[index].apply {
                    visibility = View.VISIBLE
                    text       = "${opcion.letra}) ${opcion.texto}"
                    tag        = opcion.idOpcion
                }
            }
        }

        pistaActual   = if (modoActual == "repaso") dto.pista else null
        pistaGuardada = pistaActual

        mostrarCompetenciaTag(dto.idCompetencia)
        cardFeedback.visibility = View.GONE
        btnEnviar.isEnabled     = true
        btnSubirFoto.isEnabled  = true

        // Usar nivel del API (por competencia del ejercicio actual) si está disponible
        val nivelFromApi = dto.nivelEstudianteCompetencia
        if (nivelFromApi != null) {
            nivelActualCached = nivelFromApi
            mostrarNivelEnUI(nivelFromApi)
            nivelMLPendiente = null
        } else {
            val nivelPend = nivelMLPendiente
            if (nivelPend != null) {
                nivelActualCached = nivelPend
                mostrarNivelEnUI(nivelPend)
                nivelMLPendiente = null
            } else if (nivelActualCached != null) {
                mostrarNivelEnUI(nivelActualCached)
            }
        }

        startTimeMillis = System.currentTimeMillis()

        // Solo mostrar radio y botones si el ejercicio es "para ahora"
        // (repaso siempre; evaluación solo si ya se inició)
        val mostrarBotones = modoActual == "repaso" ||
                (modoActual == "evaluacion" && evaluacionEnCurso)
        if (mostrarBotones) {
            rgAlternativas.visibility = View.VISIBLE
            btnSubirFoto.visibility   = View.VISIBLE
            btnEnviar.visibility      = View.VISIBLE
        }
    }

    private fun guardarEjercicioEnPrefs(dto: TutorExerciseDTO) {
        // ✅ FIX 3 — Solo guardar en prefs si es modo repaso
        if (modoActual != "repaso") return
        requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENUNCIADO,  dto.enunciado ?: "")
            .putString(KEY_IMAGEN_URL, dto.imagenUrl ?: "")
            .putInt(KEY_ID_EJERCICIO,  dto.idEjercicio ?: -1)
            .putInt(KEY_ID_COMP,       dto.idCompetencia ?: -1)
            .putString(KEY_PISTA,      dto.pista ?: "")
            .putString(KEY_MODO,       modoActual)
            .apply()
    }

    private fun limpiarPrefs() {
        requireContext()
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ENUNCIADO).remove(KEY_IMAGEN_URL)
            .remove(KEY_ID_EJERCICIO).remove(KEY_ID_COMP)
            .remove(KEY_PISTA).remove(KEY_PISTA_VISIBLE).remove(KEY_MODO)
            .apply()
        ejercicioGuardado = null
    }

    private fun actualizarIconoSonido() {
        btnSonido.setImageResource(
            if (sonidoActivado) R.drawable.ic_volume_on else R.drawable.ic_volume_off
        )
    }

    private fun reproducirSonido(correcto: Boolean) {
        if (!sonidoActivado) return
        viewLifecycleOwner.lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                if (correcto) {
                    // Victoria: do-mi-sol (C5 → E5 → G5), fanfarria de juego
                    playTone(523.25, 120)
                    playTone(659.25, 120)
                    playTone(784.00, 420)
                } else {
                    // Derrota: La-Re descendente (A4 → D4), sonido de fallo
                    playTone(440.00, 220)
                    playTone(293.66, 520)
                }
            } catch (_: Exception) {}
        }
    }

    private fun playTone(freqHz: Double, durationMs: Int) {
        val sampleRate = 44100
        val numSamples = sampleRate * durationMs / 1000
        val buffer     = ShortArray(numSamples)
        val fadeStart  = (numSamples * 0.75).toInt()
        for (i in 0 until numSamples) {
            val angle    = 2.0 * Math.PI * i * freqHz / sampleRate
            val envelope = if (i >= fadeStart) {
                (numSamples - i).toDouble() / (numSamples - fadeStart)
            } else 1.0
            buffer[i] = (Math.sin(angle) * envelope * Short.MAX_VALUE * 0.55).toInt().toShort()
        }
        val minBuf     = AudioTrack.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        val bufBytes   = maxOf(buffer.size * 2, minBuf)
        val track      = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        try {
            track.write(buffer, 0, buffer.size)
            track.play()
            Thread.sleep(durationMs.toLong() + 20)
            track.stop()
        } finally {
            track.release()
        }
    }

    private fun verificarRespuesta() {
        val ejercicio = currentExercise ?: return
        val idEj      = ejercicio.idEjercicio ?: return

        val mismoEjercicio = idEjercicioDesarrolloSubido == idEj
        if (modoActual != "evaluacion" &&
            !desarrolloSubidoOk && (selectedFileUri == null || !mismoEjercicio)) {
            selectedFileUri             = null
            idEjercicioDesarrolloSubido = null
            Toast.makeText(requireContext(),
                "📎 Sube tu desarrollo (foto o PDF) con el botón 📷 antes de responder.",
                Toast.LENGTH_LONG).show()
            return
        }

        val selectedId = rgAlternativas.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(requireContext(), "Selecciona una alternativa", Toast.LENGTH_SHORT).show()
            return
        }

        val idOpcion = requireView().findViewById<RadioButton>(selectedId).tag as? Int ?: return
        val idEst    = idEstudiante ?: return
        val tiempo   = maxOf(1, ((System.currentTimeMillis() - startTimeMillis) / 1000).toInt())
        attempts++
        btnEnviar.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Capturar el modo en el momento de enviar — evita race condition si el
                // alumno cambia de tab mientras la respuesta está en vuelo.
                val modoAlResponder = modoActual
                val req = TutorAnswerRequest(
                    idEstudiante         = idEst,
                    idEjercicio          = idEj,
                    idOpcionSeleccionada = idOpcion,
                    tiempoRespuesta      = tiempo,
                    usoPista             = usoPistaActual,
                    ajuste               = lastAjuste,
                    modo                 = modoAlResponder,
                    idEvaluacion         = idEvaluacionActiva
                )

                val resp = RetrofitClient.tutorApi.sendAnswer(req)
                lastRespuestaId = resp.idRespuesta

                if (!desarrolloSubidoOk && selectedFileUri != null) {
                    subirDesarrolloSiListo()
                }

                if (modoAlResponder == "evaluacion") {
                    totalEvaluacion++
                    if (resp.correcta) correctasEvaluacion++
                }

                if (resp.correcta) {
                    // A8: Haptic correcto — pulso corto confirmación
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        requireView().performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    else
                        requireView().performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

                    reproducirSonido(correcto = true)
                    ProgressEvents.notifyChanged()
                    nivelMLPendiente  = resp.nivelMLCompetencia
                    nivelActualCached = resp.nivelMLCompetencia
                    mostrarNivelEnUI(resp.nivelMLCompetencia)

                    rachaActual++
                    actualizarRachaBadge()
                    animarBarraFeedback(correcto = true)

                    if (modoVerificacion) {
                        Toast.makeText(requireContext(),
                            "¡Excelente! Pusiste en práctica lo que aprendiste. 🌟",
                            Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(requireContext(), "¡Correcto!", Toast.LENGTH_SHORT).show()
                    }

                    selectedFileUri             = null
                    desarrolloSubidoOk          = false
                    idEjercicioDesarrolloSubido = null
                    ejercicioGuardado           = null
                    limpiarPrefs()

                    delay(2000)
                    cargarNuevoEjercicio(resp.nuevoAjuste)

                } else {
                    // A8: Haptic incorrecto — pulso largo/doble para señal diferenciada
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        requireView().performHapticFeedback(HapticFeedbackConstants.REJECT)
                    else
                        requireView().performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

                    reproducirSonido(correcto = false)
                    mostrarNivelEnUI(resp.nivelMLCompetencia)
                    lastAjuste = resp.nuevoAjuste
                    rachaActual = 0
                    actualizarRachaBadge()
                    animarBarraFeedback(correcto = false)

                    if (modoAlResponder == "evaluacion") {
                        // En evaluación: sin pistas, sin diálogos, avanza directo
                        cardFeedback.visibility     = View.GONE
                        selectedFileUri             = null
                        desarrolloSubidoOk          = false
                        idEjercicioDesarrolloSubido = null
                        ejercicioGuardado           = null
                        limpiarPrefs()
                        Toast.makeText(requireContext(), "Incorrecto", Toast.LENGTH_SHORT).show()
                        delay(1500)
                        cargarNuevoEjercicio(resp.nuevoAjuste)
                    } else {
                        // Modo repaso
                        if (resp.mostrarPista) {
                            cardFeedback.visibility = View.VISIBLE
                            tvFeedbackTitle.text    = "Pista"
                            tvFeedback.text         = pistaActual?.takeIf { it.isNotBlank() }
                                ?: "Revisa el enunciado paso a paso."
                            usoPistaActual = true
                            requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit().putBoolean(KEY_PISTA_VISIBLE, true).apply()
                        } else {
                            cardFeedback.visibility = View.GONE
                        }

                        // Alerta docente: informar al estudiante que su docente será notificado
                        val compActual = currentExercise?.idCompetencia
                        if (resp.docenteAlertado == true && compActual != null &&
                            compActual !in competenciasAlertadas) {
                            competenciasAlertadas.add(compActual)
                            Toast.makeText(requireContext(),
                                "📩 Tu docente podrá ver que necesitas apoyo en este tema.",
                                Toast.LENGTH_LONG).show()
                        }

                        if (attempts >= 2) {
                            if (modoVerificacion) {
                                // En verificación nunca mostrar diálogo de material — avanzar directo
                                selectedFileUri             = null
                                desarrolloSubidoOk          = false
                                idEjercicioDesarrolloSubido = null
                                ejercicioGuardado           = null
                                limpiarPrefs()
                                mostrarMensajeAjusteNivel(resp.nuevoAjuste) {
                                    cargarNuevoEjercicio(resp.nuevoAjuste)
                                }
                            } else if (resp.materialSugerido != null || resp.recursosAdicionales != null) {
                                val enunciadoCorto = tvEnunciado.text.toString()
                                    .take(70).let { if (tvEnunciado.text.length > 70) "$it…" else it }
                                mostrarDialogoMaterial(resp.materialSugerido, resp.recursosAdicionales, enunciadoCorto)
                            } else {
                                selectedFileUri             = null
                                desarrolloSubidoOk          = false
                                idEjercicioDesarrolloSubido = null
                                ejercicioGuardado           = null
                                limpiarPrefs()
                                mostrarMensajeAjusteNivel(resp.nuevoAjuste) {
                                    cargarNuevoEjercicio(resp.nuevoAjuste)
                                }
                            }
                        } else {
                            selectedFileUri             = null
                            desarrolloSubidoOk          = false
                            idEjercicioDesarrolloSubido = null
                            btnEnviar.isEnabled         = false
                            Toast.makeText(requireContext(),
                                "Incorrecto. Sube un nuevo desarrollo para continuar.",
                                Toast.LENGTH_LONG).show()
                            btnSubirFoto.setOnClickListener { abrirSelectorArchivo() }
                        }
                    }
                }

            } catch (e: Exception) {
                btnEnviar.isEnabled = true
                Toast.makeText(requireContext(),
                    "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun animarBarraFeedback(correcto: Boolean) {
        val color = if (correcto) Color.parseColor("#34D399") else Color.parseColor("#F87171")
        viewProgressBar.setBackgroundColor(color)
        viewProgressBar.scaleX = 0f
        viewProgressBar.pivotX = 0f
        viewProgressBar.alpha  = 1f
        viewProgressBar.visibility = View.VISIBLE
        viewProgressBar.animate()
            .scaleX(1f)
            .setDuration(600)
            .withEndAction {
                viewLifecycleOwner.lifecycleScope.launch {
                    delay(if (correcto) 1400 else 700)
                    viewProgressBar.animate().alpha(0f).setDuration(350).withEndAction {
                        viewProgressBar.visibility = View.GONE
                        viewProgressBar.alpha = 1f
                    }.start()
                }
            }.start()
    }

    private fun actualizarRachaBadge() {
        if (rachaActual >= 2) {
            tvRachaBadge.text = "$rachaActual"
            val bgColor = if (rachaActual >= 5) Color.parseColor("#DC2626")
                          else Color.parseColor("#F59E0B")
            val rad = 50f * resources.displayMetrics.density
            tvRachaBadge.background = GradientDrawable().apply {
                setColor(bgColor); cornerRadius = rad
            }
            tvRachaBadge.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_local_fire, 0, 0, 0)
            tvRachaBadge.compoundDrawablePadding = (4 * resources.displayMetrics.density).toInt()
            tvRachaBadge.compoundDrawablesRelative[0]?.mutate()?.setTint(Color.WHITE)
            tvRachaBadge.visibility = View.VISIBLE
        } else {
            tvRachaBadge.visibility = View.GONE
        }
    }

    private fun mostrarCompetenciaTag(idCompetencia: Int?) {
        if (modoActual == "evaluacion" || idCompetencia == null) {
            tvCompetenciaTag.visibility = View.GONE
            return
        }
        val (bgColor, textColor, nombre) = when (idCompetencia) {
            1 -> Triple(0xFFE3F2FD.toInt(), 0xFF1565C0.toInt(), "Cantidad")
            2 -> Triple(0xFFEDE7F6.toInt(), 0xFF4527A0.toInt(), "Regularidad")
            3 -> Triple(0xFFE8F5E9.toInt(), 0xFF2E7D32.toInt(), "Forma y movimiento")
            4 -> Triple(0xFFFFF3E0.toInt(), 0xFFE65100.toInt(), "Datos")
            else -> Triple(0xFFE0F7FA.toInt(), 0xFF006064.toInt(), "Competencia $idCompetencia")
        }
        val rad = 50f * resources.displayMetrics.density
        tvCompetenciaTag.background = android.graphics.drawable.GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = rad
        }
        tvCompetenciaTag.setTextColor(textColor)
        tvCompetenciaTag.text = nombre
        tvCompetenciaTag.visibility = View.VISIBLE
    }

    private fun mostrarNivelEnUI(nivelML: String?) {
        if (modoActual == "evaluacion") {
            tvTituloTutor.text = "Evaluación de Álgebra"
            return
        }
        if (nivelML == null) {
            tvTituloTutor.text = "Práctica de Álgebra"
            return
        }
        val textoNivel = when (nivelML.lowercase()) {
            "alto"  -> "Nivel Alto"
            "medio" -> "Nivel Medio"
            "bajo"  -> "Nivel Básico"
            else    -> nivelML
        }
        tvTituloTutor.text       = "Práctica de Álgebra · $textoNivel"
        tvNivelActual.visibility = View.GONE
    }

    private fun mostrarMensajeAjusteNivel(ajuste: String?, onContinuar: () -> Unit) {
        if (ajuste != "mas_facil" && ajuste != "mas_dificil") {
            onContinuar()
            return
        }

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_alerta_seguimiento, null)

        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.88).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        if (ajuste == "mas_dificil") {
            dialogView.findViewById<TextView>(R.id.dialogTitle).text = "¡Nivel superado!"
            dialogView.findViewById<TextView>(R.id.dialogStudentName).apply {
                text = "Estás progresando muy bien"
                setTextColor(requireContext().getColor(R.color.ai_progress))
            }
            dialogView.findViewById<TextView>(R.id.dialogDescription).text =
                "Has demostrado dominar este nivel. " +
                "El siguiente ejercicio será un poco más desafiante."
            dialogView.findViewById<TextView>(R.id.dialogFecha).text =
                "¡Sigue así, vas subiendo de nivel!"
        } else {
            dialogView.findViewById<TextView>(R.id.dialogTitle).text = "¡Vamos paso a paso!"
            dialogView.findViewById<TextView>(R.id.dialogStudentName).apply {
                text = "Encontré ejercicios más adecuados para ti"
                setTextColor(requireContext().getColor(R.color.ai_primary))
            }
            dialogView.findViewById<TextView>(R.id.dialogDescription).text =
                "No te preocupes — reforzar los fundamentos es parte del aprendizaje. " +
                "El siguiente ejercicio te ayudará a consolidar mejor el tema."
            dialogView.findViewById<TextView>(R.id.dialogFecha).text =
                "📘 Cada error es una oportunidad de aprender."
        }

        dialogView.findViewById<TextView>(R.id.btnAtendida).apply {
            text = "¡Entendido, continuar! ▶"
            setOnClickListener { dialog.dismiss(); onContinuar() }
        }
        dialogView.findViewById<TextView>(R.id.btnCancelar).visibility = View.GONE
        dialog.show()
    }

    private fun mostrarDialogoMaterial(
        material:         MaterialSugeridoDTO?,
        recursos:         RecursosAdicionalesDTO?,
        enunciadoCorto:   String? = null
    ) {
        // Capturar ejercicio fallado ANTES de mostrar el diálogo para la verificación post-refuerzo
        val idCompFallado = currentExercise?.idCompetencia
        val idEjFallado   = currentExercise?.idEjercicio

        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_material_sugerido, null)

        // ── Vistas del material del docente ──────────────────────────────────
        val layoutDocente  = dialogView.findViewById<View>(R.id.layoutMaterialDocente)
        val tvTipo         = dialogView.findViewById<TextView>(R.id.tvDialogMaterialTipo)
        val tvNombre       = dialogView.findViewById<TextView>(R.id.tvDialogMaterialNombre)
        val tvContexto     = dialogView.findViewById<TextView>(R.id.tvDialogContexto)
        val btnAbrir       = dialogView.findViewById<MaterialButton>(R.id.btnDialogAbrirMaterial)
        val layoutTimer    = dialogView.findViewById<View>(R.id.layoutTimer)
        val progressTimer  = dialogView.findViewById<android.widget.ProgressBar>(R.id.progressTimer)
        val tvCountdown    = dialogView.findViewById<TextView>(R.id.tvTimerCountdown)

        // ── Vistas de recursos en línea ───────────────────────────────────────
        val layoutRecursos = dialogView.findViewById<View>(R.id.layoutRecursosOnline)
        val btnYoutube     = dialogView.findViewById<MaterialButton>(R.id.btnDialogYoutube)
        val btnWeb         = dialogView.findViewById<MaterialButton>(R.id.btnDialogWebRecursos)
        val btnPdf         = dialogView.findViewById<MaterialButton>(R.id.btnDialogPdf)

        val btnContin            = dialogView.findViewById<MaterialButton>(R.id.btnDialogContinuar)
        val layoutCheckpoint     = dialogView.findViewById<android.view.View>(R.id.layoutCheckpoint)
        val tvCheckpointEnun     = dialogView.findViewById<TextView>(R.id.tvCheckpointEnunciado)
        val btnCheckpointSi      = dialogView.findViewById<MaterialButton>(R.id.btnCheckpointSi)
        val btnCheckpointRevisar = dialogView.findViewById<MaterialButton>(R.id.btnCheckpointRevisar)

        // ── Estado del diálogo ────────────────────────────────────────────────
        var timerJob:         Job?  = null
        var waitJob:          Job?  = null   // bloqueo inicial de 60s
        var materialAbierto         = false
        var tiempoTranscurrido      = 0
        var tiempoInicioMaterial    = 0L
        // Usar el tiempo estimado real del material; mínimo 120s, máximo 600s
        val tiempoMinimo = material?.tiempoEstimado?.coerceIn(120, 600) ?: 300

        // ── Contexto del fallo — muestra el enunciado truncado ───────────────
        if (!enunciadoCorto.isNullOrBlank()) {
            tvContexto.text       = "Intentaste este ejercicio 2 veces. ¡Está bien! Revisa el material y vuelve a intentarlo."
            tvContexto.visibility = View.VISIBLE
        }

        // ── Mostrar sección material si el docente asignó uno ────────────────
        if (material != null) {
            layoutDocente.visibility = View.VISIBLE
            val esVideo = material.tipo.lowercase().contains("video")
            tvTipo.text   = if (esVideo) "▶  Video recomendado" else "🔗  Material de estudio"
            tvNombre.text = material.titulo
        } else {
            layoutDocente.visibility = View.GONE
        }

        // ── Mostrar sección búsqueda si el API devolvió URLs ─────────────────
        if (recursos != null) {
            layoutRecursos.visibility = View.VISIBLE
        } else {
            layoutRecursos.visibility = View.GONE
        }

        // btnContin inicia deshabilitado — el XML ya lo pone en enabled=false/alpha=0.5
        // Se habilita después de 60s o cuando el timer del material completa

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        // Limitar altura al 88% de la pantalla para que siempre quepa con scroll
        dialog.setOnShowListener {
            val dm = resources.displayMetrics
            dialog.window?.setLayout(
                (dm.widthPixels * 0.92).toInt(),
                (dm.heightPixels * 0.88).toInt()
            )
        }

        // ── Bloqueo inicial de 60s — fuerza al menos leer el título del material ──
        waitJob = viewLifecycleOwner.lifecycleScope.launch {
            for (i in 60 downTo 1) {
                if (!isActive) break
                btnContin.text = "Disponible en ${i}s…"
                delay(1000)
            }
            if (isActive && !materialAbierto) {
                btnContin.isEnabled = true
                btnContin.alpha     = 1f
                btnContin.text      = "Continuar sin revisar el material"
            }
        }

        // ── Observer para el timer cuando el alumno abre el material ─────────
        val lifecycleObserver = object : androidx.lifecycle.DefaultLifecycleObserver {
            override fun onResume(owner: androidx.lifecycle.LifecycleOwner) {
                if (!materialAbierto || !dialog.isShowing) return
                val tiempoFuera    = ((System.currentTimeMillis() - tiempoInicioMaterial) / 1000).toInt()
                tiempoTranscurrido = tiempoFuera
                timerJob?.cancel()

                if (tiempoTranscurrido >= tiempoMinimo) {
                    progressTimer.progress = 100
                    tvCountdown.text = "¡Listo! Ya puedes continuar."
                    tvCountdown.setTextColor(
                        androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ai_success))
                    btnContin.visibility = View.VISIBLE
                    btnContin.isEnabled  = true
                    btnContin.text       = "Continuar al siguiente ▶"
                    btnContin.alpha      = 1f
                } else {
                    btnContin.visibility = View.VISIBLE
                    btnContin.isEnabled  = true
                    btnContin.text       = "Salir (quedará inconcluso)"
                    btnContin.alpha      = 0.8f

                    timerJob = viewLifecycleOwner.lifecycleScope.launch {
                        while (tiempoTranscurrido < tiempoMinimo) {
                            val restante = tiempoMinimo - tiempoTranscurrido
                            val progreso = (tiempoTranscurrido * 100 / tiempoMinimo).coerceIn(0, 100)
                            val minutos  = restante / 60
                            val segundos = restante % 60
                            progressTimer.progress = progreso
                            tvCountdown.text = if (minutos > 0)
                                "Tiempo restante: ${minutos}m ${segundos}s"
                            else "Tiempo restante: ${segundos}s"
                            delay(1000)
                            tiempoTranscurrido++
                        }
                        progressTimer.progress = 100
                        tvCountdown.text = "¡Listo! Ya puedes continuar."
                        tvCountdown.setTextColor(
                            androidx.core.content.ContextCompat.getColor(requireContext(), R.color.ai_success))
                        btnContin.text  = "Continuar al siguiente ▶"
                        btnContin.alpha = 1f
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycle.addObserver(lifecycleObserver)

        // ── Abrir material del docente ────────────────────────────────────────
        btnAbrir.setOnClickListener {
            if (material == null) return@setOnClickListener
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(material.url)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No se pudo abrir el enlace.", Toast.LENGTH_SHORT).show()
            }
            if (!materialAbierto) {
                materialAbierto        = true
                tiempoInicioMaterial   = System.currentTimeMillis()
                waitJob?.cancel()      // el timer del material toma el control
                btnAbrir.text          = "Ya lo estoy revisando"
                btnAbrir.isEnabled     = false
                layoutTimer.visibility = View.VISIBLE
                btnContin.visibility   = View.GONE
            }
        }

        // ── Helper: abrir YouTube (ACTION_VIEW funciona bien con youtube.com) ──
        fun abrirYoutube(url: String) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "No se pudo abrir YouTube.", Toast.LENGTH_SHORT).show()
            }
        }

        // ── Helper: búsqueda web segura (evita crash del app Google) ─────────
        // ACTION_WEB_SEARCH abre el browser predeterminado sin que Google Search lo intercepte
        fun buscarEnWeb(query: String, fallbackUrl: String) {
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH)
                intent.putExtra(SearchManager.QUERY, query)
                if (intent.resolveActivity(requireContext().packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Fallback: abrir en browser con URI directamente
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                }
            } catch (e: Exception) {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)))
                } catch (e2: Exception) {
                    Toast.makeText(requireContext(),
                        "No se pudo abrir. Verifica tu conexión.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // ── Botones de búsqueda en línea ─────────────────────────────────────
        btnYoutube.setOnClickListener {
            recursos?.let { abrirYoutube(it.youtubeUrl) }
        }
        btnWeb.setOnClickListener {
            recursos?.let {
                val query = "${it.query} matemáticas ejercicio resuelto"
                buscarEnWeb(query, it.webUrl)
            }
        }
        btnPdf.setOnClickListener {
            recursos?.let {
                val query = "${it.query} matemáticas pdf apuntes ejercicios"
                buscarEnWeb(query, it.pdfUrl)
            }
        }

        // ── Regresar al mismo ejercicio desde el checkpoint (el alumno dijo "Sí, puedo") ──
        fun regresarAlMismoEjercicio() {
            waitJob?.cancel()
            timerJob?.cancel()
            viewLifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (materialAbierto && material != null) {
                registrarMaterialInconcluso(material.idMaterial, tiempoTranscurrido)
            }
            dialog.dismiss()
            selectedFileUri             = null
            desarrolloSubidoOk          = false
            idEjercicioDesarrolloSubido = null
            rgAlternativas.clearCheck()
            val ej = currentExercise ?: return
            restaurarUIDesdeEjercicio(ej)
            // Volver a mostrar la pista si el alumno ya la había visto (ya falló una vez)
            val pista = pistaActual?.takeIf { it.isNotBlank() }
            if (pista != null && modoActual == "repaso") {
                cardFeedback.visibility = View.VISIBLE
                tvFeedbackTitle.text    = "Pista"
                tvFeedback.text         = pista
            }
        }

        // ── Lógica de salida hacia nuevo ejercicio ──────────────────────────────
        fun ejecutarContinuacion() {
            waitJob?.cancel()
            timerJob?.cancel()
            viewLifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (materialAbierto && material != null) {
                registrarMaterialInconcluso(material.idMaterial, tiempoTranscurrido)
            }
            dialog.dismiss()
            selectedFileUri             = null
            desarrolloSubidoOk          = false
            idEjercicioDesarrolloSubido = null
            ejercicioGuardado           = null
            limpiarPrefs()
            if (idCompFallado != null && idEjFallado != null) {
                cargarEjercicioVerificacion(idCompFallado, idEjFallado)
            } else {
                mostrarMensajeAjusteNivel(lastAjuste) {
                    cargarNuevoEjercicio(lastAjuste)
                }
            }
        }

        // ── Continuar / salir ─────────────────────────────────────────────────
        btnContin.setOnClickListener {
            // Si el alumno SÍ revisó el material → mostrar checkpoint de comprensión
            val enunciado = enunciadoCorto?.takeIf { it.isNotBlank() }
            if (materialAbierto && enunciado != null) {
                // Animar transición: ocultar material, mostrar checkpoint
                btnContin.visibility = android.view.View.GONE
                layoutCheckpoint.visibility = android.view.View.VISIBLE
                tvCheckpointEnun.text = enunciado

                btnCheckpointSi.setOnClickListener { regresarAlMismoEjercicio() }
                btnCheckpointRevisar.setOnClickListener {
                    // Volver al material: ocultar checkpoint, mostrar botón
                    layoutCheckpoint.visibility = android.view.View.GONE
                    btnContin.visibility = android.view.View.VISIBLE
                }
            } else {
                // Sin enunciado (raro) o no abrió material → salir directo
                ejecutarContinuacion()
            }
        }

        dialog.setOnDismissListener {
            waitJob?.cancel()
            timerJob?.cancel()
            viewLifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
        }

        dialog.show()
    }

    private fun registrarMaterialInconcluso(idMaterial: Int, tiempoVisto: Int) {
        val idEst = idEstudiante ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.historialMaterialApi.registrarHistorial(
                    HistorialMaterialRequest(
                        idEstudiante  = idEst,
                        idMaterial    = idMaterial,
                        estado        = "inconcluso",
                        tiempoVisto   = tiempoVisto,
                        vecesRevisado = 1
                    )
                )
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun registrarAperturaMaterial(idMaterial: Int) {
        val idEst = idEstudiante ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RetrofitClient.tutorApi.registrarAperturaMaterial(
                    AperturaMaterialRequest(idEstudiante = idEst, idMaterial = idMaterial)
                )
            } catch (e: Exception) {
                println("Error registrando apertura material: ${e.localizedMessage}")
            }
        }
    }

    private fun finalizarEvaluacion() {
        val idEst = idEstudiante ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val idEval = idEvaluacionActiva
                if (idEval != null) {
                    val respFin = RetrofitClient.tutorApi.finalizarEvaluacion(
                        mapOf("idEstudiante" to idEst, "idEvaluacion" to idEval)
                    )
                    if (respFin.status) {
                        mostrarResultadoEvaluacion(
                            respFin.totalCorrectas, respFin.totalPreguntas, respFin.puntajeTotal)
                    } else {
                        val puntaje = if (totalEvaluacion > 0)
                            correctasEvaluacion * 100 / totalEvaluacion else 0
                        mostrarResultadoEvaluacion(correctasEvaluacion, totalEvaluacion, puntaje)
                    }
                } else {
                    val puntaje = if (totalEvaluacion > 0)
                        correctasEvaluacion * 100 / totalEvaluacion else 0
                    mostrarResultadoEvaluacion(correctasEvaluacion, totalEvaluacion, puntaje)
                }
                evaluacionEnCurso      = false
                hayEvaluacionActiva    = false
                idEvaluacionActiva     = null
                ejercicioGuardado      = null
                modoActual             = "repaso"
                modoGuardado           = "repaso"
                limpiarPrefs()
            } catch (e: Exception) {
                val puntaje = if (totalEvaluacion > 0)
                    correctasEvaluacion * 100 / totalEvaluacion else 0
                mostrarResultadoEvaluacion(correctasEvaluacion, totalEvaluacion, puntaje)
            }
        }
    }

    private fun mostrarResultadoEvaluacion(correctas: Int, total: Int, puntaje: Int) {
        cardEjercicio.visibility           = View.GONE
        rgAlternativas.visibility          = View.GONE
        cardFeedback.visibility            = View.GONE
        btnEnviar.visibility               = View.GONE
        btnSubirFoto.visibility            = View.GONE
        cardResultadoEvaluacion.visibility = View.VISIBLE
        tvResultadoCorrectas.text          = "Correctas: $correctas / $total"
        tvResultadoPuntaje.text            = "$puntaje%"
        tvTituloTutor.text                 = "Evaluación completada"

        val (mensaje, colorHex, nivelTextoRes) = when {
            puntaje >= 90 -> Triple("¡Excelente! Dominas estos temas.",     "#34D399", "Nivel Destacado")
            puntaje >= 70 -> Triple("¡Muy bien! Estás en el nivel esperado.", "#34D399", "Nivel Logrado")
            puntaje >= 50 -> Triple("Buen intento. Practica en el Tutor para mejorar.", "#FB923C", "En Proceso")
            else          -> Triple("No te desanimes: el Tutor te ayudará a avanzar.",  "#F87171", "En Inicio")
        }
        val color = Color.parseColor(colorHex)

        // A11: confetti al alcanzar nivel logrado o destacado
        if (puntaje >= 70) {
            view?.post { confettiView.launch() }
        }

        // Donut con el puntaje
        donutResultado.setPercentage(puntaje.coerceIn(0, 100).toFloat(), color)
        tvResultadoPuntaje.setTextColor(color)

        // Badge de nivel MINEDU
        tvNivelResultado.text = nivelTextoRes
        tvNivelResultado.background = GradientDrawable().apply {
            setColor(Color.parseColor(colorHex + "33")) // 20% opacity
            cornerRadius = 50f * resources.displayMetrics.density
        }
        tvNivelResultado.setTextColor(color)
        val iconNivelRes = when {
            puntaje >= 90 -> R.drawable.ic_star
            puntaje >= 70 -> R.drawable.ic_check_circle_24
            puntaje >= 50 -> R.drawable.ic_sync
            else          -> R.drawable.ic_flag
        }
        tvNivelResultado.setCompoundDrawablesRelativeWithIntrinsicBounds(iconNivelRes, 0, 0, 0)
        tvNivelResultado.compoundDrawablePadding = (4 * resources.displayMetrics.density).toInt()
        tvNivelResultado.compoundDrawablesRelative[0]?.mutate()?.setTint(color)
        tvNivelResultado.visibility = View.VISIBLE

        val tvMensaje = view?.findViewById<TextView>(R.id.tvResultadoMensaje)
        tvMensaje?.text = mensaje
        tvMensaje?.visibility = View.VISIBLE

        // Botón "Ver mi progreso →" navega al tab ProgresoFragment (índice 3)
        val btnVerProgreso = view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnVerProgreso)
        btnVerProgreso?.visibility = View.VISIBLE
        btnVerProgreso?.setOnClickListener {
            try {
                requireActivity().findViewById<ViewPager2>(R.id.viewPager).currentItem = 3
            } catch (_: Exception) { }
        }

        // ✅ Botón para verificar nueva evaluación o ir a repaso
        val btnVolverRepaso = view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnVolverRepaso)
        btnVolverRepaso?.visibility = View.VISIBLE
        btnVolverRepaso?.setOnClickListener {
            val idEst = idEstudiante ?: return@setOnClickListener
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val resp = RetrofitClient.tutorApi.getEvaluacionActiva(idEst)
                    val hayNueva = resp.hayEvaluacion && resp.evaluacion != null && !resp.yaCompleto
                    if (hayNueva && resp.evaluacion != null) {
                        hayEvaluacionActiva              = true
                        idEvaluacionActiva               = resp.evaluacion.idEvaluacion
                        cardResultadoEvaluacion.visibility = View.GONE
                        btnVolverRepaso?.visibility       = View.GONE
                        tvEvaluacionTitulo.text           = resp.evaluacion.titulo
                        tvEvaluacionDescripcion.text      = resp.evaluacion.descripcion
                            ?: "Tu docente activó una nueva evaluación."
                        modoActual   = "evaluacion"
                        modoGuardado = "evaluacion"
                        actualizarBadgeModo()
                        cardEvaluacionActiva.visibility   = View.VISIBLE
                    } else {
                        // No hay nueva → ir a repaso
                        hayEvaluacionActiva = false
                        idEvaluacionActiva  = null
                        pasarARepaso()
                    }
                } catch (_: Exception) {
                    pasarARepaso()
                }
            }
        }
    }

    private fun mostrarDialogoSalirEvaluacion() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_salir_evaluacion, null)

        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(dialogView)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnContinuarEval
        ).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<com.google.android.material.button.MaterialButton>(
            R.id.btnSalirEval
        ).setOnClickListener {
            dialog.dismiss()
            evaluacionEnCurso   = false
            hayEvaluacionActiva = false
            idEvaluacionActiva  = null
            modoActual          = "repaso"
            modoGuardado        = "repaso"
            nivelActualCached   = null
            nivelMLPendiente    = null
            actualizarBadgeModo()
            cardEvaluacionActiva.visibility    = View.GONE
            cardResultadoEvaluacion.visibility = View.GONE
            cardFeedback.visibility            = View.GONE
            cardEjercicio.visibility           = View.VISIBLE
            btnSubirFoto.visibility            = View.VISIBLE
            btnEnviar.visibility               = View.VISIBLE
            val prevExercise = repasoExerciseBeforeEval
            repasoExerciseBeforeEval = null
            if (prevExercise != null) {
                ejercicioCargadoUnaVez = true
                currentExercise        = prevExercise
                ejercicioGuardado      = prevExercise
                pistaActual            = prevExercise.pista
                guardarEjercicioEnPrefs(prevExercise)
                restaurarUIDesdeEjercicio(prevExercise)
            } else {
                ejercicioGuardado      = null
                currentExercise        = null
                ejercicioCargadoUnaVez = false
                limpiarPrefs()
                rgAlternativas.clearCheck()
                listOf(rbA, rbB, rbC, rbD, rbE).forEach { it.visibility = View.GONE }
                cargarNuevoEjercicio(null)
            }
        }

        dialog.show()
    }

    private fun pasarARepaso() {
        modoActual               = "repaso"
        modoGuardado             = "repaso"
        evaluacionEnCurso        = false
        ejercicioGuardado        = null
        currentExercise          = null
        repasoExerciseBeforeEval = null  // evaluación completada → nueva sesión repaso
        ejercicioCargadoUnaVez   = true  // evita doble carga si el fragment se recrea
        nivelActualCached        = null  // limpiar nivel cacheado de la evaluación
        nivelMLPendiente         = null
        limpiarPrefs()
        actualizarBadgeModo()
        // Limpiar texto viejo para que no se vea el ejercicio de evaluación detrás
        tvEnunciado.text                   = "Cargando ejercicio..."
        tvTituloTutor.text                 = "Práctica de Álgebra"
        cardResultadoEvaluacion.visibility = View.GONE
        cardEvaluacionActiva.visibility    = View.GONE
        cardFeedback.visibility            = View.GONE
        rgAlternativas.clearCheck()
        listOf(rbA, rbB, rbC, rbD, rbE).forEach { it.visibility = View.GONE }
        cardEjercicio.visibility           = View.VISIBLE
        rgAlternativas.visibility          = View.GONE  // se mostrará cuando cargue el ejercicio
        btnSubirFoto.visibility            = View.VISIBLE
        btnEnviar.visibility               = View.VISIBLE
        view?.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnVolverRepaso)
            ?.visibility = View.GONE
        cargarNuevoEjercicio(null)
    }

    private fun abrirSelectorArchivo() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
        }
        val prefs   = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUri = prefs.getString(KEY_LAST_FILE, null)
        if (lastUri != null) {
            try {
                intent.putExtra(
                    android.provider.DocumentsContract.EXTRA_INITIAL_URI, Uri.parse(lastUri))
            } catch (_: Exception) {}
        }
        startActivityForResult(Intent.createChooser(intent, "Selecciona imagen o PDF"), REQ_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PICK_FILE && resultCode == Activity.RESULT_OK) {
            selectedFileUri = data?.data
            if (selectedFileUri != null) {
                idEjercicioDesarrolloSubido = currentExercise?.idEjercicio
                requireContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    .edit().putString(KEY_LAST_FILE, selectedFileUri.toString()).apply()
                btnEnviar.isEnabled = true
                if (lastRespuestaId == null) {
                    Toast.makeText(requireContext(),
                        "Archivo listo. Se subirá al verificar.", Toast.LENGTH_LONG).show()
                } else {
                    subirDesarrolloSiListo()
                }
            }
        }
    }

    private fun subirDesarrolloSiListo() {
        val idRespuesta = lastRespuestaId ?: return
        val uri         = selectedFileUri  ?: return
        if (desarrolloSubidoOk) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val file        = copiarUriATmpFile(uri) ?: return@launch
                val ext         = file.extension.ifEmpty { "jpg" }
                val requestFile = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val filePart    = MultipartBody.Part.createFormData(
                    "archivo", "desarrollo_${idRespuesta}.$ext", requestFile)
                val idBody = idRespuesta.toString().toRequestBody("text/plain".toMediaTypeOrNull())

                val resp = RetrofitClient.tutorApi.uploadDevelopment(idBody, filePart)
                if (resp.status) {
                    desarrolloSubidoOk = true
                    Toast.makeText(requireContext(), "Desarrollo subido.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Error: ${resp.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun copiarUriATmpFile(uri: Uri): File? {
        return try {
            val input = requireContext().contentResolver.openInputStream(uri) ?: return null
            val name  = obtenerNombreArchivo(uri) ?: "tmp_desarrollo"
            val file  = File(requireContext().cacheDir, name)
            FileOutputStream(file).use { input.copyTo(it) }
            file
        } catch (e: Exception) { null }
    }

    private fun obtenerNombreArchivo(uri: Uri): String? {
        var nombre: String? = null
        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) nombre = it.getString(idx)
                }
            }
        }
        return nombre ?: uri.path?.substringAfterLast('/')
    }

    private fun cargarEjercicioVerificacion(idComp: Int, idEjFallado: Int) {
        val idEst = idEstudiante ?: return

        modoVerificacion            = true
        attempts                    = 0
        usoPistaActual              = false
        selectedFileUri             = null
        desarrolloSubidoOk          = false
        lastRespuestaId             = null
        nivelMLPendiente            = null
        idEjercicioDesarrolloSubido = null

        cardFeedback.visibility            = View.GONE
        cardResultadoEvaluacion.visibility = View.GONE
        cardEjercicio.visibility           = View.VISIBLE
        tvFeedback.text                    = ""
        rgAlternativas.clearCheck()
        listOf(rbA, rbB, rbC, rbD, rbE).forEach { it.visibility = View.GONE }

        tvTituloTutor.text = "Comprueba lo aprendido"
        tvBadgeModo.text   = "Verificación — ¿Comprendiste el concepto?"
        tvBadgeModo.setTextColor(resources.getColor(R.color.ai_warning, null))

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val dto = RetrofitClient.tutorApi.getNextExercise(
                    idEstudiante       = idEst,
                    idDominio          = idComp,
                    ajuste             = null,
                    modo               = "repaso",
                    idEvaluacion       = null,
                    postRefuerzo       = true,
                    idEjercicioFallado = idEjFallado
                )
                if (!dto.status || dto.sinEjercicios == true || dto.opciones.isNullOrEmpty()) {
                    // Sin ejercicio de verificación disponible → avanzar normal
                    modoVerificacion = false
                    actualizarBadgeModo()
                    mostrarMensajeAjusteNivel(lastAjuste) {
                        cargarNuevoEjercicio(lastAjuste)
                    }
                    return@launch
                }
                currentExercise = dto
                bindExerciseToUI(dto)
                // Sobreescribir título que bindExerciseToUI pone en modo repaso
                tvTituloTutor.text = "Comprueba lo aprendido"
            } catch (e: Exception) {
                modoVerificacion = false
                actualizarBadgeModo()
                Toast.makeText(requireContext(),
                    "Error al cargar ejercicio: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }
}