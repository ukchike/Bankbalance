package com.bankscope

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.bankscope.data.BankAccountDao
import com.bankscope.data.BankAccountEntity
import com.bankscope.data.BudgetEntity
import com.bankscope.data.TransactionCategory
import com.bankscope.data.TransactionCsvExporter
import com.bankscope.data.TransactionEntity
import com.bankscope.ui.components.GroupedCard
import com.bankscope.ui.components.GroupedRow
import com.bankscope.ui.components.InitialsAvatar
import com.bankscope.ui.components.LargeTitleHeader
import com.bankscope.ui.components.PillSegmentedControl
import com.bankscope.ui.components.appleCardShadow
import com.bankscope.ui.theme.AppleSemantics
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
    var filterIndex by remember { mutableIntStateOf(0) }
    val filterOptions = listOf("All", "Matched", "Review")
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
        val matchesStatus = when (filterIndex) { 2 -> row.reviewStatus != "MATCHED"; 1 -> row.reviewStatus == "MATCHED"; else -> true }
        matchesText && matchesStatus
    }
    val monthTransactions = transactions.filter { row -> row.eventAt != null && SimpleDateFormat("yyyy-MM", Locale.US).format(Date(row.eventAt)) == monthKey && row.reviewStatus == "MATCHED" }
    val spendByKey = monthTransactions.filter { it.direction == "DEBIT" && !it.isInternalTransfer && it.amount != null }
        .groupBy { it.category to it.currencyCode }
        .mapValues { (_, rows) -> rows.fold(BigDecimal.ZERO) { sum, row -> sum + BigDecimal(row.amount!!) } }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                LargeTitleHeader("Transactions", "Reconciled from notifications & SMS")
                FilledTonalButton(onClick = { exportLauncher.launch("bankbalance-transactions.csv") }, enabled = transactions.isNotEmpty(), shape = RoundedCornerShape(12.dp)) {
                    Text("Export")
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Budgets · $monthKey", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { editingBudget = true }) { Text("Manage") }
                }
                if (budgets.isEmpty()) {
                    GroupedCard(Modifier.appleCardShadow()) {
                        GroupedRow(showDivider = false) {
                            Text("No budgets set for this month. Add a category budget to track spending.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    GroupedCard(Modifier.appleCardShadow()) {
                        budgets.forEachIndexed { index, budget ->
                            val spent = spendByKey[budget.category to budget.currencyCode] ?: BigDecimal.ZERO
                            val limit = runCatching { BigDecimal(budget.limitAmount) }.getOrDefault(BigDecimal.ZERO)
                            val ratio = if (limit > BigDecimal.ZERO) (spent.toDouble() / limit.toDouble()).toFloat().coerceIn(0f, 1f) else 0f
                            val over = spent > limit
                            GroupedRow(showDivider = index != budgets.lastIndex, verticalPadding = 12.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${budget.category.replace("_", " ")} · ${budget.currencyCode}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text("${money(spent)} / ${money(limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)),
                                        color = if (over) AppleSemantics.Negative else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    Text(
                                        if (over) "Over budget by ${money(spent - limit)}" else "Remaining ${money(limit - spent)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (over) AppleSemantics.Negative else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Monthly category spending", style = MaterialTheme.typography.titleLarge)
                if (spendByKey.isEmpty()) {
                    Text("Matched debit transactions with event dates will appear here. Internal transfers are excluded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    GroupedCard(Modifier.appleCardShadow()) {
                        val ranked = spendByKey.toList().sortedByDescending { it.second }.take(8)
                        ranked.forEachIndexed { index, (key, amount) ->
                            GroupedRow(showDivider = index != ranked.lastIndex, verticalPadding = 12.dp) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(key.first.replace("_", " "), style = MaterialTheme.typography.bodyMedium)
                                    Text("${key.second} ${money(amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search institution, description, category") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                PillSegmentedControl(filterOptions, filterIndex, { filterIndex = it })
                Text("${filtered.size} transaction(s)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (filtered.isEmpty()) {
            item { Text("No transactions match this filter.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(filtered, key = { it.transactionId }) { row -> TransactionRowCard(row, onEditCategory = { categorising = row }, onAssign = { assigning = row }) }
        }
    }

    assigning?.let { row ->
        AlertDialog(
            onDismissRequest = { assigning = null },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Assign transaction") },
            text = {
                Column {
                    Text("Choose the correct account with matching institution and currency.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    accounts.filter { it.institutionName.equals(row.institutionName, true) && it.currencyCode.equals(row.currencyCode, true) }.forEach { account ->
                        TextButton(onClick = { scope.launch { dao.assignTransaction(row.transactionId, account.accountId) }; assigning = null }) {
                            Text("${account.institutionName} ••${account.accountIdentifier.takeLast(4)} (${account.currencyCode})")
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { assigning = null }) { Text("Close") } }
        )
    }
    categorising?.let { row -> CategoryEditorDialog(row.category, row.isInternalTransfer, onDismiss = { categorising = null }, onSave = { category, internal -> scope.launch { dao.updateTransactionCategory(row.transactionId, category.name, internal) }; categorising = null }) }
    if (editingBudget) BudgetEditorDialog(monthKey, onDismiss = { editingBudget = false }, onSave = { budgetId, category, currency, amount -> scope.launch { dao.upsertBudget(BudgetEntity(budgetId ?: UUID.randomUUID().toString(), category.name, currency, monthKey, amount, System.currentTimeMillis())) }; editingBudget = false }, onDelete = { budget -> scope.launch { dao.deleteBudget(budget.budgetId) } }, budgets = budgets)
}

@Composable
private fun TransactionRowCard(row: TransactionEntity, onEditCategory: () -> Unit, onAssign: () -> Unit) {
    val isCredit = row.direction == "CREDIT"
    val amountColor = when {
        row.direction == null -> MaterialTheme.colorScheme.onSurface
        isCredit -> AppleSemantics.Positive
        else -> MaterialTheme.colorScheme.onSurface
    }
    GroupedCard(Modifier.appleCardShadow()) {
        GroupedRow(showDivider = false, verticalPadding = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InitialsAvatar(row.institutionName.filter(Char::isLetter).take(2).uppercase(), tint = if (isCredit) AppleSemantics.Positive else MaterialTheme.colorScheme.primary)
                        Column {
                            Text(row.institutionName, style = MaterialTheme.typography.titleMedium)
                            Text(row.description ?: "No transaction description", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                    Text(
                        "${if (isCredit) "+" else if (row.direction == "DEBIT") "−" else ""} ${row.currencyCode} ${row.amount ?: "—"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = amountColor
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TagPill(row.category.replace("_", " "), MaterialTheme.colorScheme.primary)
                    if (row.isInternalTransfer) TagPill("Internal transfer", AppleSemantics.Warning)
                    TagPill(row.reviewStatus.replace("_", " "), if (row.reviewStatus == "MATCHED") AppleSemantics.Positive else AppleSemantics.Warning)
                }
                Text(
                    "Account ${row.accountHint?.takeLast(4)?.let { "••$it" } ?: "Not identified"} · ${row.sourceType} · ${(row.confidence * 100).toInt()}% confidence",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Event ${row.eventAt?.let { java.text.DateFormat.getDateTimeInstance().format(Date(it)) } ?: "Not supplied"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEditCategory) { Text("Edit category") }
                    if (row.reviewStatus != "MATCHED") TextButton(onClick = onAssign) { Text("Assign account") }
                }
            }
        }
    }
}

@Composable
private fun TagPill(text: String, tint: Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = tint.copy(alpha = 0.14f)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = tint, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
private fun CategoryEditorDialog(current: String, currentInternal: Boolean, onDismiss: () -> Unit, onSave: (TransactionCategory, Boolean) -> Unit) {
    var selected by remember(current) { mutableStateOf(runCatching { TransactionCategory.valueOf(current) }.getOrDefault(TransactionCategory.OTHER)) }
    var internal by remember(currentInternal) { mutableStateOf(currentInternal) }
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("Transaction category") }, text = { Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        TransactionCategory.values().forEach { category -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(selected == category, onClick = { selected = category; if (category == TransactionCategory.TRANSFER) internal = true }); Text(category.name.replace("_", " ")) } }
        Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(internal, onCheckedChange = { internal = it }); Text("Exclude as own-account transfer") }
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
    AlertDialog(onDismissRequest = onDismiss, shape = RoundedCornerShape(20.dp), title = { Text("Monthly budgets · $monthKey") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Choose a category, currency and monthly limit. Budgets are not converted between currencies.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (editingId != null) Text("Editing existing budget", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
        expenseCategories.forEach { item -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(category == item, onClick = { category = item }); Text(item.name.replace("_", " ")) } }
        OutlinedTextField(currency, { currency = it.uppercase().take(3) }, label = { Text("Currency code") }, singleLine = true, shape = RoundedCornerShape(12.dp))
        OutlinedTextField(amount, { amount = it; error = null }, label = { Text("Monthly limit") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, shape = RoundedCornerShape(12.dp))
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (budgets.isNotEmpty()) {
            HorizontalDivider()
            Text("Existing budgets", style = MaterialTheme.typography.titleSmall)
            budgets.forEach { budget ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { editingId = budget.budgetId; category = runCatching { TransactionCategory.valueOf(budget.category) }.getOrDefault(TransactionCategory.OTHER); currency = budget.currencyCode; amount = budget.limitAmount; error = null }) { Text("Edit · ${budget.category} · ${budget.currencyCode} ${budget.limitAmount}") }
                    TextButton(onClick = { onDelete(budget); if (editingId == budget.budgetId) { editingId = null; amount = "" } }) { Text("Delete") }
                }
            }
        }
    } }, confirmButton = { TextButton(onClick = { val parsed = runCatching { BigDecimal(amount.replace(",", "")) }.getOrNull(); when { currency.length != 3 -> error = "Enter a 3-letter currency code."; parsed == null || parsed <= BigDecimal.ZERO -> error = "Enter a valid positive budget."; else -> onSave(editingId, category, currency, parsed.toPlainString()) } }) { Text(if (editingId == null) "Save budget" else "Update budget") } }, dismissButton = { Row { if (editingId != null) TextButton(onClick = { editingId = null; amount = ""; category = expenseCategories.first(); currency = "NGN"; error = null }) { Text("New budget") }; TextButton(onClick = onDismiss) { Text("Close") } } })
}

private fun money(value: BigDecimal): String = NumberFormat.getNumberInstance(Locale.US).apply { minimumFractionDigits = 2; maximumFractionDigits = 2 }.format(value)
