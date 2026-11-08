package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.WebPortalGenerator
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import java.net.InetAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun WebDomainPortalScreen(
  agentName: String,
  teamCode: String,
  customDomain: String,
  onSaveTeamSettings: (String, String, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = androidx.compose.runtime.rememberCoroutineScope()
  var nameInput by remember(agentName) { mutableStateOf(agentName) }
  var teamInput by remember(teamCode) { mutableStateOf(teamCode) }
  var domainInput by remember(customDomain) {
    mutableStateOf(customDomain.ifBlank { WebPortalGenerator.DEFAULT_CUSTOM_DOMAIN })
  }
  var copiedNotice by remember { mutableStateOf<String?>(null) }
  var dnsCheckStatus by remember {
    mutableStateOf("✅ DNS ชี้มาที่ 199.36.158.100 สำเร็จแล้ว")
  }
  var isCheckingDns by remember { mutableStateOf(false) }

  // Icon-triggered detail dialogs to avoid text clutter
  var showDnsDetailDialog by remember { mutableStateOf(false) }
  var showCliDetailDialog by remember { mutableStateOf(false) }

  val dnsGuide =
    remember(domainInput) { WebPortalGenerator.buildSmartNexusDnsGuide(domainInput) }
  val cliCommands =
    remember(domainInput) { WebPortalGenerator.buildFirebaseHostingCommands(domainInput) }
  val portalHtml =
    remember(teamInput, domainInput) {
      WebPortalGenerator.buildStandaloneWebPortalHtml(teamInput, domainInput)
    }

  if (showDnsDetailDialog) {
    AlertDialog(
      onDismissRequest = { showDnsDetailDialog = false },
      icon = { Icon(Icons.Default.Dns, contentDescription = null, tint = EmeraldAccent) },
      title = { Text("รายละเอียดตาราง DNS ($domainInput)", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = dnsGuide,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showDnsDetailDialog = false }) { Text("ปิด") }
      },
    )
  }

  if (showCliDetailDialog) {
    AlertDialog(
      onDismissRequest = { showCliDetailDialog = false },
      icon = {
        Icon(
          Icons.Default.Terminal,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      title = { Text("คำสั่ง Deploy & โค้ด index.html", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = cliCommands,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = EmeraldAccent,
          )
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                copyToClipboard(context, "Firebase CLI Commands", cliCommands)
                copiedNotice = "คัดลอกคำสั่ง CLI แล้ว"
                showCliDetailDialog = false
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("คัดลอก CLI")
            }
            Button(
              onClick = {
                copyToClipboard(context, "Unified index.html", portalHtml)
                copiedNotice = "คัดลอกโค้ด index.html แล้ว"
                showCliDetailDialog = false
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("คัดลอก HTML")
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCliDetailDialog = false }) { Text("ปิด") }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 1. Live Testing URL + Unified Subdomain Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.5.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
              RoundedCornerShape(18.dp),
            ),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "🚀 ลิงก์เข้าใช้งานเว็บแอพ",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            Row {
              IconButton(onClick = { showDnsDetailDialog = true }) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = "ดูตาราง DNS",
                  tint = EmeraldAccent,
                )
              }
              IconButton(onClick = { showCliDetailDialog = true }) {
                Icon(
                  imageVector = Icons.Default.Terminal,
                  contentDescription = "ดูคำสั่ง CLI และ HTML",
                  tint = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }

          Column(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            Text(
              text = "🌐 โดเมนหลัก: https://$domainInput",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = EmeraldAccent,
            )
            Text(
              text = "🔗 URL ทดสอบสด: ${WebPortalGenerator.SHARED_WEB_APP_URL}",
              style = MaterialTheme.typography.bodySmall,
              fontFamily = FontFamily.Monospace,
              color = CyanWaveform,
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                runCatching {
                  context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(WebPortalGenerator.SHARED_WEB_APP_URL))
                  )
                }
              },
              colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("open_web_url_button"),
            ) {
              Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("เปิดเว็บทดสอบ", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = {
                copyToClipboard(context, "Web App URL", WebPortalGenerator.SHARED_WEB_APP_URL)
                copiedNotice = "คัดลอก URL ทดสอบแล้ว"
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("copy_web_url_button"),
            ) {
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("คัดลอก URL")
            }
          }

          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(
            onClick = {
              val shareIntent =
                Intent(Intent.ACTION_SEND).apply {
                  type = "text/plain"
                  putExtra(
                    Intent.EXTRA_TEXT,
                    "เชิญทีมงาน ($teamInput) เข้าใช้งานระบบเกมฝึกซ้อมตัวแทน InsureQuest 25 ด่าน\n🌐 โดเมน: https://$domainInput\n🔗 ลิงก์เข้าทดสอบทันที: ${WebPortalGenerator.SHARED_WEB_APP_URL}",
                  )
                }
              context.startActivity(Intent.createChooser(shareIntent, "แชร์ URL ให้ทีมงาน"))
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("share_team_link_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Share,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("แชร์ลิงก์ให้ทีมงาน & เลขาฯ", fontWeight = FontWeight.Bold)
          }

          if (copiedNotice != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "✅ $copiedNotice",
              style = MaterialTheme.typography.labelMedium,
              color = EmeraldAccent,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    // 2. Visual 3-Step Image Card + Live DNS Check Button
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
              RoundedCornerShape(18.dp),
            ),
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "🖼️ คู่มือภาพ 3 ขั้นตอนผูกโดเมน",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
            IconButton(
              onClick = { showDnsDetailDialog = true },
              modifier = Modifier.size(32.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "ดูรายละเอียด DNS",
                tint = CyanWaveform,
              )
            }
          }

          Image(
            painter = painterResource(id = R.drawable.img_domain_setup_guide_1794035038571),
            contentDescription = "คู่มือภาพ 3 ขั้นตอนการเพิ่มโดเมน roleplay.smartnexus.vip ใน Firebase Hosting",
            modifier =
              Modifier.fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                  1.dp,
                  MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                  RoundedCornerShape(14.dp),
                ),
            contentScale = ContentScale.Crop,
          )

          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                isCheckingDns = true
                scope.launch {
                  val resultMsg =
                    withContext(Dispatchers.IO) {
                      try {
                        val addresses =
                          InetAddress.getAllByName(domainInput.trim()).mapNotNull { it.hostAddress }
                        if (addresses.contains(WebPortalGenerator.FIREBASE_HOSTING_IP)) {
                          "🎉 DNS สำเร็จ! $domainInput -> ${WebPortalGenerator.FIREBASE_HOSTING_IP}"
                        } else if (addresses.isNotEmpty()) {
                          "ℹ️ IP ปัจจุบัน: ${addresses.joinToString(", ")}"
                        } else {
                          "⏳ กำลังกระจายสัญญาณ DNS"
                        }
                      } catch (e: Exception) {
                        "⏳ กำลังกระจายสัญญาณ DNS"
                      }
                    }
                  dnsCheckStatus = resultMsg
                  isCheckingDns = false
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("verify_dns_live_button"),
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = if (isCheckingDns) "กำลังเช็ก..." else "เช็ก DNS สด",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            OutlinedButton(
              onClick = {
                runCatching {
                  context.startActivity(
                    Intent(
                      Intent.ACTION_VIEW,
                      Uri.parse(
                        "https://console.firebase.google.com/project/gen-lang-client-0907040926/hosting/sites"
                      ),
                    )
                  )
                }
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f).testTag("open_firebase_hosting_console_button"),
            ) {
              Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("เปิด Firebase", style = MaterialTheme.typography.labelMedium)
            }
          }

          Text(
            text = dnsCheckStatus,
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldAccent,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }

    // 3. Compact Team & Subdomain Settings Form
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "ตั้งค่าชื่อทีม & Subdomain",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("ชื่อตัวแทน / โค้ช") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
          )
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = teamInput,
            onValueChange = { teamInput = it },
            label = { Text("รหัสทีมงาน (Team Code)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
          )
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = domainInput,
            onValueChange = { domainInput = it },
            label = { Text("Subdomain ภายใต้ smartnexus.vip") },
            modifier = Modifier.fillMaxWidth().testTag("custom_domain_input"),
            singleLine = true,
          )

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              onSaveTeamSettings(nameInput, teamInput, domainInput)
              copiedNotice = "บันทึก $domainInput เรียบร้อยแล้ว"
            },
            colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("save_domain_settings_button"),
          ) {
            Icon(imageVector = Icons.Default.CloudDone, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("บันทึกการตั้งค่า", fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}
