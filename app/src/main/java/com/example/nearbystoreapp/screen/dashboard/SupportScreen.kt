package com.example.nearbystoreapp.screen.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nearbystoreapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class FaqModel(
    val id       : String = "",
    val question : String = "",
    val answer   : String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var faqs      by remember { mutableStateOf<List<FaqModel>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseDatabase.getInstance().reference
            .child("faqs")
            .get()
            .addOnSuccessListener { snapshot ->
                val list = mutableListOf<FaqModel>()
                for (faq in snapshot.children) {
                    list.add(FaqModel(
                        id       = faq.key ?: "",
                        question = faq.child("question").value?.toString() ?: "",
                        answer   = faq.child("answer").value?.toString() ?: ""
                    ))
                }
                faqs = list
                isLoading = false
            }
            .addOnFailureListener { isLoading = false }
    }

    Scaffold(
        containerColor = colorResource(R.color.black2),
        topBar = {
            TopAppBar(
                title = {
                    Text("Help & Support", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = colorResource(R.color.gold))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colorResource(R.color.black2))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // ── Header ──
            item {
                Column(
                    modifier            = Modifier.fillMaxWidth().background(colorResource(R.color.black3), RoundedCornerShape(14.dp)).padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "🛠️", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("We Are Here To Help!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Scroll Down For Frequently Asked Questions", color = Color.Gray, fontSize = 13.sp)
                }
            }

            // ── FAQ Title ──
            item {
                Text("❓ Frequently Asked Questions", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 8.dp))
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = colorResource(R.color.gold))
                    }
                }
            } else if (faqs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().background(colorResource(R.color.black3), RoundedCornerShape(12.dp)).padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No FAQ available Yet", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(faqs, key = { it.id }) { faq -> FaqCard(faq = faq) }
            }

            // ── Contact Us Title ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("📞 Contact Us", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            // ── WhatsApp ──
            item {
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/918650920060?text=Hello, I Want Help ")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Text("💬", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message On WhatsApp", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // ── Email ──
            item {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:kurmiabhi027@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Support Request - One Mart")
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = colorResource(R.color.gold))
                ) {
                    Text("📧", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Email", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }

            // ── Problem Report Title ──
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("📩 Send Your Problem", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Your Message Will Be Sent To Admin", color = Color.Gray, fontSize = 12.sp)
            }

            // ── Report Form ──
            item {
                ReportForm()
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun ReportForm() {
    var problemText      by remember { mutableStateOf("") }
    var isSending        by remember { mutableStateOf(false) }
    var isSent           by remember { mutableStateOf(false) }
    var myReports        by remember { mutableStateOf<List<Map<String, String>>>(emptyList()) }
    var loadingMyReports by remember { mutableStateOf(true) }

    val uid   = FirebaseAuth.getInstance().currentUser?.uid   ?: ""
    val email = FirebaseAuth.getInstance().currentUser?.email ?: "Unknown"

    LaunchedEffect(uid) {
        if (uid.isNotEmpty()) {
            FirebaseDatabase.getInstance().reference
                .child("reports")
                .get()
                .addOnSuccessListener { snapshot ->
                    val list = mutableListOf<Map<String, String>>()
                    for (report in snapshot.children) {
                        val reportedUser = report.child("reportedUser").value?.toString() ?: ""
                        if (reportedUser == uid) {
                            list.add(mapOf(
                                "reason"     to (report.child("reason").value?.toString()     ?: ""),
                                "status"     to (report.child("status").value?.toString()     ?: "open"),
                                "adminReply" to (report.child("adminReply").value?.toString() ?: "")
                            ))
                        }
                    }
                    myReports        = list
                    loadingMyReports = false
                }
                .addOnFailureListener { loadingMyReports = false }
        } else {
            loadingMyReports = false
        }
    }

    Column(
        modifier            = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Purane reports ──
        if (loadingMyReports) {
            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colorResource(R.color.gold), modifier = Modifier.size(24.dp))
            }
        } else if (myReports.isNotEmpty()) {
            Text("📋 Your Old Messages", color = colorResource(R.color.gold), fontWeight = FontWeight.Bold, fontSize = 14.sp)

            myReports.forEach { report ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorResource(R.color.black3), RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("❓ ${report["reason"]}", color = Color.White, fontSize = 13.sp)
                    Text(
                        if (report["status"] == "resolved") "✅ Resolved" else "🔴 Pending",
                        color    = if (report["status"] == "resolved") Color.Green else Color.Red,
                        fontSize = 12.sp
                    )
                    if (report["adminReply"]?.isNotEmpty() == true) {
                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                        Text("🛡️ Admin's Answer:", color = colorResource(R.color.gold), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(report["adminReply"] ?: "", color = Color.White, fontSize = 13.sp)
                    } else {
                        Text("⏳ Waiting For Admin's Reply...", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
        }


        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.black3), RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isSent) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✅", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Your Message Sent!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Thank you for contacting us!", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { isSent = false; problemText = "" }) {
                        Text("Try Again", color = colorResource(R.color.gold))
                    }
                }
            } else {
                Text("📩 Send Your Problem", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value         = problemText,
                    onValueChange = { problemText = it },
                    placeholder   = { Text("Type your problem here...", color = Color.Gray) },
                    modifier      = Modifier.fillMaxWidth().height(120.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = colorResource(R.color.gold),
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = colorResource(R.color.gold)
                    ),
                    maxLines = 5
                )
                Button(
                    onClick = {
                        if (problemText.isNotEmpty()) {
                            isSending = true
                            val db       = FirebaseDatabase.getInstance().reference
                            val reportId = db.child("reports").push().key ?: return@Button
                            val report   = mapOf(
                                "reportId"     to reportId,
                                "reason"       to problemText,
                                "reportedBy"   to email,
                                "reportedUser" to uid,
                                "status"       to "open",
                                "adminReply"   to "",
                                "createdAt"    to System.currentTimeMillis()
                            )
                            db.child("reports").child(reportId).setValue(report)
                                .addOnSuccessListener { isSending = false; isSent = true }
                                .addOnFailureListener { isSending = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    enabled  = problemText.isNotEmpty() && !isSending,
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = colorResource(R.color.gold),
                        disabledContainerColor = Color.Gray
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(20.dp))
                    } else {
                        Text("📨 Send", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun FaqCard(faq: FaqModel) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.black3), RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth()
        ) {
            Text(faq.question, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(
                imageVector        = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint               = colorResource(R.color.gold)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(faq.answer, color = Color.Gray, fontSize = 13.sp, lineHeight = 20.sp)
            }
        }
    }
}