package com.example.cafestocklistapp

import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    var parLevel: String = "",
    var closingStock: String = "0",
    var loadingAtAkl: String = "0",
    var total: String = "0"
)

data class CategorySection(
    val name: String,
    val items: MutableList<StockItem>
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
                    MainScreen(
                        onExportPdf = { page, categories -> exportToPdf(page, categories) }
                    )
                }
            }
        }
    }

    private fun exportToPdf(pageName: String, categories: List<CategorySection>) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Title
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("$pageName - NStock List Report", 50f, 40f, paint)

        // Date
        paint.textSize = 10f
        paint.isFakeBoldText = false
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Generated: ${dateFormat.format(Date())}", 50f, 60f, paint)

        var yPos = 90f

        categories.forEach { category ->
            // Category header
            if (yPos > 750f) return@forEach // Prevent overflow

            paint.textSize = 12f
            paint.isFakeBoldText = true
            canvas.drawText(category.name, 50f, yPos, paint)
            yPos += 20f

            paint.textSize = 9f
            paint.isFakeBoldText = false

            category.items.forEach { item ->
                if (yPos > 800f) return@forEach

                canvas.drawText(item.name, 60f, yPos, paint)
                canvas.drawText(item.parLevel, 250f, yPos, paint)
                canvas.drawText(item.closingStock, 320f, yPos, paint)
                canvas.drawText(item.loadingAtAkl, 390f, yPos, paint)
                canvas.drawText(item.total, 460f, yPos, paint)
                yPos += 18f
            }
            yPos += 10f
        }

        pdfDocument.finishPage(page)

        // Save PDF
        val dateString = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${pageName}_StockList_$dateString.pdf"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            sendEmailWithPdf(file, pageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendEmailWithPdf(file: File, pageName: String) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val subject = "$pageName Stock List Report - ${dateFormat.format(Date())}"

        val uri = FileProvider.getUriForFile(
            this,
            "${applicationContext.packageName}.provider",
            file
        )

        val emailIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, "Please find attached the $pageName stock list report.")
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(emailIntent, "Send stock report via..."))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onExportPdf: (String, List<CategorySection>) -> Unit) {
    var selectedPage by remember { mutableStateOf(0) }
    val pages = listOf("Cafe Stock", "AKL - WLG (201)")

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Cafe Stock List") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                TabRow(selectedTabIndex = selectedPage) {
                    pages.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedPage == index,
                            onClick = { selectedPage = index },
                            text = { Text(title) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedPage) {
                0 -> CafeStockPage(onExportPdf = { categories ->
                    onExportPdf("Cafe Stock", categories)
                })
                1 -> AklWlgPage(onExportPdf = { categories ->
                    onExportPdf("AKL-WLG", categories)
                })
            }
        }
    }
}

@Composable
fun CafeStockPage(onExportPdf: (List<CategorySection>) -> Unit) {
    var categories by remember {
        mutableStateOf(
            listOf(
                CategorySection("SNACKS", mutableListOf(
                    StockItem(name = "Whittakers White Choc", parLevel = "48"),
                    StockItem(name = "Whittakers Brown Choc", parLevel = "48"),
                    StockItem(name = "ETA Nuts", parLevel = "24")
                )),
                CategorySection("PROPER CHIPS", mutableListOf(
                    StockItem(name = "Sea Salt", parLevel = "18"),
                    StockItem(name = "Cider Vinegar", parLevel = "18"),
                    StockItem(name = "Garden Medly", parLevel = "18")
                )),
                CategorySection("BEERS", mutableListOf(
                    StockItem(name = "Monteiths Black", parLevel = "12"),
                    StockItem(name = "Monteiths Original Ale", parLevel = "12"),
                    StockItem(name = "Parrot Dog Lager", parLevel = "12"),
                    StockItem(name = "Tuatara Pilsner", parLevel = "12"),
                    StockItem(name = "Garage Project TINY", parLevel = "12"),
                    StockItem(name = "Steinlager Pure", parLevel = "24")
                )),
                CategorySection("PRE MIXES", mutableListOf(
                    StockItem(name = "JD Whiskey & Cola", parLevel = "12"),
                    StockItem(name = "Gordon's G & T", parLevel = "12"),
                    StockItem(name = "Coruba R & C", parLevel = "12")
                )),
                CategorySection("WINES", mutableListOf(
                    StockItem(name = "Merlot 187 ml", parLevel = "24"),
                    StockItem(name = "Chardonnay 187 ml", parLevel = "24"),
                    StockItem(name = "Sav Blanc 187 ml", parLevel = "24"),
                    StockItem(name = "Lindauer 200 ml", parLevel = "24")
                )),
                CategorySection("SOFT DRINKS", mutableListOf(
                    StockItem(name = "H2go Water 750ml", parLevel = "12"),
                    StockItem(name = "NZ SP Water 500ml", parLevel = "18"),
                    StockItem(name = "Bundaberg Lemon Lime", parLevel = "10"),
                    StockItem(name = "Bundaberg Ginger Beer", parLevel = "10"),
                    StockItem(name = "7 UP", parLevel = "10"),
                    StockItem(name = "Pepsi", parLevel = "10"),
                    StockItem(name = "Pepsi Max", parLevel = "10"),
                    StockItem(name = "McCoy Orange Juice", parLevel = "15")
                )),
                CategorySection("750 ML WINE", mutableListOf(
                    StockItem(name = "O/B Sparkling 750ml", parLevel = "4"),
                    StockItem(name = "O/B Pinot Gris 750ml", parLevel = "4"),
                    StockItem(name = "O/B Sav. Blanc 750ml", parLevel = "4"),
                    StockItem(name = "O/B Pinot Noir 750ml", parLevel = "4")
                ))
            )
        )
    }

    StockPageContent(
        categories = categories,
        onCategoriesUpdate = { categories = it },
        onExportPdf = onExportPdf
    )
}

@Composable
fun AklWlgPage(onExportPdf: (List<CategorySection>) -> Unit) {
    var categories by remember {
        mutableStateOf(
            listOf(
                CategorySection("BREAKFAST", mutableListOf(
                    StockItem(name = "Chia Seeds"),
                    StockItem(name = "Fruit Salads"),
                    StockItem(name = "Growers Breakfast"),
                    StockItem(name = "Pancakes"),
                    StockItem(name = "Breakfast Croissant")
                )),
                CategorySection("SWEETS", mutableListOf(
                    StockItem(name = "Blueberry Muffins"),
                    StockItem(name = "Cheese Scones"),
                    StockItem(name = "Carrot Cake"),
                    StockItem(name = "ANZAC Biscuit"),
                    StockItem(name = "White Chocolate Biscuits")
                )),
                CategorySection("SALADS", mutableListOf(
                    StockItem(name = "Leafy Salad"),
                    StockItem(name = "Pasta Salad")
                )),
                CategorySection("SANDWICHES", mutableListOf(
                    StockItem(name = "Ham and Cheese Toastie"),
                    StockItem(name = "Chicken Sandwich"),
                    StockItem(name = "Beef Sandwich"),
                    StockItem(name = "Ham Sandwich"),
                    StockItem(name = "Vegetarian Sandwich")
                )),
                CategorySection("HOT MEALS", mutableListOf(
                    StockItem(name = "Beef Cheek"),
                    StockItem(name = "Roast Chicken"),
                    StockItem(name = "Lamb Shank"),
                    StockItem(name = "Mac & Cheese"),
                    StockItem(name = "Lasagne")
                )),
                CategorySection("PIES", mutableListOf(
                    StockItem(name = "Steak and Cheese"),
                    StockItem(name = "Vegetarian")
                )),
                CategorySection("SWEET AND CHEESY", mutableListOf(
                    StockItem(name = "Cheeseboard")
                )),
                CategorySection("ICE CREAM", mutableListOf(
                    StockItem(name = "Kapiti Boysenberry"),
                    StockItem(name = "Kapiti Passionfruit"),
                    StockItem(name = "Kapiti Chocolate Cups"),
                    StockItem(name = "Memphis Blk Bikkie")
                ))
            )
        )
    }

    StockPageContent(
        categories = categories,
        onCategoriesUpdate = { categories = it },
        onExportPdf = onExportPdf
    )
}

@Composable
fun StockPageContent(
    categories: List<CategorySection>,
    onCategoriesUpdate: (List<CategorySection>) -> Unit,
    onExportPdf: (List<CategorySection>) -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
                    val cleared = categories.map { category ->
                        category.copy(items = category.items.map {
                            it.copy(closingStock = "0", loadingAtAkl = "0", total = "0")
                        }.toMutableList())
                    }
                    onCategoriesUpdate(cleared)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                ),
                modifier = Modifier.weight(1f).padding(end = 4.dp)
            ) {
                Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Clear All")
            }

            Button(
                onClick = { showAddCategoryDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add Category")
            }

            Button(
                onClick = { onExportPdf(categories) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            ) {
                Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Email")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories and Items
        categories.forEachIndexed { categoryIndex, category ->
            // Category Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    category.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showAddItemDialog = categoryIndex }) {
                    Icon(Icons.Default.Add, "Add Item")
                }
            }

            // Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp)
            ) {
                Text("Item", modifier = Modifier.weight(2f), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Par", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text("Close", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text("Load", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                Text("Total", modifier = Modifier.weight(0.8f), fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(40.dp))
            }

            // Items
            category.items.forEachIndexed { itemIndex, item ->
                StockItemRow(
                    item = item,
                    onUpdate = { updated ->
                        val newCategories = categories.toMutableList()
                        newCategories[categoryIndex].items[itemIndex] = updated
                        onCategoriesUpdate(newCategories)
                    },
                    onDelete = {
                        val newCategories = categories.toMutableList()
                        newCategories[categoryIndex].items.removeAt(itemIndex)
                        onCategoriesUpdate(newCategories)
                    }
                )
                Divider()
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddCategoryDialog(
            onDismiss = { showAddCategoryDialog = false },
            onAdd = { name ->
                onCategoriesUpdate(categories + CategorySection(name, mutableListOf()))
                showAddCategoryDialog = false
            }
        )
    }

    // Add Item Dialog
    showAddItemDialog?.let { categoryIndex ->
        AddItemDialog(
            onDismiss = { showAddItemDialog = null },
            onAdd = { name, parLevel ->
                val newCategories = categories.toMutableList()
                newCategories[categoryIndex].items.add(StockItem(name = name, parLevel = parLevel))
                onCategoriesUpdate(newCategories)
                showAddItemDialog = null
            }
        )
    }
}

@Composable
fun StockItemRow(
    item: StockItem,
    onUpdate: (StockItem) -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            item.name,
            modifier = Modifier.weight(2f),
            fontSize = 13.sp
        )

        Text(
            item.parLevel,
            modifier = Modifier.weight(0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        OutlinedTextField(
            value = item.closingStock,
            onValueChange = {
                val updated = item.copy(closingStock = it)
                calculateTotal(updated)
                onUpdate(updated)
            },
            modifier = Modifier
                .weight(0.8f)
                .padding(horizontal = 2.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 12.sp)
        )

        OutlinedTextField(
            value = item.loadingAtAkl,
            onValueChange = {
                val updated = item.copy(loadingAtAkl = it)
                calculateTotal(updated)
                onUpdate(updated)
            },
            modifier = Modifier
                .weight(0.8f)
                .padding(horizontal = 2.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 12.sp)
        )

        Text(
            item.total,
            modifier = Modifier.weight(0.8f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, "Options", modifier = Modifier.size(20.dp))
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
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

fun calculateTotal(item: StockItem) {
    val closing = item.closingStock.toIntOrNull() ?: 0
    val loading = item.loadingAtAkl.toIntOrNull() ?: 0
    item.total = (closing + loading).toString()
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var categoryName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Category") },
        text = {
            OutlinedTextField(
                value = categoryName,
                onValueChange = { categoryName = it },
                label = { Text("Category Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { if (categoryName.isNotBlank()) onAdd(categoryName.uppercase()) },
                enabled = categoryName.isNotBlank()
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
fun AddItemDialog(onDismiss: () -> Unit, onAdd: (String, String) -> Unit) {
    var itemName by remember { mutableStateOf("") }
    var parLevel by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Item") },
        text = {
            Column {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Item Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = parLevel,
                    onValueChange = { parLevel = it },
                    label = { Text("Par Level") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (itemName.isNotBlank()) onAdd(itemName, parLevel) },
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