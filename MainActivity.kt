package com.aaknav.funding

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import org.json.JSONArray
import org.json.JSONObject

private val Navy = Color(0xFF173B9B)
private val Blue = Color(0xFF2446B8)
private val Yellow = Color(0xFFFFD21A)
private val Bg = Color(0xFFF5F7FB)

data class Customer(val name: String, val mobile: String, val business: String, val address: String)
data class ApplicationItem(val customer: String, val type: String, val amount: String, val status: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AaknavApp(this) }
    }
}

@Composable
fun AaknavApp(context: Context) {
    val nav = rememberNavController()
    var customers by remember { mutableStateOf(loadCustomers(context)) }
    var apps by remember { mutableStateOf(loadApps(context)) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Navy,
            secondary = Yellow,
            background = Bg
        )
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    val items = listOf(
                        Triple("Dashboard", "dashboard", Icons.Default.Dashboard),
                        Triple("Customers", "customers", Icons.Default.People),
                        Triple("Applications", "applications", Icons.Default.Assignment),
                        Triple("Collections", "collections", Icons.Default.Payments),
                        Triple("Reports", "reports", Icons.Default.BarChart)
                    )
                    val current = nav.currentBackStackEntryAsState().value?.destination?.route
                    items.forEach { (label, route, icon) ->
                        NavigationBarItem(
                            selected = current == route,
                            onClick = { nav.navigate(route) { launchSingleTop = true } },
                            icon = { Icon(icon, null) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { pad ->
            NavHost(nav, startDestination = "dashboard", modifier = Modifier.padding(pad)) {
                composable("dashboard") {
                    DashboardScreen(customers.size, apps.size, apps, onNew = { nav.navigate("new") })
                }
                composable("customers") {
                    CustomersScreen(customers, onAdd = { nav.navigate("newCustomer") })
                }
                composable("newCustomer") {
                    NewCustomerScreen { c ->
                        customers = customers + c
                        saveCustomers(context, customers)
                        nav.popBackStack()
                    }
                }
                composable("applications") {
                    ApplicationsScreen(apps, onNew = { nav.navigate("new") })
                }
                composable("new") {
                    NewApplicationScreen(customers) { a ->
                        apps = apps + a
                        saveApps(context, apps)
                        nav.popBackStack()
                    }
                }
                composable("collections") { CollectionsScreen(apps) }
                composable("reports") { ReportsScreen(customers.size, apps) }
            }
        }
    }
}

@Composable
fun Header(title: String, subtitle: String = "") {
    Column(
        Modifier.fillMaxWidth().background(Navy).padding(20.dp)
    ) {
        Text("AAKNAV INVEST PRIVATE LIMITED", color = Yellow, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(title, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = Color.White.copy(.85f))
    }
}

@Composable
fun DashboardScreen(customerCount: Int, appCount: Int, apps: List<ApplicationItem>, onNew: () -> Unit) {
    val requested = apps.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val pending = apps.count { it.status == "Under Review" || it.status == "Documents Pending" }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Header("Business Funding Dashboard", "Manage customers, applications and collections")
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Customers", customerCount.toString(), Icons.Default.People, Modifier.weight(1f))
                    StatCard("Applications", appCount.toString(), Icons.Default.Assignment, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Pending Cases", pending.toString(), Icons.Default.PendingActions, Modifier.weight(1f))
                    StatCard("Requested", "₹${"%,.0f".format(requested)}", Icons.Default.AccountBalanceWallet, Modifier.weight(1f))
                }
            }
            item {
                Button(onClick = onNew, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Yellow, contentColor = Navy)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("NEW FUNDING APPLICATION", fontWeight = FontWeight.Bold)
                }
            }
            item { Text("Recent Applications", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            items(apps.takeLast(5).reversed()) { a -> ApplicationRow(a) }
            if (apps.isEmpty()) item { EmptyBox("No applications yet. Add your first customer and application.") }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
    Card(modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, tint = Navy)
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Navy)
            Text(title, color = Color.Gray)
        }
    }
}

@Composable
fun CustomersScreen(customers: List<Customer>, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Header("Customers", "Customer profile, business details and contact records")
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.width(8.dp)); Text("ADD CUSTOMER")
                }
            }
            items(customers) { c ->
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(c.business)
                        Text(c.mobile)
                        Text(c.address, color = Color.Gray)
                        Text("Documents: Customer-wise record", color = Navy, modifier = Modifier.padding(top = 6.dp))
                    }
                }
            }
            if (customers.isEmpty()) item { EmptyBox("No customers added.") }
        }
    }
}

@Composable
fun NewCustomerScreen(onSave: (Customer) -> Unit) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var business by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    FormScaffold("New Customer") {
        Field("Customer Name", name) { name = it }
        Field("Mobile Number", mobile) { mobile = it }
        Field("Business / Company", business) { business = it }
        Field("Address", address) { address = it }
        Button(onClick = { if (name.isNotBlank()) onSave(Customer(name, mobile, business, address)) }, modifier = Modifier.fillMaxWidth()) {
            Text("SAVE CUSTOMER")
        }
    }
}

@Composable
fun ApplicationsScreen(apps: List<ApplicationItem>, onNew: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Header("Application Tracking", "New → Under Review → Documents Pending → Approved/Rejected → Disbursed")
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("NEW APPLICATION") } }
            items(apps.reversed()) { ApplicationRow(it) }
            if (apps.isEmpty()) item { EmptyBox("No applications found.") }
        }
    }
}

@Composable
fun ApplicationRow(a: ApplicationItem) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(a.customer, fontWeight = FontWeight.Bold)
                Text(a.status, color = if (a.status == "Approved" || a.status == "Disbursed") Color(0xFF16843A) else Navy, fontWeight = FontWeight.Bold)
            }
            Text(a.type)
            Text("Requested: ₹${a.amount}")
        }
    }
}

@Composable
fun NewApplicationScreen(customers: List<Customer>, onSave: (ApplicationItem) -> Unit) {
    var customer by remember { mutableStateOf(customers.firstOrNull()?.name ?: "") }
    var type by remember { mutableStateOf("Startup Funding") }
    var amount by remember { mutableStateOf("") }
    val types = listOf("Startup Funding", "Working Capital", "Business Expansion", "Business Support")
    FormScaffold("New Funding Application") {
        Text("Customer", fontWeight = FontWeight.Bold)
        if (customers.isEmpty()) Text("Please add a customer first.", color = Color.Red)
        else {
            var expanded by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(customer) }
            DropdownMenu(expanded, { expanded = false }) {
                customers.forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { customer = c.name; expanded = false }) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Funding Type", fontWeight = FontWeight.Bold)
        var expandedType by remember { mutableStateOf(false) }
        OutlinedButton(onClick = { expandedType = true }, modifier = Modifier.fillMaxWidth()) { Text(type) }
        DropdownMenu(expandedType, { expandedType = false }) {
            types.forEach { t -> DropdownMenuItem(text = { Text(t) }, onClick = { type = t; expandedType = false }) }
        }
        Field("Requested Amount", amount) { amount = it }
        Button(onClick = { if (customer.isNotBlank() && amount.isNotBlank()) onSave(ApplicationItem(customer, type, amount, "New")) }, modifier = Modifier.fillMaxWidth()) {
            Text("SUBMIT APPLICATION")
        }
    }
}

@Composable
fun CollectionsScreen(apps: List<ApplicationItem>) {
    val disbursed = apps.filter { it.status == "Disbursed" }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Header("Collections", "EMI, due dates, paid, pending, overdue and payment history")
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Collection Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Paid", "₹0", Icons.Default.CheckCircle, Modifier.weight(1f))
                    StatCard("Pending", "₹0", Icons.Default.Pending, Modifier.weight(1f))
                }
            }
            items(disbursed) { ApplicationRow(it) }
            if (disbursed.isEmpty()) item { EmptyBox("No disbursed cases yet.") }
        }
    }
}

@Composable
fun ReportsScreen(customerCount: Int, apps: List<ApplicationItem>) {
    val requested = apps.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    Column(Modifier.fillMaxSize().background(Bg)) {
        Header("Reports", "Daily/monthly applications, funding, collections and outstanding")
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { ReportLine("Total Customers", customerCount.toString()) }
            item { ReportLine("Total Applications", apps.size.toString()) }
            item { ReportLine("Total Requested", "₹${"%,.0f".format(requested)}") }
            item { ReportLine("Approved / Disbursed", apps.count { it.status == "Approved" || it.status == "Disbursed" }.toString()) }
            item { ReportLine("Pending Review", apps.count { it.status == "Under Review" || it.status == "Documents Pending" }.toString()) }
            item { ReportLine("Rejected", apps.count { it.status == "Rejected" }.toString()) }
        }
    }
}

@Composable
fun ReportLine(label: String, value: String) {
    Card(shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label)
            Text(value, fontWeight = FontWeight.Bold, color = Navy)
        }
    }
}

@Composable
fun FormScaffold(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(Bg)) {
        Header(title)
        LazyColumn(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = {
            item { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }
        })
    }
}

@Composable
fun Field(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
fun EmptyBox(text: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(22.dp), color = Color.Gray)
    }
}

private fun loadCustomers(context: Context): List<Customer> = runCatching {
    val arr = JSONArray(context.getSharedPreferences("data", 0).getString("customers", "[]"))
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        Customer(o.getString("name"), o.getString("mobile"), o.getString("business"), o.getString("address"))
    }
}.getOrDefault(emptyList())

private fun saveCustomers(context: Context, list: List<Customer>) {
    val arr = JSONArray()
    list.forEach { c -> arr.put(JSONObject().apply {
        put("name", c.name); put("mobile", c.mobile); put("business", c.business); put("address", c.address)
    }) }
    context.getSharedPreferences("data", 0).edit().putString("customers", arr.toString()).apply()
}

private fun loadApps(context: Context): List<ApplicationItem> = runCatching {
    val arr = JSONArray(context.getSharedPreferences("data", 0).getString("apps", "[]"))
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        ApplicationItem(o.getString("customer"), o.getString("type"), o.getString("amount"), o.getString("status"))
    }
}.getOrDefault(emptyList())

private fun saveApps(context: Context, list: List<ApplicationItem>) {
    val arr = JSONArray()
    list.forEach { a -> arr.put(JSONObject().apply {
        put("customer", a.customer); put("type", a.type); put("amount", a.amount); put("status", a.status)
    }) }
    context.getSharedPreferences("data", 0).edit().putString("apps", arr.toString()).apply()
}
