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
                    ) { pageName, categories, osm, tm, crew, date ->
                        exportToPdf(pageName, categories, osm, tm, crew, date)
                    }
                }
            }
        }
    }

    /* -------------------- PDF EXPORT -------------------- */

    private fun exportToPdf(
        pageName: String,
        categories: List<CategorySection>,
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
        canvas.drawText("WLG - AKL (200) CLOSING STOCK", 40f, 50f, paint)

        // Header info
        paint.textSize = 14f
        paint.isFakeBoldText = false
        canvas.drawText("OSM: $osm / TM: $tm / CREW: $crew / DATE: $date", 40f, 80f, paint)
        canvas.drawText("PREVIOUS DAY 201", 40f, 100f, paint)

        var y = 140f

        categories.forEach { category ->
            paint.textSize = 16f
            paint.isFakeBoldText = true
            paint.color = android.graphics.Color.BLACK
            canvas.drawRect(40f, y - 15f, 1160f, y + 5f, paint.apply {
                style = Paint.Style.FILL
                color = android.graphics.Color.LTGRAY
            })
            paint.style = Paint.Style.FILL
            paint.color = android.graphics.Color.BLACK
            canvas.drawText(category.name, 50f, y, paint)
            y += 25f

            // Column headers
            paint.textSize = 10f
            paint.isFakeBoldText = false
            canvas.drawText("Product", 50f, y, paint)
            canvas.drawText("Closing", 280f, y, paint)
            canvas.drawText("Loading", 350f, y, paint)
            canvas.drawText("Total", 420f, y, paint)
            canvas.drawText("Sales", 480f, y, paint)
            canvas.drawText("Pre-Pur", 540f, y, paint)
            canvas.drawText("Waste", 610f, y, paint)
            canvas.drawText("End Day", 670f, y, paint)

            y += 18f

            category.rows.forEach { row ->
                if (y > 1700f) return@forEach

                canvas.drawText(row.product, 50f, y, paint)
                canvas.drawText(row.closingPrev, 290f, y, paint)
                canvas.drawText(row.loading, 360f, y, paint)
                canvas.drawText(row.total, 430f, y, paint)
                canvas.drawText(row.sales, 490f, y, paint)
                canvas.drawText(row.prePurchase, 550f, y, paint)
                canvas.drawText(row.waste, 620f, y, paint)
                canvas.drawText(row.endDay, 680f, y, paint)

                y += 16f
            }
            y += 15f
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
    onExportPdf: (String, List<CategorySection>, String, String, String, String) -> Unit
) {

    val sheet200 = remember { getPage1Categories() }

    var osm by remember { mutableStateOf("") }
    var tm by remember { mutableStateOf("") }
    var crew by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    // Trigger to force recomposition
    var clearTrigger by remember { mutableStateOf(0) }

    StockSheet(
        pageName = "WLG-AKL-200",
        categories = sheet200,
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
        onExportPdf = onExportPdf
    )
}

@Composable
fun StockSheet(
    pageName: String,
    categories: MutableList<CategorySection>,
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
    onExportPdf: (String, List<CategorySection>, String, String, String, String) -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {

        // Top bar with title and dark mode toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "WLG - AKL (200)",
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
            "CLOSING STOCK - PREVIOUS DAY 201",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Info fields with placeholders
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
                    categories.forEach { cat ->
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
                    onClearAll() // Trigger recomposition
                },
                modifier = Modifier.weight(1f)
            ) { Text("Clear All", fontSize = 12.sp) }

            Button(
                onClick = {
                    onExportPdf(pageName, categories, osm, tm, crew, date)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Export PDF", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Compact Column Headers
        CompactTableHeader()

        // Categories and rows
        categories.forEach { category ->
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

    // Parse existing date if present
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

    // Auto-calculate total when Close or Load changes
    LaunchedEffect(row.closingPrev, row.loading) {
        val close = row.closingPrev.toIntOrNull() ?: 0
        val load = row.loading.toIntOrNull() ?: 0
        val total = close + load
        row.total = if (total > 0 || row.closingPrev.isNotEmpty() || row.loading.isNotEmpty()) {
            total.toString()
        } else {
            ""
        }
    }

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
                        // Detect drag direction
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

        // Product name - more space
        Text(
            row.product,
            fontSize = 10.sp,
            modifier = Modifier.weight(2.8f)
        )

        // Numeric fields
        CompactNumericField(row.closingPrev, Modifier.weight(1f), clearTrigger) { row.closingPrev = it }
        CompactNumericField(row.loading, Modifier.weight(1f), clearTrigger) { row.loading = it }

        // Total field - READ ONLY (auto-calculated)
        ReadOnlyNumericField(row.total, Modifier.weight(1f))

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
            // Allow empty or numeric values
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

/* ================= DATA INITIALIZATION ================= */

fun getPage1Categories(): MutableList<CategorySection> {
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

fun <T> MutableList<T>.swap(i: Int, j: Int) {
    val temp = this[i]
    this[i] = this[j]
    this[j] = temp
}