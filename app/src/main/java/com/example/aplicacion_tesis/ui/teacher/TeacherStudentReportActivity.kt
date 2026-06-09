package com.example.aplicacion_tesis.ui.teacher

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.aplicacion_tesis.R
import com.example.aplicacion_tesis.model.dto.MaterialStatItemDTO
import com.example.aplicacion_tesis.model.dto.ProgresoPorCompetenciaItemDTO
import com.example.aplicacion_tesis.model.dto.TiempoNivelItemDTO
import com.example.aplicacion_tesis.network.RetrofitClient
import com.example.aplicacion_tesis.network.TokenStore
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.RadarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TeacherStudentReportActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvHeaderTitle: TextView
    private lateinit var tvStudentAvatar: TextView
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentSubtitle: TextView
    private lateinit var chartProgress: com.github.mikephil.charting.charts.LineChart
    private lateinit var chartRadar: RadarChart

    private lateinit var tvReportFrecEjercicios: TextView
    private lateinit var tvReportFrecMateriales: TextView
    private lateinit var tvReportFrecTotal: TextView
    private lateinit var tvReportFrecUltima: TextView

    // ── Card materiales de estudio ────────────────────────────────────────────
    private lateinit var tvMatTotalRevisiones: TextView
    private lateinit var tvMatTiempoTotal    : TextView
    private lateinit var tvMatDistintos      : TextView
    private lateinit var tvMatEmpty          : TextView
    private lateinit var chartMatRevisiones  : HorizontalBarChart

    // ── Card tiempo por dificultad ────────────────────────────────────────────
    private lateinit var llTiempoNivelDocente:   LinearLayout
    private lateinit var tvTiempoNivelDocenteEmpty: TextView

    private var studentId: Int = -1
    private var studentProgress: Int = 0
    private var studentName: String = "Estudiante"

    // =============================================
    // COLORES — consistentes con toda la app
    // =============================================
    private val COLOR_C1 = Color.parseColor("#0A6FD4") // Azul   — Cantidad
    private val COLOR_C2 = Color.parseColor("#27AE60") // Verde  — Regularidad
    private val COLOR_C3 = Color.parseColor("#E67E22") // Naranja— Forma
    private val COLOR_C4 = Color.parseColor("#7B1FA2") // Morado — Datos

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teacher_student_report)

        bindViews()

        studentId       = intent.getIntExtra("student_id", -1)
        studentProgress = intent.getIntExtra("student_progress", 0)
        studentName     = intent.getStringExtra("student_name") ?: "Estudiante"

        tvHeaderTitle.text     = "Reporte de Estudiante"
        tvStudentName.text     = studentName
        tvStudentSubtitle.text = "Progreso general: $studentProgress%"
        tvStudentAvatar.text   = obtenerIniciales(studentName)

        btnBack.setOnClickListener { finish() }

        if (studentId <= 0) {
            Toast.makeText(this, "No se encontró el estudiante.", Toast.LENGTH_LONG).show()
            return
        }

        loadStudentDetails()
    }

    private fun bindViews() {
        btnBack                = findViewById(R.id.btnBack)
        tvHeaderTitle          = findViewById(R.id.tvHeaderTitle)
        tvStudentAvatar        = findViewById(R.id.tvStudentAvatar)
        tvStudentName          = findViewById(R.id.tvStudentName)
        tvStudentSubtitle      = findViewById(R.id.tvStudentSubtitle)
        chartProgress          = findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.chartProgressGeneral)
        chartRadar             = findViewById(R.id.chartCompetencias)
        tvReportFrecEjercicios = findViewById(R.id.tvReportFrecEjercicios)
        tvReportFrecMateriales = findViewById(R.id.tvReportFrecMateriales)
        tvReportFrecTotal      = findViewById(R.id.tvReportFrecTotal)
        tvReportFrecUltima     = findViewById(R.id.tvReportFrecUltima)

        tvMatTotalRevisiones     = findViewById(R.id.tvMatTotalRevisiones)
        tvMatTiempoTotal         = findViewById(R.id.tvMatTiempoTotal)
        tvMatDistintos           = findViewById(R.id.tvMatDistintos)
        tvMatEmpty               = findViewById(R.id.tvMatEmpty)
        chartMatRevisiones       = findViewById(R.id.chartMatRevisiones)
        llTiempoNivelDocente     = findViewById(R.id.llTiempoNivelDocente)
        tvTiempoNivelDocenteEmpty= findViewById(R.id.tvTiempoNivelDocenteEmpty)
    }

    // =============================================
    // CARGAR DATOS
    // =============================================
    private fun loadStudentDetails() {
        MainScope().launch(Dispatchers.IO) {
            try {
                val comp      = RetrofitClient.progresoApi.getPorCompetencia(studentId)
                val temas     = if (comp.status) comp.temas ?: emptyList() else emptyList()

                val resumen      = RetrofitClient.progresoApi.getResumen(studentId)
                val progresoReal = if (resumen.status) resumen.nivelPorcentaje else studentProgress

                // ✅ Datos reales del gráfico
                var chartPoints: List<com.example.aplicacion_tesis.model.dto.ChartPoint> = emptyList()
                try {
                    val chartResp = RetrofitClient.progresoApi.getChart(studentId)
                    if (chartResp.status && !chartResp.datosChart.isNullOrEmpty()) {
                        chartPoints = chartResp.datosChart
                    }
                } catch (_: Exception) {}

                var ejercicios  = 0
                var materiales  = 0
                var total       = 0
                var ultimaActiv = "Sin actividad"
                val idDocente   = TokenStore.teacherId
                if (idDocente != null && idDocente > 0) {
                    try {
                        val frecResp = RetrofitClient.docenteApi.getFrecuenciaUso(idDocente)
                        frecResp.data?.firstOrNull { it.idEstudiante == studentId }?.let {
                            ejercicios  = it.ejerciciosRespondidos
                            materiales  = it.materialesVistos
                            total       = it.totalInteracciones
                            ultimaActiv = it.ultimaActividad
                        }
                    } catch (_: Exception) {}
                }

                // ── Estadísticas de materiales de estudio ──────────────────
                var matRevisiones = 0
                var matTiempoMin  = 0f
                var matDistintos  = 0
                var matDetalle    = listOf<com.example.aplicacion_tesis.model.dto.MaterialStatItemDTO>()
                if (idDocente != null && idDocente > 0) {
                    try {
                        val statsResp = RetrofitClient.docenteApi.getMaterialesStats(idDocente, studentId)
                        if (statsResp.status) {
                            matRevisiones = statsResp.resumen?.totalRevisiones ?: 0
                            matTiempoMin  = statsResp.resumen?.tiempoTotalMin  ?: 0f
                            matDistintos  = statsResp.resumen?.materialesDistintos ?: 0
                            matDetalle    = statsResp.detalle ?: emptyList()
                        }
                    } catch (_: Exception) {}
                }

                // ── Tiempo por nivel de dificultad ────────────────────────
                var tiempoNiveles: List<TiempoNivelItemDTO> = emptyList()
                try {
                    val tiempoResp = RetrofitClient.progresoApi.getTiempoPorNivel(studentId)
                    if (tiempoResp.status) tiempoNiveles = tiempoResp.niveles
                } catch (_: Exception) {}

                val ejFinal       = ejercicios
                val matFinal      = materiales
                val totFinal      = total
                val ultFinal      = ultimaActiv
                val progFinal     = progresoReal
                val chartFinal    = chartPoints
                val matRevFinal   = matRevisiones
                val matMinFinal   = matTiempoMin
                val matDistFinal  = matDistintos
                val matDetFinal   = matDetalle
                val tiempoFinal   = tiempoNiveles

                withContext(Dispatchers.Main) {
                    tvStudentSubtitle.text = "Progreso general: $progFinal%"
                    setupProgressChart(progFinal, chartFinal)
                    setupRadarChart(temas)
                    tvReportFrecEjercicios.text = ejFinal.toString()
                    tvReportFrecMateriales.text = matFinal.toString()
                    tvReportFrecTotal.text      = totFinal.toString()
                    tvReportFrecUltima.text     = "Última actividad: $ultFinal"
                    // ── Materiales de estudio ──────────────────────────────
                    setupMaterialesChart(matRevFinal, matMinFinal, matDistFinal, matDetFinal)
                    // ── Tiempo por dificultad ──────────────────────────────
                    setupTiempoNivel(tiempoFinal)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TeacherStudentReportActivity,
                        "Error al cargar el reporte", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // =============================================
    // GRÁFICO DE BARRAS — Evolución estimada
    // =============================================
    private fun setupProgressChart(
        progressPercent: Int,
        chartPoints: List<com.example.aplicacion_tesis.model.dto.ChartPoint>
    ) {
        val lineChart = chartProgress as? com.github.mikephil.charting.charts.LineChart ?: return

        // ✅ Si hay datos reales los usa, si no genera 4 puntos estimados
        val entries: List<com.github.mikephil.charting.data.Entry>
        val labels:  List<String>

        if (chartPoints.isNotEmpty()) {
            entries = chartPoints.mapIndexed { i, p ->
                com.github.mikephil.charting.data.Entry(i.toFloat(), p.puntaje.toFloat())
            }
            labels = chartPoints.map { it.fecha }
        } else {
            val p = progressPercent.coerceIn(0, 100)
            entries = listOf(
                com.github.mikephil.charting.data.Entry(0f, p * 0.60f),
                com.github.mikephil.charting.data.Entry(1f, p * 0.75f),
                com.github.mikephil.charting.data.Entry(2f, p * 0.90f),
                com.github.mikephil.charting.data.Entry(3f, p.toFloat())
            )
            labels = listOf("Semana 1", "Semana 2", "Semana 3", "Semana 4")
        }

        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "% Correcto").apply {
            color              = COLOR_C1
            setCircleColor(COLOR_C1)
            lineWidth          = 2.5f
            circleRadius       = 4f
            setDrawCircleHole(false)
            valueTextColor     = Color.DKGRAY
            valueTextSize      = 9f
            setDrawValues(chartPoints.size <= 20) // valores solo si no hay muchos puntos
            setDrawFilled(true)
            fillColor          = COLOR_C1
            fillAlpha          = 35
            mode               = com.github.mikephil.charting.data.LineDataSet.Mode.CUBIC_BEZIER
        }

        lineChart.apply {
            data = com.github.mikephil.charting.data.LineData(dataSet)
            description.isEnabled = false
            setDrawGridBackground(false)

            // ✅ Zoom horizontal habilitado — vertical fijo
            setPinchZoom(false)
            setScaleXEnabled(true)   // zoom horizontal
            setScaleYEnabled(false)  // sin zoom vertical
            isDragEnabled = true

            // ✅ Eje Y con título
            axisLeft.apply {
                axisMinimum = 0f
                axisMaximum = 100f
                granularity = 20f
                setGranularityEnabled(true)
                textColor   = Color.DKGRAY
                textSize    = 11f
                setDrawAxisLine(true)
                // Título del eje Y
                setLabelCount(6, true)
            }
            axisRight.isEnabled = false

            // ✅ Eje X con etiquetas reales rotadas
            xAxis.apply {
                position       = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity    = 1f
                setGranularityEnabled(true)
                setDrawGridLines(false)
                textColor      = Color.DKGRAY
                textSize       = 9f
                labelRotationAngle = -45f
                // Mostrar máximo 8 etiquetas para no saturar
                setLabelCount(minOf(labels.size, 8), false)
            }

            // ✅ Leyenda con títulos de ejes
            legend.apply {
                isEnabled        = true
                textColor        = Color.DKGRAY
                textSize         = 11f
                form             = com.github.mikephil.charting.components.Legend.LegendForm.LINE
                horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            }

            // Espacio extra para etiquetas rotadas y título Y
            setExtraOffsets(8f, 12f, 8f, 20f)

            // Si hay muchos puntos mostrar instrucción de zoom
            if (chartPoints.size > 8) {
                description.isEnabled = true
                description.text      = "← Desliza para ver más →"
                description.textColor = Color.GRAY
                description.textSize  = 9f
            }

            animateXY(800, 800)
            invalidate()
        }
    }

    // =============================================
    // GRÁFICO RADAR — 4 datasets con colores reales
    // =============================================
    private fun setupRadarChart(temas: List<ProgresoPorCompetenciaItemDTO>) {

        // ✅ Buscar cada competencia por keyword — orden fijo
        fun buscar(keyword: String): Float {
            return temas.find { keyword in it.nombre.lowercase() }
                ?.porcentaje?.coerceIn(0, 100)?.toFloat() ?: 0f
        }

        val vC1 = buscar("cantidad")
        val vC2 = buscar("regularidad")
        val vC3 = buscar("forma")
        val vC4 = buscar("datos")

        // ✅ UN SOLO DataSet con los 4 valores reales
        // Sin ceros en posiciones vacías — sin líneas al centro
        val entries = listOf(
            RadarEntry(vC1),
            RadarEntry(vC2),
            RadarEntry(vC3),
            RadarEntry(vC4)
        )

        val dataSet = RadarDataSet(entries, "Competencias").apply {
            color         = COLOR_C1          // línea del borde azul
            fillColor     = COLOR_C1          // relleno azul
            setDrawFilled(true)
            fillAlpha     = 80               // transparencia del relleno
            lineWidth     = 2f
            setDrawValues(true)
            valueTextSize  = 11f
            valueTextColor = Color.DKGRAY
        }

        val labels = listOf("Cantidad", "Regularidad", "Forma", "Datos")

        chartRadar.apply {
            data = RadarData(dataSet)
            description.isEnabled = false

            // ✅ Leyenda del chart DESACTIVADA
            // La leyenda está en el XML con los colores correctos
            legend.isEnabled = false

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                textColor      = Color.DKGRAY
                textSize       = 12f
            }

            yAxis.apply {
                axisMinimum   = 0f
                axisMaximum   = 100f
                granularity   = 25f
                setDrawLabels(true)
                textColor     = Color.LTGRAY
                textSize      = 9f
            }

            webLineWidth      = 1.5f
            webColor          = Color.LTGRAY
            webLineWidthInner = 0.75f
            webColorInner     = Color.LTGRAY
            webAlpha          = 120

            setExtraOffsets(20f, 20f, 20f, 20f)
            animateXY(800, 800)
            invalidate()
        }
    }

    // =============================================
    // GRÁFICO HORIZONTAL — Materiales de estudio
    // =============================================
    private fun setupMaterialesChart(
        totalRevisiones: Int,
        tiempoMin:       Float,
        distintos:       Int,
        detalle:         List<MaterialStatItemDTO>
    ) {
        // ── Chips de resumen ───────────────────────────────────────────────────
        tvMatTotalRevisiones.text = totalRevisiones.toString()
        tvMatTiempoTotal.text     = if (tiempoMin < 1f) "<1 min"
                                    else "${tiempoMin.toInt()} min"
        tvMatDistintos.text       = distintos.toString()

        // ── Estado vacío ───────────────────────────────────────────────────────
        if (detalle.isEmpty()) {
            chartMatRevisiones.visibility = View.GONE
            tvMatEmpty.visibility         = View.VISIBLE
            return
        }

        tvMatEmpty.visibility         = View.GONE
        chartMatRevisiones.visibility = View.VISIBLE

        // Tomar los 5 más revisados (ya vienen ordenados DESC desde la API)
        val top = detalle.take(5)

        // BarEntry: x = índice (float), y = vecesRevisado
        val entries = top.mapIndexed { i, item ->
            BarEntry(i.toFloat(), item.vecesRevisado.toFloat())
        }

        // Colores: más revisado = morado oscuro, resto = degradado
        val colors = top.map { item ->
            when {
                item.vecesRevisado >= 4 -> COLOR_C4              // morado  ≥4
                item.vecesRevisado >= 2 -> COLOR_C1              // azul    2-3
                else                   -> COLOR_C2              // verde   1
            }
        }

        val dataSet = BarDataSet(entries, "Revisiones").apply {
            setColors(colors)
            valueTextColor = Color.DKGRAY
            valueTextSize  = 10f
            setDrawValues(true)
        }

        // Etiquetas del eje Y (títulos truncados a 18 caracteres)
        val labels = top.map { item ->
            if (item.titulo.length > 18) item.titulo.take(16) + "…"
            else item.titulo
        }

        chartMatRevisiones.apply {
            data = BarData(dataSet).apply { barWidth = 0.55f }

            description.isEnabled = false
            setDrawGridBackground(false)
            setFitBars(true)
            setScaleEnabled(false)
            isDragEnabled = false

            // Eje X (horizontal) = cantidad de revisiones
            xAxis.apply {
                position    = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                setGranularityEnabled(true)
                setDrawGridLines(true)
                textColor   = Color.DKGRAY
                textSize    = 10f
                axisMinimum = 0f
            }

            // Eje Y izquierdo = etiquetas de materiales
            axisLeft.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity    = 1f
                setGranularityEnabled(true)
                setDrawGridLines(false)
                textColor      = Color.DKGRAY
                textSize       = 9f
                setLabelCount(top.size, true)
            }
            axisRight.isEnabled = false

            legend.isEnabled = false

            setExtraOffsets(4f, 8f, 16f, 8f)
            animateY(700)
            invalidate()
        }
    }

    // =============================================
    // TABLA TIEMPO POR NIVEL DE DIFICULTAD
    // =============================================
    private fun setupTiempoNivel(niveles: List<TiempoNivelItemDTO>) {
        llTiempoNivelDocente.removeAllViews()
        if (niveles.isEmpty()) {
            tvTiempoNivelDocenteEmpty.visibility = View.VISIBLE
            return
        }
        tvTiempoNivelDocenteEmpty.visibility = View.GONE
        niveles.forEach { llTiempoNivelDocente.addView(crearFilaNivel(it)) }
    }

    private fun crearFilaNivel(item: TiempoNivelItemDTO): View {
        val ctx = this
        val badgeColor = when (item.nivelEjercicio) {
            1 -> Color.parseColor("#27AE60")
            2 -> Color.parseColor("#0A6FD4")
            3 -> Color.parseColor("#E67E22")
            4 -> Color.parseColor("#7B1FA2")
            else -> Color.parseColor("#607D8B")
        }
        val aciertoColor = when {
            item.tasaAcierto >= 0.70f -> Color.parseColor("#27AE60")
            item.tasaAcierto >= 0.45f -> Color.parseColor("#E67E22")
            else                      -> Color.parseColor("#E74C3C")
        }
        val px8 = (8 * resources.displayMetrics.density).toInt()
        val px4 = (4 * resources.displayMetrics.density).toInt()
        val px2 = (2 * resources.displayMetrics.density).toInt()

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, px4, 0, px4)
        }
        val badge = TextView(ctx).apply {
            text = "N${item.nivelEjercicio}"
            textSize = 11f
            setTextColor(Color.WHITE)
            setBackgroundColor(badgeColor)
            setPadding(px8, px2, px8, px2)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = px8 }
        }
        val tvNombre = TextView(ctx).apply {
            text = item.nombreNivel
            textSize = 13f
            setTextColor(Color.parseColor("#212121"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f)
        }
        val tvTiempo = TextView(ctx).apply {
            text = item.promedioFormato
            textSize = 13f
            setTextColor(Color.parseColor("#424242"))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f)
        }
        val pct = (item.tasaAcierto * 100).toInt()
        val tvAcierto = TextView(ctx).apply {
            text = "$pct%"
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(aciertoColor)
            gravity = android.view.Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTotal = TextView(ctx).apply {
            text = item.totalRespuestas.toString()
            textSize = 12f
            setTextColor(Color.parseColor("#9E9E9E"))
            gravity = android.view.Gravity.END
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(badge)
        row.addView(tvNombre)
        row.addView(tvTiempo)
        row.addView(tvAcierto)
        row.addView(tvTotal)
        return row
    }

    // =============================================
    // UTILIDADES
    // =============================================
    private fun obtenerIniciales(nombre: String): String {
        return try {
            if (nombre.contains(",")) {
                val partes   = nombre.split(",")
                val apellido = partes[0].trim()
                val nom      = partes[1].trim()
                val initA    = apellido.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                val initN    = nom.firstOrNull()?.uppercaseChar()?.toString() ?: ""
                "$initA$initN"
            } else {
                nombre.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .joinToString("") { it.first().uppercaseChar().toString() }
            }
        } catch (_: Exception) { "??" }
    }
}