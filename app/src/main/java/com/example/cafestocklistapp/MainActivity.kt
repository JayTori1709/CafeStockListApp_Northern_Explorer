package com.example.cafestocklistapp

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

data class StockItem(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var cafeCount: String = "0",
    var beerCount: String = "0",
    var akfCount: String = "0"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CafeStockListAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    StockListScreen(
                        onExportPdf = { items -> exportToPdf(items) }
                    )
                }
            }
        }
    }

    private fun exportToPdf(items: List<StockItem>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Title
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Cafe Stock List Report", 50f, 50f, paint)

        // Date
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Generated: ${dateFormat.format(Date())}", 50f, 75f, paint)

        // Table headers
        var yPos = 120f
        paint.isFakeBoldText = true
        paint.textSize = 12f
        canvas.drawText("Item", 50f, yPos, paint)
        canvas.drawText("Cafe Fridge", 200f, yPos, paint)
        canvas.drawText("Beer Fridge", 320f, yPos, paint)
        canvas.drawText("AKF Van", 440f, yPos, paint)

        // Draw line
        yPos += 10f
        canvas.drawLine(50f, yPos, 540f, yPos, paint)
        yPos += 20f

        // Table content
        paint.isFakeBoldText = false
        items.forEach { item ->
            canvas.drawText(item.name, 50f, yPos, paint)
            canvas.drawText(item.cafeCount, 200f, yPos, paint)
            canvas.drawText(item.beerCount, 320f, yPos, paint)
            canvas.drawText(item.akfCount, 440f, yPos, paint)
            yPos += 25f
        }

        pdfDocument.finishPage(page)

        // Save PDF
        val dateString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "StockList_$dateString.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()

            // Create email intent
            sendEmailWithPdf(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendEmailWithPdf(file: File) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val subject = "Stock List Report - ${dateFormat.format(Date())}"

        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Please find attached the stock list report.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(emailIntent, "Send stock report via..."))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockListScreen(onExportPdf: (List<StockItem>) -> Unit) {
    var stockItems by remember {
        mutableStateOf(
            listOf(
                StockItem(name = "Milk"),
                StockItem(name = "Coffee Beans"),
                StockItem(name = "Sugar"),
                StockItem(name = "Beer - Lager"),
                StockItem(name = "Beer - IPA"),
                StockItem(name = "Soft Drinks"),
                StockItem(name = "Snacks"),
                StockItem(name = "Cups"),
            )
        )
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cafe Stock List") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Item")
                }
                FloatingActionButton(
                    onClick = { onExportPdf(stockItems) },
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(Icons.Default.Email, "Export & Email")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        stockItems = stockItems.map { it.copy(
                            cafeCount = "0",
                            beerCount = "0",
                            akfCount = "0"
                        ) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Refresh All")
                }

                Button(
                    onClick = {
                        stockItems = stockItems.map { it.copy(
                            cafeCount = "0",
                            beerCount = "0",
                            akfCount = "0"
                        ) }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear All")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp)
            ) {
                Text("Item", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold)
                Text("Cafe", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("Beer", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text("AKF", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(48.dp))
            }

            Divider()

            // Stock Items
            stockItems.forEachIndexed { index, item ->
                StockItemRow(
                    item = item,
                    onUpdate = { updated ->
                        stockItems = stockItems.toMutableList().apply {
                            this[index] = updated
                        }
                    },
                    onDelete = {
                        stockItems = stockItems.filterIndexed { i, _ -> i != index }
                    },
                    onInsertAbove = {
                        stockItems = stockItems.toMutableList().apply {
                            add(index, StockItem(name = "New Item"))
                        }
                    },
                    onInsertBelow = {
                        stockItems = stockItems.toMutableList().apply {
                            add(index + 1, StockItem(name = "New Item"))
                        }
                    },
                    onEdit = { editingItemIndex = index }
                )
                Divider()
            }
        }
    }

    // Add Item Dialog
    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name ->
                stockItems = stockItems + StockItem(name = name)
                showAddDialog = false
            }
        )
    }

    // Edit Item Dialog
    editingItemIndex?.let { index ->
        EditItemDialog(
            currentName = stockItems[index].name,
            onDismiss = { editingItemIndex = null },
            onSave = { newName ->
                stockItems = stockItems.toMutableList().apply {
                    this[index] = this[index].copy(name = newName)
                }
                editingItemIndex = null
            }
        )
    }
}

@Composable
fun StockItemRow(
    item: StockItem,
    onUpdate: (StockItem) -> Unit,
    onDelete: () -> Unit,
    onInsertAbove: () -> Unit,
    onInsertBelow: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            item.name,
            modifier = Modifier.weight(1.5f),
            fontSize = 14.sp
        )

        OutlinedTextField(
            value = item.cafeCount,
            onValueChange = { onUpdate(item.copy(cafeCount = it)) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        OutlinedTextField(
            value = item.beerCount,
            onValueChange = { onUpdate(item.copy(beerCount = it)) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        OutlinedTextField(
            value = item.akfCount,
            onValueChange = { onUpdate(item.copy(akfCount = it)) },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center)
        )

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Options")
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Name") },
                    onClick = {
                        onEdit()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text("Insert Above") },
                    onClick = {
                        onInsertAbove()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.KeyboardArrowUp, null) }
                )
                DropdownMenuItem(
                    text = { Text("Insert Below") },
                    onClick = {
                        onInsertBelow()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.KeyboardArrowDown, null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}

@Composable
fun AddItemDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var itemName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item") },
        text = {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onAdd(itemName) },
                enabled = itemName.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditItemDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var itemName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Item Name") },
        text = {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("Item Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onSave(itemName) },
                enabled = itemName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}