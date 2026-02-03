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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
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

/* -------------------- DATA MODELS -------------------- */

// Food stock data model
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

// Beverage stock data model (with CAFÉ and AG columns)
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
                Surface(color = MaterialTheme.colorScheme.background) {
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

    /* -------------------- PDF EXPORT -------------------- */

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

        // Title
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("$pageName CLOSING STOCK", 40f, 50f, paint)

        // Header info
        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("OSM: $osm / TM: $tm / CREW: $crew / DATE: $date", 40f, 80f, paint)

        var y = 120f

        // Food categories
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
                    60f,
                    y,
                    paint
                )
                y += 16f
            }
            y += 15f
        }

        // Beverage categories
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("RETAIL STOCK", 50f, y, paint)
        y += 25f

        beverages.forEach { section ->
            paint.textSize = 14f
            paint.isFakeBoldText = true
            canvas.drawText(section.name, 50f, y, paint)
            y += 18f

            paint.textSize = 9f
            paint.isFakeBoldText = false
            section.rows.forEach { row ->
                if (y > 1700f) return@forEach
                canvas.drawText(
                    "${row.product} (${row.parLevel}): Café:${row.closingCafe} AG:${row.closingAG}",
                    60f,
                    y,
                    paint
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
            var date by remember { mutableStateOf("") }
            var clearTrigger by remember { mutableStateOf(0) }

            StockSheet(
                pageName = "WLG-AKL-200",
                displayTitle = "WLG → AKL (200)",
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
            var date by remember { mutableStateOf("") }
            var clearTrigger by remember { mutableStateOf(0) }

            StockSheet(
                pageName = "AKL-WLG-201",
                displayTitle = "AKL → WLG (201)",
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode"
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Text(
            "Cafe Stock Sheet",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )

        Spacer(Modifier.height(16.dp))

        Text(
            "What trip are we doing today?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(48.dp))

        TripButton(
            title = "WLG → AKL",
            subtitle = "Service 200",
            onClick = { onTripSelected("200") }
        )

        Spacer(Modifier.height(24.dp))

        TripButton(
            title = "AKL → WLG",
            subtitle = "Service 201",
            onClick = { onTripSelected("201") }
        )
    }
}

@Composable
fun TripButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockSheet(
    pageName: String,
    displayTitle: String,
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

    Column(
        Modifier
            .fillMaxSize()
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
                    contentDescription = "Back to Home"
                )
            }

            Text(
                displayTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            IconButton(onClick = onToggleDarkMode) {
                Icon(
                    imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Dark Mode"
                )
            }
        }

        Text(
            "CLOSING STOCK - PREVIOUS DAY",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Info fields
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaceholderTextField("OSM", osm, onOsmChange, Modifier.weight(1f))
            PlaceholderTextField("TM", tm, onTmChange, Modifier.weight(1f))
            PlaceholderTextField("CREW", crew, onCrewChange, Modifier.weight(1f))
            DatePickerField("DATE", date, onDateChange, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
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
                modifier = Modifier.weight(1f)
            ) { Text("Clear All", fontSize = 12.sp) }

            Button(
                onClick = {
                    onExportPdf(pageName, foodCategories, beverageCategories, osm, tm, crew, date)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Export PDF", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // FOOD TABLE
        Text(
            "FOOD STOCK",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

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
                        if (index < category.rows.lastIndex) category.rows.swap(
                            index,
                            index + 1
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        HorizontalDivider(thickness = 2.dp, color = MaterialTheme.colorScheme.primary)

        Spacer(Modifier.height(16.dp))

        // BEVERAGE TABLE (SEPARATE WITH CAFÉ/AG)
        Text(
            "RETAIL STOCK (CAFÉ & AG)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        BeverageTableHeader()

        beverageCategories.forEach { section ->
            BeverageCategoryHeader(section.name)
            section.rows.forEach { row ->
                BeverageStockRow(row = row, clearTrigger = clearTrigger)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Footer sections
        Text(
            "Notes:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            placeholder = { Text("Enter notes...") }
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Requirements:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        OutlinedTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp),
            placeholder = { Text("Enter requirements...") }
        )
    }
}

/* ================= FOOD TABLE COMPONENTS ================= */

@Composable
fun PlaceholderTextField(
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        ),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    if (value.isNotEmpty()) {
        try {
            val sdf = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
            calendar.time = sdf.parse(value) ?: Date()
        } catch (e: Exception) {
            // Use current date if parsing fails
        }
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
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape = MaterialTheme.shapes.small)
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
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Date",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CompactTableHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Product",
            fontSize = 9.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(2.8f)
        )
        Text(
            "Close",
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            "Load",
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            "Total",
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            "Sales",
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            "Pre",
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            "Waste",
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        Text(
            "End",
            fontSize = 8.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun CompactCategoryHeader(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp)
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun DraggableStockRow(
    row: StockRow,
    clearTrigger: Int,
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
            if (total > 0 || closeValue.isNotEmpty() || loadValue.isNotEmpty()) {
                total.toString()
            } else {
                ""
            }
        }
    }

    row.total = calculatedTotal

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDragging) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        if (dragAmount.y < -20) {
                            onMoveUp()
                        } else if (dragAmount.y > 20) {
                            onMoveDown()
                        }
                    }
                )
            }
            .padding(vertical = 4.dp, horizontal = 6.dp)
    ) {

        Text(
            row.product,
            fontSize = 10.sp,
            modifier = Modifier.weight(2.8f)
        )

        CompactNumericField(closeValue, Modifier.weight(1f), clearTrigger) {
            closeValue = it
            row.closingPrev = it
        }
        CompactNumericField(loadValue, Modifier.weight(1f), clearTrigger) {
            loadValue = it
            row.loading = it
        }

        ReadOnlyNumericField(calculatedTotal, Modifier.weight(1f))

        CompactNumericField(row.sales, Modifier.weight(1f), clearTrigger) { row.sales = it }
        CompactNumericField(row.prePurchase, Modifier.weight(1f), clearTrigger) { row.prePurchase = it }
        CompactNumericField(row.waste, Modifier.weight(1f), clearTrigger) { row.waste = it }
        CompactNumericField(row.endDay, Modifier.weight(1f), clearTrigger) { row.endDay = it }
    }
}

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
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                textValue = newValue
                onChange(newValue)
            }
        },
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(38.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
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
fun ReadOnlyNumericField(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .height(38.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outline),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

/* ================= BEVERAGE TABLE COMPONENTS ================= */

@Composable
fun BeverageTableHeader() {
    Column {
        // First row - main headers
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(vertical = 6.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PAR\nLEVEL", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, lineHeight = 8.sp)
            Text("CLOSING STOCK\nPREVIOUS DAY 201", fontSize = 6.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(1.6f), textAlign = TextAlign.Center, lineHeight = 7.sp)
            Text("LOADING\n@ WLG", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, lineHeight = 8.sp)
            Text("TOTAL", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
            Text("SALES FOR\nTHE DAY", fontSize = 6.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.9f), textAlign = TextAlign.Center, lineHeight = 7.sp)
            Text("PRE\nPURCHASE", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, lineHeight = 8.sp)
            Text("WASTE\n@ AKL", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center, lineHeight = 8.sp)
            Text("END OF DAY\nTOTAL", fontSize = 6.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(1.6f), textAlign = TextAlign.Center, lineHeight = 7.sp)
        }

        // Second row - CAFÉ / AG labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.outline)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.weight(0.8f)) // PAR LEVEL
            Text("CAFÉ", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
            Text("AG", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
            Spacer(Modifier.weight(0.9f)) // LOADING
            Spacer(Modifier.weight(0.8f)) // TOTAL
            Spacer(Modifier.weight(0.9f)) // SALES
            Spacer(Modifier.weight(0.8f)) // PRE PURCHASE
            Spacer(Modifier.weight(0.8f)) // WASTE
            Text("CAFÉ", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
            Text("AG", fontSize = 7.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BeverageCategoryHeader(name: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .border(1.dp, MaterialTheme.colorScheme.outline)
            .padding(8.dp)
    ) {
        Text(
            name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            fontSize = 11.sp,
            color = Color.White
        )
    }
}

@Composable
fun BeverageStockRow(
    row: BeverageRow,
    clearTrigger: Int
) {
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline)
            .padding(vertical = 3.dp, horizontal = 4.dp)
    ) {
        // Product name and PAR
        Column(Modifier.weight(0.8f)) {
            Text(row.product, fontSize = 9.sp, lineHeight = 10.sp)
            Text(row.parLevel, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Closing - CAFÉ
        TinyNumericField(closingCafe, Modifier.weight(0.8f), clearTrigger) {
            closingCafe = it
            row.closingCafe = it
        }
        // Closing - AG
        TinyNumericField(closingAG, Modifier.weight(0.8f), clearTrigger) {
            closingAG = it
            row.closingAG = it
        }

        // Loading
        TinyNumericField(loading, Modifier.weight(0.9f), clearTrigger) {
            loading = it
            row.loading = it
        }

        // Total (calculated)
        ReadOnlyTinyField(calculatedTotal, Modifier.weight(0.8f))

        // Sales
        TinyNumericField(row.sales, Modifier.weight(0.9f), clearTrigger) { row.sales = it }

        // Pre Purchase
        TinyNumericField(row.prePurchase, Modifier.weight(0.8f), clearTrigger) { row.prePurchase = it }

        // Waste
        TinyNumericField(row.waste, Modifier.weight(0.8f), clearTrigger) { row.waste = it }

        // End Day - CAFÉ
        TinyNumericField(row.endDayCafe, Modifier.weight(0.8f), clearTrigger) { row.endDayCafe = it }
        // End Day - AG
        TinyNumericField(row.endDayAG, Modifier.weight(0.8f), clearTrigger) { row.endDayAG = it }
    }
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
        onValueChange = { newValue ->
            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                textValue = newValue
                onChange(newValue)
            }
        },
        modifier = modifier
            .padding(horizontal = 1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surface)
            .border(0.5.dp, MaterialTheme.colorScheme.outline),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
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
fun ReadOnlyTinyField(value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 1.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(0.5.dp, MaterialTheme.colorScheme.outline),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 9.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
        )
    }
}

/* ================= DATA INITIALIZATION ================= */

fun getFoodCategories(): MutableList<CategorySection> {
    return mutableListOf(
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