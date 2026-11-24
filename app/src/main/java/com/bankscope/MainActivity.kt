package com.bankscope

import android.os.Bundle
import android.Manifest
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.List as ListIcon
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.bankscope.notifications.NotificationAccess
import com.bankscope.notifications.SmsInboxImporter
import com.bankscope.notifications.NotificationDiagnostics
import com.bankscope.data.BankAccountEntity
import com.bankscope.data.BankAccountRepository
import com.bankscope.data.BankBalanceDatabase
import com.bankscope.ui.components.GroupedCard
import com.bankscope.ui.components.GroupedRow
import com.bankscope.ui.components.InitialsAvatar
import com.bankscope.ui.components.LargeTitleHeader
import com.bankscope.ui.components.appleCardShadow
import com.bankscope.ui.theme.AppleSemantics
import com.bankscope.ui.theme.BankBalanceTheme
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

private enum class AppTab(val label: String) { DASHBOARD("Dashboard"), ACCOUNTS("Accounts"), TRANSACTIONS("Transactions") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankBalanceApp(repository: BankAccountRepository, dao: com.bankscope.data.BankAccountDao) {
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showAddDialog by remember { mutableStateOf(false) }
    var smsImportMessage by remember { mutableStateOf<String?>(null) }
    var importingSms by remember { mutableStateOf(false) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var diagnosticSnapshot by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val smsPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            importingSms = true
            scope.launch {
                val result = runCatching { SmsInboxImporter(context, dao).importRecent(90) }
                smsImportMessage = result.fold(
                    { result -> "Scanned " + result.scanned + " SMS messages. Imported " + result.imported + " transactions; " + result.ignored + " were not recognised." },
                    { error -> "SMS import failed: " + (error.message ?: "Unknown error") }
                )
                importingSms = false
            }
        } else smsImportMessage = "SMS permission was not granted. No messages were read."
    }
    val appearance = remember { context.getSharedPreferences("appearance", 0) }
    var themeChoice by remember { mutableStateOf(appearance.getString("theme", "SYSTEM") ?: "SYSTEM") }
    var showThemeMenu by remember { mutableStateOf(false) }
    val useDarkTheme = when (themeChoice) {
        "DARK" -> true
        "LIGHT" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val accounts by repository.observeAll().collectAsState(initial = emptyList())
    val visibleAccounts = accounts.filter { !it.isHidden }
    val totals = accounts.filter { it.isIncluded && it.latestBalanceAmount != null }
        .groupBy { it.currencyCode }
        .mapValues { (_, rows) ->
            rows.fold(BigDecimal.ZERO) { total, account ->
                total + BigDecimal(account.latestBalanceAmount!!)
            }
        }

    BankBalanceTheme(useDarkTheme = useDarkTheme) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.92f)
                    ),
                    title = {
                        Text("BankBalance", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleLarge)
                    },
                    actions = {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add account")
                        }
                        Box {
                            IconButton(onClick = { showOverflowMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "More")
                            }
                            DropdownMenu(expanded = showOverflowMenu, onDismissRequest = { showOverflowMenu = false }) {
                                DropdownMenuItem(text = { Text("Theme: " + themeChoice.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { showOverflowMenu = false; showThemeMenu = true })
                                DropdownMenuItem(text = { Text("Diagnostics") }, leadingIcon = { Icon(Icons.Filled.Sms, null) }, onClick = {
                                    diagnosticSnapshot = NotificationDiagnostics(context).snapshot(); showDiagnostics = true; showOverflowMenu = false
                                })
                                DropdownMenuItem(text = { Text("Notification access") }, leadingIcon = { Icon(Icons.Filled.Notifications, null) }, onClick = {
                                    NotificationAccess.openSettings(context); showOverflowMenu = false
                                })
                                DropdownMenuItem(
                                    text = { Text(if (importingSms) "Importing…" else "Import SMS") },
                                    enabled = !importingSms,
                                    onClick = { smsPermissionLauncher.launch(Manifest.permission.READ_SMS); showOverflowMenu = false }
                                )
                            }
                            DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                                listOf("SYSTEM", "LIGHT", "DARK").forEach { choice ->
                                    DropdownMenuItem(text = { Text(choice.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = {
                                        themeChoice = choice
                                        appearance.edit().putString("theme", choice).apply()
                                        showThemeMenu = false
                                    })
                                }
                            }
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    NavigationBarItem(
                        selected = selectedTab == AppTab.DASHBOARD,
                        onClick = { selectedTab = AppTab.DASHBOARD },
                        icon = { Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null) },
                        label = { Text("Dashboard") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.ACCOUNTS,
                        onClick = { selectedTab = AppTab.ACCOUNTS },
                        icon = { Icon(Icons.AutoMirrored.Filled.ListIcon, contentDescription = null) },
                        label = { Text("Accounts") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == AppTab.TRANSACTIONS,
                        onClick = { selectedTab = AppTab.TRANSACTIONS },
                        icon = { Icon(Icons.Filled.Sms, contentDescription = null) },
                        label = { Text("Transactions") }
                    )
                }
            }
        ) { padding ->
            AnimatedContent(
                targetState = selectedTab,
                modifier = Modifier.padding(padding),
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(220)) { it / 16 })
                        .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(150)) { -it / 16 })
                },
                label = "tab-content"
            ) { tab ->
                when (tab) {
                    AppTab.DASHBOARD -> DashboardScreen(visibleAccounts, totals)
                    AppTab.ACCOUNTS -> AccountsScreen(accounts, { id, value -> scope.launch { repository.setIncluded(id, value) } }, { id, value -> scope.launch { repository.setHidden(id, value) } })
                    AppTab.TRANSACTIONS -> TransactionHistoryScreen(dao, accounts)
                }
            }
        }

        if (showDiagnostics) AlertDialog(onDismissRequest = { showDiagnostics = false }, title = { Text("Capture diagnostics") }, text = { Column { Text("Notification access: " + if (NotificationAccess.isGranted(context)) "Granted" else "Not granted"); diagnosticSnapshot.forEach { (key, value) -> Text(key.replace("_", " ").replaceFirstChar { it.uppercase() } + ": " + value) } } }, confirmButton = { TextButton(onClick = { diagnosticSnapshot = NotificationDiagnostics(context).snapshot() }) { Text("Refresh") } }, dismissButton = { TextButton(onClick = { showDiagnostics = false }) { Text("Close") } })

        smsImportMessage?.let { message -> AlertDialog(onDismissRequest = { smsImportMessage = null }, title = { Text("SMS import result") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { smsImportMessage = null }) { Text("Close") } }) }

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
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { LargeTitleHeader("Balances", "Across every included account") }
        item {
            if (totals.isEmpty()) {
                GroupedCard(Modifier.appleCardShadow()) {
                    GroupedRow(showDivider = false, verticalPadding = 20.dp) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("No balances recorded yet", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Add an account and enter its latest reported balance, or enable notifications to connect supported banking apps.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                GroupedCard(Modifier.appleCardShadow()) {
                    val sorted = totals.toSortedMap().toList()
                    sorted.forEachIndexed { index, (currency, total) ->
                        GroupedRow(showDivider = index != sorted.lastIndex, verticalPadding = 16.dp) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Total balance", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(currency, style = MaterialTheme.typography.titleSmall)
                                }
                                Text(formatMoney(total), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
        item { Text("Accounts", style = MaterialTheme.typography.titleLarge) }
        if (accounts.isEmpty()) {
            item {
                Text(
                    "No visible accounts. Add an account or unhide one in Account management.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(accounts, key = { it.accountId }) { account -> AccountBalanceCard(account) }
        }
        item {
            Text(
                "Balances are reported observations, not live bank balances. Totals are separated by currency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountBalanceCard(account: BankAccountEntity) {
    GroupedCard(Modifier.appleCardShadow()) {
        GroupedRow(showDivider = false, verticalPadding = 14.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InitialsAvatar(account.institutionName.filter(Char::isLetter).take(2).uppercase())
                        Column {
                            Text(account.institutionName, style = MaterialTheme.typography.titleMedium)
                            Text("••${account.accountIdentifier.takeLast(4)} · ${account.currencyCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        account.latestBalanceAmount?.let { "${account.currencyCode} ${formatMoney(BigDecimal(it))}" } ?: "Balance unavailable",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatusPill(
                        text = if (account.isIncluded) "Included in totals" else "Excluded from totals",
                        tint = if (account.isIncluded) AppleSemantics.Positive else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                account.lastReceivedAt?.let {
                    Text(
                        "Last recorded ${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(it))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, tint: androidx.compose.ui.graphics.Color) {
    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp), color = tint.copy(alpha = 0.14f)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tint, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
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
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No accounts added yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item { LargeTitleHeader("Accounts", "Manage visibility and totals") }
        items(accounts, key = { it.accountId }) { account ->
            GroupedCard(Modifier.appleCardShadow()) {
                GroupedRow(showDivider = true, verticalPadding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        InitialsAvatar(account.institutionName.filter(Char::isLetter).take(2).uppercase())
                        Column {
                            Text(account.institutionName, style = MaterialTheme.typography.titleMedium)
                            Text("••${account.accountIdentifier.takeLast(4)} · ${account.currencyCode}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                GroupedRow(showDivider = true, verticalPadding = 12.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Include in totals", style = MaterialTheme.typography.bodyLarge)
                            Text("Exclude without deleting account history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = account.isIncluded, onCheckedChange = { onIncludeChange(account.accountId, it) })
                    }
                }
                GroupedRow(showDivider = false, verticalPadding = 12.dp) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Show on dashboard", style = MaterialTheme.typography.bodyLarge)
                            Text("Hiding does not stop notification collection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
        title = { Text("Add bank account", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(bank, { bank = it; error = null }, label = { Text("Bank / institution") }, singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                OutlinedTextField(identifier, { identifier = it }, label = { Text("Account number or identifier") }, singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                OutlinedTextField(currency, { currency = it.uppercase() }, label = { Text("Currency (e.g. NGN, USD)") }, singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                OutlinedTextField(type, { type = it }, label = { Text("Account type (optional)") }, singleLine = true, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                OutlinedTextField(
                    balance, { balance = it; error = null },
                    label = { Text("Latest balance (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                onClick = {
                    val validAmount = balance.isBlank() || runCatching {
                        BigDecimal(balance.replace(",", ""))
                    }.isSuccess
                    when {
                        bank.isBlank() || identifier.isBlank() || currency.trim().length != 3 ->
                            error = "Enter a bank, account identifier and 3-letter currency code."
                        !validAmount -> error = "Enter a valid balance amount."
                        else -> onSave(bank, identifier, currency, type, balance.replace(",", "").takeIf { it.isNotBlank() })
                    }
                }
            ) { Text("Save account") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatMoney(amount: BigDecimal): String =
    NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(amount)
