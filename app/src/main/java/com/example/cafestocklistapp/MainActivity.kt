package com.example.cafestocklistapp

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.cafestocklistapp.ui.theme.CafeStockListAppTheme
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/* ==================== KIWIRAIL BRANDING COLORS ==================== */
private val KiwiRailOrange = Color(0xFFFF6600)
private val KiwiRailBlack = Color(0xFF1A1A1A)
private val KiwiRailWhite = Color(0xFFFFFFFF)
private val KiwiRailLightGray = Color(0xFFF5F5F5)
private val KiwiRailDarkGray = Color(0xFF4A4A4A)
private val KiwiRailGreen = Color(0xFF4CAF50)
private val KiwiRailRed = Color(0xFFE53935)

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
                    ) { pageName, categories, beverages, osm, tm, crew, date ->
                        exportToPdf(pageName, categories, beverages, osm, tm, crew, date)
                    }
                }
            }
        }
    }

    private fun exportToPdf(
        pageName: String,
        categories: List<CategorySection>,
        beverages: List<BeverageSection>,
        osm: String,
        tm: String,
        crew: String,
        date: String
    ) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(1200, 1800, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("$pageName CLOSING STOCK", 40f, 50f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("OSM: $osm / TM: $tm / CREW: $crew / DATE: $date", 40f, 80f, paint)

        var y = 120f

        categories.forEach { category ->
            paint.textSize = 16f
            paint.isFakeBoldText = true
            canvas.drawText(category.name, 50f, y, paint)
            y += 20f

            paint.textSize = 10f
            paint.isFakeBoldText = false
            category.rows.forEach { row ->
                if (y > 1700f) return@forEach
                canvas.drawText(
                    "${row.product}: C:${row.closingPrev} L:${row.loading} T:${row.total}",
                    60f, y, paint
                )
                y += 16f
            }
            y += 15f
        }

        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("RETAIL STOCK", 50f, y, paint)
        y += 25f

        beverages.forEach { section ->
            paint.textSize = 14f
            canvas.drawText(section.name, 50f, y, paint)
            y += 18f

            paint.textSize = 9f
            paint.isFakeBoldText = false
            section.rows.forEach { row ->
                if (y > 1700f) return@forEach
                canvas.drawText(
                    "${row.product} (${row.parLevel}): Café:${row.closingCafe} AG:${row.closingAG}",
                    60f, y, paint
                )
                y += 14f
            }
            y += 12f
        }

        pdf.finishPage(page)

        val fileName = "${pageName}_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        pdf.writeTo(FileOutputStream(file))
        pdf.close()

        sendEmail(file, pageName)
    }

    private fun sendEmail(file: File, pageName: String) {
        val subject = "$pageName Stock Sheet - ${
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        }"

        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Attached stock sheet for $pageName.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(intent, "Send Stock Sheet via"))
    }
}

/* ================= UI SYSTEM ================= */

@Composable
fun MainScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onExportPdf: (String, List<CategorySection>, List<BeverageSection>, String, String, String, String) -> Unit
) {
    var selectedTrip by remember { mutableStateOf<String?>(null) }

    when (selectedTrip) {
        null -> {
            TripSelectionScreen(
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                onTripSelected = { trip -> selectedTrip = trip }
            )
        }
        "200" -> {
            val sheet200 = remember { getFoodCategories() }
            val beverages200 = remember { getBeverageCategories() }
            var osm by remember { mutableStateOf("") }
            var tm by remember { mutableStateOf("") }
            var crew by remember { mutableStateOf("") }
            var date by remember {
                mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date()))
            }
            var clearTrigger by remember { mutableStateOf(0) }

            StockSheet(
                pageName = "WLG-AKL-200",
                displayTitle = "WLG → AKL (200)",
                serviceNumber = "200",
                foodCategories = sheet200,
                beverageCategories = beverages200,
                osm = osm,
                tm = tm,
                crew = crew,
                date = date,
                onOsmChange = { osm = it },
                onTmChange = { tm = it },
                onCrewChange = { crew = it },
                onDateChange = { date = it },
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                clearTrigger = clearTrigger,
                onClearAll = { clearTrigger++ },
                onBack = { selectedTrip = null },
                onExportPdf = onExportPdf
            )
        }
        "201" -> {
            val sheet201 = remember { getFoodCategories() }
            val beverages201 = remember { getBeverageCategories() }
            var osm by remember { mutableStateOf("") }
            var tm by remember { mutableStateOf("") }
            var crew by remember { mutableStateOf("") }
            var date by remember {
                mutableStateOf(SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date()))
            }
            var clearTrigger by remember { mutableStateOf(0) }

            StockSheet(
                pageName = "AKL-WLG-201",
                displayTitle = "AKL → WLG (201)",
                serviceNumber = "201",
                foodCategories = sheet201,
                beverageCategories = beverages201,
                osm = osm,
                tm = tm,
                crew = crew,
                date = date,
                onOsmChange = { osm = it },
                onTmChange = { tm = it },
                onCrewChange = { crew = it },
                onDateChange = { date = it },
                isDarkMode = isDarkMode,
                onToggleDarkMode = onToggleDarkMode,
                clearTrigger = clearTrigger,
                onClearAll = { clearTrigger++ },
                onBack = { selectedTrip = null },
                onExportPdf = onExportPdf
            )
        }
    }
}

@Composable
fun TripSelectionScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onTripSelected: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(50.dp))
                    .background(KiwiRailOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "KR",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = KiwiRailWhite
                )
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "KiwiRail Cafe",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
            )

            Text(
                "Stock Management",
                fontSize = 16.sp,
                color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "Select Your Service",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = KiwiRailOrange
            )

            Spacer(Modifier.height(40.dp))

            TripButton(
                title = "WLG → AKL",
                subtitle = "Service 200",
                isDarkMode = isDarkMode,
                onClick = { onTripSelected("200") }
            )

            Spacer(Modifier.height(20.dp))

            TripButton(
                title = "AKL → WLG",
                subtitle = "Service 201",
                isDarkMode = isDarkMode,
                onClick = { onTripSelected("201") }
            )
        }

        IconButton(
            onClick = onToggleDarkMode,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Toggle Dark Mode",
                tint = KiwiRailOrange
            )
        }
    }
}

@Composable
fun TripButton(
    title: String,
    subtitle: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    fontSize = 14.sp,
                    color = KiwiRailOrange,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(KiwiRailOrange),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "→",
                    fontSize = 32.sp,
                    color = KiwiRailWhite,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    onExportPdf: (String, List<CategorySection>, List<BeverageSection>, String, String, String, String) -> Unit
) {
    // Calculate statistics
    val foodItemsCount = foodCategories.sumOf { category -> category.rows.count { row -> row.endDay.isNotEmpty() } }
    val beverageItemsCount = beverageCategories.sumOf { category -> category.rows.count { row ->
        row.endDayCafe.isNotEmpty() || row.endDayAG.isNotEmpty()
    } }
    val lowStockItems = beverageCategories.flatMap { it.rows }.count { row ->
        val endTotal = (row.endDayCafe.toIntOrNull() ?: 0) + (row.endDayAG.toIntOrNull() ?: 0)
        val par = row.parLevel.toIntOrNull() ?: 0
        endTotal > 0 && par > 0 && endTotal < par
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = KiwiRailOrange
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    displayTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
                )
                Text(
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()),
                    fontSize = 12.sp,
                    color = if (isDarkMode) KiwiRailLightGray else KiwiRailDarkGray
                )
            }

            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode",
                    tint = KiwiRailOrange
                )
            }
        }

        // Quick Stats Card
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

        Text(
            "CLOSING STOCK - PREVIOUS DAY",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PlaceholderTextField("OSM", osm, onOsmChange, Modifier.weight(1f), isDarkMode)
            PlaceholderTextField("TM", tm, onTmChange, Modifier.weight(1f), isDarkMode)
            PlaceholderTextField("CREW", crew, onCrewChange, Modifier.weight(1f), isDarkMode)
            DatePickerField("DATE", date, onDateChange, Modifier.weight(1f), isDarkMode)
        }

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = {
                    foodCategories.forEach { cat ->
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
                    beverageCategories.forEach { cat ->
                        cat.rows.forEach { row ->
                            row.closingCafe = ""
                            row.closingAG = ""
                            row.loading = ""
                            row.total = ""
                            row.sales = ""
                            row.prePurchase = ""
                            row.waste = ""
                            row.endDayCafe = ""
                            row.endDayAG = ""
                        }
                    }
                    onClearAll()
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = KiwiRailOrange
                ),
                border = androidx.compose.foundation.BorderStroke(2.dp, KiwiRailOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Clear", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    // Fill PAR levels to closing stock
                    beverageCategories.forEach { cat ->
                        cat.rows.forEach { row ->
                            val par = row.parLevel.toIntOrNull() ?: 0
                            if (par > 0 && row.closingCafe.isEmpty()) {
                                row.closingCafe = (par / 2).toString()
                                row.closingAG = (par / 2).toString()
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite,
                    contentColor = KiwiRailOrange
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Fill PAR", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    onExportPdf(pageName, foodCategories, beverageCategories, osm, tm, crew, date)
                },
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KiwiRailOrange,
                    contentColor = KiwiRailWhite
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            "FOOD STOCK",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Column {
                CompactTableHeader()

                foodCategories.forEach { category ->
                    CompactCategoryHeader(category.name, isDarkMode)

                    category.rows.forEachIndexed { index, row ->
                        DraggableStockRow(
                            row = row,
                            clearTrigger = clearTrigger,
                            isDarkMode = isDarkMode,
                            onMoveUp = {
                                if (index > 0) category.rows.swap(index, index - 1)
                            },
                            onMoveDown = {
                                if (index < category.rows.lastIndex) category.rows.swap(
                                    index,
                                    index + 1
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        HorizontalDivider(thickness = 2.dp, color = KiwiRailOrange)

        Spacer(Modifier.height(16.dp))

        Text(
            "RETAIL STOCK (CAFÉ & AG)",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent
        ) {
            Column {
                BeverageTableHeader(serviceNumber = serviceNumber)

                beverageCategories.forEach { section ->
                    BeverageCategoryHeader(section.name)
                    section.rows.forEach { row ->
                        BeverageStockRow(row = row, clearTrigger = clearTrigger, isDarkMode = isDarkMode)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun StatItem(label: String, value: String, isDarkMode: Boolean, isWarning: Boolean = false) {
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


@Composable
fun PlaceholderTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)
            .border(
                1.dp,
                if (value.isNotEmpty()) KiwiRailOrange else if (isDarkMode) KiwiRailDarkGray.copy(alpha = 0.5f) else Color.LightGray,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack
        ),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    fontSize = 13.sp,
                    color = if (isDarkMode) KiwiRailDarkGray else Color.Gray
                )
            }
            innerTextField()
        }
    )
}

@Composable
fun DatePickerField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    if (value.isNotEmpty()) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            calendar.time = sdf.parse(value) ?: Date()
        } catch (e: Exception) { }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            onValueChange(sdf.format(selectedDate.time))
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
                1.dp,
                if (value.isNotEmpty()) KiwiRailOrange else if (isDarkMode) KiwiRailDarkGray.copy(alpha = 0.5f) else Color.LightGray,
                RoundedCornerShape(8.dp)
            )
            .clickable { datePickerDialog.show() }
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = value.ifEmpty { placeholder },
                fontSize = 13.sp,
                color = if (value.isEmpty())
                    if (isDarkMode) KiwiRailDarkGray else Color.Gray
                else
                    if (isDarkMode) KiwiRailWhite else KiwiRailBlack
            )
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Date",
                modifier = Modifier.size(16.dp),
                tint = KiwiRailOrange
            )
        }
    }
}

@Composable
fun CompactTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(KiwiRailOrange, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Product", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(2.8f))
        Text("Close", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Load", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Total", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Sales", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Pre", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("Waste", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text("End", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
    }
}

@Composable
fun CompactCategoryHeader(name: String, isDarkMode: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(KiwiRailBlack)
            .padding(10.dp)
    ) {
        Text(name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite)
    }
}

@Composable
fun DraggableStockRow(
    row: StockRow,
    clearTrigger: Int,
    isDarkMode: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var isDragging by remember { mutableStateOf(false) }
    var closeValue by remember(clearTrigger) { mutableStateOf(row.closingPrev) }
    var loadValue by remember(clearTrigger) { mutableStateOf(row.loading) }

    val calculatedTotal by remember {
        derivedStateOf {
            val close = closeValue.toIntOrNull() ?: 0
            val load = loadValue.toIntOrNull() ?: 0
            val total = close + load
            if (total > 0 || closeValue.isNotEmpty() || loadValue.isNotEmpty()) total.toString() else ""
        }
    }

    row.total = calculatedTotal

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDragging) KiwiRailOrange.copy(alpha = 0.15f)
                else if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite
            )
            .border(0.5.dp, if (isDarkMode) KiwiRailDarkGray.copy(alpha = 0.5f) else Color.LightGray)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { _, dragAmount ->
                        if (dragAmount.y < -20) onMoveUp()
                        else if (dragAmount.y > 20) onMoveDown()
                    }
                )
            }
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {
        Text(row.product, fontSize = 10.sp, modifier = Modifier.weight(2.8f), color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
        CompactNumericField(closeValue, Modifier.weight(1f), clearTrigger, isDarkMode) { closeValue = it; row.closingPrev = it }
        CompactNumericField(loadValue, Modifier.weight(1f), clearTrigger, isDarkMode) { loadValue = it; row.loading = it }
        ReadOnlyNumericField(calculatedTotal, Modifier.weight(1f))
        CompactNumericField(row.sales, Modifier.weight(1f), clearTrigger, isDarkMode) { row.sales = it }
        CompactNumericField(row.prePurchase, Modifier.weight(1f), clearTrigger, isDarkMode) { row.prePurchase = it }
        CompactNumericField(row.waste, Modifier.weight(1f), clearTrigger, isDarkMode) { row.waste = it }
        CompactNumericField(row.endDay, Modifier.weight(1f), clearTrigger, isDarkMode) { row.endDay = it }
    }
}

@Composable
fun CompactNumericField(
    value: String,
    modifier: Modifier = Modifier,
    clearTrigger: Int = 0,
    isDarkMode: Boolean = false,
    onChange: (String) -> Unit
) {
    var textValue by remember(value, clearTrigger) { mutableStateOf(value) }

    BasicTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                textValue = newValue
                onChange(newValue)
            }
        },
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)
            .border(
                1.dp,
                if (textValue.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.5f)
                else if (isDarkMode) KiwiRailDarkGray else Color.LightGray,
                RoundedCornerShape(6.dp)
            ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            fontWeight = if (textValue.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
        ),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                innerTextField()
            }
        }
    )
}

@Composable
fun ReadOnlyNumericField(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(38.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(KiwiRailOrange.copy(alpha = 0.15f))
            .border(1.dp, KiwiRailOrange.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = value, fontSize = 12.sp, textAlign = TextAlign.Center, color = KiwiRailOrange, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BeverageTableHeader(serviceNumber: String) {
    val closingDay = if (serviceNumber == "200") "201" else "200"
    val loadingLocation = if (serviceNumber == "200") "WLG" else "AKL"
    val wasteLocation = if (serviceNumber == "200") "AKL" else "WLG"

    Column {
        Row(
            modifier = Modifier.fillMaxWidth().background(KiwiRailOrange, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(vertical = 8.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PAR\nLEVEL", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, lineHeight = 9.sp, color = KiwiRailWhite)
            Text("CLOSING STOCK\nPREVIOUS DAY $closingDay", fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.6f), textAlign = TextAlign.Center, lineHeight = 8.sp, color = KiwiRailWhite)
            Text("LOADING\n@ $loadingLocation", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, lineHeight = 9.sp, color = KiwiRailWhite)
            Text("TOTAL", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, color = KiwiRailWhite)
            Text("SALES FOR\nTHE DAY", fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, lineHeight = 8.sp, color = KiwiRailWhite)
            Text("PRE\nPURCHASE", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, lineHeight = 9.sp, color = KiwiRailWhite)
            Text("WASTE\n@ $wasteLocation", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, lineHeight = 9.sp, color = KiwiRailWhite)
            Text("END OF DAY\nTOTAL", fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.6f), textAlign = TextAlign.Center, lineHeight = 8.sp, color = KiwiRailWhite)
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(KiwiRailOrange.copy(alpha = 0.9f)).padding(vertical = 6.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(0.8f))
            Text("CAFÉ", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, color = KiwiRailWhite)
            Text("AG", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, color = KiwiRailWhite)
            Spacer(Modifier.weight(0.9f))
            Spacer(Modifier.weight(0.8f))
            Spacer(Modifier.weight(0.9f))
            Spacer(Modifier.weight(0.8f))
            Spacer(Modifier.weight(0.8f))
            Text("CAFÉ", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, color = KiwiRailWhite)
            Text("AG", fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, color = KiwiRailWhite)
        }
    }
}

@Composable
fun BeverageCategoryHeader(name: String) {
    Box(modifier = Modifier.fillMaxWidth().background(KiwiRailBlack).padding(10.dp)) {
        Text(name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KiwiRailWhite)
    }
}

@Composable
fun BeverageStockRow(row: BeverageRow, clearTrigger: Int, isDarkMode: Boolean) {
    var closingCafe by remember(clearTrigger) { mutableStateOf(row.closingCafe) }
    var closingAG by remember(clearTrigger) { mutableStateOf(row.closingAG) }
    var loading by remember(clearTrigger) { mutableStateOf(row.loading) }

    val calculatedTotal by remember {
        derivedStateOf {
            val cafe = closingCafe.toIntOrNull() ?: 0
            val ag = closingAG.toIntOrNull() ?: 0
            val load = loading.toIntOrNull() ?: 0
            val total = cafe + ag + load
            if (total > 0) total.toString() else ""
        }
    }

    row.total = calculatedTotal

    // Check if below PAR
    val endTotal = (row.endDayCafe.toIntOrNull() ?: 0) + (row.endDayAG.toIntOrNull() ?: 0)
    val par = row.parLevel.toIntOrNull() ?: 0
    val isBelowPar = endTotal > 0 && par > 0 && endTotal < par

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDarkMode) KiwiRailDarkGray else KiwiRailWhite)
            .border(
                width = if (isBelowPar) 2.dp else 0.5.dp,
                color = if (isBelowPar) KiwiRailOrange else if (isDarkMode) KiwiRailDarkGray.copy(alpha = 0.5f) else Color.LightGray
            )
            .padding(vertical = 3.dp, horizontal = 4.dp)
    ) {
        Column(Modifier.weight(0.8f)) {
            Text(row.product, fontSize = 9.sp, lineHeight = 10.sp, color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.parLevel, fontSize = 8.sp, color = KiwiRailOrange, fontWeight = FontWeight.Bold)
                if (isBelowPar) {
                    Spacer(Modifier.width(4.dp))
                    Text("⚠", fontSize = 8.sp, color = KiwiRailOrange)
                }
            }
        }

        TinyNumericField(closingCafe, Modifier.weight(0.8f), clearTrigger, isDarkMode) { closingCafe = it; row.closingCafe = it }
        TinyNumericField(closingAG, Modifier.weight(0.8f), clearTrigger, isDarkMode) { closingAG = it; row.closingAG = it }
        TinyNumericField(loading, Modifier.weight(0.9f), clearTrigger, isDarkMode) { loading = it; row.loading = it }
        ReadOnlyTinyField(calculatedTotal, Modifier.weight(0.8f))
        TinyNumericField(row.sales, Modifier.weight(0.9f), clearTrigger, isDarkMode) { row.sales = it }
        TinyNumericField(row.prePurchase, Modifier.weight(0.8f), clearTrigger, isDarkMode) { row.prePurchase = it }
        TinyNumericField(row.waste, Modifier.weight(0.8f), clearTrigger, isDarkMode) { row.waste = it }
        TinyNumericField(row.endDayCafe, Modifier.weight(0.8f), clearTrigger, isDarkMode) { row.endDayCafe = it }
        TinyNumericField(row.endDayAG, Modifier.weight(0.8f), clearTrigger, isDarkMode) { row.endDayAG = it }
    }
}

@Composable
fun TinyNumericField(
    value: String,
    modifier: Modifier = Modifier,
    clearTrigger: Int = 0,
    isDarkMode: Boolean = false,
    onChange: (String) -> Unit
) {
    var textValue by remember(value, clearTrigger) { mutableStateOf(value) }

    BasicTextField(
        value = textValue,
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                textValue = newValue
                onChange(newValue)
            }
        },
        modifier = modifier
            .padding(horizontal = 1.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (isDarkMode) KiwiRailBlack else KiwiRailLightGray)
            .border(
                1.dp,
                if (textValue.isNotEmpty()) KiwiRailOrange.copy(alpha = 0.5f)
                else if (isDarkMode) KiwiRailDarkGray else Color.LightGray,
                RoundedCornerShape(4.dp)
            ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            color = if (isDarkMode) KiwiRailWhite else KiwiRailBlack,
            fontWeight = if (textValue.isNotEmpty()) FontWeight.Bold else FontWeight.Normal
        ),
        decorationBox = { innerTextField ->
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                innerTextField()
            }
        }
    )
}

@Composable
fun ReadOnlyTinyField(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(KiwiRailOrange.copy(alpha = 0.15f))
            .border(1.dp, KiwiRailOrange.copy(alpha = 0.5f), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = value, fontSize = 10.sp, textAlign = TextAlign.Center, color = KiwiRailOrange, fontWeight = FontWeight.Bold)
    }
}

fun getFoodCategories(): MutableList<CategorySection> {
    return mutableListOf(
        CategorySection("BREAKFAST", mutableListOf(
            StockRow(product = "Growers Breakfast"), StockRow(product = "Breakfast Croissant"),
            StockRow(product = "Big Breakfast"), StockRow(product = "Pancakes"),
            StockRow(product = "Chia Seeds"), StockRow(product = "Fruit Salads")
        )),
        CategorySection("SWEETS", mutableListOf(
            StockRow(product = "Brownie Slices"), StockRow(product = "Cookie Time Biscuits"),
            StockRow(product = "Cookie Time GF Biscuits"), StockRow(product = "Carrot Cake"),
            StockRow(product = "ANZAC Biscuits"), StockRow(product = "Blueberry Muffins"),
            StockRow(product = "Cheese Scones")
        )),
        CategorySection("SALADS", mutableListOf(
            StockRow(product = "Leafy Salad"), StockRow(product = "Smoked Chicken Pasta Salad")
        )),
        CategorySection("SANDWICHES AND WRAP", mutableListOf(
            StockRow(product = "BLT"), StockRow(product = "Chicken Wrap"),
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
            StockRow(product = "KAPITI BOYSENBERRY"), StockRow(product = "KAPITI PASSIONFRUIT"),
            StockRow(product = "KAPITI CHOCOLATE CUPS"), StockRow(product = "MEMPHIS BIK BIKKIE")
        )),
        CategorySection("CHEESEBOARD", mutableListOf(StockRow(product = "Cheeseboard")))
    )
}

fun getBeverageCategories(): MutableList<BeverageSection> {
    return mutableListOf(
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
}

fun <T> MutableList<T>.swap(i: Int, j: Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}