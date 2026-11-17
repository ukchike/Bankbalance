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
import androidx.compose.ui.unit.dp
import com.bankscope.data.BankAccountDao
import com.bankscope.data.BankAccountEntity
import com.bankscope.data.TransactionCsvExporter
import com.bankscope.data.TransactionEntity
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun TransactionHistoryScreen(
    dao: BankAccountDao,
    accounts: List<BankAccountEntity>,
    modifier: Modifier = Modifier
) {
    val transactions by dao.observeTransactions().collectAsState(initial = emptyList())
    val summaries by dao.observeMonthlySummaries().collectAsState(initial = emptyList())
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var assigning by remember { mutableStateOf<TransactionEntity?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(TransactionCsvExporter.toCsv(transactions))
                    }
                }
            }
        }
    }

    val filtered = transactions.filter { row ->
        val matchesText = listOf(row.institutionName, row.accountHint.orEmpty(),
            row.description.orEmpty(), row.currencyCode, row.sourceType)
            .any { it.contains(search, ignoreCase = true) }
        val matchesStatus = when (filter) {
            "REVIEW" -> row.reviewStatus != "MATCHED"
            "MATCHED" -> row.reviewStatus == "MATCHED"
            else -> true
        }
        matchesText && matchesStatus
    }

    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Transactions", style = MaterialTheme.typography.headlineSmall)
            Button(onClick = { exportLauncher.launch("bankbalance-transactions.csv") }, enabled = transactions.isNotEmpty()) {
                Text("Export CSV")
            }
        }
        Text("Monthly summaries", style = MaterialTheme.typography.titleMedium)
        if (summaries.isEmpty()) {
            Text("Monthly summaries will appear when transactions with reliable event dates are recorded and matched.")
        } else {
            summaries.take(6).forEach { summary ->
                val amount = NumberFormat.getNumberInstance(Locale.US).format(summary.total)
                Text("${summary.monthKey} · ${summary.direction ?: "Unknown"} · ${summary.currencyCode} $amount (${summary.transactionCount})",
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Search institution, description, currency") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("ALL", "MATCHED", "REVIEW").forEach { value ->
                FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
            }
        }
        Text("${filtered.size} transaction(s)", style = MaterialTheme.typography.labelMedium)
        if (filtered.isEmpty()) {
            Text("No transactions match this filter.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.transactionId }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(row.institutionName, style = MaterialTheme.typography.titleSmall)
                                Text("${row.direction ?: "UNCLASSIFIED"} ${row.currencyCode} ${row.amount ?: "—"}")
                            }
                            Text(row.description ?: "No transaction description")
                            Text("Account: ${row.accountHint?.takeLast(4)?.let { "••$it" } ?: "Not identified"} · ${row.sourceType}",
                                style = MaterialTheme.typography.bodySmall)
                            Text("Status: ${row.reviewStatus} · Confidence: ${(row.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall)
                            Text("Event: ${row.eventAt?.let { java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it)) } ?: "Not supplied"}",
                                style = MaterialTheme.typography.bodySmall)
                            if (row.reviewStatus != "MATCHED") {
                                TextButton(onClick = { assigning = row }) { Text("Assign to account") }
                            }
                        }
                    }
                }
            }
        }
    }

    assigning?.let { row ->
        AlertDialog(
            onDismissRequest = { assigning = null },
            title = { Text("Assign transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Choose the correct account. Verify the currency and last four digits before confirming.")
                    accounts.filter { it.institutionName.equals(row.institutionName, true) && it.currencyCode.equals(row.currencyCode, true) }
                        .forEach { account ->
                            TextButton(onClick = {
                                scope.launch { dao.assignTransaction(row.transactionId, account.accountId) }
                                assigning = null
                            }) {
                                Text("${account.institutionName} ••${account.accountIdentifier.takeLast(4)} (${account.currencyCode})")
                            }
                        }
                }
            },
            confirmButton = { TextButton(onClick = { assigning = null }) { Text("Close") } }
        )
    }
}
