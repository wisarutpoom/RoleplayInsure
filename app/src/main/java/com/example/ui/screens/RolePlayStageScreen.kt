package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.GameAudioAndLipSyncEngine
import com.example.audio.VoiceAnalyzerRecorder
import com.example.audio.VoiceCallState
import com.example.data.ChatTurn
import com.example.data.EvaluationResult
import com.example.data.FreeTierQuotaGuard
import com.example.data.RolePlayStage
import com.example.data.VoiceRecordingMetrics
import com.example.ui.components.DualAvatarRolePlayStageView
import com.example.ui.components.FiveDimensionRadarAndBars
import com.example.ui.components.LiveVoiceWaveformBar
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.viewmodel.RolePlayAiState

@Composable
fun RolePlayStageScreen(
  stage: RolePlayStage,
  chatTurns: List<ChatTurn>,
  aiState: RolePlayAiState,
  evaluationResult: EvaluationResult?,
  agentName: String = "จอมยุทธ์นักขาย",
  useBuiltInOnDeviceAi: Boolean = false,
  quotaExhaustedPopupMessage: String? = null,
  onDismissQuotaExhaustedPopup: () -> Unit = {},
  onSimulateQuotaExhausted: () -> Unit = {},
  onResetQuotaCounter: () -> Unit = {},
  onToggleAiMode: () -> Unit = {},
  onSendTurn: (String, ByteArray?, VoiceRecordingMetrics?) -> Unit,
  onSimulateVoicePattern: (String) -> Unit = {},
  onEvaluateStage: () -> Unit,
  onReplayStage: () -> Unit,
  onNextStage: () -> Unit,
  onBack: () -> Unit,
  onDismissError: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler { onBack() }

  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val voiceRecorder = remember(context) { VoiceAnalyzerRecorder(context) }
  val gameEngine = remember(context) { GameAudioAndLipSyncEngine(context, scope) }

  val isRecording by voiceRecorder.isRecording.collectAsStateWithLifecycle()
  val currentAmplitude by voiceRecorder.currentAmplitude.collectAsStateWithLifecycle()
  val elapsedSeconds by voiceRecorder.elapsedSeconds.collectAsStateWithLifecycle()

  val isBgmEnabled by gameEngine.isBgmEnabled.collectAsStateWithLifecycle()
  val isCustomerVoiceEnabled by gameEngine.isCustomerVoiceEnabled.collectAsStateWithLifecycle()
  val isLiveCallMode by gameEngine.isLiveCallMode.collectAsStateWithLifecycle()
  val voiceCallState by gameEngine.voiceCallState.collectAsStateWithLifecycle()
  val isListeningRealTime by gameEngine.isListeningRealTime.collectAsStateWithLifecycle()
  val livePartialTranscript by gameEngine.livePartialTranscript.collectAsStateWithLifecycle()
  val liveWpmEstimate by gameEngine.liveWpmEstimate.collectAsStateWithLifecycle()
  val isCustomerSpeaking by gameEngine.isCustomerSpeaking.collectAsStateWithLifecycle()
  val customerViseme by gameEngine.customerViseme.collectAsStateWithLifecycle()
  val customerMouthOpenness by gameEngine.customerMouthOpenness.collectAsStateWithLifecycle()
  val isAgentSpeaking by gameEngine.isAgentSpeaking.collectAsStateWithLifecycle()
  val agentViseme by gameEngine.agentViseme.collectAsStateWithLifecycle()
  val agentMouthOpenness by gameEngine.agentMouthOpenness.collectAsStateWithLifecycle()
  val quotaStatus by FreeTierQuotaGuard.quotaStatus.collectAsStateWithLifecycle()

  var messageInput by remember { mutableStateOf("") }
  var permissionDeniedMessage by remember { mutableStateOf<String?>(null) }

  var showStageInfoDialog by remember { mutableStateOf(false) }
  var showTipsDialog by remember { mutableStateOf(false) }
  var showQuotaDialog by remember { mutableStateOf(false) }

  val agentTurnCount = remember(chatTurns) { chatTurns.count { it.isAgent } }
  val trustScore =
    remember(agentTurnCount, stage.difficulty) {
      val base = 52 - (stage.difficulty * 3)
      (base + agentTurnCount * 14).coerceIn(35, 98)
    }

  val startRealTimeTurnListening = {
    val hasPerm =
      ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
        PackageManager.PERMISSION_GRANTED
    if (hasPerm) {
      gameEngine.startRealTimeListening { spokenText, wpm, isFast, isQuiet ->
        val rtMetrics =
          VoiceRecordingMetrics(
            durationSeconds = 4,
            speechRateLabel =
              if (isFast) "พูดเร็วเกินไป ($wpm WPM > 165)"
              else "จังหวะพอดีเป็นธรรมชาติ ($wpm WPM)",
            energyLevelLabel =
              if (isQuiet) "เสียงเบาเกินไป ควรเพิ่มพลังเสียง"
              else "ระดับเสียงอบอุ่น มั่นคง น่าเชื่อถือ",
            pauseBalanceLabel =
              if (isFast) "พูดรัวติดกัน ควรเว้นจังหวะท้ายประโยค"
              else "การเว้นจังหวะหายใจสมดุลดี",
            estimatedWordsPerMinute = wpm,
            confidenceIndex = if (isFast || isQuiet) 72 else 92,
            avgAmplitude = if (isQuiet) 1800 else 7600,
            isTooFast = isFast,
            isTooQuiet = isQuiet,
            voiceCoachingTip =
              VoiceAnalyzerRecorder.buildVoiceCoachingFeedbackTip(
                wpm = wpm,
                isTooFast = isFast,
                isTooQuiet = isQuiet,
              ),
          )
        onSendTurn(spokenText, null, rtMetrics)
      }
    }
  }

  DisposableEffect(Unit) {
    gameEngine.playStageStartSfx()
    onDispose {
      if (voiceRecorder.isRecording.value) {
        voiceRecorder.stopAndAnalyze()
      }
      gameEngine.release()
    }
  }

  LaunchedEffect(currentAmplitude, isRecording) {
    if (isRecording) {
      gameEngine.updateAgentMicAmplitude(currentAmplitude, true)
    }
  }

  val lastTurn = chatTurns.lastOrNull()
  LaunchedEffect(lastTurn?.id) {
    if (lastTurn != null) {
      if (!lastTurn.isAgent) {
        gameEngine.playCustomerReplySfx()
        gameEngine.speakAndLipSyncCustomerLine(
          customerText = lastTurn.text,
          customerAge = stage.customerAge,
          customerName = stage.customerName,
          onCustomerFinished = {
            if (isLiveCallMode && evaluationResult == null) {
              startRealTimeTurnListening()
            }
          },
        )
      } else if (!lastTurn.voiceCoachingAlert.isNullOrBlank()) {
        val alertText = lastTurn.voiceCoachingAlert
        when {
          alertText.contains("พูดเร็ว") || alertText.contains("> 165") ->
            gameEngine.playFastSpeechCoachingPattern()
          alertText.contains("เสียงเบา") ->
            gameEngine.playQuietVoiceCoachingPattern()
          else ->
            gameEngine.playBalancedVoiceCoachingPattern()
        }
      }
    }
  }

  LaunchedEffect(evaluationResult) {
    if (evaluationResult != null) {
      gameEngine.stopRealTimeListening()
      if (evaluationResult.passed) {
        gameEngine.playVictoryFanfareSfx()
      } else {
        gameEngine.playComboUpSfx()
      }
    }
  }

  val audioPermissionLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      if (granted) {
        permissionDeniedMessage = null
        if (isLiveCallMode) {
          startRealTimeTurnListening()
        } else {
          gameEngine.playAgentMicSfx()
          voiceRecorder.startRecording(scope)
        }
      } else {
        permissionDeniedMessage =
          "ต้องการสิทธิ์ไมโครโฟนเพื่อพูดคุยสด (หรือแตะปุ่มจำลองการพูดด้านล่างแทนได้ทันที)"
      }
    }

  val listState = rememberLazyListState()
  LaunchedEffect(chatTurns.size, evaluationResult) {
    if (chatTurns.isNotEmpty()) {
      listState.animateScrollToItem(chatTurns.size - 1)
    }
  }

  // 0. Popup แจ้งเตือนทันทีเมื่อโควต้าฟรี Gemini API หมด พร้อมระบุว่าวันพรุ่งนี้กลับมาต่อได้กี่โมง!
  if (quotaExhaustedPopupMessage != null) {
    AlertDialog(
      onDismissRequest = onDismissQuotaExhaustedPopup,
      icon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = CoralAlert) },
      title = {
        Text(
          text = "🛑 โควต้าฟรี Gemini API วันนี้ครบแล้ว (0.00฿)",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.ExtraBold,
          color = CoralAlert,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = quotaExhaustedPopupMessage,
            style = MaterialTheme.typography.bodySmall,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(EmeraldAccent.copy(alpha = 0.14f))
                .padding(10.dp)
          ) {
            Text(
              text = "💡 ระหว่างรอรีเซ็ตโควต้าฟรีของพรุ่งนี้ ระบบได้สลับเป็น AI สำรองในแอพให้คุณฝึกซ้อมต่อได้ฟรีทันทีโดยไม่มีสะดุด!",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = EmeraldAccent,
            )
          }
        }
      },
      confirmButton = {
        Button(onClick = onDismissQuotaExhaustedPopup) {
          Text("รับทราบ (ซ้อมต่อฟรี)")
        }
      },
      dismissButton = {
        TextButton(onClick = onResetQuotaCounter) {
          Text("รีเซ็ตตัวนับทดสอบ")
        }
      },
    )
  }

  // 1. Dialog แฟ้มลับเตรียมตัวก่อนคุย (เปิดเมื่อกดไอคอน ℹ️)
  if (showStageInfoDialog) {
    AlertDialog(
      onDismissRequest = { showStageInfoDialog = false },
      icon = { Icon(Icons.Default.Info, contentDescription = null, tint = GoldPrimary) },
      title = {
        Text(
          text = "📂 แฟ้มข้อมูลเตรียมตัว: ${stage.title}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "👤 คู่สนทนา: ${stage.customerName} (${stage.customerAge} ปี • ${stage.customerOccupation})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
          )
          Text(
            text = "🎭 นิสัย & ท่าที: ${stage.customerPersona}",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "🔍 ข้อมูลลับทางการเงินที่ต้องค้นหา: ${stage.hiddenFinancialFact}",
            style = MaterialTheme.typography.bodySmall,
            color = CoralAlert,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "🎯 เป้าหมายภารกิจ: ${stage.missionObjective}",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldAccent,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "🧭 หมวดความคุ้มครองที่ต้องชี้เป้า: ${stage.targetInsuranceDirection}",
            style = MaterialTheme.typography.bodySmall,
            color = CyanWaveform,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showStageInfoDialog = false }) { Text("พร้อมลุย!") }
      },
    )
  }

  // 2. Dialog เทคนิคการพูด & ประโยคแนะนำพูดสด (เปิดเมื่อกดไอคอน 💡)
  if (showTipsDialog) {
    AlertDialog(
      onDismissRequest = { showTipsDialog = false },
      icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldPrimary) },
      title = {
        Text(
          text = "💡 คัมภีร์พูดพิชิตใจ ${stage.customerName}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          stage.keyTips.forEachIndexed { idx, tip ->
            Text(text = "${idx + 1}. $tip", style = MaterialTheme.typography.bodySmall)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "⚡ แตะเพื่อส่งเสียงพูดโต้ตอบกับ Gemini API ทันที:",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
          )
          OutlinedButton(
            onClick = {
              showTipsDialog = false
              val line =
                "เข้าใจเลยครับคุณ${stage.customerName.substringBefore(" ")} วันนี้ผมไม่ได้มาเสนอขายแต่อยากขออนุญาตแลกเปลี่ยนมุมมองสั้นๆ ไม่เกิน 15 นาทีครับ"
              gameEngine.animateAgentSpeakingText(line)
              onSendTurn(line, null, null)
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("💬 พูดประโยคเปิดใจลดแรงต้านทันที", style = MaterialTheme.typography.labelMedium)
          }
          OutlinedButton(
            onClick = {
              showTipsDialog = false
              val line =
                "จากข้อมูลเรื่อง ${stage.hiddenFinancialFact.take(35)} ผมแนะนำให้วางโครงสร้างหมวด ${stage.targetInsuranceDirection} เป็นอันดับแรกครับ"
              gameEngine.animateAgentSpeakingText(line)
              onSendTurn(line, null, null)
            },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text("🎯 พูดประโยคสรุปชี้เป้าหมวดประกันทันที", style = MaterialTheme.typography.labelMedium)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showTipsDialog = false }) { Text("ปิด") }
      },
    )
  }

  // 3. Dialog สถานะโควต้าฟรี Gemini API & เวลารีเซ็ตตามเวลาไทย (เปิดเมื่อกดไอคอน 🛡️)
  if (showQuotaDialog) {
    AlertDialog(
      onDismissRequest = { showQuotaDialog = false },
      icon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = EmeraldAccent) },
      title = {
        Text(
          text = "🛡️ โควต้าฟรี Gemini API & เวลารีเซ็ต",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text =
              if (!useBuiltInOnDeviceAi)
                "✨ เชื่อมต่อ: ${quotaStatus.modelName} (ไม่คิดเงิน 100%)"
              else
                "🧠 เชื่อมต่อ: AI สำรองฝังในแอพ (ไม่ใช้โควต้า API)",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = EmeraldAccent,
          )
          Text(
            text = "• โควต้ารายนาที: ${quotaStatus.rpmUsed} / ${quotaStatus.rpmLimit} ครั้งต่อนาที",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "• โควต้ารายวันวันนี้: ${quotaStatus.rpdUsed} / ${quotaStatus.rpdLimit} ครั้งต่อวัน",
            style = MaterialTheme.typography.bodySmall,
          )
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(GoldPrimary.copy(alpha = 0.14f))
                .padding(10.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                text = "🕒 เวลาที่รีเซ็ตโควต้าฟรีรอบถัดไป:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
              )
              Text(
                text = "👉 ${quotaStatus.nextResetTimeThaiLabel}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = "(Google รีเซ็ตโควต้าฟรีทุกวันเวลาเที่ยงคืนแปซิฟิก 00:00 PT ซึ่งตรงกับ 15:00 น. ตามเวลาไทยในช่วง พ.ย.-มี.ค. และ 14:00 น. ในช่วง มี.ค.-พ.ย.)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = {
                showQuotaDialog = false
                onSimulateQuotaExhausted()
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Text("🔔 ทดสอบแจ้งเตือนโควต้าหมด", style = MaterialTheme.typography.labelSmall)
            }
            OutlinedButton(
              onClick = { onToggleAiMode() },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f),
            ) {
              Text(
                text = if (!useBuiltInOnDeviceAi) "สลับเป็น AI ในแอพ" else "สลับเป็น Gemini API",
                style = MaterialTheme.typography.labelSmall,
              )
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showQuotaDialog = false }) { Text("เข้าใจแล้ว") }
      },
    )
  }

  Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
    // Compact Top Stage Header Bar
    Surface(
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 4.dp,
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
          IconButton(
            onClick = onBack,
            modifier = Modifier.size(38.dp).testTag("back_to_stages_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "ย้อนกลับ",
              tint = MaterialTheme.colorScheme.onSurface,
            )
          }
          Column(
            modifier =
              Modifier.weight(1f)
                .clickable { showStageInfoDialog = true }
                .padding(horizontal = 4.dp)
          ) {
            Text(
              text = "ด่าน ${stage.stageNumber}/25 • Gemini Free (${quotaStatus.rpdLimit - quotaStatus.rpdUsed} คงเหลือ)",
              style = MaterialTheme.typography.labelSmall,
              color = GoldPrimary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = stage.title,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Button(
            onClick = onEvaluateStage,
            enabled =
              aiState !is RolePlayAiState.Thinking && aiState !is RolePlayAiState.Evaluating,
            colors =
              ButtonDefaults.buttonColors(
                containerColor = EmeraldAccent,
                contentColor = MaterialTheme.colorScheme.onSecondary,
              ),
            shape = RoundedCornerShape(10.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("evaluate_stage_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Analytics,
              contentDescription = "ประเมินคะแนน",
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "ประเมินผล",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }
    }

    if (evaluationResult != null) {
      EvaluationScorecardView(
        stage = stage,
        result = evaluationResult,
        onReplayStage = onReplayStage,
        onNextStage = onNextStage,
        onBackToMap = onBack,
        modifier = Modifier.weight(1f),
      )
    } else {
      // 1. ฉากตัวละครคุยกันซึ่งหน้าพร้อมระบบ Lip-Sync อัตโนมัติ (Dual-Avatar Stage)
      DualAvatarRolePlayStageView(
        customerName = stage.customerName,
        customerAge = stage.customerAge,
        customerOccupation = stage.customerOccupation,
        agentName = agentName,
        isCustomerSpeaking = isCustomerSpeaking,
        customerViseme = customerViseme,
        customerMouthOpenness = customerMouthOpenness,
        isAgentSpeaking = isAgentSpeaking || isListeningRealTime,
        agentViseme = agentViseme,
        agentMouthOpenness = agentMouthOpenness,
        trustScore = trustScore,
        comboCount = agentTurnCount,
        isBgmEnabled = isBgmEnabled,
        isCustomerVoiceEnabled = isCustomerVoiceEnabled,
        quotaStatus = quotaStatus,
        onToggleBgm = { gameEngine.toggleBgm() },
        onToggleCustomerVoice = { gameEngine.toggleCustomerVoice() },
        onReplayCustomerVoice = {
          val lastCustomerLine =
            chatTurns.lastOrNull { !it.isAgent }?.text ?: stage.openingCustomerLine
          gameEngine.speakAndLipSyncCustomerLine(
            customerText = lastCustomerLine,
            customerAge = stage.customerAge,
            customerName = stage.customerName,
          )
        },
        onShowStageInfoDialog = { showStageInfoDialog = true },
        onShowTipsDialog = { showTipsDialog = true },
        onShowQuotaDialog = { showQuotaDialog = true },
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
      )

      // 2. แถบควบคุมความเสถียรของระบบพูดอัตโนมัติ (Half-Duplex Echo Guard + Gemini API Status)
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
      ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Icon(
                imageVector = Icons.Default.PhoneInTalk,
                contentDescription = null,
                tint =
                  when (voiceCallState) {
                    VoiceCallState.CUSTOMER_SPEAKING -> CyanWaveform
                    VoiceCallState.ECHO_COOLDOWN -> GoldPrimary
                    VoiceCallState.AGENT_LISTENING -> EmeraldAccent
                    VoiceCallState.DEBOUNCING_SPEECH -> GoldPrimary
                    VoiceCallState.IDLE -> MaterialTheme.colorScheme.primary
                  },
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text =
                  when (voiceCallState) {
                    VoiceCallState.CUSTOMER_SPEAKING ->
                      "🔊 ลูกค้าพูดสด (ปิดไมค์อัตโนมัติป้องกันเสียงลำโพงสะท้อน)"
                    VoiceCallState.ECHO_COOLDOWN ->
                      "⏳ กำลังเคลียร์เสียงสะท้อน 0.4 วิ ก่อนสลับเปิดไมค์..."
                    VoiceCallState.AGENT_LISTENING ->
                      if (livePartialTranscript.isNotBlank())
                        "🎙️ ฟังสด ($liveWpmEstimate WPM): \"$livePartialTranscript\""
                      else "🎙️ ไมค์เปิดรับเสียงคุณ (ตัดเสียงรบกวน BGM แล้ว) พูดได้เลย!"
                    VoiceCallState.DEBOUNCING_SPEECH ->
                      "⏳ รอจังหวะเว้นวรรคส่งให้ Gemini API: \"$livePartialTranscript\""
                    VoiceCallState.IDLE ->
                      "✨ เชื่อม Gemini API (รีเซ็ตโควต้าฟรี ${quotaStatus.nextResetTimeThaiLabel})"
                  },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color =
                  when (voiceCallState) {
                    VoiceCallState.CUSTOMER_SPEAKING -> CyanWaveform
                    VoiceCallState.AGENT_LISTENING -> EmeraldAccent
                    else -> MaterialTheme.colorScheme.onSurface
                  },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            if (voiceCallState == VoiceCallState.DEBOUNCING_SPEECH) {
              Text(
                text = "ส่งทันที ▶",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier =
                  Modifier.clip(RoundedCornerShape(99.dp))
                    .background(GoldPrimary)
                    .clickable { gameEngine.flushPendingSpeechNow() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
              )
            } else {
              Text(
                text = if (isLiveCallMode) "🔄 สลับไมค์อัตโนมัติ" else "👆 กดพูดเอง (เสถียรสุด)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isLiveCallMode) EmeraldAccent else GoldPrimary,
                modifier =
                  Modifier.clip(RoundedCornerShape(99.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable { gameEngine.toggleLiveCallMode() }
                    .padding(horizontal = 8.dp, vertical = 3.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(4.dp))

          // ปุ่มลัดทดสอบ Voice Coaching + ทดสอบแจ้งเตือนโควต้าฟรีหมด
          LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
              Text(
                text = "✅ พูดจังหวะมืออาชีพ (132 WPM)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = EmeraldAccent,
                modifier =
                  Modifier.clip(RoundedCornerShape(99.dp))
                    .background(EmeraldAccent.copy(alpha = 0.14f))
                    .clickable {
                      gameEngine.animateAgentSpeakingText("พูดจังหวะมืออาชีพน้ำเสียงอบอุ่น")
                      onSimulateVoicePattern("BALANCED")
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("sim_voice_balanced_btn"),
              )
            }
            item {
              Text(
                text = "⚡ ทดสอบพูดเร็ว (178 WPM > 165)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CoralAlert,
                modifier =
                  Modifier.clip(RoundedCornerShape(99.dp))
                    .background(CoralAlert.copy(alpha = 0.15f))
                    .clickable {
                      gameEngine.animateAgentSpeakingText("พูดเร็วรัวติดกันเกินร้อยหกสิบห้าคำต่อนาที")
                      onSimulateVoicePattern("FAST_WPM")
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("sim_voice_fast_btn"),
              )
            }
            item {
              Text(
                text = "🔉 ทดสอบพูดเสียงเบา",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
                modifier =
                  Modifier.clip(RoundedCornerShape(99.dp))
                    .background(GoldPrimary.copy(alpha = 0.15f))
                    .clickable {
                      gameEngine.animateAgentSpeakingText("พูดเสียงเบาไม่มั่นใจ")
                      onSimulateVoicePattern("QUIET_VOICE")
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("sim_voice_quiet_btn"),
              )
            }
            item {
              Text(
                text = "🕒 ดูเวลารีเซ็ตโควต้าพรุ่งนี้",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = CyanWaveform,
                modifier =
                  Modifier.clip(RoundedCornerShape(99.dp))
                    .background(CyanWaveform.copy(alpha = 0.14f))
                    .clickable { showQuotaDialog = true }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
              )
            }
          }
        }
      }

      // 3. รายการบทสนทนา
      LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f).fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(chatTurns, key = { it.id }) { turn ->
          ChatTurnBubble(
            turn = turn,
            customerName = stage.customerName,
            onSpeakCustomerLine = {
              gameEngine.speakAndLipSyncCustomerLine(
                customerText = turn.text,
                customerAge = stage.customerAge,
                customerName = stage.customerName,
              )
            },
          )
        }

        item {
          when (aiState) {
            is RolePlayAiState.Thinking -> {
              Row(
                modifier = Modifier.fillMaxWidth().padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Gemini API (Free Tier) กำลังคิดคำตอบและสังเคราะห์เสียงพูด...",
                  style = MaterialTheme.typography.labelMedium,
                  color = CyanWaveform,
                )
              }
            }
            is RolePlayAiState.Evaluating -> {
              Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
              ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = aiState.progressText,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                  color = GoldPrimary,
                )
              }
            }
            is RolePlayAiState.Error -> {
              Card(
                colors =
                  CardDefaults.cardColors(containerColor = CoralAlert.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Text(
                    text = aiState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = CoralAlert,
                    modifier = Modifier.weight(1f),
                  )
                  OutlinedButton(onClick = onDismissError) { Text("ปิด") }
                }
              }
            }
            RolePlayAiState.Idle -> {}
          }
        }
      }

      // 4. แผงควบคุมไมโครโฟนเรียลไทม์และช่องพิมพ์ด้านล่าง
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
          if (permissionDeniedMessage != null) {
            Text(
              text = permissionDeniedMessage!!,
              style = MaterialTheme.typography.labelSmall,
              color = CoralAlert,
              modifier = Modifier.padding(bottom = 4.dp),
            )
          }

          if (isRecording || isListeningRealTime) {
            LiveVoiceWaveformBar(
              amplitude = if (isRecording) currentAmplitude else 0.45f,
              isRecording = true,
              elapsedSeconds = elapsedSeconds,
            )
            Spacer(modifier = Modifier.height(6.dp))
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(
              onClick = {
                if (isListeningRealTime || voiceCallState == VoiceCallState.DEBOUNCING_SPEECH) {
                  gameEngine.flushPendingSpeechNow()
                } else if (isRecording) {
                  val (audioBytes, metrics) = voiceRecorder.stopAndAnalyze(messageInput)
                  val textToSend = messageInput
                  messageInput = ""
                  gameEngine.playAgentMicSfx()
                  gameEngine.animateAgentSpeakingText(
                    textToSend.ifBlank { "ส่งเสียงพูดจริงให้ลูกค้าฟัง" }
                  )
                  onSendTurn(textToSend, audioBytes, metrics)
                } else {
                  val hasPermission =
                    ContextCompat.checkSelfPermission(
                      context,
                      Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                  if (hasPermission) {
                    startRealTimeTurnListening()
                  } else {
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                  }
                }
              },
              modifier =
                Modifier.size(48.dp)
                  .clip(CircleShape)
                  .background(
                    if (isRecording || isListeningRealTime) CoralAlert else CyanWaveform
                  )
                  .testTag("voice_record_button"),
            ) {
              Icon(
                imageVector =
                  if (isRecording || isListeningRealTime) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription =
                  if (isRecording || isListeningRealTime) "หยุดและส่งเสียง" else "กดพูดคุยสดเรียลไทม์",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
              value = messageInput,
              onValueChange = { messageInput = it },
              placeholder = {
                Text(
                  text =
                    when {
                      isListeningRealTime ->
                        "🎙️ กำลังฟังสด... พูดจบเว้นวรรค 1 วิ ระบบส่งให้อัตโนมัติ!"
                      isRecording ->
                        "🎙️ กำลังอัดเสียง... กดปุ่มสีแดงเพื่อส่ง"
                      else ->
                        "กดไมค์คุยสดกับ Gemini API หรือพิมพ์บทพูด..."
                    },
                  style = MaterialTheme.typography.bodySmall,
                )
              },
              trailingIcon = {
                IconButton(onClick = { showTipsDialog = true }) {
                  Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = "เลือกประโยคแนะนำ",
                    tint = GoldPrimary,
                    modifier = Modifier.size(20.dp),
                  )
                }
              },
              modifier = Modifier.weight(1f).testTag("agent_speech_input"),
              shape = RoundedCornerShape(14.dp),
              maxLines = 2,
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
              onClick = {
                if (isListeningRealTime || voiceCallState == VoiceCallState.DEBOUNCING_SPEECH) {
                  gameEngine.flushPendingSpeechNow()
                } else if (isRecording) {
                  val (audioBytes, metrics) = voiceRecorder.stopAndAnalyze(messageInput)
                  val textToSend = messageInput
                  messageInput = ""
                  gameEngine.playAgentMicSfx()
                  gameEngine.animateAgentSpeakingText(
                    textToSend.ifBlank { "ส่งเสียงพูดจริงให้ลูกค้าฟัง" }
                  )
                  onSendTurn(textToSend, audioBytes, metrics)
                } else if (messageInput.isNotBlank()) {
                  val textToSend = messageInput
                  messageInput = ""
                  gameEngine.playAgentMicSfx()
                  gameEngine.animateAgentSpeakingText(textToSend)
                  onSendTurn(textToSend, null, null)
                }
              },
              enabled =
                (isRecording || isListeningRealTime || messageInput.isNotBlank()) &&
                  aiState !is RolePlayAiState.Thinking,
              modifier =
                Modifier.size(48.dp)
                  .clip(CircleShape)
                  .background(GoldPrimary)
                  .testTag("send_turn_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "ส่งคำพูดตัวแทน",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChatTurnBubble(
  turn: ChatTurn,
  customerName: String,
  onSpeakCustomerLine: () -> Unit,
) {
  var showCoachTip by remember { mutableStateOf(false) }
  var showVoiceMetrics by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (turn.isAgent) Alignment.End else Alignment.Start,
  ) {
    Box(
      modifier =
        Modifier.fillMaxWidth(0.90f)
          .clip(
            RoundedCornerShape(
              topStart = 14.dp,
              topEnd = 14.dp,
              bottomStart = if (turn.isAgent) 14.dp else 4.dp,
              bottomEnd = if (turn.isAgent) 4.dp else 14.dp,
            )
          )
          .background(
            if (turn.isAgent) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
          )
          .border(
            width = 1.dp,
            color =
              if (turn.isVoiceAlertWarning) CoralAlert
              else if (turn.isAgent) GoldPrimary.copy(alpha = 0.4f)
              else CyanWaveform.copy(alpha = 0.3f),
            shape = RoundedCornerShape(14.dp),
          )
          .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (turn.isAgent) "⚜️ คุณ (พูดสด)" else "👤 $customerName",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (turn.isAgent) GoldPrimary else CyanWaveform,
          )

          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (!turn.isAgent) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = "ฟังเสียงลูกค้าพูดซ้ำ",
                tint = CyanWaveform,
                modifier = Modifier.size(16.dp).clickable { onSpeakCustomerLine() },
              )
            }
            if (!turn.voiceMetricsSummary.isNullOrBlank()) {
              Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "ดูผลวิเคราะห์คลื่นเสียง",
                tint = if (showVoiceMetrics) GoldPrimary else CyanWaveform,
                modifier = Modifier.size(16.dp).clickable { showVoiceMetrics = !showVoiceMetrics },
              )
            }
            if (!turn.coachTip.isNullOrBlank()) {
              Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "ดูคำแนะนำโค้ช AI",
                tint = GoldPrimary,
                modifier = Modifier.size(16.dp).clickable { showCoachTip = !showCoachTip },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = turn.text,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )

        if (!turn.voiceCoachingAlert.isNullOrBlank() && (turn.isVoiceAlertWarning || showVoiceMetrics)) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                  if (turn.isVoiceAlertWarning) CoralAlert.copy(alpha = 0.16f)
                  else EmeraldAccent.copy(alpha = 0.14f)
                )
                .border(
                  1.dp,
                  if (turn.isVoiceAlertWarning) CoralAlert.copy(alpha = 0.5f)
                  else EmeraldAccent.copy(alpha = 0.4f),
                  RoundedCornerShape(8.dp),
                )
                .padding(8.dp)
                .testTag("voice_coaching_bubble_tip")
          ) {
            Text(
              text = turn.voiceCoachingAlert,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = if (turn.isVoiceAlertWarning) CoralAlert else EmeraldAccent,
            )
          }
        }

        AnimatedVisibility(visible = showVoiceMetrics && !turn.voiceMetricsSummary.isNullOrBlank()) {
          Text(
            text = "🎙️ ${turn.voiceMetricsSummary}",
            style = MaterialTheme.typography.labelSmall,
            color = CyanWaveform,
            modifier = Modifier.padding(top = 6.dp),
          )
        }

        AnimatedVisibility(visible = showCoachTip && !turn.coachTip.isNullOrBlank()) {
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.65f))
                .padding(8.dp)
          ) {
            Text(
              text = "💡 ${turn.coachTip}",
              style = MaterialTheme.typography.labelSmall,
              color = GoldPrimary,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EvaluationScorecardView(
  stage: RolePlayStage,
  result: EvaluationResult,
  onReplayStage: () -> Unit,
  onNextStage: () -> Unit,
  onBackToMap: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showVoiceDetailDialog by remember { mutableStateOf(false) }
  var showStrengthsDialog by remember { mutableStateOf(false) }

  if (showVoiceDetailDialog) {
    AlertDialog(
      onDismissRequest = { showVoiceDetailDialog = false },
      icon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = CyanWaveform) },
      title = { Text("🎙️ วิเคราะห์น้ำเสียง & การชี้เป้า", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "ผลวิเคราะห์น้ำเสียง (${result.voiceToneScore}/100):",
            style = MaterialTheme.typography.labelLarge,
            color = CyanWaveform,
            fontWeight = FontWeight.Bold,
          )
          Text(text = result.voiceFeedback, style = MaterialTheme.typography.bodySmall)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "การชี้เป้าหมวดประกัน:",
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldAccent,
            fontWeight = FontWeight.Bold,
          )
          Text(text = result.recommendedDirectionCheck, style = MaterialTheme.typography.bodySmall)
        }
      },
      confirmButton = {
        TextButton(onClick = { showVoiceDetailDialog = false }) { Text("ปิด") }
      },
    )
  }

  if (showStrengthsDialog) {
    AlertDialog(
      onDismissRequest = { showStrengthsDialog = false },
      icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldPrimary) },
      title = { Text("💡 จุดเด่น & จุดที่ควรพัฒนาเพิ่ม", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = "✅ จุดเด่นที่ทำได้ดี:",
            style = MaterialTheme.typography.labelLarge,
            color = EmeraldAccent,
            fontWeight = FontWeight.Bold,
          )
          result.strengths.forEach { item ->
            Text(text = "• $item", style = MaterialTheme.typography.bodySmall)
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "🚀 จุดที่ควรพัฒนาเพิ่ม:",
            style = MaterialTheme.typography.labelLarge,
            color = GoldPrimary,
            fontWeight = FontWeight.Bold,
          )
          result.improvements.forEach { item ->
            Text(text = "• $item", style = MaterialTheme.typography.bodySmall)
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showStrengthsDialog = false }) { Text("ปิด") }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Card(
        colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector =
              if (result.passed) Icons.Default.EmojiEvents else Icons.Default.Analytics,
            contentDescription = "ผลการประเมิน",
            tint = if (result.passed) GoldPrimary else CoralAlert,
            modifier = Modifier.size(44.dp),
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              if (result.passed) "🎉 พิชิตด่านสำเร็จ! (STAGE CLEAR)"
              else "แนะนำให้ฝึกซ้ำเพื่อเก็บดาวเพิ่ม",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (result.passed) EmeraldAccent else CoralAlert,
          )
          Text(
            text = "${result.overallScore} / 100",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = GoldPrimary,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = result.coachSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )

          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { showVoiceDetailDialog = true },
              shape = RoundedCornerShape(99.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = CyanWaveform,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("ดูผลน้ำเสียง", style = MaterialTheme.typography.labelMedium)
            }
            OutlinedButton(
              onClick = { showStrengthsDialog = true },
              shape = RoundedCornerShape(99.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(16.dp),
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("ดูจุดเด่น/คำแนะนำ", style = MaterialTheme.typography.labelMedium)
            }
          }
        }
      }
    }

    if (result.newlyUnlockedBadges.isNotEmpty()) {
      item {
        Card(
          colors =
            CardDefaults.cardColors(containerColor = GoldPrimary.copy(alpha = 0.14f)),
          shape = RoundedCornerShape(16.dp),
          modifier =
            Modifier.fillMaxWidth()
              .border(1.5.dp, GoldPrimary, RoundedCornerShape(16.dp)),
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Text(
              text = "🏅 ปลดล็อกเหรียญตรา & ฉายาใหม่!",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = GoldPrimary,
            )
            result.newlyUnlockedBadges.forEach { badge ->
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = badge.emoji, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Text(
                    text = "${badge.title} • ฉายา: \"${badge.funnyRankName}\"",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = badge.conditionDescription,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
    }

    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "📊 คะแนนความสามารถ 5 มิติ",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
          )
          Spacer(modifier = Modifier.height(8.dp))
          FiveDimensionRadarAndBars(
            rapportScore = result.rapportScore,
            objectionScore = result.objectionScore,
            factFindingScore = result.factFindingScore,
            preApproachScore = result.preApproachScore,
            voiceToneScore = result.voiceToneScore,
          )
        }
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onReplayStage,
          modifier = Modifier.weight(1f).height(48.dp).testTag("replay_stage_button"),
          shape = RoundedCornerShape(12.dp),
        ) {
          Icon(imageVector = Icons.Default.Refresh, contentDescription = "ฝึกด่านเดิมซ้ำ")
          Spacer(modifier = Modifier.width(6.dp))
          Text("เล่นซ้ำ")
        }

        if (stage.stageNumber < 25) {
          Button(
            onClick = onNextStage,
            modifier = Modifier.weight(1f).height(48.dp).testTag("next_stage_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
          ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "ด่านถัดไป")
            Spacer(modifier = Modifier.width(6.dp))
            Text("ลุยด่านที่ ${stage.stageNumber + 1}", fontWeight = FontWeight.Bold)
          }
        } else {
          Button(
            onClick = onBackToMap,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
          ) {
            Text("กลับยุทธภพ")
          }
        }
      }
    }
  }
}
