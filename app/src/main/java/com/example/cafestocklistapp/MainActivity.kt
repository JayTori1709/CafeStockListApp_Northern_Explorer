package com.example.cafestocklistapp

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.core.content.FileProvider
import com.example.cafestocklistapp.ui.theme.CafeStockListAppTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/* ==================== KIWIRAIL BRANDING COLORS ==================== */
private val KiwiRailOrange = Color(0xFFFF6600)
private val KiwiRailOrangeLight = Color(0xFFFF9966)
private val KiwiRailBlack = Color(0xFF1A1A1A)
private val KiwiRailWhite = Color(0xFFFFFFFF)
private val KiwiRailLightGray = Color(0xFFF5F5F5)
private val KiwiRailDarkGray = Color(0xFF4A4A4A)
private val KiwiRailGreen = Color(0xFF4CAF50)
private val KiwiRailRed = Color(0xFFE53935)
private val KiwiRailInfo = Color(0xFF0288D1)

// Row colors - always white with black outlines for maximum readability
private val RowBackground = KiwiRailWhite
private val RowBorderNormal = Color(0xFFCCCCCC)
private val RowTextColor = KiwiRailBlack
private val InputBackground = Color(0xFFF8F8F8)

/* -------------------- DATA MODELS -------------------- */

data class StockRow(
    val id: String = UUID.randomUUID().toString(),
    var product: String,
    var closingPrev: String = "",
    var loading: String = "",
    var total: String = "",
    var sales: String = "",
    var prePurchase: String = "",
    var waste: String = "",
    var endDay: String = ""
)

data class CategorySection(
    var name: String,
    val rows: MutableList<StockRow>
)

data class BeverageRow(
    val id: String = UUID.randomUUID().toString(),
    var product: String,
    var parLevel: String = "",
    var closingCafe: String = "",
    var closingAG: String = "",
    var loading: String = "",
    var total: String = "",
    var sales: String = "",
    var prePurchase: String = "",
    var waste: String = "",
    var endDayCafe: String = "",
    var endDayAG: String = ""
)

data class BeverageSection(
    var name: String,
    val rows: MutableList<BeverageRow>
)

/* -------------------- MAIN ACTIVITY -------------------- */

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkMode by remember { mutableStateOf(false) }
            CafeStockListAppTheme(darkTheme = isDarkMode) {
                Surface(color = KiwiRailLightGray) {
                    MainScreen(
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = { isDarkMode = !isDarkMode }
                    ) { pageName, categories, beverages, osm, tm, crew, date, cats200, cats201 ->
                        exportToPdf(pageName, categories, beverages, osm, tm, crew, date, cats200, cats201)
                    }
                }
            }
        }
    }

    // ── PDF colours ───────────────────────────────────
    private val pdfOrange = android.graphics.Color.parseColor("#FF6600")
    private val pdfBlack = android.graphics.Color.parseColor("#1A1A1A")
    private val pdfWhite = android.graphics.Color.WHITE
    private val pdfLightGray = android.graphics.Color.parseColor("#F5F5F5")
    private val pdfMidGray = android.graphics.Color.parseColor("#CCCCCC")
    private val pdfDarkGray = android.graphics.Color.parseColor("#4A4A4A")
    private val pdfGreen = android.graphics.Color.parseColor("#2E7D32")

    private val PW = 1240f   // page width
    private val PH = 1754f   // page height
    private val ML = 60f     // margin left
    private val MR = 60f     // margin right
    private val CW = PW - ML - MR  // content width

    // ── PDF helper functions ──────────────────────────
    private fun android.graphics.Canvas.fillRect(
        l: Float, t: Float, r: Float, b: Float, color: Int
    ) {
        val p = Paint().also { it.color = color; it.style = Paint.Style.FILL }
        drawRect(l, t, r, b, p)
    }

    private fun android.graphics.Canvas.strokeRect(
        l: Float, t: Float, r: Float, b: Float, color: Int, stroke: Float = 1f
    ) {
        val p = Paint().also { it.color = color; it.style = Paint.Style.STROKE; it.strokeWidth = stroke }
        drawRect(l, t, r, b, p)
    }

    private fun newPage(pdf: PdfDocument, num: Int): Pair<PdfDocument.Page, android.graphics.Canvas> {
        val info = PdfDocument.PageInfo.Builder(PW.toInt(), PH.toInt(), num).create()
        val page = pdf.startPage(info)
        val c = page.canvas
        c.fillRect(0f, 0f, PW, PH, pdfWhite)
        return page to c
    }

    // ── Improved PDF header with better readability ──
    private fun drawPageHeader(
        canvas: android.graphics.Canvas,
        title: String, subtitle: String,
        osm: String, tm: String, crew: String, date: String,
        pageNum: Int
    ): Float {
        // Orange top bar
        canvas.fillRect(0f, 0f, PW, 110f, pdfOrange)

        // KR logo
        canvas.fillRect(ML, 15f, ML + 80f, 95f, android.graphics.Color.parseColor("#CC4400"))
        val logoPaint = Paint().also {
            it.color = pdfWhite
            it.textSize = 26f
            it.isFakeBoldText = true
            it.textAlign = Paint.Align.CENTER
        }
        canvas.drawText("KR", ML + 40f, 65f, logoPaint)

        // Title - larger font for better readability
        val titlePaint = Paint().also {
            it.color = pdfWhite
            it.textSize = 32f
            it.isFakeBoldText = true
        }
        canvas.drawText(title, ML + 95f, 55f, titlePaint)

        val subPaint = Paint().also {
            it.color = android.graphics.Color.parseColor("#FFD0A0")
            it.textSize = 16f
        }
        canvas.drawText(subtitle, ML + 97f, 80f, subPaint)

        // Page number
        val pgPaint = Paint().also {
            it.color = pdfWhite
            it.textSize = 14f
            it.textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $pageNum", PW - MR, 60f, pgPaint)

        // Crew info strip - larger font
        canvas.fillRect(0f, 110f, PW, 155f, android.graphics.Color.parseColor("#FFF0E0"))
        val crewPaint = Paint().also { it.color = pdfDarkGray; it.textSize = 14f }
        val crewBold = Paint().also { it.color = pdfOrange; it.textSize = 14f; it.isFakeBoldText = true }
        var cx = ML
        listOf("OSM" to osm, "TM" to tm, "CREW" to crew, "DATE" to date).forEach { (lbl, v) ->
            canvas.drawText("$lbl: ", cx, 140f, crewBold)
            val lw = crewBold.measureText("$lbl: ")
            canvas.drawText(v.ifEmpty { "—" }, cx + lw, 140f, crewPaint)
            cx += CW / 4f
        }

        canvas.fillRect(0f, 155f, PW, 157f, pdfOrange)
        return 180f
    }

    // ── Section heading ──────────────────────────────
    private fun drawSectionHeading(canvas: android.graphics.Canvas, title: String, y: Float): Float {
        canvas.fillRect(ML, y, ML + CW, y + 36f, pdfBlack)
        val p = Paint().also {
            it.color = pdfOrange
            it.textSize = 18f
            it.isFakeBoldText = true
        }
        canvas.drawText(title, ML + 12f, y + 26f, p)
        return y + 36f
    }

    // ── Category heading ─────────────────────────────
    private fun drawCategoryHeading(canvas: android.graphics.Canvas, name: String, y: Float): Float {
        canvas.fillRect(ML, y, ML + CW, y + 28f, pdfDarkGray)
        val p = Paint().also {
            it.color = pdfWhite
            it.textSize = 14f
            it.isFakeBoldText = true
        }
        canvas.drawText(name, ML + 10f, y + 20f, p)
        return y + 28f
    }

    // ── Food table columns - wider spacing for readability ──
    private val foodCols = listOf(0f, 340f, 430f, 520f, 610f, 700f, 790f, 880f)
    private val foodHdrs = listOf("PRODUCT", "CLOSE", "LOAD", "TOTAL", "SALES", "PRE", "WASTE", "END")

    private fun drawFoodTableHeader(canvas: android.graphics.Canvas, y: Float): Float {
        canvas.fillRect(ML, y, ML + CW, y + 32f, pdfOrange)
        val p = Paint().also {
            it.color = pdfWhite
            it.textSize = 12f
            it.isFakeBoldText = true
        }
        foodHdrs.forEachIndexed { i, hdr ->
            val xa = ML + foodCols[i] + 4f
            p.textAlign = if (i == 0) Paint.Align.LEFT else Paint.Align.CENTER
            val cx = if (i == 0) xa else ML + foodCols[i] + (foodCols[i + 1] - foodCols[i]) / 2f
            canvas.drawText(hdr, if (i == 0) xa else cx, y + 22f, p)
        }
        return y + 32f
    }

    private fun drawFoodRow(canvas: android.graphics.Canvas, row: StockRow, y: Float, shaded: Boolean): Float {
        val rowH = 26f
        if (shaded) canvas.fillRect(ML, y, ML + CW, y + rowH, pdfLightGray)
        canvas.strokeRect(ML, y, ML + CW, y + rowH, pdfMidGray, 0.5f)

        val normal = Paint().also { it.color = pdfBlack; it.textSize = 11f }
        val bold = Paint().also { it.color = pdfOrange; it.textSize = 11f; it.isFakeBoldText = true }
        val center = Paint().also { it.color = pdfDarkGray; it.textSize = 11f; it.textAlign = Paint.Align.CENTER }
        val totalP = Paint().also { it.color = pdfOrange; it.textSize = 11f; it.isFakeBoldText = true; it.textAlign = Paint.Align.CENTER }

        canvas.drawText(row.product, ML + 4f, y + 18f, normal)

        val cells = listOf(row.closingPrev, row.loading, row.total, row.sales, row.prePurchase, row.waste, row.endDay)
        cells.forEachIndexed { i, v ->
            val colIdx = i + 1
            val colX = ML + foodCols[colIdx]
            val colW = foodCols[colIdx + 1] - foodCols[colIdx]
            val cx = colX + colW / 2f

            if (i == 2 && v.isNotEmpty()) {
                canvas.fillRect(colX + 1f, y + 2f, colX + colW - 1f, y + rowH - 2f, android.graphics.Color.parseColor("#FFF0E0"))
                canvas.drawText(v, cx, y + 18f, totalP)
            } else {
                canvas.drawText(v.ifEmpty { "—" }, cx, y + 18f, if (i == 2) totalP else center)
            }
            canvas.fillRect(colX, y, colX + 0.5f, y + rowH, pdfMidGray)
        }
        return y + rowH
    }

    // ── Beverage table columns - wider spacing ──
    private val bevCols = listOf(0f, 200f, 270f, 340f, 410f, 485f, 560f, 635f, 710f, 790f, 870f, 950f)

    private fun drawBevTableHeader(canvas: android.graphics.Canvas, y: Float, serviceNumber: String): Float {
        val closingDay = if (serviceNumber == "200") "201" else "200"
        val loadLoc = if (serviceNumber == "200") "WLG" else "AKL"
        val wasteLoc = if (serviceNumber == "200") "AKL" else "WLG"

        canvas.fillRect(ML, y, ML + CW, y + 30f, pdfOrange)
        val p = Paint().also {
            it.color = pdfWhite
            it.textSize = 10f
            it.isFakeBoldText = true
            it.textAlign = Paint.Align.CENTER
        }

        fun colCx(a: Int, b: Int) = ML + (bevCols[a] + bevCols[b]) / 2f

        canvas.drawText("PRODUCT", colCx(0, 1), y + 20f, p)
        canvas.drawText("PAR", colCx(1, 2), y + 20f, p)
        canvas.drawText("PREVIOUS DAY STOCK $closingDay", colCx(2, 4), y + 20f, p)
        canvas.drawText("LOAD\n$loadLoc", colCx(4, 5), y + 12f, p)
        canvas.drawText("TOTAL", colCx(5, 6), y + 20f, p)
        canvas.drawText("SALES", colCx(6, 7), y + 20f, p)
        canvas.drawText("PRE", colCx(7, 8), y + 20f, p)
        canvas.drawText("WASTE\n$wasteLoc", colCx(8, 9), y + 12f, p)
        canvas.drawText("END CAFÉ", colCx(9, 10), y + 20f, p)
        canvas.drawText("END AG", colCx(10, 11), y + 20f, p)

        return y + 30f
    }

    private fun drawBevRow(canvas: android.graphics.Canvas, row: BeverageRow, y: Float, shaded: Boolean): Float {
        val rowH = 26f
        val endTotal = (row.endDayCafe.toIntOrNull() ?: 0) + (row.endDayAG.toIntOrNull() ?: 0)
        val par = row.parLevel.toIntOrNull() ?: 0
        val isBelowPar = endTotal > 0 && par > 0 && endTotal < par

        if (isBelowPar) canvas.fillRect(ML, y, ML + CW, y + rowH, android.graphics.Color.parseColor("#FFF3E0"))
        else if (shaded) canvas.fillRect(ML, y, ML + CW, y + rowH, pdfLightGray)
        canvas.strokeRect(ML, y, ML + CW, y + rowH, pdfMidGray, 0.5f)

        val normal = Paint().also { it.color = pdfBlack; it.textSize = 10f }
        val parPaint = Paint().also {
            it.color = if (isBelowPar) pdfOrange else pdfDarkGray
            it.textSize = 10f
            it.textAlign = Paint.Align.CENTER
            it.isFakeBoldText = isBelowPar
        }
        val center = Paint().also { it.color = pdfDarkGray; it.textSize = 10f; it.textAlign = Paint.Align.CENTER }
        val totPaint = Paint().also { it.color = pdfOrange; it.textSize = 10f; it.textAlign = Paint.Align.CENTER; it.isFakeBoldText = true }

        fun colCx(a: Int) = ML + (bevCols[a] + bevCols[a + 1]) / 2f

        canvas.drawText(row.product, ML + 4f, y + 18f, normal.also { it.textSize = 10f })
        canvas.drawText(row.parLevel.ifEmpty { "—" }, colCx(1), y + 18f, parPaint)
        canvas.drawText(row.closingCafe.ifEmpty { "—" }, colCx(2), y + 18f, center)
        canvas.drawText(row.closingAG.ifEmpty { "—" }, colCx(3), y + 18f, center)
        canvas.drawText(row.loading.ifEmpty { "—" }, colCx(4), y + 18f, center)

        val totalVal = ((row.closingCafe.toIntOrNull() ?: 0) + (row.closingAG.toIntOrNull() ?: 0) + (row.loading.toIntOrNull() ?: 0))
        if (totalVal > 0) {
            canvas.fillRect(ML + bevCols[5] + 1f, y + 2f, ML + bevCols[6] - 1f, y + rowH - 2f, android.graphics.Color.parseColor("#FFF0E0"))
            canvas.drawText(totalVal.toString(), colCx(5), y + 18f, totPaint)
        } else canvas.drawText("—", colCx(5), y + 18f, center)

        canvas.drawText(row.sales.ifEmpty { "—" }, colCx(6), y + 18f, center)
        canvas.drawText(row.prePurchase.ifEmpty { "—" }, colCx(7), y + 18f, center)
        canvas.drawText(row.waste.ifEmpty { "—" }, colCx(8), y + 18f, center)
        canvas.drawText(row.endDayCafe.ifEmpty { "—" }, colCx(9), y + 18f, center)
        canvas.drawText(row.endDayAG.ifEmpty { "—" }, colCx(10), y + 18f, center)

        if (isBelowPar) {
            val w = Paint().also { it.color = pdfOrange; it.textSize = 9f; it.textAlign = Paint.Align.RIGHT }
            canvas.drawText("▼ LOW STOCK", ML + CW - 4f, y + 18f, w)
        }

        (1..10).forEach { i -> canvas.fillRect(ML + bevCols[i], y, ML + bevCols[i] + 0.5f, y + rowH, pdfMidGray) }
        return y + rowH
    }

    private fun drawPageFooter(canvas: android.graphics.Canvas, generated: String) {
        canvas.fillRect(0f, PH - 40f, PW, PH - 38f, pdfOrange)
        val p = Paint().also { it.color = pdfDarkGray; it.textSize = 11f }
        canvas.drawText("KiwiRail Café Stock Management  •  Generated: $generated", ML, PH - 18f, p)
        val r = Paint().also { it.color = pdfDarkGray; it.textSize = 11f; it.textAlign = Paint.Align.RIGHT }
        canvas.drawText("CONFIDENTIAL", PW - MR, PH - 18f, r)
    }

    // ── Main export function ──────────────────────────
    private fun exportToPdf(
        pageName: String,
        categories: List<CategorySection>,
        beverages: List<BeverageSection>,
        osm: String, tm: String, crew: String, date: String,
        categories200: List<CategorySection> = categories,
        categories201: List<CategorySection> = emptyList()
    ) {
        val pdf = PdfDocument()
        val generated = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val serviceNum = if (pageName.contains("200")) "200" else "201"
        val docTitle = if (serviceNum == "200") "Wellington → Auckland  (Service 200)" else "Auckland → Wellington  (Service 201)"

        var pageNum = 1

        // Page 1: Food Stock
        var (page, canvas) = newPage(pdf, pageNum)
        var y = drawPageHeader(canvas, docTitle, "FOOD STOCK — CLOSING SHEET", osm, tm, crew, date, pageNum)
        drawPageFooter(canvas, generated)

        y = drawSectionHeading(canvas, "FOOD STOCK", y) + 4f
        y = drawFoodTableHeader(canvas, y)

        var rowShade = false
        categories.forEach { category ->
            if (y > PH - 100f) {
                pdf.finishPage(page)
                pageNum++
                val np = newPage(pdf, pageNum)
                page = np.first; canvas = np.second
                y = drawPageHeader(canvas, docTitle, "FOOD STOCK (cont.)", osm, tm, crew, date, pageNum)
                drawPageFooter(canvas, generated)
                y = drawFoodTableHeader(canvas, y)
            }
            y = drawCategoryHeading(canvas, category.name, y)
            rowShade = false
            category.rows.forEach { row ->
                if (y > PH - 80f) {
                    pdf.finishPage(page)
                    pageNum++
                    val np = newPage(pdf, pageNum)
                    page = np.first; canvas = np.second
                    y = drawPageHeader(canvas, docTitle, "FOOD STOCK (cont.)", osm, tm, crew, date, pageNum)
                    drawPageFooter(canvas, generated)
                    y = drawFoodTableHeader(canvas, y)
                    rowShade = false
                }
                y = drawFoodRow(canvas, row, y, rowShade)
                rowShade = !rowShade
            }
        }
        pdf.finishPage(page)

        // Page(s): Retail Stock
        pageNum++
        var bp = newPage(pdf, pageNum)
        page = bp.first; canvas = bp.second
        y = drawPageHeader(canvas, docTitle, "RETAIL STOCK — CAFÉ & AG", osm, tm, crew, date, pageNum)
        drawPageFooter(canvas, generated)

        y = drawSectionHeading(canvas, "RETAIL STOCK (CAFÉ & AG)", y) + 4f
        y = drawBevTableHeader(canvas, y, serviceNum)

        rowShade = false
        beverages.forEach { section ->
            if (y > PH - 100f) {
                pdf.finishPage(page)
                pageNum++
                val np = newPage(pdf, pageNum)
                page = np.first; canvas = np.second
                y = drawPageHeader(canvas, docTitle, "RETAIL STOCK (cont.)", osm, tm, crew, date, pageNum)
                drawPageFooter(canvas, generated)
                y = drawBevTableHeader(canvas, y, serviceNum)
            }
            y = drawCategoryHeading(canvas, section.name, y)
            rowShade = false
            section.rows.forEach { row ->
                if (y > PH - 80f) {
                    pdf.finishPage(page)
                    pageNum++
                    val np = newPage(pdf, pageNum)
                    page = np.first; canvas = np.second
                    y = drawPageHeader(canvas, docTitle, "RETAIL STOCK (cont.)", osm, tm, crew, date, pageNum)
                    drawPageFooter(canvas, generated)
                    y = drawBevTableHeader(canvas, y, serviceNum)
                    rowShade = false
                }
                y = drawBevRow(canvas, row, y, rowShade)
                rowShade = !rowShade
            }
        }
        pdf.finishPage(page)

        // Save and share
        val fileName = "${pageName}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        pdf.writeTo(FileOutputStream(file))
        pdf.close()
        sendEmail(file, pageName)
    }

    private fun sendEmail(file: File, pageName: String) {
        val uri = FileProvider.getUriForFile(this, "${applicationContext.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "$pageName Stock Sheet - ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            putExtra(Intent.EXTRA_TEXT, "Attached stock sheet for $pageName.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Send Stock Sheet via"))
    }
}

/* ================= MAIN SCREEN ================= */

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onExportPdf: (String, List<CategorySection>, List<BeverageSection>, String, String, String, String, List<CategorySection>, List<CategorySection>) -> Unit
) {
    var selectedTrip by remember { mutableStateOf<String?>(null) }
    val sheet200 = remember { getFoodCategories() }
    val beverages200 = remember { getBeverageCategories() }
    val sheet201 = remember { getFoodCategories() }
    val beverages201 = remember { getBeverageCategories() }

    // Initialize PAR values into closingAG for both services when first loaded
    LaunchedEffect(Unit) {
        // Initialize Service 200 with PAR values in closingAG
        beverages200.forEach { cat ->
            cat.rows.forEach { row ->
                val par = row.parLevel.toIntOrNull() ?: 0
                if (par > 0 && row.closingAG.isEmpty()) {
                    row.closingAG = par.toString()
                }
            }
        }
        // Initialize Service 201 with PAR values in closingAG
        beverages201.forEach { cat ->
            cat.rows.forEach { row ->
                val par = row.parLevel.toIntOrNull() ?: 0
                if (par > 0 && row.closingAG.isEmpty()) {
                    row.closingAG = par.toString()
                }
            }
        }
    }

    when (selectedTrip) {
        null -> TripSelectionScreen(isDarkMode, onToggleDarkMode) { selectedTrip = it }

        "200" -> {
            var osm by remember { mutableStateOf("") }
            var tm by remember { mutableStateOf("") }
            var crew by remember { mutableStateOf("") }
            var date by remember { mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())) }
            var clearTrigger by remember { mutableStateOf(0) }

            // Create local references to the data for this service
            val localFoodCategories = sheet200
            val localBeverageCategories = beverages200

            StockSheet(
                pageName = "WLG-AKL-200",
                displayTitle = "Wellington → Auckland",
                serviceNumber = "200",
                foodCategories = localFoodCategories,
                beverageCategories = localBeverageCategories,
                osm = osm, tm = tm, crew = crew, date = date,
                onOsmChange = { osm = it },
                onTmChange = { tm = it },
                onCrewChange = { crew = it },
                onDateChange = { date = it },
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                clearTrigger = clearTrigger,
                onClearAll = {
                    // Clear all stock data but PRESERVE PAR values in closingAG
                    localFoodCategories.forEach { cat ->
                        cat.rows.forEach { row ->
                            row.closingPrev = ""
                            row.loading = ""
                            row.total = ""
                            row.sales = ""
                            row.prePurchase = ""
                            row.waste = ""
                            row.endDay = ""
                        }
                    }
                    localBeverageCategories.forEach { cat ->
                        cat.rows.forEach { row ->
                            // Preserve PAR values in closingAG, clear everything else
                            val parValue = row.parLevel
                            row.closingCafe = ""
                            row.closingAG = parValue // Restore PAR value
                            row.loading = ""
                            row.total = ""
                            row.sales = ""
                            row.prePurchase = ""
                            row.waste = ""
                            row.endDayCafe = ""
                            row.endDayAG = ""
                        }
                    }
                    // Clear crew information
                    osm = ""
                    tm = ""
                    crew = ""
                    date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
                    clearTrigger++
                },
                onBack = { selectedTrip = null },
                onExportPdf = { pn, cats, bevs, o, t, c, d ->
                    onExportPdf(pn, cats, bevs, o, t, c, d, sheet200, sheet201)
                },
                onTransferTotals = {
                    // Transfer Service 200 END totals to Service 201 CLOSING PREV
                    sheet200.forEachIndexed { ci, cat ->
                        cat.rows.forEachIndexed { ri, row ->
                            if (row.endDay.isNotEmpty()) {
                                sheet201[ci].rows[ri].closingPrev = row.endDay
                            }
                        }
                    }
                    // Transfer Service 200 END CAFE/AG to Service 201 PREVIOUS DAY STOCK
                    beverages200.forEachIndexed { ci, cat ->
                        cat.rows.forEachIndexed { ri, row ->
                            // Transfer END CAFE value to next service's CLOSING CAFE
                            if (row.endDayCafe.isNotEmpty()) {
                                beverages201[ci].rows[ri].closingCafe = row.endDayCafe
                            }
                            // Transfer END AG value to next service's CLOSING AG (preserves PAR if not changed)
                            if (row.endDayAG.isNotEmpty()) {
                                beverages201[ci].rows[ri].closingAG = row.endDayAG
                            }
                        }
                    }
                }
            )
        }

        "201" -> {
            var osm by remember { mutableStateOf("") }
            var tm by remember { mutableStateOf("") }
            var crew by remember { mutableStateOf("") }
            var date by remember { mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())) }
            var clearTrigger by remember { mutableStateOf(0) }

            // Create local references to the data for this service
            val localFoodCategories = sheet201
            val localBeverageCategories = beverages201

            StockSheet(
                pageName = "AKL-WLG-201",
                displayTitle = "Auckland → Wellington",
                serviceNumber = "201",
                foodCategories = localFoodCategories,
                beverageCategories = localBeverageCategories,
                osm = osm, tm = tm, crew = crew, date = date,
                onOsmChange = { osm = it },
                onTmChange = { tm = it },
                onCrewChange = { crew = it },
                onDateChange = { date = it },
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                clearTrigger = clearTrigger,
                onClearAll = {
                    // Clear all stock data but PRESERVE PAR values in closingAG
                    localFoodCategories.forEach { cat ->
                        cat.rows.forEach { row ->
                            row.closingPrev = ""
                            row.loading = ""
                            row.total = ""
                            row.sales = ""
                            row.prePurchase = ""
                            row.waste = ""
                            row.endDay = ""
                        }
                    }
                    localBeverageCategories.forEach { cat ->
                        cat.rows.forEach { row ->
                            // Preserve PAR values in closingAG, clear everything else
                            val parValue = row.parLevel
                            row.closingCafe = ""
                            row.closingAG = parValue // Restore PAR value
                            row.loading = ""
                            row.total = ""
                            row.sales = ""
                            row.prePurchase = ""
                            row.waste = ""
                            row.endDayCafe = ""
                            row.endDayAG = ""
                        }
                    }
                    // Clear crew information
                    osm = ""
                    tm = ""
                    crew = ""
                    date = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
                    clearTrigger++
                },
                onBack = { selectedTrip = null },
                onExportPdf = { pn, cats, bevs, o, t, c, d ->
                    onExportPdf(pn, cats, bevs, o, t, c, d, sheet200, sheet201)
                },
                onTransferTotals = {
                    // Transfer Service 201 END totals to Service 200 CLOSING PREV
                    sheet201.forEachIndexed { ci, cat ->
                        cat.rows.forEachIndexed { ri, row ->
                            if (row.endDay.isNotEmpty()) {
                                sheet200[ci].rows[ri].closingPrev = row.endDay
                            }
                        }
                    }
                    // Transfer Service 201 END CAFE/AG to Service 200 PREVIOUS DAY STOCK
                    beverages201.forEachIndexed { ci, cat ->
                        cat.rows.forEachIndexed { ri, row ->
                            // Transfer END CAFE value to next service's CLOSING CAFE
                            if (row.endDayCafe.isNotEmpty()) {
                                beverages200[ci].rows[ri].closingCafe = row.endDayCafe
                            }
                            // Transfer END AG value to next service's CLOSING AG (preserves PAR if not changed)
                            if (row.endDayAG.isNotEmpty()) {
                                beverages200[ci].rows[ri].closingAG = row.endDayAG
                            }
                        }
                    }
                }
            )
        }
    }
}

/* ================= TRIP SELECTION ================= */

@Composable
fun TripSelectionScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onTripSelected: (String) -> Unit
) {
    Box(modifier = Modifier
        .fillMaxSize()
        .background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(KiwiRailOrange, KiwiRailOrangeLight)))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Filled.Train,
                    "KiwiRail",
                    modifier = Modifier.size(64.dp),
                    tint = KiwiRailWhite
                )
            }
            Spacer(Modifier.height(32.dp))
            Text(
                "KiwiRail Café Stock",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
            )
            Text(
                "Stock Management System",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp),
                color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
            )
            Spacer(Modifier.height(48.dp))
            ServiceCard("Wellington → Auckland", "Service 200", "Departs 07:45", isDarkMode) {
                onTripSelected("200")
            }
            Spacer(Modifier.height(20.dp))
            ServiceCard("Auckland → Wellington", "Service 201", "Departs 08:45", isDarkMode) {
                onTripSelected("201")
            }
            Spacer(Modifier.height(40.dp))
            Text(
                "Select a service to begin stock count",
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                color = if (isDarkMode) KiwiRailDarkGray else Color.Gray
            )
        }
        FloatingActionButton(
            onClick = onToggleDarkMode,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp),
            containerColor = KiwiRailOrange,
            contentColor = KiwiRailWhite,
            shape = CircleShape
        ) {
            Icon(
                if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                "Toggle theme"
            )
        }
    }
}

@Composable
fun ServiceCard(
    title: String,
    subtitle: String,
    departureTime: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                interactionSource = interactionSource,
                indication = null
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPressed) 2.dp else 8.dp
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(KiwiRailGreen)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = KiwiRailGreen
                    )
                }
                Text(
                    title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Text(
                    departureTime,
                    fontSize = 12.sp,
                    color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
                )
            }
            Icon(
                Icons.Filled.ArrowForward,
                "Select",
                tint = KiwiRailOrange,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/* ================= STOCK SHEET ================= */

@Composable
fun StockSheet(
    pageName: String,
    displayTitle: String,
    serviceNumber: String,
    foodCategories: MutableList<CategorySection>,
    beverageCategories: MutableList<BeverageSection>,
    osm: String,
    tm: String,
    crew: String,
    date: String,
    onOsmChange: (String) -> Unit,
    onTmChange: (String) -> Unit,
    onCrewChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    clearTrigger: Int,
    onClearAll: () -> Unit,
    onBack: () -> Unit,
    onExportPdf: (String, List<CategorySection>, List<BeverageSection>, String, String, String, String) -> Unit,
    onTransferTotals: () -> Unit
) {
    val foodItemsCount = foodCategories.sumOf { cat -> cat.rows.count { it.endDay.isNotEmpty() } }
    val beverageItemsCount = beverageCategories.sumOf { cat ->
        cat.rows.count { it.endDayCafe.isNotEmpty() || it.endDayAG.isNotEmpty() }
    }
    val lowStockItems = beverageCategories.flatMap { it.rows }.count { row ->
        val e = (row.endDayCafe.toIntOrNull() ?: 0) + (row.endDayAG.toIntOrNull() ?: 0)
        val p = row.parLevel.toIntOrNull() ?: 0
        e > 0 && p > 0 && e < p
    }

    var showExportError by remember { mutableStateOf(false) }
    var showTransferError by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val isCrewInfoComplete = osm.isNotBlank() && tm.isNotBlank() && crew.isNotBlank() && date.isNotBlank()

    Column(
        Modifier
            .fillMaxSize()
            .background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    "Back",
                    tint = KiwiRailOrange
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    displayTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                )
                Text(
                    "Service $serviceNumber • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                    fontSize = 10.sp,
                    color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
                )
            }

            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Filled.MoreVert,
                        "Menu",
                        tint = KiwiRailOrange
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Calculate All Totals",
                                fontSize = 13.sp,
                                color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                            )
                        },
                        onClick = {
                            foodCategories.forEach { cat ->
                                cat.rows.forEach { row ->
                                    row.total = ((row.closingPrev.toIntOrNull() ?: 0) +
                                            (row.loading.toIntOrNull() ?: 0)).toString()
                                }
                            }
                            beverageCategories.forEach { cat ->
                                cat.rows.forEach { row ->
                                    row.total = ((row.closingCafe.toIntOrNull() ?: 0) +
                                            (row.closingAG.toIntOrNull() ?: 0) +
                                            (row.loading.toIntOrNull() ?: 0)).toString()
                                }
                            }
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Calculate, null, tint = KiwiRailOrange)
                        }
                    )

                    HorizontalDivider()

                    DropdownMenuItem(
                        text = {
                            Text(
                                "Toggle Theme",
                                fontSize = 13.sp,
                                color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                            )
                        },
                        onClick = {
                            onToggleDarkMode()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.DarkMode, null, tint = KiwiRailOrange)
                        }
                    )
                }
            }
        }

        // Progress Indicator
        CompletionProgress(foodCategories, beverageCategories, isDarkMode)

        // Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("Food Items", foodItemsCount.toString(), isDarkMode)
                StatItem("Beverages", beverageItemsCount.toString(), isDarkMode)
                StatItem(
                    "Low Stock",
                    lowStockItems.toString(),
                    isDarkMode,
                    isWarning = lowStockItems > 0
                )
            }
        }

        // Crew Information
        Text(
            "Crew Information",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrewField(
                "OSM", osm, onOsmChange,
                Modifier.weight(1f),
                isDarkMode,
                hasAttemptedSubmit
            )
            CrewField(
                "TM", tm, onTmChange,
                Modifier.weight(1f),
                isDarkMode,
                hasAttemptedSubmit
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CrewField(
                "CREW", crew, onCrewChange,
                Modifier.weight(1f),
                isDarkMode,
                hasAttemptedSubmit
            )
            DatePickerField(
                "DATE", date, onDateChange,
                Modifier.weight(1f),
                isDarkMode,
                hasAttemptedSubmit
            )
        }

        Spacer(Modifier.height(16.dp))

        // Action Buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    onClearAll()
                    hasAttemptedSubmit = false
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KiwiRailOrange
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, KiwiRailOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    if (!isCrewInfoComplete) showExportError = true
                    else onExportPdf(pageName, foodCategories, beverageCategories, osm, tm, crew, date)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KiwiRailOrange,
                    contentColor = KiwiRailWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export PDF", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (showExportError) {
            AlertDialog(
                onDismissRequest = { showExportError = false },
                title = {
                    Text(
                        "Missing Information",
                        fontWeight = FontWeight.Bold,
                        color = KiwiRailOrange
                    )
                },
                text = {
                    Column {
                        Text("Please fill in all crew information fields before exporting:")
                        Spacer(Modifier.height(8.dp))
                        if (osm.isBlank()) Text("• OSM is required", color = KiwiRailRed)
                        if (tm.isBlank()) Text("• TM is required", color = KiwiRailRed)
                        if (crew.isBlank()) Text("• CREW is required", color = KiwiRailRed)
                        if (date.isBlank()) Text("• DATE is required", color = KiwiRailRed)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showExportError = false },
                        colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        // Food Stock Section
        Text(
            "FOOD STOCK",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column {
            CompactTableHeader()
            foodCategories.forEach { category ->
                CompactCategoryHeader(category.name)
                category.rows.forEachIndexed { index, row ->
                    DraggableStockRow(
                        row = row,
                        clearTrigger = clearTrigger,
                        onMoveUp = {
                            if (index > 0) category.rows.swap(index, index - 1)
                        },
                        onMoveDown = {
                            if (index < category.rows.lastIndex) category.rows.swap(index, index + 1)
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(
            thickness = 1.dp,
            color = KiwiRailOrange.copy(alpha = 0.3f),
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Spacer(Modifier.height(16.dp))

        // Retail Stock Section
        Text(
            "RETAIL STOCK (CAFÉ & AG)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Column {
            BeverageTableHeader(serviceNumber = serviceNumber)
            beverageCategories.forEach { section ->
                BeverageCategoryHeader(section.name)
                section.rows.forEach { row ->
                    ModernBeverageStockRow(
                        row = row,
                        clearTrigger = clearTrigger
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Transfer Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Ready to move to next service?",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Transfer end-of-day totals to ${if (serviceNumber == "200") "Service 201" else "Service 200"}'s opening stock",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (!isCrewInfoComplete) showTransferError = true
                        else showTransferDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KiwiRailOrange,
                        contentColor = KiwiRailWhite
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowForward,
                        null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Transfer End Totals to ${if (serviceNumber == "200") "201" else "200"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showTransferError) {
            AlertDialog(
                onDismissRequest = { showTransferError = false },
                title = {
                    Text(
                        "Missing Information",
                        fontWeight = FontWeight.Bold,
                        color = KiwiRailOrange
                    )
                },
                text = {
                    Column {
                        Text("Please fill in all crew information fields before transferring totals:")
                        Spacer(Modifier.height(8.dp))
                        if (osm.isBlank()) Text("• OSM is required", color = KiwiRailRed)
                        if (tm.isBlank()) Text("• TM is required", color = KiwiRailRed)
                        if (crew.isBlank()) Text("• CREW is required", color = KiwiRailRed)
                        if (date.isBlank()) Text("• DATE is required", color = KiwiRailRed)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showTransferError = false },
                        colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange)
                    ) {
                        Text("OK")
                    }
                },
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showTransferDialog) {
            AlertDialog(
                onDismissRequest = { showTransferDialog = false },
                title = {
                    Text(
                        "Transfer End Totals?",
                        fontWeight = FontWeight.Bold,
                        color = KiwiRailOrange
                    )
                },
                text = {
                    Column {
                        Text("This will copy:", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("• Food END values → Service ${if (serviceNumber == "200") "201" else "200"} CLOSING PREV")
                        Text("• Beverage END CAFÉ values → Service ${if (serviceNumber == "200") "201" else "200"} CLOSING CAFÉ")
                        Text("• Beverage END AG values → Service ${if (serviceNumber == "200") "201" else "200"} CLOSING AG")
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Make sure you've completed Service $serviceNumber before transferring!",
                            fontSize = 12.sp,
                            color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray,
                            fontStyle = FontStyle.Italic
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onTransferTotals()
                            showTransferDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange)
                    ) {
                        Text("Transfer Now")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTransferDialog = false }) {
                        Text(
                            "Cancel",
                            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                        )
                    }
                },
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* ================= CREW FIELD ================= */

@Composable
fun CrewField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    showError: Boolean = false
) {
    val isError = showError && value.isBlank()

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)
            .border(
                1.5.dp,
                when {
                    isError -> KiwiRailRed
                    value.isNotEmpty() -> KiwiRailOrange
                    isDarkMode -> Color(0xFF666666)
                    else -> Color.LightGray
                },
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
        ),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(
                        if (isError) "$placeholder *" else placeholder,
                        fontSize = 13.sp,
                        color = if (isError) KiwiRailRed else if (isDarkMode) Color(0xFF888888) else Color.Gray
                    )
                }
                innerTextField()
            }
        }
    )
}

/* ================= DATE PICKER FIELD ================= */

@Composable
fun DatePickerField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false,
    showError: Boolean = false
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val isError = showError && value.isBlank()

    if (value.isNotEmpty()) {
        try {
            calendar.time = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(value) ?: Date()
        } catch (_: Exception) {
        }
    }

    val dialog = DatePickerDialog(
        context,
        { _, y, m, d ->
            val c = Calendar.getInstance().apply { set(y, m, d) }
            onValueChange(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(c.time))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)
            .border(
                1.5.dp,
                when {
                    isError -> KiwiRailRed
                    value.isNotEmpty() -> KiwiRailOrange
                    isDarkMode -> Color(0xFF666666)
                    else -> Color.LightGray
                },
                RoundedCornerShape(8.dp)
            )
            .clickable { dialog.show() }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                value.ifEmpty { if (isError) "$placeholder *" else placeholder },
                fontSize = 13.sp,
                color = when {
                    isError && value.isEmpty() -> KiwiRailRed
                    value.isEmpty() -> if (isDarkMode) Color(0xFF888888) else Color.Gray
                    else -> if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                }
            )
            Icon(
                Icons.Filled.CalendarToday,
                "Select Date",
                modifier = Modifier.size(16.dp),
                tint = KiwiRailOrange
            )
        }
    }
}

/* ================= PROGRESS / STATS ================= */

@Composable
fun CompletionProgress(
    foodCategories: List<CategorySection>,
    beverageCategories: List<BeverageSection>,
    isDarkMode: Boolean
) {
    val totalFood = foodCategories.sumOf { it.rows.size }
    val doneFood = foodCategories.sumOf { cat ->
        cat.rows.count {
            it.closingPrev.isNotEmpty() && it.loading.isNotEmpty() && it.endDay.isNotEmpty()
        }
    }
    val totalBev = beverageCategories.sumOf { it.rows.size }
    val doneBev = beverageCategories.sumOf { cat ->
        cat.rows.count {
            it.closingCafe.isNotEmpty() && it.closingAG.isNotEmpty() &&
                    it.endDayCafe.isNotEmpty() && it.endDayAG.isNotEmpty()
        }
    }
    val total = totalFood + totalBev
    val done = doneFood + doneBev
    val progress = if (total > 0) done.toFloat() / total.toFloat() else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Progress",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                )
                Text(
                    "$done/$total items",
                    fontSize = 12.sp,
                    color = KiwiRailOrange
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = KiwiRailOrange,
                trackColor = if (isDarkMode) KiwiRailDarkGray.copy(alpha = 0.5f) else KiwiRailLightGray
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Food: $doneFood/$totalFood",
                    fontSize = 11.sp,
                    color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
                )
                Text(
                    "Beverages: $doneBev/$totalBev",
                    fontSize = 11.sp,
                    color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
                )
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    isDarkMode: Boolean,
    isWarning: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) KiwiRailOrange else if (isDarkMode) KiwiRailWhite else KiwiRailBlack
        )
        Text(
            label,
            fontSize = 10.sp,
            color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
        )
    }
}

/* ================= TABLE HEADERS ================= */

@Composable
fun CompactTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KiwiRailOrange, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Product",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = KiwiRailWhite,
            modifier = Modifier.weight(2.8f)
        )
        listOf("Close", "Load", "Total", "Sales", "Pre", "Waste", "End").forEach { lbl ->
            Text(
                lbl,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = KiwiRailWhite,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactCategoryHeader(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KiwiRailBlack)
            .padding(10.dp)
    ) {
        Text(
            name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = KiwiRailWhite
        )
    }
}

@Composable
fun BeverageTableHeader(serviceNumber: String) {
    val closingDay = if (serviceNumber == "200") "201" else "200"
    val loadLoc = if (serviceNumber == "200") "WLG" else "AKL"
    val wasteLoc = if (serviceNumber == "200") "AKL" else "WLG"

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KiwiRailOrange, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "PAR\nLEVEL", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                lineHeight = 9.sp, color = KiwiRailWhite
            )
            Text(
                "PREVIOUS DAY STOCK $closingDay", fontSize = 7.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.6f),
                textAlign = TextAlign.Center, lineHeight = 8.sp, color = KiwiRailWhite
            )
            Text(
                "LOADING\n@ $loadLoc", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center,
                lineHeight = 9.sp, color = KiwiRailWhite
            )
            Text(
                "TOTAL", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                color = KiwiRailWhite
            )
            Text(
                "SALES FOR\nTHE DAY", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center,
                lineHeight = 8.sp, color = KiwiRailWhite
            )
            Text(
                "PRE\nPURCHASE", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                lineHeight = 9.sp, color = KiwiRailWhite
            )
            Text(
                "WASTE\n@ $wasteLoc", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                lineHeight = 9.sp, color = KiwiRailWhite
            )
            Text(
                "END OF DAY\nTOTAL", fontSize = 7.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1.6f), textAlign = TextAlign.Center,
                lineHeight = 8.sp, color = KiwiRailWhite
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KiwiRailOrange.copy(alpha = 0.9f))
                .padding(vertical = 6.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(0.8f))
            Text(
                "CAFÉ", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                color = KiwiRailWhite
            )
            Text(
                "AG", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                color = KiwiRailWhite
            )
            Spacer(Modifier.weight(0.9f))
            Spacer(Modifier.weight(0.8f))
            Spacer(Modifier.weight(0.9f))
            Spacer(Modifier.weight(0.8f))
            Spacer(Modifier.weight(0.8f))
            Text(
                "CAFÉ", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                color = KiwiRailWhite
            )
            Text(
                "AG", fontSize = 8.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center,
                color = KiwiRailWhite
            )
        }
    }
}

@Composable
fun BeverageCategoryHeader(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KiwiRailBlack)
            .padding(10.dp)
    ) {
        Text(
            name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = KiwiRailWhite
        )
    }
}

/* ================= FOOD ROW - FIXED DRAG AND DROP ================= */

@Composable
fun DraggableStockRow(
    row: StockRow,
    clearTrigger: Int,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(
        if (isDragging) 1.04f else 1f,
        animationSpec = tween(150)
    )

    var closeValue by remember(clearTrigger, row.closingPrev) { mutableStateOf(row.closingPrev) }
    var loadValue by remember(clearTrigger, row.loading) { mutableStateOf(row.loading) }

    val calculatedTotal by remember(closeValue, loadValue) {
        derivedStateOf {
            val c = closeValue.toIntOrNull() ?: 0
            val l = loadValue.toIntOrNull() ?: 0
            if (c + l > 0 || closeValue.isNotEmpty() || loadValue.isNotEmpty()) (c + l).toString() else ""
        }
    }
    row.total = calculatedTotal

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleAnim)
            .background(if (isDragging) KiwiRailOrange.copy(alpha = 0.08f) else RowBackground)
            .border(
                if (isDragging) 2.dp else 0.5.dp,
                if (isDragging) KiwiRailOrange else RowBorderNormal
            )
            .pointerInput(row.id) {
                var dragAccumulator = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragAccumulator = 0f
                    },
                    onDragEnd = {
                        isDragging = false
                        dragAccumulator = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        dragAccumulator = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount.y

                        when {
                            dragAccumulator < -30f -> {
                                onMoveUp()
                                dragAccumulator = 0f
                            }
                            dragAccumulator > 30f -> {
                                onMoveDown()
                                dragAccumulator = 0f
                            }
                        }
                    }
                )
            }
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        // Product name with drag handle
        Row(
            modifier = Modifier.weight(2.8f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.DragHandle,
                "Drag to reorder",
                modifier = Modifier
                    .size(16.dp)
                    .alpha(if (isDragging) 1f else 0.45f),
                tint = KiwiRailOrange
            )
            Spacer(Modifier.width(4.dp))
            Text(
                row.product,
                fontSize = 11.sp,
                color = RowTextColor
            )
        }

        CompactNumericField(closeValue, Modifier.weight(1f), clearTrigger) {
            closeValue = it
            row.closingPrev = it
        }
        CompactNumericField(loadValue, Modifier.weight(1f), clearTrigger) {
            loadValue = it
            row.loading = it
        }

        // Total field
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (calculatedTotal.isNotEmpty())
                        KiwiRailOrange.copy(alpha = 0.12f)
                    else Color(0xFFF0F0F0)
                )
                .border(
                    1.dp,
                    if (calculatedTotal.isNotEmpty())
                        KiwiRailOrange.copy(alpha = 0.4f)
                    else Color(0xFFDDDDDD),
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                calculatedTotal,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (calculatedTotal.isNotEmpty()) KiwiRailOrange else Color.Gray
            )
        }

        CompactNumericField(row.sales, Modifier.weight(1f), clearTrigger) { row.sales = it }
        CompactNumericField(row.prePurchase, Modifier.weight(1f), clearTrigger) { row.prePurchase = it }
        CompactNumericField(row.waste, Modifier.weight(1f), clearTrigger) { row.waste = it }
        CompactNumericField(row.endDay, Modifier.weight(1f), clearTrigger) { row.endDay = it }
    }
}

/* ================= BEVERAGE ROW - WITH PAR IN CLOSING AG ================= */

@Composable
fun ModernBeverageStockRow(row: BeverageRow, clearTrigger: Int) {
    // Force recomposition when clearTrigger changes AND when row values change externally
    var closingCafe by remember(clearTrigger, row.closingCafe) { mutableStateOf(row.closingCafe) }
    var closingAG by remember(clearTrigger, row.closingAG) { mutableStateOf(row.closingAG) }
    var loading by remember(clearTrigger, row.loading) { mutableStateOf(row.loading) }

    // Update local state when row values change externally
    LaunchedEffect(row.closingCafe, row.closingAG, row.loading) {
        closingCafe = row.closingCafe
        closingAG = row.closingAG
        loading = row.loading
    }

    val calculatedTotal by remember(closingCafe, closingAG, loading) {
        derivedStateOf {
            val t = (closingCafe.toIntOrNull() ?: 0) +
                    (closingAG.toIntOrNull() ?: 0) +
                    (loading.toIntOrNull() ?: 0)
            if (t > 0) t.toString() else ""
        }
    }
    row.total = if (calculatedTotal.isEmpty()) "0" else calculatedTotal

    val endTotal = (row.endDayCafe.toIntOrNull() ?: 0) + (row.endDayAG.toIntOrNull() ?: 0)
    val par = row.parLevel.toIntOrNull() ?: 0
    val isBelowPar = endTotal > 0 && par > 0 && endTotal < par

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(RowBackground)
            .border(
                if (isBelowPar) 2.dp else 0.5.dp,
                if (isBelowPar) KiwiRailOrange else RowBorderNormal
            )
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Column(Modifier.weight(0.8f)) {
            Text(
                row.product,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                color = RowTextColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.parLevel,
                    fontSize = 8.sp,
                    color = if (isBelowPar) KiwiRailOrange else KiwiRailInfo,
                    fontWeight = FontWeight.Bold
                )
                if (isBelowPar) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Filled.Warning,
                        "Low stock",
                        modifier = Modifier.size(8.dp),
                        tint = KiwiRailOrange
                    )
                }
            }
        }

        TinyNumericField(closingCafe, Modifier.weight(0.8f), clearTrigger) {
            closingCafe = it
            row.closingCafe = it
        }

        // AG column - shows PAR value by default, can be edited
        TinyNumericField(closingAG, Modifier.weight(0.8f), clearTrigger) {
            closingAG = it
            row.closingAG = it
        }

        TinyNumericField(loading, Modifier.weight(0.9f), clearTrigger) {
            loading = it
            row.loading = it
        }

        // Total field
        Box(
            modifier = Modifier
                .weight(0.8f)
                .padding(horizontal = 1.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(
                    if (calculatedTotal.isNotEmpty())
                        KiwiRailOrange.copy(alpha = 0.12f)
                    else Color(0xFFF0F0F0)
                )
                .border(
                    1.dp,
                    if (calculatedTotal.isNotEmpty())
                        KiwiRailOrange.copy(alpha = 0.4f)
                    else Color(0xFFDDDDDD),
                    RoundedCornerShape(4.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                calculatedTotal,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (calculatedTotal.isNotEmpty()) KiwiRailOrange else Color.Gray
            )
        }

        TinyNumericField(row.sales, Modifier.weight(0.9f), clearTrigger) { row.sales = it }
        TinyNumericField(row.prePurchase, Modifier.weight(0.8f), clearTrigger) { row.prePurchase = it }
        TinyNumericField(row.waste, Modifier.weight(0.8f), clearTrigger) { row.waste = it }
        TinyNumericField(row.endDayCafe, Modifier.weight(0.8f), clearTrigger) {
            row.endDayCafe = it
        }
        TinyNumericField(row.endDayAG, Modifier.weight(0.8f), clearTrigger) {
            row.endDayAG = it
        }
    }
}

/* ================= INPUT FIELDS ================= */

@Composable
fun CompactNumericField(
    value: String,
    modifier: Modifier = Modifier,
    clearTrigger: Int = 0,
    onChange: (String) -> Unit
) {
    var textValue by remember(value, clearTrigger) { mutableStateOf(value) }

    BasicTextField(
        value = textValue,
        onValueChange = {
            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                textValue = it
                onChange(it)
            }
        },
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(InputBackground)
            .border(
                1.dp,
                if (textValue.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.6f) else RowBorderNormal,
                RoundedCornerShape(6.dp)
            ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = KiwiRailBlack,
            fontWeight = if (textValue.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
        ),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                innerTextField()
            }
        }
    )
}

@Composable
fun TinyNumericField(
    value: String,
    modifier: Modifier = Modifier,
    clearTrigger: Int = 0,
    onChange: (String) -> Unit
) {
    var textValue by remember(value, clearTrigger) { mutableStateOf(value) }

    BasicTextField(
        value = textValue,
        onValueChange = {
            if (it.isEmpty() || it.all { c -> c.isDigit() }) {
                textValue = it
                onChange(it)
            }
        },
        modifier = modifier
            .padding(horizontal = 1.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(InputBackground)
            .border(
                1.dp,
                if (textValue.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.6f) else RowBorderNormal,
                RoundedCornerShape(4.dp)
            ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = KiwiRailBlack,
            fontWeight = if (textValue.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
        ),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                innerTextField()
            }
        }
    )
}

/* ================= DATA ================= */

fun getFoodCategories(): MutableList<CategorySection> = mutableListOf(
    CategorySection("BREAKFAST", mutableListOf(
        StockRow(product = "Growers Breakfast"),
        StockRow(product = "Breakfast Croissant"),
        StockRow(product = "Big Breakfast"),
        StockRow(product = "Pancakes"),
        StockRow(product = "Chia Seeds"),
        StockRow(product = "Fruit Salads")
    )),
    CategorySection("SWEETS", mutableListOf(
        StockRow(product = "Brownie Slices"),
        StockRow(product = "Cookie Time Biscuits"),
        StockRow(product = "Cookie Time GF Biscuits"),
        StockRow(product = "Carrot Cake"),
        StockRow(product = "ANZAC Biscuits"),
        StockRow(product = "Blueberry Muffins"),
        StockRow(product = "Cheese Scones")
    )),
    CategorySection("SALADS", mutableListOf(
        StockRow(product = "Leafy Salad"),
        StockRow(product = "Smoked Chicken Pasta Salad")
    )),
    CategorySection("SANDWICHES AND WRAP", mutableListOf(
        StockRow(product = "BLT"),
        StockRow(product = "Chicken Wrap"),
        StockRow(product = "Beef Pickle"),
        StockRow(product = "Ham and Cheese Toastie")
    )),
    CategorySection("HOT MEALS", mutableListOf(
        StockRow(product = "Mac & Cheese"),
        StockRow(product = "Lasagne"),
        StockRow(product = "Roast Chicken"),
        StockRow(product = "Lamb Shank"),
        StockRow(product = "Beef Cheek")
    )),
    CategorySection("PIES", mutableListOf(
        StockRow(product = "Steak and Cheese"),
        StockRow(product = "Vegetarian")
    )),
    CategorySection("SWEET AND ICE CREAM", mutableListOf(
        StockRow(product = "KAPITI BOYSENBERRY"),
        StockRow(product = "KAPITI PASSIONFRUIT"),
        StockRow(product = "KAPITI CHOCOLATE CUPS"),
        StockRow(product = "MEMPHIS BIK BIKKIE")
    )),
    CategorySection("CHEESEBOARD", mutableListOf(
        StockRow(product = "Cheeseboard")
    ))
)

fun getBeverageCategories(): MutableList<BeverageSection> = mutableListOf(
    BeverageSection("SNACKS", mutableListOf(
        BeverageRow(product = "Whittakers White Choc", parLevel = "48"),
        BeverageRow(product = "Whittakers Brown Choc", parLevel = "48"),
        BeverageRow(product = "ETA Nuts", parLevel = "24")
    )),
    BeverageSection("PROPER CHIPS", mutableListOf(
        BeverageRow(product = "Sea Salt", parLevel = "18"),
        BeverageRow(product = "Cider Vinegar", parLevel = "18"),
        BeverageRow(product = "Garden Medly", parLevel = "18")
    )),
    BeverageSection("BEERS", mutableListOf(
        BeverageRow(product = "Steinlager Ultra", parLevel = "24"),
        BeverageRow(product = "Duncans Pilsner", parLevel = "12"),
        BeverageRow(product = "Ruapehu Stout", parLevel = "12"),
        BeverageRow(product = "Parrot dog Hazy IPA", parLevel = "12"),
        BeverageRow(product = "Garage Project TINY", parLevel = "12"),
        BeverageRow(product = "Panhead Supercharger", parLevel = "12"),
        BeverageRow(product = "Sawmill Nimble", parLevel = "12")
    )),
    BeverageSection("PRE MIXES", mutableListOf(
        BeverageRow(product = "Pals Vodka", parLevel = "10"),
        BeverageRow(product = "Scapegrace Gin", parLevel = "12"),
        BeverageRow(product = "Coruba Rum & Cola", parLevel = "12"),
        BeverageRow(product = "Apple Cider", parLevel = "12"),
        BeverageRow(product = "AF Apero Spirtz", parLevel = "12")
    )),
    BeverageSection("WINES", mutableListOf(
        BeverageRow(product = "Joiy the Gryphon 250ml", parLevel = "24"),
        BeverageRow(product = "The Ned Sav 250ml", parLevel = "24"),
        BeverageRow(product = "Matahiwi Cuvee 250ml", parLevel = "24"),
        BeverageRow(product = "Summer Love 250ml", parLevel = "24")
    )),
    BeverageSection("SOFT DRINKS", mutableListOf(
        BeverageRow(product = "H2go Water 750ml", parLevel = "12"),
        BeverageRow(product = "NZ SP Water 500ml", parLevel = "18"),
        BeverageRow(product = "Bundaberg Lemon Lime", parLevel = "10"),
        BeverageRow(product = "Bundaberg Ginger Beer", parLevel = "10"),
        BeverageRow(product = "7 UP", parLevel = "10"),
        BeverageRow(product = "Pepsi", parLevel = "10"),
        BeverageRow(product = "Pepsi Max", parLevel = "10"),
        BeverageRow(product = "McCoy Orange Juice", parLevel = "15"),
        BeverageRow(product = "Boss Coffee", parLevel = "6")
    )),
    BeverageSection("750 ML WINE", mutableListOf(
        BeverageRow(product = "Hunters 750ml", parLevel = "6"),
        BeverageRow(product = "Kumeru Pinot Gris 750ml", parLevel = "6"),
        BeverageRow(product = "Dog Point Sav 750ml", parLevel = "6"),
        BeverageRow(product = "Clearview Chardonnay 750ml", parLevel = "6")
    ))
)

fun <T> MutableList<T>.swap(i: Int, j: Int) {
    val tmp = this[i]
    this[i] = this[j]
    this[j] = tmp
}