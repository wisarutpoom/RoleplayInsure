package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.MouthViseme
import com.example.data.FreeQuotaStatus
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * ฉากเกม Role-Play แบบตัวละครคุยกันซึ่งหน้า (Dual-Avatar Interactive Lip-Sync Stage)
 * ดีไซน์มินิมอลเน้นภาพตัวละครขยับปากสมจริง และใช้ปุ่มไอคอน (ℹ️, 💡, 🎵, 🔊, 🛡️) เพื่อซ่อนข้อความยาวๆ
 */
@Composable
fun DualAvatarRolePlayStageView(
  customerName: String,
  customerAge: Int,
  customerOccupation: String,
  agentName: String,
  isCustomerSpeaking: Boolean,
  customerViseme: MouthViseme,
  customerMouthOpenness: Float,
  isAgentSpeaking: Boolean,
  agentViseme: MouthViseme,
  agentMouthOpenness: Float,
  trustScore: Int,
  comboCount: Int,
  isBgmEnabled: Boolean,
  isCustomerVoiceEnabled: Boolean,
  quotaStatus: FreeQuotaStatus,
  onToggleBgm: () -> Unit,
  onToggleCustomerVoice: () -> Unit,
  onReplayCustomerVoice: () -> Unit,
  onShowStageInfoDialog: () -> Unit,
  onShowTipsDialog: () -> Unit,
  onShowQuotaDialog: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val infiniteTransition = rememberInfiniteTransition(label = "stage_game_loop")

  val breathPhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = (2f * PI).toFloat(),
      animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
      label = "breath_phase",
    )

  val auraPulse by
    infiniteTransition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.22f,
      animationSpec =
        infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "aura_pulse",
    )

  val blinkTicker by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 100f,
      animationSpec = infiniteRepeatable(tween(3400, easing = LinearEasing), RepeatMode.Restart),
      label = "blink_ticker",
    )

  val smoothCustomerOpen by
    animateFloatAsState(
      targetValue = if (isCustomerSpeaking) customerMouthOpenness else 0.04f,
      animationSpec = tween(durationMillis = 65),
      label = "smooth_customer_mouth",
    )

  val smoothAgentOpen by
    animateFloatAsState(
      targetValue = if (isAgentSpeaking) agentMouthOpenness else 0.08f,
      animationSpec = tween(durationMillis = 55),
      label = "smooth_agent_mouth",
    )

  val smoothTrust by
    animateFloatAsState(
      targetValue = (trustScore / 100f).coerceIn(0.1f, 1f),
      animationSpec = tween(durationMillis = 450),
      label = "smooth_trust",
    )

  val isFemaleCustomer =
    customerName.contains("หญิง") ||
      customerName.contains("พิมพ์") ||
      customerName.contains("นภา") ||
      customerName.contains("กานดา") ||
      customerName.contains("อรอนงค์") ||
      customerName.contains("ป้า") ||
      customerName.contains("เจ๊") ||
      customerName.contains("คุณแม่") ||
      customerName.contains("ลลิตา") ||
      customerName.contains("แพรว")

  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(20.dp),
    tonalElevation = 6.dp,
    modifier =
      modifier
        .fillMaxWidth()
        .border(
          width = 1.5.dp,
          color =
            when {
              isAgentSpeaking -> GoldPrimary
              isCustomerSpeaking -> CyanWaveform
              else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            },
          shape = RoundedCornerShape(20.dp),
        ),
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      // 1. Compact Game HUD Bar: Trust Meter + Combo + Icon Action Buttons
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Customer Trust Bar + Combo Badge
        Column(modifier = Modifier.weight(1f)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = "❤️ ความเชื่อใจ $trustScore%",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color =
                when {
                  trustScore >= 75 -> EmeraldAccent
                  trustScore >= 50 -> GoldPrimary
                  else -> CoralAlert
                },
            )
            if (comboCount > 0) {
              Text(
                text = "🔥 x$comboCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = GoldPrimary,
              )
            }
          }
          Spacer(modifier = Modifier.height(3.dp))
          LinearProgressIndicator(
            progress = { smoothTrust },
            color =
              when {
                trustScore >= 75 -> EmeraldAccent
                trustScore >= 50 -> GoldPrimary
                else -> CoralAlert
              },
            trackColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)),
          )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Clean Icon Buttons Row (Info, Tips, BGM, Voice, Replay, Free Quota Shield)
        Row(
          horizontalArrangement = Arrangement.spacedBy(5.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          HudRoundIconButton(
            icon = Icons.Default.Info,
            contentDescription = "ดูข้อมูลลูกค้าและภารกิจด่าน",
            tint = GoldPrimary,
            active = true,
            onClick = onShowStageInfoDialog,
            testTag = "stage_info_icon_button",
          )
          HudRoundIconButton(
            icon = Icons.Default.Lightbulb,
            contentDescription = "ดูเทคนิคการพูดและประโยคแนะนำ",
            tint = GoldPrimary,
            active = true,
            onClick = onShowTipsDialog,
            testTag = "stage_tips_icon_button",
          )
          HudRoundIconButton(
            icon = if (isBgmEnabled) Icons.Default.MusicNote else Icons.Default.MusicOff,
            contentDescription = "เปิด/ปิดเพลงประกอบ BGM",
            tint = GoldPrimary,
            active = isBgmEnabled,
            onClick = onToggleBgm,
            testTag = "toggle_bgm_button",
          )
          HudRoundIconButton(
            icon =
              if (isCustomerVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp
              else Icons.AutoMirrored.Filled.VolumeOff,
            contentDescription = "เปิด/ปิดเสียงพูดลูกค้า",
            tint = CyanWaveform,
            active = isCustomerVoiceEnabled,
            onClick = onToggleCustomerVoice,
            testTag = "toggle_customer_voice_button",
          )
          HudRoundIconButton(
            icon = Icons.Default.Replay,
            contentDescription = "ฟังเสียงลูกค้าพูดซ้ำ",
            tint = CyanWaveform,
            active = true,
            onClick = onReplayCustomerVoice,
            testTag = "replay_customer_voice_button",
          )
          HudRoundIconButton(
            icon = Icons.Default.VerifiedUser,
            contentDescription = "ดูสถานะล็อกโควต้าฟรี Gemini 0 บาท",
            tint = EmeraldAccent,
            active = !quotaStatus.isTemporarilyCapped,
            onClick = onShowQuotaDialog,
            testTag = "quota_info_icon_button",
          )
        }
      }

      // 2. Interactive Dual-Character Stage Canvas (Customer vs Agent Lip-Sync)
      Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
        val stageBgTop = MaterialTheme.colorScheme.background
        val stageBgBottom = MaterialTheme.colorScheme.surfaceVariant
        val customerBlink = blinkTicker in 46f..50f
        val agentBlink = blinkTicker in 78f..82f

        Canvas(modifier = Modifier.fillMaxSize()) {
          val w = size.width
          val h = size.height

          drawRect(
            brush = Brush.verticalGradient(colors = listOf(stageBgTop, stageBgBottom))
          )

          // Floating Golden Silk Particles
          for (i in 0 until 12) {
            val px = ((w * (i * 0.085f + 0.04f)) + sin(breathPhase + i) * 14f).coerceIn(10f, w - 10f)
            val py = ((h * ((i % 4) * 0.22f + 0.12f)) + cos(breathPhase + i * 0.7f) * 10f)
            drawCircle(
              color = GoldPrimary.copy(alpha = 0.18f + (i % 3) * 0.06f),
              radius = (2.5f + (i % 3) * 1.5f),
              center = Offset(px, py),
            )
          }

          val leftCenterX = w * 0.25f
          val rightCenterX = w * 0.75f
          val avatarBaseY = h * 0.56f

          // Customer Speaking Aura
          if (isCustomerSpeaking) {
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      CyanWaveform.copy(alpha = 0.32f),
                      CyanWaveform.copy(alpha = 0.0f),
                    ),
                  center = Offset(leftCenterX, avatarBaseY - 12f),
                  radius = 112f * auraPulse,
                ),
              radius = 112f * auraPulse,
              center = Offset(leftCenterX, avatarBaseY - 12f),
            )
            drawCircle(
              color = CyanWaveform.copy(alpha = 0.45f),
              radius = 66f * auraPulse,
              center = Offset(leftCenterX, avatarBaseY - 12f),
              style = Stroke(width = 2.5f),
            )
          }

          // Agent Speaking Aura
          if (isAgentSpeaking) {
            drawCircle(
              brush =
                Brush.radialGradient(
                  colors =
                    listOf(
                      GoldPrimary.copy(alpha = 0.35f),
                      GoldPrimary.copy(alpha = 0.0f),
                    ),
                  center = Offset(rightCenterX, avatarBaseY - 12f),
                  radius = 112f * auraPulse,
                ),
              radius = 112f * auraPulse,
              center = Offset(rightCenterX, avatarBaseY - 12f),
            )
            drawCircle(
              color = GoldPrimary.copy(alpha = 0.55f),
              radius = 66f * auraPulse,
              center = Offset(rightCenterX, avatarBaseY - 12f),
              style = Stroke(width = 2.5f),
            )
          }

          // Interactive Sound Wave Bridge between Customer & Agent
          drawConversationSoundBridge(
            startX = leftCenterX + 58f,
            endX = rightCenterX - 58f,
            centerY = avatarBaseY - 8f,
            phase = breathPhase * 3f,
            isCustomerSpeaking = isCustomerSpeaking,
            isAgentSpeaking = isAgentSpeaking,
            customerOpen = smoothCustomerOpen,
            agentOpen = smoothAgentOpen,
          )

          // Left: Customer Avatar with Automatic Lip-Sync
          val custBobY =
            if (isCustomerSpeaking) sin(breathPhase * 4f) * 3.5f else sin(breathPhase) * 1.8f
          drawExpressiveCharacterAvatar(
            centerX = leftCenterX,
            centerY = avatarBaseY + custBobY,
            isAgent = false,
            isFemale = isFemaleCustomer,
            age = customerAge,
            isSpeaking = isCustomerSpeaking,
            viseme = customerViseme,
            mouthOpenness = smoothCustomerOpen,
            isBlinking = customerBlink,
            trustScore = trustScore,
          )

          // Right: Our Agent Avatar with Real-Time Lip-Sync
          val agentBobY =
            if (isAgentSpeaking) sin(breathPhase * 4.2f) * 3.5f else cos(breathPhase) * 1.8f
          drawExpressiveCharacterAvatar(
            centerX = rightCenterX,
            centerY = avatarBaseY + agentBobY,
            isAgent = true,
            isFemale = false,
            age = 32,
            isSpeaking = isAgentSpeaking,
            viseme = agentViseme,
            mouthOpenness = smoothAgentOpen,
            isBlinking = agentBlink,
            trustScore = 90,
          )
        }

        // Compact Character Name Pills at Bottom of Stage
        Row(
          modifier =
            Modifier.align(Alignment.BottomCenter)
              .fillMaxWidth()
              .padding(horizontal = 10.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(99.dp))
                .background(
                  if (isCustomerSpeaking) CyanWaveform.copy(alpha = 0.22f)
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
                .border(
                  1.dp,
                  if (isCustomerSpeaking) CyanWaveform else CyanWaveform.copy(alpha = 0.35f),
                  RoundedCornerShape(99.dp),
                )
                .clickable(onClick = onShowStageInfoDialog)
                .padding(horizontal = 10.dp, vertical = 4.dp)
          ) {
            Text(
              text =
                if (isCustomerSpeaking) "🗣️ $customerName • กำลังพูด"
                else "👤 $customerName ($customerOccupation)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = CyanWaveform,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Box(
            modifier =
              Modifier.weight(1f)
                .clip(RoundedCornerShape(99.dp))
                .background(
                  if (isAgentSpeaking) GoldPrimary.copy(alpha = 0.24f)
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
                .border(
                  1.dp,
                  if (isAgentSpeaking) GoldPrimary else GoldPrimary.copy(alpha = 0.35f),
                  RoundedCornerShape(99.dp),
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.CenterEnd,
          ) {
            Text(
              text =
                if (isAgentSpeaking) "🎙️ คุณกำลังพูด • ปากขยับสด"
                else "⚜️ คุณ ($agentName)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = GoldPrimary,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HudRoundIconButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  contentDescription: String,
  tint: Color,
  active: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  Box(
    modifier =
      Modifier.size(32.dp)
        .clip(CircleShape)
        .background(
          if (active) tint.copy(alpha = 0.16f)
          else MaterialTheme.colorScheme.background
        )
        .border(
          1.dp,
          if (active) tint.copy(alpha = 0.7f)
          else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
          CircleShape,
        )
        .clickable(onClick = onClick)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      tint = if (active) tint else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(16.dp),
    )
  }
}

private fun DrawScope.drawConversationSoundBridge(
  startX: Float,
  endX: Float,
  centerY: Float,
  phase: Float,
  isCustomerSpeaking: Boolean,
  isAgentSpeaking: Boolean,
  customerOpen: Float,
  agentOpen: Float,
) {
  if (endX <= startX) return
  val bars = 14
  val span = endX - startX
  val step = span / bars
  val activeColor =
    when {
      isAgentSpeaking -> GoldPrimary
      isCustomerSpeaking -> CyanWaveform
      else -> GoldPrimary.copy(alpha = 0.22f)
    }
  val intensity =
    when {
      isAgentSpeaking -> agentOpen.coerceAtLeast(0.35f)
      isCustomerSpeaking -> customerOpen.coerceAtLeast(0.35f)
      else -> 0.08f
    }

  for (i in 0 until bars) {
    val x = startX + i * step + step * 0.5f
    val wave = abs(sin(phase + i * 0.55f)).toFloat()
    val barHeight = (6f + 36f * intensity * wave)
    drawLine(
      color = activeColor,
      start = Offset(x, centerY - barHeight / 2f),
      end = Offset(x, centerY + barHeight / 2f),
      strokeWidth = 3.5f,
      cap = StrokeCap.Round,
    )
  }
}

private fun DrawScope.drawExpressiveCharacterAvatar(
  centerX: Float,
  centerY: Float,
  isAgent: Boolean,
  isFemale: Boolean,
  age: Int,
  isSpeaking: Boolean,
  viseme: MouthViseme,
  mouthOpenness: Float,
  isBlinking: Boolean,
  trustScore: Int,
) {
  val headRadius = 38f
  val skinColor = if (isAgent) Color(0xFFF6D5B8) else Color(0xFFF2CCA8)
  val skinShadow = if (isAgent) Color(0xFFE2B692) else Color(0xFFDDB18A)
  val suitColor =
    if (isAgent) Color(0xFF10243E)
    else if (isFemale) Color(0xFF4A2040)
    else Color(0xFF1F3347)
  val lapelAccent = if (isAgent) GoldPrimary else CyanWaveform

  val bodyPath =
    Path().apply {
      moveTo(centerX - 50f, centerY + 66f)
      quadraticTo(centerX - 46f, centerY + 26f, centerX - 18f, centerY + 24f)
      lineTo(centerX + 18f, centerY + 24f)
      quadraticTo(centerX + 46f, centerY + 26f, centerX + 50f, centerY + 66f)
      close()
    }
  drawPath(path = bodyPath, color = suitColor)
  drawPath(path = bodyPath, color = lapelAccent.copy(alpha = 0.6f), style = Stroke(width = 2f))

  val shirtPath =
    Path().apply {
      moveTo(centerX - 12f, centerY + 24f)
      lineTo(centerX + 12f, centerY + 24f)
      lineTo(centerX, centerY + 52f)
      close()
    }
  drawPath(path = shirtPath, color = Color(0xFFFDFBF7))
  val tiePath =
    Path().apply {
      moveTo(centerX - 4f, centerY + 28f)
      lineTo(centerX + 4f, centerY + 28f)
      lineTo(centerX + 5f, centerY + 54f)
      lineTo(centerX, centerY + 60f)
      lineTo(centerX - 5f, centerY + 54f)
      close()
    }
  drawPath(path = tiePath, color = lapelAccent)

  if (isAgent) {
    drawCircle(
      color = GoldPrimary,
      radius = 4.5f,
      center = Offset(centerX - 20f, centerY + 40f),
    )
  }

  drawRoundRect(
    color = skinShadow,
    topLeft = Offset(centerX - 10f, centerY + 14f),
    size = Size(20f, 16f),
    cornerRadius = CornerRadius(6f, 6f),
  )

  val hairColor =
    when {
      !isAgent && age >= 55 -> Color(0xFF4A4E54)
      isAgent -> Color(0xFF171A21)
      isFemale -> Color(0xFF281914)
      else -> Color(0xFF1E2026)
    }
  if (isFemale && !isAgent) {
    drawRoundRect(
      color = hairColor,
      topLeft = Offset(centerX - headRadius - 6f, centerY - 26f),
      size = Size((headRadius + 6f) * 2f, 54f),
      cornerRadius = CornerRadius(24f, 24f),
    )
  }

  drawCircle(color = skinShadow, radius = 8f, center = Offset(centerX - headRadius + 2f, centerY - 6f))
  drawCircle(color = skinShadow, radius = 8f, center = Offset(centerX + headRadius - 2f, centerY - 6f))
  drawCircle(color = skinColor, radius = headRadius, center = Offset(centerX, centerY - 10f))

  drawCircle(
    color = Color(0xFFE88D84).copy(alpha = 0.25f),
    radius = 8f,
    center = Offset(centerX - 20f, centerY - 2f),
  )
  drawCircle(
    color = Color(0xFFE88D84).copy(alpha = 0.25f),
    radius = 8f,
    center = Offset(centerX + 20f, centerY - 2f),
  )

  val topHairPath =
    Path().apply {
      moveTo(centerX - headRadius, centerY - 14f)
      quadraticTo(centerX - headRadius, centerY - 54f, centerX, centerY - 52f)
      quadraticTo(centerX + headRadius, centerY - 54f, centerX + headRadius, centerY - 14f)
      quadraticTo(centerX + 12f, centerY - 34f, centerX - 10f, centerY - 32f)
      close()
    }
  drawPath(path = topHairPath, color = hairColor)

  val eyeY = centerY - 14f
  val leftEyeX = centerX - 14f
  val rightEyeX = centerX + 14f

  val browLift = if (isSpeaking) -2.5f else 0f
  val browSlant = if (!isAgent && trustScore < 45) 2.5f else -1f
  drawLine(
    color = hairColor,
    start = Offset(leftEyeX - 7f, eyeY - 9f + browLift - browSlant),
    end = Offset(leftEyeX + 7f, eyeY - 9f + browLift + browSlant),
    strokeWidth = 3f,
    cap = StrokeCap.Round,
  )
  drawLine(
    color = hairColor,
    start = Offset(rightEyeX - 7f, eyeY - 9f + browLift + browSlant),
    end = Offset(rightEyeX + 7f, eyeY - 9f + browLift - browSlant),
    strokeWidth = 3f,
    cap = StrokeCap.Round,
  )

  if (isBlinking) {
    drawLine(
      color = Color(0xFF1F242D),
      start = Offset(leftEyeX - 6f, eyeY),
      end = Offset(leftEyeX + 6f, eyeY),
      strokeWidth = 2.5f,
      cap = StrokeCap.Round,
    )
    drawLine(
      color = Color(0xFF1F242D),
      start = Offset(rightEyeX - 6f, eyeY),
      end = Offset(rightEyeX + 6f, eyeY),
      strokeWidth = 2.5f,
      cap = StrokeCap.Round,
    )
  } else {
    drawCircle(color = Color.White, radius = 6.2f, center = Offset(leftEyeX, eyeY))
    drawCircle(color = Color.White, radius = 6.2f, center = Offset(rightEyeX, eyeY))
    val gazeOffsetX = if (isAgent) -1.5f else 1.5f
    drawCircle(
      color = Color(0xFF1C222B),
      radius = 3.8f,
      center = Offset(leftEyeX + gazeOffsetX, eyeY),
    )
    drawCircle(
      color = Color(0xFF1C222B),
      radius = 3.8f,
      center = Offset(rightEyeX + gazeOffsetX, eyeY),
    )
    drawCircle(
      color = Color.White,
      radius = 1.4f,
      center = Offset(leftEyeX + gazeOffsetX - 1f, eyeY - 1.5f),
    )
    drawCircle(
      color = Color.White,
      radius = 1.4f,
      center = Offset(rightEyeX + gazeOffsetX - 1f, eyeY - 1.5f),
    )
  }

  if (!isAgent && age >= 45) {
    drawCircle(
      color = GoldPrimary.copy(alpha = 0.85f),
      radius = 9f,
      center = Offset(leftEyeX, eyeY),
      style = Stroke(width = 1.8f),
    )
    drawCircle(
      color = GoldPrimary.copy(alpha = 0.85f),
      radius = 9f,
      center = Offset(rightEyeX, eyeY),
      style = Stroke(width = 1.8f),
    )
    drawLine(
      color = GoldPrimary.copy(alpha = 0.85f),
      start = Offset(leftEyeX + 9f, eyeY),
      end = Offset(rightEyeX - 9f, eyeY),
      strokeWidth = 1.8f,
    )
  }

  drawLine(
    color = skinShadow,
    start = Offset(centerX, eyeY + 4f),
    end = Offset(centerX + (if (isAgent) -2f else 2f), eyeY + 12f),
    strokeWidth = 2.2f,
    cap = StrokeCap.Round,
  )

  val mouthY = centerY + 10f
  val openAmount = mouthOpenness.coerceIn(0f, 1f)

  if (!isSpeaking || openAmount < 0.12f || viseme == MouthViseme.CLOSED || viseme == MouthViseme.SMILE) {
    val smilePath =
      Path().apply {
        moveTo(centerX - 11f, mouthY - 1f)
        quadraticTo(
          centerX,
          mouthY + (if (isAgent || trustScore >= 60) 6f else 1.5f),
          centerX + 11f,
          mouthY - 1f,
        )
      }
    drawPath(
      path = smilePath,
      color = Color(0xFFB54848),
      style = Stroke(width = 3f, cap = StrokeCap.Round),
    )
  } else {
    val mouthWidth =
      when (viseme) {
        MouthViseme.ROUND_OH -> 13f
        MouthViseme.WIDE_AH -> 22f
        else -> 18f
      }
    val mouthHeight = (6f + 16f * openAmount).coerceIn(6f, 20f)
    val mouthRect =
      Rect(
        left = centerX - mouthWidth / 2f,
        top = mouthY - mouthHeight / 2f,
        right = centerX + mouthWidth / 2f,
        bottom = mouthY + mouthHeight / 2f,
      )

    drawRoundRect(
      color = Color(0xFF5E1924),
      topLeft = mouthRect.topLeft,
      size = mouthRect.size,
      cornerRadius = CornerRadius(mouthWidth / 2f, mouthHeight / 2f),
    )

    if (mouthHeight > 8f) {
      drawRoundRect(
        color = Color.White,
        topLeft = Offset(mouthRect.left + 2.5f, mouthRect.top + 1f),
        size = Size(mouthWidth - 5f, (mouthHeight * 0.28f).coerceAtMost(4.5f)),
        cornerRadius = CornerRadius(2f, 2f),
      )
    }

    if (mouthHeight > 10f) {
      drawRoundRect(
        color = Color(0xFFE06377),
        topLeft = Offset(mouthRect.left + 3.5f, mouthRect.bottom - mouthHeight * 0.42f),
        size = Size(mouthWidth - 7f, mouthHeight * 0.36f),
        cornerRadius = CornerRadius(6f, 6f),
      )
    }

    drawRoundRect(
      color = Color(0xFFC44D56),
      topLeft = mouthRect.topLeft,
      size = mouthRect.size,
      cornerRadius = CornerRadius(mouthWidth / 2f, mouthHeight / 2f),
      style = Stroke(width = 2.2f),
    )
  }

  if (isAgent) {
    val micBoomPath =
      Path().apply {
        moveTo(centerX + headRadius - 2f, centerY - 4f)
        quadraticTo(centerX + 22f, centerY + 14f, centerX + 8f, mouthY + 2f)
      }
    drawPath(
      path = micBoomPath,
      color = if (isSpeaking) GoldPrimary else Color(0xFF9AA5B4),
      style = Stroke(width = 2.5f, cap = StrokeCap.Round),
    )
    drawCircle(
      color = if (isSpeaking) EmeraldAccent else GoldPrimary,
      radius = if (isSpeaking) 4.5f else 3.2f,
      center = Offset(centerX + 8f, mouthY + 2f),
    )
  }
}
