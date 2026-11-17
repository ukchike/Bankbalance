package com.bankscope

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.bankscope.notifications.NotificationAccess
import com.bankscope.data.BankAccountEntity
import com.bankscope.data.BankAccountRepository
import com.bankscope.data.BankBalanceDatabase
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = BankBalanceDatabase.getInstance(this).bankAccountDao()
        val repository = BankAccountRepository(dao)
        setContent { BankBalanceApp(repository, dao) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankBalanceApp(repository: BankAccountRepository, dao: com.bankscope.data.BankAccountDao) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts by repository.observeAll().collectAsState(initial = emptyList())
    val visibleAccounts = accounts.filter { !it.isHidden }
    val totals = accounts.filter { it.isIncluded && it.latestBalanceAmount != null }
        .groupBy { it.currencyCode }
        .mapValues { (_, rows) ->
            rows.fold(BigDecimal.ZERO) { total, account ->
                total + BigDecimal(account.latestBalanceAmount!!)
            }
        }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("BankBalance")
                            Text("Personal balance monitor", style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    actions = {
                        TextButton(onClick = { NotificationAccess.openSettings(context) }) { Text("Enable notifications") }
                        TextButton(onClick = { showAddDialog = true }) { Text("Add account") }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Text("₦") },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Text("≡") },
                        label = { Text("Accounts") }
                    )
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Text("₦") }, label = { Text("Transactions") })
                }
            }
        ) { padding ->
            when (selectedTab) {
                0 -> DashboardScreen(visibleAccounts, totals, Modifier.padding(padding))
                1 -> AccountsScreen(accounts, { id, value -> scope.launch { repository.setIncluded(id, value) } }, { id, value -> scope.launch { repository.setHidden(id, value) } }, Modifier.padding(padding))
                else -> TransactionHistoryScreen(dao, accounts, Modifier.padding(padding))
            }
        }

        if (showAddDialog) {
            AddAccountDialog(
                onDismiss = { showAddDialog = false },
                onSave = { bank, identifier, currency, type, balance ->
                    scope.launch {
                        repository.addAccount(bank, identifier, currency, type, balance)
                        showAddDialog = false
                    }
                }
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    accounts: List<BankAccountEntity>,
    totals: Map<String, BigDecimal>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Included balances", style = MaterialTheme.typography.titleMedium)
        if (totals.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("No balances recorded yet", style = MaterialTheme.typography.titleMedium)
                    Text("Add an account and enter its latest reported balance. Use Enable notifications to connect supported Android app alerts.")
                }
            }
        } else {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    totals.toSortedMap().forEach { (currency, total) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total $currency", style = MaterialTheme.typography.titleSmall)
                            Text(formatMoney(total), style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
        Text("Accounts", style = MaterialTheme.typography.titleMedium)
        if (accounts.isEmpty()) {
            Text("No visible accounts. Add an account or unhide one in Account management.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accounts, key = { it.accountId }) { account -> AccountBalanceCard(account) }
            }
        }
        Text(
            "Balances are reported observations, not live bank balances. Totals are separated by currency.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AccountBalanceCard(account: BankAccountEntity) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(account.institutionName, style = MaterialTheme.typography.titleMedium)
                    Text("••${account.accountIdentifier.takeLast(4)} · ${account.currencyCode}", style = MaterialTheme.typography.bodySmall)
                }
                Text(
                    account.latestBalanceAmount?.let { "${account.currencyCode} ${formatMoney(BigDecimal(it))}" } ?: "Balance unavailable",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                if (account.isIncluded) "Included in totals" else "Excluded from totals",
                style = MaterialTheme.typography.labelSmall,
                color = if (account.isIncluded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            account.lastReceivedAt?.let {
                Text(
                    "Last recorded: ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AccountsScreen(
    accounts: List<BankAccountEntity>,
    onIncludeChange: (String, Boolean) -> Unit,
    onHiddenChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    if (accounts.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No accounts added yet.") }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Manage each account independently", style = MaterialTheme.typography.titleMedium) }
        items(accounts, key = { it.accountId }) { account ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(account.institutionName, style = MaterialTheme.typography.titleMedium)
                    Text("••${account.accountIdentifier.takeLast(4)} · ${account.currencyCode}", style = MaterialTheme.typography.bodySmall)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Include in totals")
                            Text("Exclude without deleting account history.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = account.isIncluded, onCheckedChange = { onIncludeChange(account.accountId, it) })
                    }
                    HorizontalDivider()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Show on dashboard")
                            Text("Hiding does not stop notification collection.", style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = !account.isHidden, onCheckedChange = { onHiddenChange(account.accountId, !it) })
                    }
                }
            }
        }
    }
}

@Composable
private fun AddAccountDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String?) -> Unit
) {
    var bank by remember { mutableStateOf("") }
    var identifier by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("NGN") }
    var type by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add bank account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(bank, { bank = it; error = null }, label = { Text("Bank / institution") }, singleLine = true)
                OutlinedTextField(identifier, { identifier = it }, label = { Text("Account number or identifier") }, singleLine = true)
                OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency (e.g. NGN, USD)") }, singleLine = true)
                OutlinedTextField(type, { type = it }, label = { Text("Account type (optional)") }, singleLine = true)
                OutlinedTextField(
                    balance, { balance = it; error = null },
                    label = { Text("Latest balance (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val validAmount = balance.isBlank() || runCatching {
                    BigDecimal(balance.replace(",", ""))
                }.isSuccess
                when {
                    bank.isBlank() || identifier.isBlank() || currency.trim().length != 3 ->
                        error = "Enter a bank, account identifier and 3-letter currency code."
                    !validAmount -> error = "Enter a valid balance amount."
                    else -> onSave(bank, identifier, currency, type, balance.replace(",", "").takeIf { it.isNotBlank() })
                }
            }) { Text("Save account") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatMoney(amount: BigDecimal): String =
    NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)
