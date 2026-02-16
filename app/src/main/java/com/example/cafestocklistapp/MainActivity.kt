package com.example.cafestocklistapp

import android.app.DatePickerDialog
import android.content.Context
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
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
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

private val RowBackground = KiwiRailWhite
private val RowBorderNormal = Color(0xFFCCCCCC)
private val RowTextColor = KiwiRailBlack
private val InputBackground = Color(0xFFF8F8F8)

/* ==================== DATA MODELS ==================== */

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

/* ==================== AUTOSAVE MANAGER ====================
 *
 * Saves all sheet data to Android SharedPreferences as JSON.
 * SharedPreferences survives:
 *   ✓ App close / swipe away
 *   ✓ App crash
 *   ✓ Device rotation
 *   ✓ OS killing the app for memory
 *
 * Data is cleared only when the user taps "Clear All".
 *
 * HOW IT WORKS:
 *   - SaveManager.save()   → serialises both sheets to JSON and commits to prefs
 *   - SaveManager.restore() → reads JSON from prefs and fills the sheet lists
 *   - SaveManager.clear()   → removes saved data (called by Clear All)
 */
object SaveManager {

    private const val PREFS_NAME  = "kiwirail_autosave"
    private const val KEY_200_OSM  = "s200_osm"
    private const val KEY_200_TM   = "s200_tm"
    private const val KEY_200_CREW = "s200_crew"
    private const val KEY_200_DATE = "s200_date"
    private const val KEY_201_OSM  = "s201_osm"
    private const val KEY_201_TM   = "s201_tm"
    private const val KEY_201_CREW = "s201_crew"
    private const val KEY_201_DATE = "s201_date"
    private const val KEY_200_FOOD = "s200_food"
    private const val KEY_200_BEV  = "s200_bev"
    private const val KEY_201_FOOD = "s201_food"
    private const val KEY_201_BEV  = "s201_bev"
    private const val KEY_SAVED_AT = "saved_at"

    // ── Serialise ──────────────────────────────────────────────────

    private fun stockRowToJson(r: StockRow) = JSONObject().apply {
        put("id",           r.id)
        put("product",      r.product)
        put("closingPrev",  r.closingPrev)
        put("loading",      r.loading)
        put("total",        r.total)
        put("sales",        r.sales)
        put("prePurchase",  r.prePurchase)
        put("waste",        r.waste)
        put("endDay",       r.endDay)
    }

    private fun categoryToJson(c: CategorySection) = JSONObject().apply {
        put("name", c.name)
        put("rows", JSONArray().also { arr -> c.rows.forEach { arr.put(stockRowToJson(it)) } })
    }

    private fun bevRowToJson(r: BeverageRow) = JSONObject().apply {
        put("id",          r.id)
        put("product",     r.product)
        put("parLevel",    r.parLevel)
        put("closingCafe", r.closingCafe)
        put("closingAG",   r.closingAG)
        put("loading",     r.loading)
        put("total",       r.total)
        put("sales",       r.sales)
        put("prePurchase", r.prePurchase)
        put("waste",       r.waste)
        put("endDayCafe",  r.endDayCafe)
        put("endDayAG",    r.endDayAG)
    }

    private fun bevSectionToJson(s: BeverageSection) = JSONObject().apply {
        put("name", s.name)
        put("rows", JSONArray().also { arr -> s.rows.forEach { arr.put(bevRowToJson(it)) } })
    }

    // ── Deserialise ────────────────────────────────────────────────

    private fun jsonToStockRow(j: JSONObject, template: StockRow) = template.also {
        it.closingPrev  = j.optString("closingPrev",  "")
        it.loading      = j.optString("loading",      "")
        it.total        = j.optString("total",        "")
        it.sales        = j.optString("sales",        "")
        it.prePurchase  = j.optString("prePurchase",  "")
        it.waste        = j.optString("waste",        "")
        it.endDay       = j.optString("endDay",       "")
    }

    private fun jsonToBevRow(j: JSONObject, template: BeverageRow) = template.also {
        it.closingCafe  = j.optString("closingCafe",  "")
        it.closingAG    = j.optString("closingAG",    "")
        it.loading      = j.optString("loading",      "")
        it.total        = j.optString("total",        "")
        it.sales        = j.optString("sales",        "")
        it.prePurchase  = j.optString("prePurchase",  "")
        it.waste        = j.optString("waste",        "")
        it.endDayCafe   = j.optString("endDayCafe",   "")
        it.endDayAG     = j.optString("endDayAG",     "")
    }

    // ── Public API ─────────────────────────────────────────────────

    /** Save everything — called automatically on field change (debounced) and onStop. */
    fun save(
        context: Context,
        osm200: String, tm200: String, crew200: String, date200: String,
        food200: List<CategorySection>, bev200: List<BeverageSection>,
        osm201: String, tm201: String, crew201: String, date201: String,
        food201: List<CategorySection>, bev201: List<BeverageSection>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_200_OSM,  osm200)
            putString(KEY_200_TM,   tm200)
            putString(KEY_200_CREW, crew200)
            putString(KEY_200_DATE, date200)
            putString(KEY_201_OSM,  osm201)
            putString(KEY_201_TM,   tm201)
            putString(KEY_201_CREW, crew201)
            putString(KEY_201_DATE, date201)
            putString(KEY_200_FOOD, JSONArray().also { food200.forEach { c -> it.put(categoryToJson(c)) } }.toString())
            putString(KEY_200_BEV,  JSONArray().also { bev200.forEach  { s -> it.put(bevSectionToJson(s)) } }.toString())
            putString(KEY_201_FOOD, JSONArray().also { food201.forEach { c -> it.put(categoryToJson(c)) } }.toString())
            putString(KEY_201_BEV,  JSONArray().also { bev201.forEach  { s -> it.put(bevSectionToJson(s)) } }.toString())
            putString(KEY_SAVED_AT, SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()))
            apply() // async commit — fast, survives crash
        }
    }

    /**
     * Restore saved data into the provided sheet lists.
     * Returns a SavedCrewInfo bundle (or null if nothing was previously saved).
     */
    fun restore(
        context: Context,
        food200: MutableList<CategorySection>, bev200: MutableList<BeverageSection>,
        food201: MutableList<CategorySection>, bev201: MutableList<BeverageSection>
    ): SavedCrewInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_SAVED_AT)) return null  // nothing saved yet

        try {
            restoreFood(prefs.getString(KEY_200_FOOD, null), food200)
            restoreBev(prefs.getString(KEY_200_BEV,  null), bev200)
            restoreFood(prefs.getString(KEY_201_FOOD, null), food201)
            restoreBev(prefs.getString(KEY_201_BEV,  null), bev201)
        } catch (_: Exception) {
            return null  // if JSON is corrupt, start fresh
        }

        return SavedCrewInfo(
            osm200  = prefs.getString(KEY_200_OSM,  "") ?: "",
            tm200   = prefs.getString(KEY_200_TM,   "") ?: "",
            crew200 = prefs.getString(KEY_200_CREW, "") ?: "",
            date200 = prefs.getString(KEY_200_DATE, "") ?: "",
            osm201  = prefs.getString(KEY_201_OSM,  "") ?: "",
            tm201   = prefs.getString(KEY_201_TM,   "") ?: "",
            crew201 = prefs.getString(KEY_201_CREW, "") ?: "",
            date201 = prefs.getString(KEY_201_DATE, "") ?: "",
            savedAt = prefs.getString(KEY_SAVED_AT, "") ?: ""
        )
    }

    private fun restoreFood(json: String?, target: MutableList<CategorySection>) {
        if (json.isNullOrBlank()) return
        val arr = JSONArray(json)
        for (ci in 0 until minOf(arr.length(), target.size)) {
            val catObj = arr.getJSONObject(ci)
            val rowArr = catObj.getJSONArray("rows")
            for (ri in 0 until minOf(rowArr.length(), target[ci].rows.size)) {
                jsonToStockRow(rowArr.getJSONObject(ri), target[ci].rows[ri])
            }
        }
    }

    private fun restoreBev(json: String?, target: MutableList<BeverageSection>) {
        if (json.isNullOrBlank()) return
        val arr = JSONArray(json)
        for (ci in 0 until minOf(arr.length(), target.size)) {
            val secObj = arr.getJSONObject(ci)
            val rowArr = secObj.getJSONArray("rows")
            for (ri in 0 until minOf(rowArr.length(), target[ci].rows.size)) {
                jsonToBevRow(rowArr.getJSONObject(ri), target[ci].rows[ri])
            }
        }
    }

    /** Wipe saved data — called by Clear All. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    /** Returns the timestamp of the last save, or null. */
    fun lastSavedAt(context: Context): String? =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SAVED_AT, null)
}

data class SavedCrewInfo(
    val osm200: String, val tm200: String, val crew200: String, val date200: String,
    val osm201: String, val tm201: String, val crew201: String, val date201: String,
    val savedAt: String
)

/* ==================== MAIN ACTIVITY ==================== */

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

    // ── PDF colours ──────────────────────────────────────────────────
    private val pdfOrange    = android.graphics.Color.parseColor("#FF6600")
    private val pdfBlack     = android.graphics.Color.parseColor("#1A1A1A")
    private val pdfWhite     = android.graphics.Color.WHITE
    private val pdfLightGray = android.graphics.Color.parseColor("#F5F5F5")
    private val pdfMidGray   = android.graphics.Color.parseColor("#CCCCCC")
    private val pdfDarkGray  = android.graphics.Color.parseColor("#4A4A4A")
    private val pdfGreen     = android.graphics.Color.parseColor("#2E7D32")

    private val PW = 1240f
    private val PH = 1754f
    private val ML = 60f
    private val MR = 60f
    private val CW = PW - ML - MR

    private fun android.graphics.Canvas.fillRect(l: Float, t: Float, r: Float, b: Float, color: Int) {
        val p = Paint().also { it.color = color; it.style = Paint.Style.FILL }
        drawRect(l, t, r, b, p)
    }

    private fun android.graphics.Canvas.strokeRect(l: Float, t: Float, r: Float, b: Float, color: Int, stroke: Float = 1f) {
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

    private fun drawPageHeader(
        canvas: android.graphics.Canvas,
        title: String, subtitle: String,
        osm: String, tm: String, crew: String, date: String,
        pageNum: Int
    ): Float {
        canvas.fillRect(0f, 0f, PW, 110f, pdfOrange)
        canvas.fillRect(ML, 15f, ML + 80f, 95f, android.graphics.Color.parseColor("#CC4400"))
        val logoPaint = Paint().also { it.color = pdfWhite; it.textSize = 26f; it.isFakeBoldText = true; it.textAlign = Paint.Align.CENTER }
        canvas.drawText("KR", ML + 40f, 65f, logoPaint)
        val titlePaint = Paint().also { it.color = pdfWhite; it.textSize = 32f; it.isFakeBoldText = true }
        canvas.drawText(title, ML + 95f, 55f, titlePaint)
        val subPaint = Paint().also { it.color = android.graphics.Color.parseColor("#FFD0A0"); it.textSize = 16f }
        canvas.drawText(subtitle, ML + 97f, 80f, subPaint)
        val pgPaint = Paint().also { it.color = pdfWhite; it.textSize = 14f; it.textAlign = Paint.Align.RIGHT }
        canvas.drawText("Page $pageNum", PW - MR, 60f, pgPaint)
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

    private fun drawSectionHeading(canvas: android.graphics.Canvas, title: String, y: Float): Float {
        canvas.fillRect(ML, y, ML + CW, y + 36f, pdfBlack)
        val p = Paint().also { it.color = pdfOrange; it.textSize = 18f; it.isFakeBoldText = true }
        canvas.drawText(title, ML + 12f, y + 26f, p)
        return y + 36f
    }

    private fun drawCategoryHeading(canvas: android.graphics.Canvas, name: String, y: Float): Float {
        canvas.fillRect(ML, y, ML + CW, y + 28f, pdfDarkGray)
        val p = Paint().also { it.color = pdfWhite; it.textSize = 14f; it.isFakeBoldText = true }
        canvas.drawText(name, ML + 10f, y + 20f, p)
        return y + 28f
    }

    private val foodCols = listOf(0f, 340f, 430f, 520f, 610f, 700f, 790f, 880f)
    private val foodHdrs = listOf("PRODUCT", "CLOSE", "LOAD", "TOTAL", "SALES", "PRE", "WASTE", "END")

    private fun drawFoodTableHeader(canvas: android.graphics.Canvas, y: Float): Float {
        canvas.fillRect(ML, y, ML + CW, y + 32f, pdfOrange)
        val p = Paint().also { it.color = pdfWhite; it.textSize = 12f; it.isFakeBoldText = true }
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
            } else canvas.drawText(v.ifEmpty { "—" }, cx, y + 18f, if (i == 2) totalP else center)
            canvas.fillRect(colX, y, colX + 0.5f, y + rowH, pdfMidGray)
        }
        return y + rowH
    }

    private val bevCols = listOf(0f, 200f, 270f, 340f, 410f, 485f, 560f, 635f, 710f, 790f, 870f, 950f)

    private fun drawBevTableHeader(canvas: android.graphics.Canvas, y: Float, serviceNumber: String): Float {
        val closingDay = if (serviceNumber == "200") "201" else "200"
        val loadLoc = if (serviceNumber == "200") "WLG" else "AKL"
        val wasteLoc = if (serviceNumber == "200") "AKL" else "WLG"
        canvas.fillRect(ML, y, ML + CW, y + 30f, pdfOrange)
        val p = Paint().also { it.color = pdfWhite; it.textSize = 10f; it.isFakeBoldText = true; it.textAlign = Paint.Align.CENTER }
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
        val parPaint = Paint().also { it.color = if (isBelowPar) pdfOrange else pdfDarkGray; it.textSize = 10f; it.textAlign = Paint.Align.CENTER; it.isFakeBoldText = isBelowPar }
        val center = Paint().also { it.color = pdfDarkGray; it.textSize = 10f; it.textAlign = Paint.Align.CENTER }
        val totPaint = Paint().also { it.color = pdfOrange; it.textSize = 10f; it.textAlign = Paint.Align.CENTER; it.isFakeBoldText = true }
        fun colCx(a: Int) = ML + (bevCols[a] + bevCols[a + 1]) / 2f
        canvas.drawText(row.product, ML + 4f, y + 18f, normal)
        canvas.drawText(row.parLevel.ifEmpty { "—" }, colCx(1), y + 18f, parPaint)
        canvas.drawText(row.closingCafe.ifEmpty { "—" }, colCx(2), y + 18f, center)
        canvas.drawText(row.closingAG.ifEmpty { "—" }, colCx(3), y + 18f, center)
        canvas.drawText(row.loading.ifEmpty { "—" }, colCx(4), y + 18f, center)
        val totalVal = (row.closingCafe.toIntOrNull() ?: 0) + (row.closingAG.toIntOrNull() ?: 0) + (row.loading.toIntOrNull() ?: 0)
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

        var (page, canvas) = newPage(pdf, pageNum)
        var y = drawPageHeader(canvas, docTitle, "FOOD STOCK — CLOSING SHEET", osm, tm, crew, date, pageNum)
        drawPageFooter(canvas, generated)
        y = drawSectionHeading(canvas, "FOOD STOCK", y) + 4f
        y = drawFoodTableHeader(canvas, y)

        var rowShade = false
        categories.forEach { category ->
            if (y > PH - 100f) {
                pdf.finishPage(page); pageNum++
                val np = newPage(pdf, pageNum); page = np.first; canvas = np.second
                y = drawPageHeader(canvas, docTitle, "FOOD STOCK (cont.)", osm, tm, crew, date, pageNum)
                drawPageFooter(canvas, generated); y = drawFoodTableHeader(canvas, y)
            }
            y = drawCategoryHeading(canvas, category.name, y); rowShade = false
            category.rows.forEach { row ->
                if (y > PH - 80f) {
                    pdf.finishPage(page); pageNum++
                    val np = newPage(pdf, pageNum); page = np.first; canvas = np.second
                    y = drawPageHeader(canvas, docTitle, "FOOD STOCK (cont.)", osm, tm, crew, date, pageNum)
                    drawPageFooter(canvas, generated); y = drawFoodTableHeader(canvas, y); rowShade = false
                }
                y = drawFoodRow(canvas, row, y, rowShade); rowShade = !rowShade
            }
        }
        pdf.finishPage(page)

        pageNum++
        var bp = newPage(pdf, pageNum); page = bp.first; canvas = bp.second
        y = drawPageHeader(canvas, docTitle, "RETAIL STOCK — CAFÉ & AG", osm, tm, crew, date, pageNum)
        drawPageFooter(canvas, generated)
        y = drawSectionHeading(canvas, "RETAIL STOCK (CAFÉ & AG)", y) + 4f
        y = drawBevTableHeader(canvas, y, serviceNum)

        rowShade = false
        beverages.forEach { section ->
            if (y > PH - 100f) {
                pdf.finishPage(page); pageNum++
                val np = newPage(pdf, pageNum); page = np.first; canvas = np.second
                y = drawPageHeader(canvas, docTitle, "RETAIL STOCK (cont.)", osm, tm, crew, date, pageNum)
                drawPageFooter(canvas, generated); y = drawBevTableHeader(canvas, y, serviceNum)
            }
            y = drawCategoryHeading(canvas, section.name, y); rowShade = false
            section.rows.forEach { row ->
                if (y > PH - 80f) {
                    pdf.finishPage(page); pageNum++
                    val np = newPage(pdf, pageNum); page = np.first; canvas = np.second
                    y = drawPageHeader(canvas, docTitle, "RETAIL STOCK (cont.)", osm, tm, crew, date, pageNum)
                    drawPageFooter(canvas, generated); y = drawBevTableHeader(canvas, y, serviceNum); rowShade = false
                }
                y = drawBevRow(canvas, row, y, rowShade); rowShade = !rowShade
            }
        }
        pdf.finishPage(page)

        val fileName = "${pageName}_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)
        pdf.writeTo(FileOutputStream(file)); pdf.close()
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

/* ==================== MAIN SCREEN ==================== */

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onExportPdf: (String, List<CategorySection>, List<BeverageSection>, String, String, String, String, List<CategorySection>, List<CategorySection>) -> Unit
) {
    val context = LocalContext.current

    // ── Sheet data — initialised from saved state immediately ─────────
    val sheet200 = remember { getFoodCategories() }
    val beverages200 = remember { getBeverageCategories() }
    val sheet201 = remember { getFoodCategories() }
    val beverages201 = remember { getBeverageCategories() }

    // ── Crew fields ────────────────────────────────────────────────────
    var osm200  by remember { mutableStateOf("") }
    var tm200   by remember { mutableStateOf("") }
    var crew200 by remember { mutableStateOf("") }
    var date200 by remember { mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())) }
    var osm201  by remember { mutableStateOf("") }
    var tm201   by remember { mutableStateOf("") }
    var crew201 by remember { mutableStateOf("") }
    var date201 by remember { mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())) }

    // ── Autosave state ─────────────────────────────────────────────────
    var lastSavedTime by remember { mutableStateOf("") }
    var showRestoredBanner by remember { mutableStateOf(false) }

    // ── RESTORE on first composition ───────────────────────────────────
    // Runs once: fills sheets and crew fields from SharedPreferences.
    LaunchedEffect(Unit) {
        val saved = SaveManager.restore(context, sheet200, beverages200, sheet201, beverages201)
        if (saved != null) {
            osm200  = saved.osm200;  tm200   = saved.tm200
            crew200 = saved.crew200; date200 = saved.date200.ifEmpty { date200 }
            osm201  = saved.osm201;  tm201   = saved.tm201
            crew201 = saved.crew201; date201 = saved.date201.ifEmpty { date201 }
            lastSavedTime = saved.savedAt
            showRestoredBanner = true
            delay(3000)
            showRestoredBanner = false
        }
    }

    // ── DEBOUNCED AUTOSAVE ─────────────────────────────────────────────
    // Any time crew fields or a saveVersion counter changes, wait 1.5 s of
    // inactivity then write to SharedPreferences.  Field edits bump saveVersion
    // via the StockSheet callbacks; direct crew-field changes trigger directly.
    var saveVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(
        osm200, tm200, crew200, date200,
        osm201, tm201, crew201, date201,
        saveVersion
    ) {
        delay(1500)  // debounce: wait for user to pause typing
        SaveManager.save(
            context,
            osm200, tm200, crew200, date200, sheet200, beverages200,
            osm201, tm201, crew201, date201, sheet201, beverages201
        )
        lastSavedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
    }

    var selectedTrip by remember { mutableStateOf<String?>(null) }
    var clearTrigger200 by remember { mutableIntStateOf(0) }
    var clearTrigger201 by remember { mutableIntStateOf(0) }

    // ── Restored data banner ────────────────────────────────────────────
    if (showRestoredBanner) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            // Shown briefly as a snackbar-style banner
        }
    }

    when (selectedTrip) {
        null -> TripSelectionScreen(
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            lastSavedTime = lastSavedTime,
            showRestoredBanner = showRestoredBanner
        ) { selectedTrip = it }

        "200" -> StockSheet(
            pageName = "WLG-AKL-200",
            displayTitle = "Wellington → Auckland",
            serviceNumber = "200",
            foodCategories = sheet200,
            beverageCategories = beverages200,
            osm = osm200, tm = tm200, crew = crew200, date = date200,
            onOsmChange  = { osm200  = it },
            onTmChange   = { tm200   = it },
            onCrewChange = { crew200 = it },
            onDateChange = { date200 = it },
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            clearTrigger = clearTrigger200,
            lastSavedTime = lastSavedTime,
            onFieldChanged = { saveVersion++ },  // bump to trigger debounced save
            onClearAll = {
                sheet200.forEach { cat -> cat.rows.forEach { row ->
                    row.closingPrev = ""; row.loading = ""; row.total = ""
                    row.sales = ""; row.prePurchase = ""; row.waste = ""; row.endDay = ""
                }}
                beverages200.forEach { cat -> cat.rows.forEach { row ->
                    row.closingCafe = ""; row.closingAG = ""; row.loading = ""; row.total = ""
                    row.sales = ""; row.prePurchase = ""; row.waste = ""; row.endDayCafe = ""; row.endDayAG = ""
                }}
                osm200 = ""; tm200 = ""; crew200 = ""
                date200 = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
                SaveManager.clear(context)
                lastSavedTime = ""
                clearTrigger200++
            },
            onBack = { selectedTrip = null },
            onExportPdf = { pn, cats, bevs, o, t, c, d ->
                onExportPdf(pn, cats, bevs, o, t, c, d, sheet200, sheet201)
            },
            onTransferTotals = {
                sheet200.forEachIndexed { ci, cat -> cat.rows.forEachIndexed { ri, row ->
                    if (row.endDay.isNotEmpty()) sheet201[ci].rows[ri].closingPrev = row.endDay
                }}
                beverages200.forEachIndexed { ci, cat -> cat.rows.forEachIndexed { ri, row ->
                    if (row.endDayCafe.isNotEmpty()) beverages201[ci].rows[ri].closingCafe = row.endDayCafe
                    if (row.endDayAG.isNotEmpty())   beverages201[ci].rows[ri].closingAG   = row.endDayAG
                }}
                saveVersion++
            }
        )

        "201" -> StockSheet(
            pageName = "AKL-WLG-201",
            displayTitle = "Auckland → Wellington",
            serviceNumber = "201",
            foodCategories = sheet201,
            beverageCategories = beverages201,
            osm = osm201, tm = tm201, crew = crew201, date = date201,
            onOsmChange  = { osm201  = it },
            onTmChange   = { tm201   = it },
            onCrewChange = { crew201 = it },
            onDateChange = { date201 = it },
            isDarkMode = isDarkMode,
            onToggleDarkMode = onToggleDarkMode,
            clearTrigger = clearTrigger201,
            lastSavedTime = lastSavedTime,
            onFieldChanged = { saveVersion++ },
            onClearAll = {
                sheet201.forEach { cat -> cat.rows.forEach { row ->
                    row.closingPrev = ""; row.loading = ""; row.total = ""
                    row.sales = ""; row.prePurchase = ""; row.waste = ""; row.endDay = ""
                }}
                beverages201.forEach { cat -> cat.rows.forEach { row ->
                    row.closingCafe = ""; row.closingAG = ""; row.loading = ""; row.total = ""
                    row.sales = ""; row.prePurchase = ""; row.waste = ""; row.endDayCafe = ""; row.endDayAG = ""
                }}
                osm201 = ""; tm201 = ""; crew201 = ""
                date201 = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date())
                SaveManager.clear(context)
                lastSavedTime = ""
                clearTrigger201++
            },
            onBack = { selectedTrip = null },
            onExportPdf = { pn, cats, bevs, o, t, c, d ->
                onExportPdf(pn, cats, bevs, o, t, c, d, sheet200, sheet201)
            },
            onTransferTotals = {
                sheet201.forEachIndexed { ci, cat -> cat.rows.forEachIndexed { ri, row ->
                    if (row.endDay.isNotEmpty()) sheet200[ci].rows[ri].closingPrev = row.endDay
                }}
                beverages201.forEachIndexed { ci, cat -> cat.rows.forEachIndexed { ri, row ->
                    if (row.endDayCafe.isNotEmpty()) beverages200[ci].rows[ri].closingCafe = row.endDayCafe
                    if (row.endDayAG.isNotEmpty())   beverages200[ci].rows[ri].closingAG   = row.endDayAG
                }}
                saveVersion++
            }
        )
    }
}

/* ==================== TRIP SELECTION ==================== */

@Composable
fun TripSelectionScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    lastSavedTime: String,
    showRestoredBanner: Boolean,
    onTripSelected: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(KiwiRailOrange, KiwiRailOrangeLight))).padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Train, "KiwiRail", modifier = Modifier.size(64.dp), tint = KiwiRailWhite)
            }
            Spacer(Modifier.height(32.dp))
            Text("KiwiRail Café Stock", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
            Text("Stock Management System", fontSize = 16.sp, modifier = Modifier.padding(top = 4.dp),
                color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)

            // ── Autosave / restore status ──────────────────────────────
            Spacer(Modifier.height(16.dp))
            if (showRestoredBanner && lastSavedTime.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = KiwiRailGreen.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = KiwiRailGreen, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Session restored", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KiwiRailGreen)
                            Text("Your data from $lastSavedTime was automatically recovered",
                                fontSize = 11.sp, color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)
                        }
                    }
                }
            } else if (lastSavedTime.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CloudDone, null, tint = KiwiRailGreen, modifier = Modifier.size(14.dp).alpha(0.7f))
                    Spacer(Modifier.width(4.dp))
                    Text("Auto-saved at $lastSavedTime", fontSize = 11.sp,
                        color = if (isDarkMode) KiwiRailDarkGray else Color(0xFF888888))
                }
            }

            Spacer(Modifier.height(32.dp))
            ServiceCard("Wellington → Auckland", "Service 200", "Departs 07:45", isDarkMode) { onTripSelected("200") }
            Spacer(Modifier.height(20.dp))
            ServiceCard("Auckland → Wellington", "Service 201", "Departs 08:45", isDarkMode) { onTripSelected("201") }
            Spacer(Modifier.height(40.dp))
            Text("Select a service to begin stock count", fontSize = 12.sp, fontStyle = FontStyle.Italic,
                color = if (isDarkMode) KiwiRailDarkGray else Color.Gray)
        }

        FloatingActionButton(
            onClick = onToggleDarkMode,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(56.dp),
            containerColor = KiwiRailOrange, contentColor = KiwiRailWhite, shape = CircleShape
        ) {
            Icon(if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode, "Toggle theme")
        }
    }
}

@Composable
fun ServiceCard(title: String, subtitle: String, departureTime: String, isDarkMode: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick, interactionSource = interactionSource, indication = null),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPressed) 2.dp else 8.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(KiwiRailGreen))
                    Spacer(Modifier.width(8.dp))
                    Text(subtitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KiwiRailGreen)
                }
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack, modifier = Modifier.padding(vertical = 4.dp))
                Text(departureTime, fontSize = 12.sp, color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)
            }
            Icon(Icons.Filled.ArrowForward, "Select", tint = KiwiRailOrange, modifier = Modifier.size(24.dp))
        }
    }
}

/* ==================== STOCK SHEET ==================== */

@Composable
fun StockSheet(
    pageName: String, displayTitle: String, serviceNumber: String,
    foodCategories: MutableList<CategorySection>, beverageCategories: MutableList<BeverageSection>,
    osm: String, tm: String, crew: String, date: String,
    onOsmChange: (String) -> Unit, onTmChange: (String) -> Unit,
    onCrewChange: (String) -> Unit, onDateChange: (String) -> Unit,
    isDarkMode: Boolean, onToggleDarkMode: () -> Unit,
    clearTrigger: Int,
    lastSavedTime: String,
    onFieldChanged: () -> Unit,   // called after any stock field edit to trigger debounced save
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
        Modifier.fillMaxSize()
            .background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Filled.ArrowBack, "Back", tint = KiwiRailOrange)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(displayTitle, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
                // ── Autosave indicator under the title ─────────────────
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (lastSavedTime.isNotEmpty()) {
                        Icon(Icons.Filled.CloudDone, null, tint = KiwiRailGreen,
                            modifier = Modifier.size(11.dp).alpha(0.8f))
                        Spacer(Modifier.width(3.dp))
                        Text("Saved $lastSavedTime", fontSize = 9.sp,
                            color = if (isDarkMode) KiwiRailDarkGray else Color(0xFF888888))
                    } else {
                        Text("Service $serviceNumber • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}",
                            fontSize = 10.sp, color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)
                    }
                }
            }
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.MoreVert, "Menu", tint = KiwiRailOrange)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)) {
                    DropdownMenuItem(
                        text = { Text("Quick Fill PAR", fontSize = 13.sp, color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack) },
                        onClick = {
                            beverageCategories.forEach { cat -> cat.rows.forEach { row ->
                                val par = row.parLevel.toIntOrNull() ?: 0
                                if (par > 0) row.loading = par.toString()
                            }}
                            onFieldChanged()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.AutoFixHigh, null, tint = KiwiRailOrange) }
                    )
                    DropdownMenuItem(
                        text = { Text("Calculate All Totals", fontSize = 13.sp, color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack) },
                        onClick = {
                            foodCategories.forEach { cat -> cat.rows.forEach { row ->
                                row.total = ((row.closingPrev.toIntOrNull() ?: 0) + (row.loading.toIntOrNull() ?: 0)).toString()
                            }}
                            beverageCategories.forEach { cat -> cat.rows.forEach { row ->
                                row.total = ((row.closingCafe.toIntOrNull() ?: 0) + (row.closingAG.toIntOrNull() ?: 0) + (row.loading.toIntOrNull() ?: 0)).toString()
                            }}
                            onFieldChanged()
                            showMenu = false
                        },
                        leadingIcon = { Icon(Icons.Filled.Calculate, null, tint = KiwiRailOrange) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Toggle Theme", fontSize = 13.sp, color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack) },
                        onClick = { onToggleDarkMode(); showMenu = false },
                        leadingIcon = { Icon(Icons.Filled.DarkMode, null, tint = KiwiRailOrange) }
                    )
                }
            }
        }

        CompletionProgress(foodCategories, beverageCategories, isDarkMode)

        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite),
            shape = RoundedCornerShape(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Food Items", foodItemsCount.toString(), isDarkMode)
                StatItem("Beverages", beverageItemsCount.toString(), isDarkMode)
                StatItem("Low Stock", lowStockItems.toString(), isDarkMode, isWarning = lowStockItems > 0)
            }
        }

        Text("Crew Information", fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CrewField("OSM",  osm,  onOsmChange,  Modifier.weight(1f), isDarkMode, hasAttemptedSubmit)
            CrewField("TM",   tm,   onTmChange,   Modifier.weight(1f), isDarkMode, hasAttemptedSubmit)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CrewField("CREW", crew, onCrewChange, Modifier.weight(1f), isDarkMode, hasAttemptedSubmit)
            DatePickerField("DATE", date, onDateChange, Modifier.weight(1f), isDarkMode, hasAttemptedSubmit)
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onClearAll(); hasAttemptedSubmit = false },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = KiwiRailOrange),
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
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange, contentColor = KiwiRailWhite),
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
                title = { Text("Missing Information", fontWeight = FontWeight.Bold, color = KiwiRailOrange) },
                text = {
                    Column {
                        Text("Please fill in all crew information fields before exporting:")
                        Spacer(Modifier.height(8.dp))
                        if (osm.isBlank())  Text("• OSM is required",  color = KiwiRailRed)
                        if (tm.isBlank())   Text("• TM is required",   color = KiwiRailRed)
                        if (crew.isBlank()) Text("• CREW is required", color = KiwiRailRed)
                        if (date.isBlank()) Text("• DATE is required", color = KiwiRailRed)
                    }
                },
                confirmButton = {
                    Button(onClick = { showExportError = false },
                        colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange)) { Text("OK") }
                },
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(Modifier.height(20.dp))

        Text("FOOD STOCK", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack, modifier = Modifier.padding(bottom = 12.dp))

        Column {
            CompactTableHeader()
            foodCategories.forEach { category ->
                CompactCategoryHeader(category.name)
                category.rows.forEachIndexed { index, row ->
                    DraggableStockRow(
                        row = row, clearTrigger = clearTrigger,
                        onMoveUp   = { if (index > 0) category.rows.swap(index, index - 1) },
                        onMoveDown = { if (index < category.rows.lastIndex) category.rows.swap(index, index + 1) },
                        onFieldChanged = onFieldChanged
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(thickness = 1.dp, color = KiwiRailOrange.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
        Spacer(Modifier.height(16.dp))

        Text("RETAIL STOCK (CAFÉ & AG)", fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack, modifier = Modifier.padding(bottom = 12.dp))

        Column {
            BeverageTableHeader(serviceNumber = serviceNumber)
            beverageCategories.forEach { section ->
                BeverageCategoryHeader(section.name)
                section.rows.forEach { row ->
                    ModernBeverageStockRow(row = row, clearTrigger = clearTrigger, onFieldChanged = onFieldChanged)
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite),
            shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Ready to move to next service?", fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
                Spacer(Modifier.height(8.dp))
                Text("Transfer end-of-day totals to ${if (serviceNumber == "200") "Service 201" else "Service 200"}'s opening stock",
                    fontSize = 12.sp, textAlign = TextAlign.Center,
                    color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (!isCrewInfoComplete) showTransferError = true else showTransferDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange, contentColor = KiwiRailWhite),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Transfer End Totals to ${if (serviceNumber == "200") "201" else "200"}",
                        fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (showTransferError) {
            AlertDialog(
                onDismissRequest = { showTransferError = false },
                title = { Text("Missing Information", fontWeight = FontWeight.Bold, color = KiwiRailOrange) },
                text = {
                    Column {
                        Text("Please fill in all crew information fields before transferring totals:")
                        Spacer(Modifier.height(8.dp))
                        if (osm.isBlank())  Text("• OSM is required",  color = KiwiRailRed)
                        if (tm.isBlank())   Text("• TM is required",   color = KiwiRailRed)
                        if (crew.isBlank()) Text("• CREW is required", color = KiwiRailRed)
                        if (date.isBlank()) Text("• DATE is required", color = KiwiRailRed)
                    }
                },
                confirmButton = {
                    Button(onClick = { showTransferError = false },
                        colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange)) { Text("OK") }
                },
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        if (showTransferDialog) {
            AlertDialog(
                onDismissRequest = { showTransferDialog = false },
                title = { Text("Transfer End Totals?", fontWeight = FontWeight.Bold, color = KiwiRailOrange) },
                text = {
                    Column {
                        Text("This will copy:", fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp))
                        Text("• Food END values → Service ${if (serviceNumber == "200") "201" else "200"} CLOSING PREV")
                        Text("• Beverage END CAFÉ/AG → Service ${if (serviceNumber == "200") "201" else "200"} CLOSING STOCK")
                        Spacer(Modifier.height(12.dp))
                        Text("Make sure you've completed Service $serviceNumber before transferring!",
                            fontSize = 12.sp, color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray,
                            fontStyle = FontStyle.Italic)
                    }
                },
                confirmButton = {
                    Button(onClick = { onTransferTotals(); showTransferDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = KiwiRailOrange)) { Text("Transfer Now") }
                },
                dismissButton = {
                    TextButton(onClick = { showTransferDialog = false }) {
                        Text("Cancel", color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
                    }
                },
                containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite,
                shape = RoundedCornerShape(16.dp)
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

/* ==================== CREW / DATE FIELDS ==================== */

@Composable
fun CrewField(
    placeholder: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, isDarkMode: Boolean = false, showError: Boolean = false
) {
    val isError = showError && value.isBlank()
    BasicTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.height(48.dp).clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)
            .border(1.5.dp, when {
                isError -> KiwiRailRed; value.isNotEmpty() -> KiwiRailOrange
                isDarkMode -> Color(0xFF666666); else -> Color.LightGray
            }, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack),
        decorationBox = { innerTextField ->
            Box {
                if (value.isEmpty()) {
                    Text(if (isError) "$placeholder *" else placeholder, fontSize = 13.sp,
                        color = if (isError) KiwiRailRed else if (isDarkMode) Color(0xFF888888) else Color.Gray)
                }
                innerTextField()
            }
        }
    )
}

@Composable
fun DatePickerField(
    placeholder: String, value: String, onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier, isDarkMode: Boolean = false, showError: Boolean = false
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    val isError = showError && value.isBlank()
    if (value.isNotEmpty()) {
        try { calendar.time = SimpleDateFormat("dd/MM/yy", Locale.getDefault()).parse(value) ?: Date() } catch (_: Exception) {}
    }
    val dialog = DatePickerDialog(context, { _, y, m, d ->
        val c = Calendar.getInstance().apply { set(y, m, d) }
        onValueChange(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(c.time))
    }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

    Box(modifier = modifier.height(48.dp).clip(RoundedCornerShape(8.dp))
        .background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)
        .border(1.5.dp, when {
            isError -> KiwiRailRed; value.isNotEmpty() -> KiwiRailOrange
            isDarkMode -> Color(0xFF666666); else -> Color.LightGray
        }, RoundedCornerShape(8.dp))
        .clickable { dialog.show() }.padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(value.ifEmpty { if (isError) "$placeholder *" else placeholder }, fontSize = 13.sp,
                color = when {
                    isError && value.isEmpty() -> KiwiRailRed
                    value.isEmpty() -> if (isDarkMode) Color(0xFF888888) else Color.Gray
                    else -> if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                })
            Icon(Icons.Filled.CalendarToday, "Select Date", modifier = Modifier.size(16.dp), tint = KiwiRailOrange)
        }
    }
}

/* ==================== PROGRESS / STATS ==================== */

@Composable
fun CompletionProgress(foodCategories: List<CategorySection>, beverageCategories: List<BeverageSection>, isDarkMode: Boolean) {
    val totalFood = foodCategories.sumOf { it.rows.size }
    val doneFood  = foodCategories.sumOf { cat -> cat.rows.count { it.closingPrev.isNotEmpty() && it.loading.isNotEmpty() && it.endDay.isNotEmpty() } }
    val totalBev  = beverageCategories.sumOf { it.rows.size }
    val doneBev   = beverageCategories.sumOf { cat -> cat.rows.count { it.closingCafe.isNotEmpty() && it.closingAG.isNotEmpty() && it.endDayCafe.isNotEmpty() && it.endDayAG.isNotEmpty() } }
    val total = totalFood + totalBev; val done = doneFood + doneBev
    val progress = if (total > 0) done.toFloat() / total.toFloat() else 0f

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite),
        shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progress", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
                Text("$done/$total items", fontSize = 12.sp, color = KiwiRailOrange)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = KiwiRailOrange,
                trackColor = if (isDarkMode) KiwiRailDarkGray.copy(alpha = 0.5f) else KiwiRailLightGray)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Food: $doneFood/$totalFood", fontSize = 11.sp, color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)
                Text("Beverages: $doneBev/$totalBev", fontSize = 11.sp, color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, isDarkMode: Boolean, isWarning: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold,
            color = if (isWarning) KiwiRailOrange else if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
        Text(label, fontSize = 10.sp, color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray)
    }
}

/* ==================== TABLE HEADERS ==================== */

@Composable
fun CompactTableHeader() {
    Row(modifier = Modifier.fillMaxWidth()
        .background(KiwiRailOrange, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
        .padding(vertical = 10.dp, horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Product", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(2.8f))
        listOf("Close", "Load", "Total", "Sales", "Pre", "Waste", "End").forEach { lbl ->
            Text(lbl, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CompactCategoryHeader(name: String) {
    Box(modifier = Modifier.fillMaxWidth().background(KiwiRailBlack).padding(10.dp)) {
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite)
    }
}

@Composable
fun BeverageTableHeader(serviceNumber: String) {
    val closingDay = if (serviceNumber == "200") "201" else "200"
    val loadLoc = if (serviceNumber == "200") "WLG" else "AKL"
    val wasteLoc = if (serviceNumber == "200") "AKL" else "WLG"
    Column {
        Row(modifier = Modifier.fillMaxWidth()
            .background(KiwiRailOrange, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("PAR\nLEVEL", fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, lineHeight=9.sp, color=KiwiRailWhite)
            Text("PREVIOUS DAY STOCK $closingDay", fontSize=7.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1.6f), textAlign=TextAlign.Center, lineHeight=8.sp, color=KiwiRailWhite)
            Text("LOADING\n@ $loadLoc", fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.9f), textAlign=TextAlign.Center, lineHeight=9.sp, color=KiwiRailWhite)
            Text("TOTAL", fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, color=KiwiRailWhite)
            Text("SALES FOR\nTHE DAY", fontSize=7.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.9f), textAlign=TextAlign.Center, lineHeight=8.sp, color=KiwiRailWhite)
            Text("PRE\nPURCHASE", fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, lineHeight=9.sp, color=KiwiRailWhite)
            Text("WASTE\n@ $wasteLoc", fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, lineHeight=9.sp, color=KiwiRailWhite)
            Text("END OF DAY\nTOTAL", fontSize=7.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(1.6f), textAlign=TextAlign.Center, lineHeight=8.sp, color=KiwiRailWhite)
        }
        Row(modifier = Modifier.fillMaxWidth().background(KiwiRailOrange.copy(alpha = 0.9f))
            .padding(vertical=6.dp, horizontal=6.dp), verticalAlignment=Alignment.CenterVertically) {
            Spacer(Modifier.weight(0.8f))
            Text("CAFÉ", fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, color=KiwiRailWhite)
            Text("AG",   fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, color=KiwiRailWhite)
            Spacer(Modifier.weight(0.9f)); Spacer(Modifier.weight(0.8f)); Spacer(Modifier.weight(0.9f))
            Spacer(Modifier.weight(0.8f)); Spacer(Modifier.weight(0.8f))
            Text("CAFÉ", fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, color=KiwiRailWhite)
            Text("AG",   fontSize=8.sp, fontWeight=FontWeight.Bold, modifier=Modifier.weight(0.8f), textAlign=TextAlign.Center, color=KiwiRailWhite)
        }
    }
}

@Composable
fun BeverageCategoryHeader(name: String) {
    Box(modifier = Modifier.fillMaxWidth().background(KiwiRailBlack).padding(10.dp)) {
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite)
    }
}

/* ==================== FOOD ROW ==================== */

@Composable
fun DraggableStockRow(
    row: StockRow, clearTrigger: Int,
    onMoveUp: () -> Unit, onMoveDown: () -> Unit,
    onFieldChanged: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    val scaleAnim by animateFloatAsState(if (isDragging) 1.04f else 1f, animationSpec = tween(150))

    var closeValue by remember(clearTrigger, row.closingPrev) { mutableStateOf(row.closingPrev) }
    var loadValue  by remember(clearTrigger, row.loading)     { mutableStateOf(row.loading) }

    val calculatedTotal by remember(closeValue, loadValue) {
        derivedStateOf {
            val c = closeValue.toIntOrNull() ?: 0
            val l = loadValue.toIntOrNull()  ?: 0
            if (c + l > 0 || closeValue.isNotEmpty() || loadValue.isNotEmpty()) (c + l).toString() else ""
        }
    }
    row.total = calculatedTotal

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().scale(scaleAnim)
            .background(if (isDragging) KiwiRailOrange.copy(alpha = 0.08f) else RowBackground)
            .border(if (isDragging) 2.dp else 0.5.dp, if (isDragging) KiwiRailOrange else RowBorderNormal)
            .pointerInput(row.id) {
                var acc = 0f
                detectDragGesturesAfterLongPress(
                    onDragStart  = { isDragging = true;  acc = 0f },
                    onDragEnd    = { isDragging = false; acc = 0f },
                    onDragCancel = { isDragging = false; acc = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume(); acc += dragAmount.y
                        when { acc < -30f -> { onMoveUp();   acc = 0f }; acc > 30f -> { onMoveDown(); acc = 0f } }
                    }
                )
            }
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Row(modifier = Modifier.weight(2.8f), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DragHandle, "Drag", modifier = Modifier.size(16.dp).alpha(if (isDragging) 1f else 0.45f), tint = KiwiRailOrange)
            Spacer(Modifier.width(4.dp))
            Text(row.product, fontSize = 11.sp, color = RowTextColor)
        }
        CompactNumericField(closeValue, Modifier.weight(1f), clearTrigger) { closeValue = it; row.closingPrev = it; onFieldChanged() }
        CompactNumericField(loadValue,  Modifier.weight(1f), clearTrigger) { loadValue  = it; row.loading     = it; onFieldChanged() }
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 2.dp).height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (calculatedTotal.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.12f) else Color(0xFFF0F0F0))
                .border(1.dp, if (calculatedTotal.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.4f) else Color(0xFFDDDDDD), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(calculatedTotal, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = if (calculatedTotal.isNotEmpty()) KiwiRailOrange else Color.Gray)
        }
        CompactNumericField(row.sales,       Modifier.weight(1f), clearTrigger) { row.sales       = it; onFieldChanged() }
        CompactNumericField(row.prePurchase, Modifier.weight(1f), clearTrigger) { row.prePurchase = it; onFieldChanged() }
        CompactNumericField(row.waste,       Modifier.weight(1f), clearTrigger) { row.waste       = it; onFieldChanged() }
        CompactNumericField(row.endDay,      Modifier.weight(1f), clearTrigger) { row.endDay      = it; onFieldChanged() }
    }
}

/* ==================== BEVERAGE ROW ==================== */

@Composable
fun ModernBeverageStockRow(row: BeverageRow, clearTrigger: Int, onFieldChanged: () -> Unit) {
    // Use only clearTrigger as the remember key — prevents cross-field contamination on recompose
    var closingCafe by remember(clearTrigger) { mutableStateOf(row.closingCafe) }
    var closingAG   by remember(clearTrigger) { mutableStateOf(row.closingAG) }
    var loading     by remember(clearTrigger) { mutableStateOf(row.loading) }

    val calculatedTotal by remember(closingCafe, closingAG, loading) {
        derivedStateOf {
            val t = (closingCafe.toIntOrNull() ?: 0) + (closingAG.toIntOrNull() ?: 0) + (loading.toIntOrNull() ?: 0)
            if (t > 0) t.toString() else ""
        }
    }
    row.total = if (calculatedTotal.isEmpty()) "0" else calculatedTotal

    val endTotal   = (row.endDayCafe.toIntOrNull() ?: 0) + (row.endDayAG.toIntOrNull() ?: 0)
    val par        = row.parLevel.toIntOrNull() ?: 0
    val isBelowPar = endTotal > 0 && par > 0 && endTotal < par

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
            .background(RowBackground)
            .border(if (isBelowPar) 2.dp else 0.5.dp, if (isBelowPar) KiwiRailOrange else RowBorderNormal)
            .padding(vertical = 4.dp, horizontal = 4.dp)
    ) {
        Column(Modifier.weight(0.8f)) {
            Text(row.product, fontSize = 9.sp, lineHeight = 10.sp, color = RowTextColor)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.parLevel, fontSize = 8.sp, color = if (isBelowPar) KiwiRailOrange else KiwiRailInfo, fontWeight = FontWeight.Bold)
                if (isBelowPar) { Spacer(Modifier.width(2.dp)); Icon(Icons.Filled.Warning, "Low stock", modifier = Modifier.size(8.dp), tint = KiwiRailOrange) }
            }
        }
        TinyNumericField(closingCafe, Modifier.weight(0.8f), clearTrigger) { closingCafe = it; row.closingCafe = it; onFieldChanged() }
        TinyNumericField(closingAG,   Modifier.weight(0.8f), clearTrigger) { closingAG   = it; row.closingAG   = it; onFieldChanged() }
        TinyNumericField(loading,     Modifier.weight(0.9f), clearTrigger) { loading     = it; row.loading     = it; onFieldChanged() }
        Box(
            modifier = Modifier.weight(0.8f).padding(horizontal = 1.dp).height(32.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (calculatedTotal.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.12f) else Color(0xFFF0F0F0))
                .border(1.dp, if (calculatedTotal.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.4f) else Color(0xFFDDDDDD), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(calculatedTotal, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                color = if (calculatedTotal.isNotEmpty()) KiwiRailOrange else Color.Gray)
        }
        TinyNumericField(row.sales,       Modifier.weight(0.9f), clearTrigger) { row.sales       = it; onFieldChanged() }
        TinyNumericField(row.prePurchase, Modifier.weight(0.8f), clearTrigger) { row.prePurchase = it; onFieldChanged() }
        TinyNumericField(row.waste,       Modifier.weight(0.8f), clearTrigger) { row.waste       = it; onFieldChanged() }
        TinyNumericField(row.endDayCafe,  Modifier.weight(0.8f), clearTrigger) { row.endDayCafe  = it; onFieldChanged() }
        TinyNumericField(row.endDayAG,    Modifier.weight(0.8f), clearTrigger) { row.endDayAG    = it; onFieldChanged() }
    }
}

/* ==================== INPUT FIELDS ==================== */

@Composable
fun CompactNumericField(value: String, modifier: Modifier = Modifier, clearTrigger: Int = 0, onChange: (String) -> Unit) {
    var textValue by remember(value, clearTrigger) { mutableStateOf(value) }
    BasicTextField(
        value = textValue,
        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) { textValue = it; onChange(it) } },
        modifier = modifier.padding(horizontal = 2.dp).height(36.dp).clip(RoundedCornerShape(6.dp))
            .background(InputBackground)
            .border(1.dp, if (textValue.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.6f) else RowBorderNormal, RoundedCornerShape(6.dp)),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(fontSize = 11.sp, textAlign = TextAlign.Center, color = KiwiRailBlack,
            fontWeight = if (textValue.isNotEmpty()) FontWeight.Bold else FontWeight.Normal),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { innerTextField() }
        }
    )
}

@Composable
fun TinyNumericField(value: String, modifier: Modifier = Modifier, clearTrigger: Int = 0, onChange: (String) -> Unit) {
    // Only clearTrigger as key — prevents AG field contamination when CAFÉ is typed
    var textValue by remember(clearTrigger) { mutableStateOf(value) }
    BasicTextField(
        value = textValue,
        onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) { textValue = it; onChange(it) } },
        modifier = modifier.padding(horizontal = 1.dp).height(32.dp).clip(RoundedCornerShape(4.dp))
            .background(InputBackground)
            .border(1.dp, if (textValue.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.6f) else RowBorderNormal, RoundedCornerShape(4.dp)),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, textAlign = TextAlign.Center, color = KiwiRailBlack,
            fontWeight = if (textValue.isNotEmpty()) FontWeight.Bold else FontWeight.Normal),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { innerTextField() }
        }
    )
}

/* ==================== DATA ==================== */

fun getFoodCategories(): MutableList<CategorySection> = mutableListOf(
    CategorySection("BREAKFAST", mutableListOf(
        StockRow(product = "Growers Breakfast"), StockRow(product = "Breakfast Croissant"),
        StockRow(product = "Big Breakfast"),     StockRow(product = "Pancakes"),
        StockRow(product = "Chia Seeds"),        StockRow(product = "Fruit Salads")
    )),
    CategorySection("SWEETS", mutableListOf(
        StockRow(product = "Brownie Slices"),           StockRow(product = "Cookie Time Biscuits"),
        StockRow(product = "Cookie Time GF Biscuits"),  StockRow(product = "Carrot Cake"),
        StockRow(product = "ANZAC Biscuits"),           StockRow(product = "Blueberry Muffins"),
        StockRow(product = "Cheese Scones")
    )),
    CategorySection("SALADS", mutableListOf(
        StockRow(product = "Leafy Salad"), StockRow(product = "Smoked Chicken Pasta Salad")
    )),
    CategorySection("SANDWICHES AND WRAP", mutableListOf(
        StockRow(product = "BLT"),         StockRow(product = "Chicken Wrap"),
        StockRow(product = "Beef Pickle"), StockRow(product = "Ham and Cheese Toastie")
    )),
    CategorySection("HOT MEALS", mutableListOf(
        StockRow(product = "Mac & Cheese"), StockRow(product = "Lasagne"),
        StockRow(product = "Roast Chicken"), StockRow(product = "Lamb Shank"),
        StockRow(product = "Beef Cheek")
    )),
    CategorySection("PIES", mutableListOf(
        StockRow(product = "Steak and Cheese"), StockRow(product = "Vegetarian")
    )),
    CategorySection("SWEET AND ICE CREAM", mutableListOf(
        StockRow(product = "KAPITI BOYSENBERRY"),    StockRow(product = "KAPITI PASSIONFRUIT"),
        StockRow(product = "KAPITI CHOCOLATE CUPS"), StockRow(product = "MEMPHIS BIK BIKKIE")
    )),
    CategorySection("CHEESEBOARD", mutableListOf(StockRow(product = "Cheeseboard")))
)

fun getBeverageCategories(): MutableList<BeverageSection> = mutableListOf(
    BeverageSection("SNACKS", mutableListOf(
        BeverageRow(product = "Whittakers White Choc", parLevel = "48"),
        BeverageRow(product = "Whittakers Brown Choc", parLevel = "48"),
        BeverageRow(product = "ETA Nuts",              parLevel = "24")
    )),
    BeverageSection("PROPER CHIPS", mutableListOf(
        BeverageRow(product = "Sea Salt",      parLevel = "18"),
        BeverageRow(product = "Cider Vinegar", parLevel = "18"),
        BeverageRow(product = "Garden Medly",  parLevel = "18")
    )),
    BeverageSection("BEERS", mutableListOf(
        BeverageRow(product = "Steinlager Ultra",     parLevel = "24"),
        BeverageRow(product = "Duncans Pilsner",      parLevel = "12"),
        BeverageRow(product = "Ruapehu Stout",        parLevel = "12"),
        BeverageRow(product = "Parrot dog Hazy IPA",  parLevel = "12"),
        BeverageRow(product = "Garage Project TINY",  parLevel = "12"),
        BeverageRow(product = "Panhead Supercharger", parLevel = "12"),
        BeverageRow(product = "Sawmill Nimble",       parLevel = "12")
    )),
    BeverageSection("PRE MIXES", mutableListOf(
        BeverageRow(product = "Pals Vodka",        parLevel = "10"),
        BeverageRow(product = "Scapegrace Gin",    parLevel = "12"),
        BeverageRow(product = "Coruba Rum & Cola", parLevel = "12"),
        BeverageRow(product = "Apple Cider",       parLevel = "12"),
        BeverageRow(product = "AF Apero Spirtz",   parLevel = "12")
    )),
    BeverageSection("WINES", mutableListOf(
        BeverageRow(product = "Joiy the Gryphon 250ml", parLevel = "24"),
        BeverageRow(product = "The Ned Sav 250ml",      parLevel = "24"),
        BeverageRow(product = "Matahiwi Cuvee 250ml",   parLevel = "24"),
        BeverageRow(product = "Summer Love 250ml",      parLevel = "24")
    )),
    BeverageSection("SOFT DRINKS", mutableListOf(
        BeverageRow(product = "H2go Water 750ml",      parLevel = "12"),
        BeverageRow(product = "NZ SP Water 500ml",     parLevel = "18"),
        BeverageRow(product = "Bundaberg Lemon Lime",  parLevel = "10"),
        BeverageRow(product = "Bundaberg Ginger Beer", parLevel = "10"),
        BeverageRow(product = "7 UP",                  parLevel = "10"),
        BeverageRow(product = "Pepsi",                 parLevel = "10"),
        BeverageRow(product = "Pepsi Max",             parLevel = "10"),
        BeverageRow(product = "McCoy Orange Juice",    parLevel = "15"),
        BeverageRow(product = "Boss Coffee",           parLevel = "6")
    )),
    BeverageSection("750 ML WINE", mutableListOf(
        BeverageRow(product = "Hunters 750ml",              parLevel = "6"),
        BeverageRow(product = "Kumeru Pinot Gris 750ml",    parLevel = "6"),
        BeverageRow(product = "Dog Point Sav 750ml",        parLevel = "6"),
        BeverageRow(product = "Clearview Chardonnay 750ml", parLevel = "6")
    ))
)

fun <T> MutableList<T>.swap(i: Int, j: Int) {
    val tmp = this[i]; this[i] = this[j]; this[j] = tmp
}