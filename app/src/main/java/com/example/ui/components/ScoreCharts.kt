package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.IndigoBadge
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class DimensionMetric(
  val label: String,
  val score: Int,
  val color: Color,
)

@Composable
fun FiveDimensionRadarAndBars(
  rapportScore: Int,
  objectionScore: Int,
  factFindingScore: Int,
  preApproachScore: Int,
  voiceToneScore: Int,
  modifier: Modifier = Modifier,
) {
  val metrics =
    listOf(
      DimensionMetric("การสร้างความไว้วางใจ (Rapport)", rapportScore, EmeraldAccent),
      DimensionMetric("การรับมือข้อโต้แย้ง (Objection)", objectionScore, GoldPrimary),
      DimensionMetric("การเก็บข้อมูลลูกค้า (Fact-Finding)", factFindingScore, IndigoBadge),
      DimensionMetric("การวิเคราะห์หน้างาน (Pre-Approach)", preApproachScore, CoralAlert),
      DimensionMetric("น้ำเสียงและจังหวะ (Voice Tone)", voiceToneScore, CyanWaveform),
    )

  Column(modifier = modifier.fillMaxWidth()) {
    // Pentagon Radar Canvas
    Box(
      modifier = Modifier.fillMaxWidth().height(210.dp),
      contentAlignment = Alignment.Center,
    ) {
      val primaryColor = MaterialTheme.colorScheme.primary
      val tertiaryColor = MaterialTheme.colorScheme.tertiary
      val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)

      Canvas(modifier = Modifier.size(190.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.minDimension / 2.2f
        val axes = 5

        // Draw concentric pentagons (25%, 50%, 75%, 100%)
        for (level in 1..4) {
          val r = maxRadius * (level / 4f)
          val ringPath = Path()
          for (i in 0 until axes) {
            val angle = (2 * PI * i / axes) - (PI / 2)
            val x = center.x + (r * cos(angle)).toFloat()
            val y = center.y + (r * sin(angle)).toFloat()
            if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
          }
          ringPath.close()
          drawPath(ringPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
        }

        // Draw axis spokes
        for (i in 0 until axes) {
          val angle = (2 * PI * i / axes) - (PI / 2)
          val end =
            Offset(
              center.x + (maxRadius * cos(angle)).toFloat(),
              center.y + (maxRadius * sin(angle)).toFloat(),
            )
          drawLine(gridColor, start = center, end = end, strokeWidth = 1.dp.toPx())
        }

        // Draw Agent Score Polygon
        val values =
          listOf(
            rapportScore,
            objectionScore,
            factFindingScore,
            preApproachScore,
            voiceToneScore,
          )
        val dataPath = Path()
        values.forEachIndexed { index, score ->
          val normalized = (score.coerceIn(10, 100) / 100f) * maxRadius
          val angle = (2 * PI * index / axes) - (PI / 2)
          val x = center.x + (normalized * cos(angle)).toFloat()
          val y = center.y + (normalized * sin(angle)).toFloat()
          if (index == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
        }
        dataPath.close()

        drawPath(dataPath, color = tertiaryColor.copy(alpha = 0.28f))
        drawPath(dataPath, color = primaryColor, style = Stroke(width = 3.dp.toPx()))

        // Draw vertices
        values.forEachIndexed { index, score ->
          val normalized = (score.coerceIn(10, 100) / 100f) * maxRadius
          val angle = (2 * PI * index / axes) - (PI / 2)
          val pt =
            Offset(
              center.x + (normalized * cos(angle)).toFloat(),
              center.y + (normalized * sin(angle)).toFloat(),
            )
          drawCircle(color = metrics[index].color, radius = 5.dp.toPx(), center = pt)
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Horizontal Dimension Bars
    metrics.forEach { metric ->
      val animatedFraction by
        animateFloatAsState(
          targetValue = (metric.score.coerceIn(0, 100) / 100f),
          animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
          label = metric.label,
        )

      Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = metric.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = "${metric.score}/100",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = metric.color,
          )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
          modifier =
            Modifier.fillMaxWidth()
              .height(10.dp)
              .clip(RoundedCornerShape(99.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
          Box(
            modifier =
              Modifier.fillMaxWidth(animatedFraction)
                .height(10.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(metric.color)
          )
        }
      }
    }
  }
}

@Composable
fun LiveVoiceWaveformBar(
  amplitude: Float,
  isRecording: Boolean,
  elapsedSeconds: Int,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
        .padding(horizontal = 14.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier =
          Modifier.size(10.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(if (isRecording) CoralAlert else EmeraldAccent)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text =
          if (isRecording) "กำลังบันทึกและวิเคราะห์น้ำเสียง... (${elapsedSeconds}s)"
          else "พร้อมรับเสียงพูดของคุณ",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.SemiBold,
      )
    }

    Canvas(modifier = Modifier.width(96.dp).height(26.dp)) {
      val barCount = 12
      val spacing = size.width / barCount
      val barWidth = spacing * 0.55f
      for (i in 0 until barCount) {
        val factor =
          if (isRecording) {
            val wave = sin((i * 0.7f) + (amplitude * 10f)).toFloat()
            ((amplitude * 0.8f) + (wave * 0.2f)).coerceIn(0.15f, 1.0f)
          } else {
            0.2f
          }
        val barHeight = size.height * factor
        val top = (size.height - barHeight) / 2f
        drawRoundRect(
          color = if (isRecording) CyanWaveform else CyanWaveform.copy(alpha = 0.4f),
          topLeft = Offset(i * spacing, top),
          size = Size(barWidth, barHeight),
          cornerRadius = CornerRadius(4f, 4f),
        )
      }
    }
  }
}
