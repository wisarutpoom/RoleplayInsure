package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AgentTelemetry
import com.example.data.StaffPermission
import com.example.data.StageAttempt
import com.example.data.WebPortalGenerator
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.viewmodel.AdminTelemetryUiState
import com.example.viewmodel.StaffPermissionsUiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminDashboardScreen(
  currentUserEmail: String,
  currentUserName: String,
  isMasterAdmin: Boolean,
  telemetryUiState: AdminTelemetryUiState,
  allAttempts: List<StageAttempt>,
  staffPermissionsUiState: StaffPermissionsUiState,
  actionMessage: String?,
  onDismissActionMessage: () -> Unit,
  onSaveStaffPermission:
    (
      existingId: String?,
      targetUserId: String?,
      staffEmail: String,
      staffName: String,
      role: String,
      status: String,
      canViewVisitsAndStages: Boolean,
      canViewScores: Boolean,
      canExportCsv: Boolean,
      canManageTeam: Boolean,
    ) -> Unit,
  onRequestSecretaryAccess: (staffEmail: String, staffName: String) -> Unit,
  onDeleteStaffPermission: (permissionId: String, staffEmail: String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  var selectedSection by rememberSaveable { mutableIntStateOf(0) }
  var localCopiedToast by remember { mutableStateOf<String?>(null) }

  val permissionsList =
    (staffPermissionsUiState as? StaffPermissionsUiState.Success)?.permissions ?: emptyList()

  // Find if the signed-in user has an approved Secretary/Sub-Admin permission record by Gmail
  val myStaffRecord =
    remember(permissionsList, currentUserEmail) {
      permissionsList.firstOrNull {
        it.staffEmail.equals(currentUserEmail.trim(), ignoreCase = true)
      }
    }

  val isApprovedSecretary = myStaffRecord?.status == "APPROVED"
  val effectiveCanViewVisitsAndStages =
    isMasterAdmin || (isApprovedSecretary && myStaffRecord?.canViewVisitsAndStages == true) || !isApprovedSecretary
  val effectiveCanViewScores =
    isMasterAdmin || (isApprovedSecretary && myStaffRecord?.canViewScores == true) || !isApprovedSecretary
  val effectiveCanExportCsv =
    isMasterAdmin || (isApprovedSecretary && myStaffRecord?.canExportCsv == true)
  val effectiveCanManageTeam =
    isMasterAdmin || (isApprovedSecretary && myStaffRecord?.canManageTeam == true)

  val telemetryList =
    (telemetryUiState as? AdminTelemetryUiState.Success)?.telemetryList ?: emptyList()

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Top Admin & Gmail Role Identity Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.dp, GoldPrimary.copy(alpha = 0.45f), RoundedCornerShape(18.dp)),
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier =
                  Modifier.size(44.dp)
                    .clip(CircleShape)
                    .background(GoldPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Default.AdminPanelSettings,
                  contentDescription = null,
                  tint = GoldPrimary,
                  modifier = Modifier.size(26.dp),
                )
              }
              Spacer(modifier = Modifier.width(12.dp))
              Column {
                Text(
                  text = "เมนูผู้ดูแลระบบ & เลขาฯ (Admin Console)",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = GoldPrimary,
                )
                Text(
                  text =
                    "เข้าสู่ระบบด้วย Gmail: ${currentUserEmail.ifBlank { "ยังไม่ได้ระบุอีเมล" }}",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            val roleBadgeText =
              when {
                isMasterAdmin -> "Master Admin"
                isApprovedSecretary ->
                  if (myStaffRecord?.role == "SUB_ADMIN") "ผู้ช่วยแอดมิน (อนุมัติแล้ว)"
                  else "เลขาฯ (อนุมัติแล้ว)"
                myStaffRecord?.status == "PENDING" -> "รออนุมัติสิทธิ์"
                else -> "โหมดส่วนตัวแทน"
              }
            val roleBadgeColor =
              when {
                isMasterAdmin || isApprovedSecretary -> EmeraldAccent
                myStaffRecord?.status == "PENDING" -> GoldPrimary
                else -> CyanWaveform
              }

            Box(
              modifier =
                Modifier.clip(RoundedCornerShape(99.dp))
                  .background(roleBadgeColor.copy(alpha = 0.18f))
                  .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Text(
                text = roleBadgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = roleBadgeColor,
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          // Sub-navigation buttons inside Admin Menu
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Button(
              onClick = { selectedSection = 0 },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor =
                    if (selectedSection == 0) GoldPrimary
                    else MaterialTheme.colorScheme.surfaceVariant,
                  contentColor =
                    if (selectedSection == 0) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("admin_subtab_telemetry"),
            ) {
              Icon(
                imageVector = Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "สถิติการเข้าเล่น & คะแนน",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }

            Button(
              onClick = { selectedSection = 1 },
              colors =
                ButtonDefaults.buttonColors(
                  containerColor =
                    if (selectedSection == 1) GoldPrimary
                    else MaterialTheme.colorScheme.surfaceVariant,
                  contentColor =
                    if (selectedSection == 1) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("admin_subtab_permissions"),
            ) {
              Icon(
                imageVector = Icons.Default.ManageAccounts,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "ตั้งค่าเลขาฯ (Gmail)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
              )
            }
          }

          if (actionMessage != null || localCopiedToast != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
              colors =
                CardDefaults.cardColors(containerColor = EmeraldAccent.copy(alpha = 0.15f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = localCopiedToast ?: actionMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall,
                  color = EmeraldAccent,
                  fontWeight = FontWeight.SemiBold,
                  modifier = Modifier.weight(1f),
                )
                OutlinedButton(
                  onClick = {
                    localCopiedToast = null
                    onDismissActionMessage()
                  },
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                ) {
                  Text("รับทราบ", style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }
        }
      }
    }

    if (selectedSection == 0) {
      // SECTION 0: TEAM TELEMETRY, STAGES PLAYED, SCORES & CSV EXPORT
      item {
        val totalAgents = telemetryList.size
        val totalVisits = telemetryList.sumOf { it.visitCount }
        val totalEvaluations = telemetryList.sumOf { it.totalAttemptsCount }
        val avgTeamScore =
          if (telemetryList.isNotEmpty()) {
            telemetryList.map { it.averageOverallScore }.average().toInt()
          } else 0
        val avgVoiceScore =
          if (telemetryList.isNotEmpty()) {
            telemetryList.map { it.averageVoiceScore }.average().toInt()
          } else 0

        Card(
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "📈 ภาพรวมการเข้าใช้งานและผลการประเมิน (ซิงค์กับเว็บไซต์ Admin)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = GoldPrimary,
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              KpiStatBox(
                label = "ตัวแทนในระบบ",
                value = "$totalAgents คน",
                color = CyanWaveform,
                modifier = Modifier.weight(1f),
              )
              KpiStatBox(
                label = "จำนวนเข้าแอพรวม",
                value = "$totalVisits ครั้ง",
                color = GoldPrimary,
                modifier = Modifier.weight(1f),
              )
              KpiStatBox(
                label = "ส่งประเมินแล้ว",
                value = "$totalEvaluations รอบ",
                color = EmeraldAccent,
                modifier = Modifier.weight(1f),
              )
            }

            if (effectiveCanViewScores) {
              Spacer(modifier = Modifier.height(8.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                KpiStatBox(
                  label = "คะแนนเฉลี่ย 5 มิติ",
                  value = "$avgTeamScore / 100",
                  color = EmeraldAccent,
                  modifier = Modifier.weight(1f),
                )
                KpiStatBox(
                  label = "คะแนนน้ำเสียงเฉลี่ย",
                  value = "$avgVoiceScore / 100",
                  color = CyanWaveform,
                  modifier = Modifier.weight(1f),
                )
              }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Button(
                onClick = {
                  if (!effectiveCanExportCsv && !isMasterAdmin) {
                    localCopiedToast =
                      "บัญชีเลขาฯ ของคุณยังไม่ได้รับสิทธิ์ดาวน์โหลด CSV กรุณาให้ Admin อนุมัติสิทธิ์"
                    return@Button
                  }
                  val csv = WebPortalGenerator.buildCsvReport(telemetryList, allAttempts)
                  val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                      type = "text/csv"
                      putExtra(Intent.EXTRA_SUBJECT, "InsureQuest_Admin_Report.csv")
                      putExtra(Intent.EXTRA_TEXT, csv)
                    }
                  context.startActivity(
                    Intent.createChooser(shareIntent, "ส่งออกรายงาน CSV (Excel / Google Sheets)")
                  )
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("export_csv_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.Download,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  text = "ส่งออกรายงาน CSV (Excel)",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                )
              }

              OutlinedButton(
                onClick = {
                  val csv = WebPortalGenerator.buildCsvReport(telemetryList, allAttempts)
                  val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("InsureQuest CSV", csv))
                  localCopiedToast = "คัดลอกตาราง CSV สำหรับวางใน Excel / Google Sheets แล้ว"
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f).testTag("copy_csv_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("คัดลอก CSV", style = MaterialTheme.typography.labelMedium)
              }
            }
          }
        }
      }

      // Per-Agent Telemetry Cards
      item {
        Text(
          text =
            if (isMasterAdmin)
              "👥 รายชื่อตัวแทนในทีม: จำนวนครั้งที่เข้า, ด่านที่เข้าเล่นแล้ว (1-25) และผลคะแนน"
            else "👤 ข้อมูลการเข้าใช้งานและด่านที่เข้าเล่นแล้ว (ซิงค์ไปยัง Admin อัตโนมัติ)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      if (telemetryList.isEmpty()) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "กำลังรวบรวมข้อมูลการเข้าใช้งานของทีมงาน...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(16.dp),
            )
          }
        }
      } else {
        items(telemetryList, key = { it.id.ifBlank { it.userId } }) { agent ->
          AgentTelemetryCard(
            telemetry = agent,
            canViewVisitsAndStages = effectiveCanViewVisitsAndStages,
            canViewScores = effectiveCanViewScores,
          )
        }
      }
    } else {
      // SECTION 1: SECRETARY & STAFF GMAIL PERMISSION MANAGER
      item {
        SecretaryPermissionManagerCard(
          currentUserEmail = currentUserEmail,
          currentUserName = currentUserName,
          isMasterAdmin = isMasterAdmin,
          canManageTeam = effectiveCanManageTeam,
          myStaffRecord = myStaffRecord,
          permissionsList = permissionsList,
          onSavePermission = onSaveStaffPermission,
          onRequestSecretaryAccess = onRequestSecretaryAccess,
          onDeletePermission = onDeleteStaffPermission,
          onSendGmailNotification = { staffPerm ->
            sendGmailApprovalIntent(context, staffPerm)
          },
        )
      }
    }
  }
}

@Composable
private fun KpiStatBox(
  label: String,
  value: String,
  color: androidx.compose.ui.graphics.Color,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier =
      modifier
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.background)
        .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
        .padding(10.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = value,
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.ExtraBold,
      color = color,
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgentTelemetryCard(
  telemetry: AgentTelemetry,
  canViewVisitsAndStages: Boolean,
  canViewScores: Boolean,
) {
  val sdf = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }
  val lastSeenText =
    telemetry.updatedAt?.toDate()?.let { sdf.format(it) }
      ?: telemetry.createdAt?.toDate()?.let { sdf.format(it) }
      ?: "เพิ่งเข้าใช้งาน"

  val enteredSet = remember(telemetry.enteredStagesCsv) { telemetry.parsedEnteredStages().toSet() }
  val passedSet =
    remember(telemetry.completedStagesCsv) { telemetry.parsedCompletedStages().toSet() }

  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    modifier = Modifier.fillMaxWidth().testTag("telemetry_card_${telemetry.userId}"),
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "${telemetry.agentName} (${telemetry.teamCode})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
          )
          Text(
            text = "ใช้งานล่าสุด: $lastSeenText",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        if (canViewVisitsAndStages) {
          Box(
            modifier =
              Modifier.clip(RoundedCornerShape(99.dp))
                .background(CyanWaveform.copy(alpha = 0.16f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text = "เข้าแอพ ${telemetry.visitCount} ครั้ง",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = CyanWaveform,
            )
          }
        }
      }

      if (canViewScores) {
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          KpiStatBox(
            label = "คะแนนสูงสุด",
            value = "${telemetry.bestOverallScore}/100",
            color = GoldPrimary,
            modifier = Modifier.weight(1f),
          )
          KpiStatBox(
            label = "คะแนนเฉลี่ยรวม",
            value = "${telemetry.averageOverallScore}/100",
            color = EmeraldAccent,
            modifier = Modifier.weight(1f),
          )
          KpiStatBox(
            label = "คะแนนน้ำเสียง",
            value = "${telemetry.averageVoiceScore}/100",
            color = CyanWaveform,
            modifier = Modifier.weight(1f),
          )
        }
      }

      if (canViewVisitsAndStages) {
        Spacer(modifier = Modifier.height(10.dp))
        Text(
          text =
            "🎯 ด่านที่เข้าเล่นแล้ว ${enteredSet.size}/25 ด่าน • สอบผ่านแล้ว ${passedSet.size}/25 ด่าน (ด่านล่าสุด: ${telemetry.lastActiveStageTitle.ifBlank { "-" }})",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))

        // 25 Stage Mini Badges (Green = Passed, Cyan = Entered, Dark = Not yet played)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          for (stageNum in 1..25) {
            val isPassed = passedSet.contains(stageNum)
            val isEntered = enteredSet.contains(stageNum)
            val badgeBg =
              when {
                isPassed -> EmeraldAccent.copy(alpha = 0.25f)
                isEntered -> CyanWaveform.copy(alpha = 0.22f)
                else -> MaterialTheme.colorScheme.background
              }
            val badgeColor =
              when {
                isPassed -> EmeraldAccent
                isEntered -> CyanWaveform
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
              }
            Box(
              modifier =
                Modifier.size(28.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(badgeBg)
                  .border(1.dp, badgeColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = "$stageNum",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecretaryPermissionManagerCard(
  currentUserEmail: String,
  currentUserName: String,
  isMasterAdmin: Boolean,
  canManageTeam: Boolean,
  myStaffRecord: StaffPermission?,
  permissionsList: List<StaffPermission>,
  onSavePermission:
    (
      existingId: String?,
      targetUserId: String?,
      staffEmail: String,
      staffName: String,
      role: String,
      status: String,
      canViewVisitsAndStages: Boolean,
      canViewScores: Boolean,
      canExportCsv: Boolean,
      canManageTeam: Boolean,
    ) -> Unit,
  onRequestSecretaryAccess: (staffEmail: String, staffName: String) -> Unit,
  onDeletePermission: (permissionId: String, staffEmail: String) -> Unit,
  onSendGmailNotification: (StaffPermission) -> Unit,
) {
  var editingPermissionId by remember { mutableStateOf<String?>(null) }
  var editingTargetUserId by remember { mutableStateOf<String?>(null) }
  var emailInput by rememberSaveable { mutableStateOf("") }
  var nameInput by rememberSaveable { mutableStateOf("") }
  var selectedRole by rememberSaveable { mutableStateOf("SECRETARY") }
  var canViewVisits by rememberSaveable { mutableStateOf(true) }
  var canViewScores by rememberSaveable { mutableStateOf(true) }
  var canExportCsv by rememberSaveable { mutableStateOf(true) }
  var canManage by rememberSaveable { mutableStateOf(false) }

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Card 1: Add / Approve Secretary via Gmail
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = GoldPrimary,
            modifier = Modifier.size(26.dp),
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "ตั้งค่าเลขาฯ & กำหนดสิทธิ์การเข้าถึงผ่าน Gmail",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = GoldPrimary,
            )
            Text(
              text =
                "เพิ่มบัญชี Gmail ของเลขาฯ หรือผู้ช่วยแอดมิน กำหนดสิทธิ์การดูข้อมูลแต่ละส่วน และส่งอีเมลอนุมัติผ่าน Gmail ได้ทันที",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (isMasterAdmin || canManageTeam) {
          OutlinedTextField(
            value = emailInput,
            onValueChange = { emailInput = it },
            label = { Text("อีเมล Gmail ของเลขาฯ / ผู้ช่วยที่จะอนุมัติสิทธิ์") },
            placeholder = { Text("เช่น secretary.team@gmail.com") },
            modifier = Modifier.fillMaxWidth().testTag("secretary_email_input"),
            singleLine = true,
          )
          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = nameInput,
            onValueChange = { nameInput = it },
            label = { Text("ชื่อ-นามสกุล / ตำแหน่งเลขาฯ") },
            placeholder = { Text("เช่น คุณพลอย (เลขาฯ ทีมประกัน)") },
            modifier = Modifier.fillMaxWidth().testTag("secretary_name_input"),
            singleLine = true,
          )
          Spacer(modifier = Modifier.height(10.dp))

          Text(
            text = "ระดับตำแหน่ง (Role):",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
          Spacer(modifier = Modifier.height(4.dp))
          FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "SECRETARY" to "เลขาฯ ช่วยดูแลทีม (Secretary)",
                "SUB_ADMIN" to "ผู้ช่วยแอดมิน (Sub-Admin)",
                "VIEWER" to "ผู้ดูรายงาน (Viewer)",
              )
              .forEach { (roleCode, roleLabel) ->
                FilterChip(
                  selected = selectedRole == roleCode,
                  onClick = { selectedRole = roleCode },
                  label = { Text(roleLabel) },
                )
              }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "กำหนดสิทธิ์การเข้าถึงข้อมูลที่อนุญาตให้เลขาฯ เห็น:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = CyanWaveform,
          )
          Spacer(modifier = Modifier.height(6.dp))

          PermissionToggleRow(
            title = "1. ดูจำนวนครั้งที่เข้าแอพ & รายชื่อด่านที่เข้าเล่นแล้ว (ด่าน 1-25)",
            checked = canViewVisits,
            onCheckedChange = { canViewVisits = it },
          )
          PermissionToggleRow(
            title = "2. ดูผลคะแนนประเมิน 5 มิติ & คะแนนวิเคราะห์น้ำเสียง AI",
            checked = canViewScores,
            onCheckedChange = { canViewScores = it },
          )
          PermissionToggleRow(
            title = "3. ดาวน์โหลดและส่งออกไฟล์รายงานสรุปผล CSV (Excel / Sheets)",
            checked = canExportCsv,
            onCheckedChange = { canExportCsv = it },
          )
          PermissionToggleRow(
            title = "4. ช่วยจัดการรายชื่อทีมและอนุมัติสิทธิ์ผู้ช่วยคนอื่น",
            checked = canManage,
            onCheckedChange = { canManage = it },
          )

          Spacer(modifier = Modifier.height(12.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                onSavePermission(
                  editingPermissionId,
                  editingTargetUserId,
                  emailInput,
                  nameInput,
                  selectedRole,
                  "APPROVED",
                  canViewVisits,
                  canViewScores,
                  canExportCsv,
                  canManage,
                )
                editingPermissionId = null
                editingTargetUserId = null
                emailInput = ""
                nameInput = ""
              },
              colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.weight(1f).testTag("approve_secretary_button"),
            ) {
              Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text =
                  if (editingPermissionId == null) "อนุมัติสิทธิ์ผ่าน Gmail ทันที"
                  else "บันทึกการแก้ไขสิทธิ์",
                fontWeight = FontWeight.Bold,
              )
            }

            if (editingPermissionId != null) {
              OutlinedButton(
                onClick = {
                  editingPermissionId = null
                  editingTargetUserId = null
                  emailInput = ""
                  nameInput = ""
                },
                shape = RoundedCornerShape(12.dp),
              ) {
                Text("ยกเลิก")
              }
            }
          }
        } else {
          // Non-Admin view: Show current Gmail status and allow requesting Secretary access
          Card(
            colors =
              CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector =
                    if (myStaffRecord?.status == "APPROVED") Icons.Default.CheckCircle
                    else Icons.Default.Lock,
                  contentDescription = null,
                  tint =
                    if (myStaffRecord?.status == "APPROVED") EmeraldAccent else GoldPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text =
                    when (myStaffRecord?.status) {
                      "APPROVED" ->
                        "บัญชี Gmail ของคุณ ($currentUserEmail) ได้รับการอนุมัติเป็นเลขาฯ แล้ว"
                      "PENDING" ->
                        "คำขอสิทธิ์เลขาฯ สำหรับ $currentUserEmail อยู่ระหว่างรอ Admin อนุมัติ"
                      else ->
                        "หากคุณเป็นเลขาฯ หรือผู้ช่วยทีม กดส่งคำขออนุมัติสิทธิ์ด้วย Gmail ของคุณด้านล่าง"
                    },
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                )
              }

              if (myStaffRecord == null || myStaffRecord.status != "APPROVED") {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                  onClick = { onRequestSecretaryAccess(currentUserEmail, currentUserName) },
                  colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth().testTag("request_secretary_access_button"),
                ) {
                  Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = "ขออนุมัติสิทธิ์เลขาฯ ด้วย Gmail ($currentUserEmail)",
                    fontWeight = FontWeight.Bold,
                  )
                }
              }
            }
          }
        }
      }
    }

    // Card 2: List of Approved / Pending Secretaries
    Card(
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      shape = RoundedCornerShape(18.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "📋 รายชื่อเลขาฯ และผู้ช่วยที่ผูกกับ Gmail (${permissionsList.size} บัญชี)",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = GoldPrimary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (permissionsList.isEmpty()) {
          Text(
            text =
              "ยังไม่มีรายชื่อเลขาฯ ในระบบ คุณสามารถกรอกอีเมล Gmail ของเลขาฯ ด้านบนแล้วกด 'อนุมัติสิทธิ์ผ่าน Gmail ทันที' ได้เลย",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          permissionsList.forEachIndexed { idx, perm ->
            if (idx > 0) {
              HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            }
            Column(modifier = Modifier.fillMaxWidth()) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "${perm.staffName} (${perm.role})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                  )
                  Text(
                    text = "✉️ Gmail: ${perm.staffEmail}",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanWaveform,
                  )
                  Text(
                    text = "ผู้อนุมัติ: ${perm.approvedByEmail.ifBlank { "-" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }

                val statusColor =
                  when (perm.status) {
                    "APPROVED" -> EmeraldAccent
                    "PENDING" -> GoldPrimary
                    else -> CoralAlert
                  }
                val statusLabel =
                  when (perm.status) {
                    "APPROVED" -> "อนุมัติแล้ว"
                    "PENDING" -> "รออนุมัติ"
                    else -> "ระงับสิทธิ์"
                  }

                Box(
                  modifier =
                    Modifier.clip(RoundedCornerShape(99.dp))
                      .background(statusColor.copy(alpha = 0.18f))
                      .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                  Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                  )
                }
              }

              Spacer(modifier = Modifier.height(6.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                PermissionChipBadge("ดูจำนวนเข้า & ด่าน 1-25", perm.canViewVisitsAndStages)
                PermissionChipBadge("ดูคะแนน 5 มิติ & น้ำเสียง", perm.canViewScores)
                PermissionChipBadge("ดาวน์โหลด CSV", perm.canExportCsv)
                PermissionChipBadge("จัดการสิทธิ์ทีม", perm.canManageTeam)
              }

              if (isMasterAdmin || canManageTeam) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  if (perm.status != "APPROVED") {
                    Button(
                      onClick = {
                        onSavePermission(
                          perm.id,
                          perm.userId,
                          perm.staffEmail,
                          perm.staffName,
                          perm.role,
                          "APPROVED",
                          perm.canViewVisitsAndStages,
                          perm.canViewScores,
                          perm.canExportCsv,
                          perm.canManageTeam,
                        )
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                      contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                      shape = RoundedCornerShape(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                      )
                      Spacer(modifier = Modifier.width(4.dp))
                      Text("อนุมัติทันที", style = MaterialTheme.typography.labelSmall)
                    }
                  }

                  OutlinedButton(
                    onClick = { onSendGmailNotification(perm) },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                  ) {
                    Icon(
                      imageVector = Icons.Default.Email,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ส่ง Gmail แจ้งสิทธิ์", style = MaterialTheme.typography.labelSmall)
                  }

                  IconButton(
                    onClick = {
                      editingPermissionId = perm.id
                      editingTargetUserId = perm.userId
                      emailInput = perm.staffEmail
                      nameInput = perm.staffName
                      selectedRole = perm.role
                      canViewVisits = perm.canViewVisitsAndStages
                      canViewScores = perm.canViewScores
                      canExportCsv = perm.canExportCsv
                      canManage = perm.canManageTeam
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.Edit,
                      contentDescription = "แก้ไขสิทธิ์เลขาฯ",
                      tint = GoldPrimary,
                    )
                  }

                  IconButton(onClick = { onDeletePermission(perm.id, perm.staffEmail) }) {
                    Icon(
                      imageVector = Icons.Default.Delete,
                      contentDescription = "ลบสิทธิ์เลขาฯ",
                      tint = CoralAlert,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun PermissionToggleRow(
  title: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f).padding(end = 8.dp),
    )
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun PermissionChipBadge(label: String, enabled: Boolean) {
  val color =
    if (enabled) EmeraldAccent
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
  Box(
    modifier =
      Modifier.clip(RoundedCornerShape(6.dp))
        .background(color.copy(alpha = 0.14f))
        .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
        .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = if (enabled) "✓ $label" else "✕ $label",
      style = MaterialTheme.typography.labelSmall,
      color = color,
      fontWeight = FontWeight.SemiBold,
    )
  }
}

private fun sendGmailApprovalIntent(context: Context, perm: StaffPermission) {
  val grantedList = buildList {
    if (perm.canViewVisitsAndStages) add("- ดูจำนวนครั้งที่เข้าแอพและด่านที่เข้าเล่นแล้ว (ด่าน 1-25)")
    if (perm.canViewScores) add("- ดูผลคะแนนประเมิน 5 มิติและวิเคราะห์น้ำเสียง AI")
    if (perm.canExportCsv) add("- ดาวน์โหลดรายงานสรุปผลทีมเป็นไฟล์ CSV (Excel / Google Sheets)")
    if (perm.canManageTeam) add("- ช่วยจัดการรายชื่อทีมและอนุมัติสิทธิ์")
  }
  val subject = "อนุมัติสิทธิ์ผู้ช่วย/เลขาฯ ระบบ InsureQuest Admin (${perm.staffName})"
  val body =
    """
    สวัสดีคุณ ${perm.staffName} (${perm.staffEmail}),

    ผู้ดูแลระบบได้อนุมัติสิทธิ์การใช้งานเมนู Admin ของระบบฝึกซ้อมตัวแทนประกัน InsureQuest ให้กับบัญชี Gmail ของคุณเรียบร้อยแล้ว

    สถานะ: ${perm.status} (${perm.role})
    สิทธิ์การเข้าถึงที่ได้รับอนุมัติ:
    ${grantedList.joinToString("\n")}

    วิธีเข้าใช้งาน (ใช้โดเมนเดียวเข้าได้ทั้งหน้าฝึกซ้อมและเมนู Admin):
    1. เปิดผ่านโดเมนหลักของทีม: https://${WebPortalGenerator.DEFAULT_CUSTOM_DOMAIN}
       (หรือลิงก์ตรง: ${WebPortalGenerator.SHARED_WEB_APP_URL})
    2. กดปุ่ม "เข้าสู่ระบบด้วยบัญชี Google (Gmail)" โดยใช้อีเมล ${perm.staffEmail}
    3. ไปที่แถบเมนู "เมนู Admin" ด้านล่างในเว็บแอพเพื่อดูสถิติทีมงานและดาวน์โหลดรายงานได้ทันที
    """
      .trimIndent()

  val intent =
    Intent(Intent.ACTION_SENDTO).apply {
      data = Uri.parse("mailto:${Uri.encode(perm.staffEmail)}")
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, body)
    }
  runCatching {
    context.startActivity(Intent.createChooser(intent, "ส่งอีเมลแจ้งอนุมัติสิทธิ์ผ่าน Gmail"))
  }
}
