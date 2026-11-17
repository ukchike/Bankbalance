package com.bankscope

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.bankscope.data.BankAccountDao
import com.bankscope.data.BankAccountEntity
import com.bankscope.data.BudgetEntity
import com.bankscope.data.TransactionCategory
import com.bankscope.data.TransactionCsvExporter
import com.bankscope.data.TransactionEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun TransactionHistoryScreen(dao: BankAccountDao, accounts: List<BankAccountEntity>, modifier: Modifier = Modifier) {
    val transactions by dao.observeTransactions().collectAsState(initial = emptyList())
    val monthKey = remember { SimpleDateFormat("yyyy-MM", Locale.US).format(Date()) }
    val budgets by dao.observeBudgets(monthKey).collectAsState(initial = emptyList())
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var assigning by remember { mutableStateOf<TransactionEntity?>(null) }
    var categorising by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingBudget by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null) scope.launch { runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(TransactionCsvExporter.toCsv(transactions)) } } }
    }
    val filtered = transactions.filter { row ->
        val matchesText = listOf(row.institutionName, row.accountHint.orEmpty(), row.description.orEmpty(), row.currencyCode, row.sourceType, row.category).any { it.contains(search, true) }
        val matchesStatus = when (filter) { "REVIEW" -> row.reviewStatus != "MATCHED"; "MATCHED" -> row.reviewStatus == "MATCHED"; else -> true }
        matchesText && matchesStatus
    }
    val monthTransactions = transactions.filter { row -> row.eventAt != null && SimpleDateFormat("yyyy-MM", Locale.US).format(Date(row.eventAt)) == monthKey && row.reviewStatus == "MATCHED" }
    val spendByKey = monthTransactions.filter { it.direction == "DEBIT" && !it.isInternalTransfer && it.amount != null }
        .groupBy { it.category to it.currencyCode }
        .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { sum, row -> sum + BigDecimal(row.amount!!) } }
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { exportLauncher.launch("bankbalance-transactions.csv") }, enabled = transactions.isNotEmpty()) { Text("Export CSV") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Budgets · $monthKey", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { editingBudget = true }) { Text("Manage budgets") }
        }
        if (budgets.isEmpty()) Text("No budgets set for this month. Add a category budget to track spending.")
        budgets.forEach { budget ->
            val spent = spendByKey[budget.category to budget.currencyCode] ?: BigDecimal.ZERO
            val limit = runCatching { BigDecimal(budget.limitAmount) }.getOrDefault(BigDecimal.ZERO)
            val ratio = if (limit > BigDecimal.ZERO) (spent.toDouble() / limit.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
            Column(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${budget.category.replace("_", " ")} · ${budget.currencyCode}", style = MaterialTheme.typography.bodySmall)
                    Text("${money(spent)} / ${money(limit)}", style = MaterialTheme.typography.bodySmall)
                }
                LinearProgressIndicator(progress = ratio, modifier = Modifier.fillMaxWidth())
                Text(if (spent > limit) "Over budget by ${money(spent - limit)}" else "Remaining ${money(limit - spent)}", style = MaterialTheme.typography.labelSmall)
            }
        }
        HorizontalDivider()
        Text("Monthly category spending", style = MaterialTheme.typography.titleMedium)
        if (spendByKey.isEmpty()) Text("Matched debit transactions with event dates will appear here. Internal transfers are excluded.")
        else spendByKey.toList().sortedByDescending { it.second }.take(8).forEach { (key, amount) -> Text("${key.first.replace("_", " ")} · ${key.second} ${money(amount)}", style = MaterialTheme.typography.bodySmall) }
        HorizontalDivider()
        OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search institution, description, category") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("ALL", "MATCHED", "REVIEW").forEach { value -> FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) }) } }
        Text("${filtered.size} transaction(s)", style = MaterialTheme.typography.labelMedium)
        if (filtered.isEmpty()) Text("No transactions match this filter.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered, key = { it.transactionId }) { row ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(row.institutionName, style = MaterialTheme.typography.titleSmall); Text("${row.direction ?: "UNCLASSIFIED"} ${row.currencyCode} ${row.amount ?: "—"}") }
                        Text(row.description ?: "No transaction description")
                        Text("Category: ${row.category.replace("_", " ")}${if (row.isInternalTransfer) " · Internal transfer" else ""}", style = MaterialTheme.typography.bodySmall)
                        Text("Account: ${row.accountHint?.takeLast(4)?.let { "••$it" } ?: "Not identified"} · ${row.sourceType}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${row.reviewStatus} · Confidence: ${(row.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                        Text("Event: ${row.eventAt?.let { java.text.DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Not supplied"}", style = MaterialTheme.typography.bodySmall)
                        Row { TextButton(onClick = { categorising = row }) { Text("Edit category") }; if (row.reviewStatus != "MATCHED") TextButton(onClick = { assigning = row }) { Text("Assign account") } }
                    }
                }
            }
        }
    }
    assigning?.let { row -> AlertDialog(onDismissRequest = { assigning = null }, title = { Text("Assign transaction") }, text = { Column { Text("Choose the correct account with matching institution and currency."); accounts.filter { it.institutionName.equals(row.institutionName, true) && it.currencyCode.equals(row.currencyCode, true) }.forEach { account -> TextButton(onClick = { scope.launch { dao.assignTransaction(row.transactionId, account.accountId) }; assigning = null }) { Text("${account.institutionName} ••${account.accountIdentifier.takeLast(4)} (${account.currencyCode})") } } } }, confirmButton = { TextButton(onClick = { assigning = null }) { Text("Close") } }) }
    categorising?.let { row -> CategoryEditorDialog(row.category, row.isInternalTransfer, onDismiss = { categorising = null }, onSave = { category, internal -> scope.launch { dao.updateTransactionCategory(row.transactionId, category.name, internal) }; categorising = null }) }
    if (editingBudget) BudgetEditorDialog(monthKey, onDismiss = { editingBudget = false }, onSave = { budgetId, category, currency, amount -> scope.launch { dao.upsertBudget(BudgetEntity(budgetId ?: UUID.randomUUID().toString(), category.name, currency, monthKey, amount, System.currentTimeMillis())) }; editingBudget = false }, onDelete = { budget -> scope.launch { dao.deleteBudget(budget.budgetId) } }, budgets = budgets)
}

@Composable
private fun CategoryEditorDialog(current: String, currentInternal: Boolean, onDismiss: () -> Unit, onSave: (TransactionCategory, Boolean) -> Unit) {
    var selected by remember(current) { mutableStateOf(runCatching { TransactionCategory.valueOf(current) }.getOrDefault(TransactionCategory.OTHER)) }
    var internal by remember(currentInternal) { mutableStateOf(currentInternal) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Transaction category") }, text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TransactionCategory.values().forEach { category -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(selected == category, onClick = { selected = category; if (category == TransactionCategory.TRANSFER) internal = true }); Text(category.name.replace("_", " ")) } }
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Checkbox(internal, onCheckedChange = { internal = it }); Text("Exclude as own-account transfer") }
    } }, confirmButton = { TextButton(onClick = { onSave(selected, internal || selected == TransactionCategory.TRANSFER) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun BudgetEditorDialog(monthKey: String, budgets: List<BudgetEntity>, onDismiss: () -> Unit, onSave: (String?, TransactionCategory, String, String) -> Unit, onDelete: (BudgetEntity) -> Unit) {
    val expenseCategories = TransactionCategory.values().filter { it !in setOf(TransactionCategory.TRANSFER, TransactionCategory.INCOME) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf(expenseCategories.first()) }
    var currency by remember { mutableStateOf("NGN") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Monthly budgets · $monthKey") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Choose a category, currency and monthly limit. Budgets are not converted between currencies.", style = MaterialTheme.typography.bodySmall)
        if (editingId != null) Text("Editing existing budget", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        expenseCategories.forEach { item -> Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(category == item, onClick = { category = item }); Text(item.name.replace("_", " ")) } }
        OutlinedTextField(currency, { currency = it.uppercase().take(3) }, label = { Text("Currency code") }, singleLine = true)
        OutlinedTextField(amount, { amount = it; error = null }, label = { Text("Monthly limit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (budgets.isNotEmpty()) { HorizontalDivider(); Text("Existing budgets", style = MaterialTheme.typography.titleSmall); budgets.forEach { budget -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { TextButton(onClick = { editingId = budget.budgetId; category = runCatching { TransactionCategory.valueOf(budget.category) }.getOrDefault(TransactionCategory.OTHER); currency = budget.currencyCode; amount = budget.limitAmount; error = null }) { Text("Edit · ${budget.category} · ${budget.currencyCode} ${budget.limitAmount}") }; TextButton(onClick = { onDelete(budget); if (editingId == budget.budgetId) { editingId = null; amount = "" } }) { Text("Delete") } } } }
    } }, confirmButton = { TextButton(onClick = { val parsed = runCatching { BigDecimal(amount.replace(",", "")) }.getOrNull(); when { currency.length != 3 -> error = "Enter a 3-letter currency code."; parsed == null || parsed <= BigDecimal.ZERO -> error = "Enter a valid positive budget."; else -> onSave(editingId, category, currency, parsed.toPlainString()) } }) { Text(if (editingId == null) "Save budget" else "Update budget") } }, dismissButton = { Row { if (editingId != null) TextButton(onClick = { editingId = null; amount = ""; category = expenseCategories.first(); currency = "NGN"; error = null }) { Text("New budget") }; TextButton(onClick = onDismiss) { Text("Close") } } })
}

private fun money(value: BigDecimal): String = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(value)