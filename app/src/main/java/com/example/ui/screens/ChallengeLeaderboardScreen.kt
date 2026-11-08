package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.LeaderboardCompetitor
import com.example.data.OnDeviceRolePlayAiEngine
import com.example.data.RolePlayCurriculum
import com.example.data.StageAttempt
import com.example.data.StageRankSummary
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary

@Composable
fun ChallengeLeaderboardScreen(
  playerName: String,
  equippedFunnyTitle: String,
  attempts: List<StageAttempt>,
  onStartStage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = รางวัลคะแนนสูงสุด, 1 = รางวัลจำนวนครั้ง, 2 = อันดับของคุณใน 25 ด่าน
  var showRulesDialog by remember { mutableStateOf(false) }
  var selectedStageRankDetail by remember { mutableStateOf<StageRankSummary?>(null) }

  val (highScoreBoard, mostAttemptsBoard) =
    remember(playerName, equippedFunnyTitle, attempts) {
      OnDeviceRolePlayAiEngine.buildChallengeLeaderboards(
        currentPlayerName = playerName,
        currentFunnyTitle = equippedFunnyTitle,
        attempts = attempts,
      )
    }

  val stageRanks =
    remember(attempts) {
      OnDeviceRolePlayAiEngine.buildStageRanksForAll25Stages(attempts)
    }

  val myHighScoreEntry = highScoreBoard.find { it.isCurrentUser } ?: highScoreBoard.last()
  val myAttemptsEntry = mostAttemptsBoard.find { it.isCurrentUser } ?: mostAttemptsBoard.last()

  if (showRulesDialog) {
    AlertDialog(
      onDismissRequest = { showRulesDialog = false },
      icon = { Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldPrimary) },
      title = {
        Text("🏆 กติกาศึกชิงแชมป์ & รางวัล Top 1–10", fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "1. 🏆 รางวัลคะแนนสูงสุด (High Score Award): จัดอันดับ Top 1–10 จากคะแนนสูงสุดของแต่ละด่านรวมกัน + โบนัสความขยัน",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "2. 🔥 รางวัลจำนวนครั้งสูงสุด (Most Attempts Award): จัดอันดับ Top 1–10 นักซ้อมสายถึกที่เข้าฝึกซ้อมมากครั้งที่สุด",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "3. 📍 อันดับรายด่าน (Stage Rank #1–#10): แสดงว่าในแต่ละด่าน (1–25) คะแนนสูงสุดของคุณอยู่อันดับที่เท่าไรเมื่อเทียบกับคู่แข่งระดับท็อป!",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      },
      confirmButton = {
        TextButton(onClick = { showRulesDialog = false }) { Text("เข้าใจแล้ว") }
      },
    )
  }

  val detailRank = selectedStageRankDetail
  if (detailRank != null) {
    val stageObj = RolePlayCurriculum.getStageByNumber(detailRank.stageNumber)
    AlertDialog(
      onDismissRequest = { selectedStageRankDetail = null },
      icon = { Icon(Icons.Default.Info, contentDescription = null, tint = CyanWaveform) },
      title = {
        Text(detailRank.stageTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "🏅 อันดับของคุณในด่านนี้: อันดับที่ #${detailRank.yourRank} / 10",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (detailRank.yourRank <= 3) GoldPrimary else CyanWaveform,
          )
          Text(
            text = "📊 คะแนนสูงสุดของคุณ: ${detailRank.yourBestScore}/100 (ซ้อมแล้ว ${detailRank.yourAttempts} ครั้ง)",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "👑 สถิติสูงสุดของด่านนี้: ${detailRank.topScoreOnStage}/100 โดย ${detailRank.topHolderName}",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldAccent,
          )
          Text(
            text = "👤 คู่สนทนา: ${stageObj.customerName} (${stageObj.customerOccupation})\n🎯 ภารกิจ: ${stageObj.missionObjective}",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val num = detailRank.stageNumber
            selectedStageRankDetail = null
            onStartStage(num)
          }
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ลุยชิงอันดับ 1")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedStageRankDetail = null }) { Text("ปิด") }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 1. Your Personal Challenge Rank Summary Banner
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.5.dp, GoldPrimary.copy(alpha = 0.6f), RoundedCornerShape(20.dp)),
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = GoldPrimary,
                modifier = Modifier.size(26.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  text = "ศึกชิงแชมป์ยุทธภพ (Challenge Top 1–10)",
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.ExtraBold,
                  color = GoldPrimary,
                )
                Text(
                  text = "ฉายาของคุณ: \"$equippedFunnyTitle\"",
                  style = MaterialTheme.typography.labelSmall,
                  color = CyanWaveform,
                )
              }
            }
            IconButton(
              onClick = { showRulesDialog = true },
              modifier = Modifier.size(34.dp),
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "กติกาการแข่งขัน",
                tint = GoldPrimary,
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // กล่องอันดับคะแนนสูงสุด
            Box(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(GoldPrimary.copy(alpha = 0.12f))
                  .padding(10.dp)
            ) {
              Column {
                Text(
                  text = "🏆 อันดับคะแนนรวม",
                  style = MaterialTheme.typography.labelSmall,
                  color = GoldPrimary,
                )
                Text(
                  text = "อันดับ #${myHighScoreEntry.rank}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "${myHighScoreEntry.highScorePoints} แต้มสะสม",
                  style = MaterialTheme.typography.labelSmall,
                  color = EmeraldAccent,
                )
              }
            }

            // กล่องอันดับจำนวนครั้งสูงสุด
            Box(
              modifier =
                Modifier.weight(1f)
                  .clip(RoundedCornerShape(14.dp))
                  .background(CyanWaveform.copy(alpha = 0.12f))
                  .padding(10.dp)
            ) {
              Column {
                Text(
                  text = "🔥 อันดับขยันซ้อม",
                  style = MaterialTheme.typography.labelSmall,
                  color = CyanWaveform,
                )
                Text(
                  text = "อันดับ #${myAttemptsEntry.rank}",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.ExtraBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                  text = "ซ้อมแล้ว ${myAttemptsEntry.totalAttempts} ครั้ง",
                  style = MaterialTheme.typography.labelSmall,
                  color = GoldPrimary,
                )
              }
            }
          }
        }
      }
    }

    // 2. Sub-Tab Selector (รางวัลคะแนนสูงสุด / รางวัลจำนวนครั้ง / อันดับรายด่าน 1-25)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        FilterChip(
          selected = selectedSubTab == 0,
          onClick = { selectedSubTab = 0 },
          label = { Text("🏆 คะแนนสูงสุด (1-10)") },
          modifier = Modifier.testTag("challenge_tab_highscore"),
        )
        FilterChip(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          label = { Text("🔥 จำนวนครั้ง (1-10)") },
          modifier = Modifier.testTag("challenge_tab_attempts"),
        )
        FilterChip(
          selected = selectedSubTab == 2,
          onClick = { selectedSubTab = 2 },
          label = { Text("📍 อันดับ 25 ด่าน") },
          modifier = Modifier.testTag("challenge_tab_stageranks"),
        )
      }
    }

    // 3. Content according to selected sub-tab
    when (selectedSubTab) {
      0 -> {
        items(highScoreBoard, key = { "hs_${it.playerId}" }) { entry ->
          CompetitorRankRow(
            entry = entry,
            isAttemptsMode = false,
          )
        }
      }
      1 -> {
        items(mostAttemptsBoard, key = { "att_${it.playerId}" }) { entry ->
          CompetitorRankRow(
            entry = entry,
            isAttemptsMode = true,
          )
        }
      }
      else -> {
        items(stageRanks, key = { "sr_${it.stageNumber}" }) { stageRank ->
          StageRankCardRow(
            stageRank = stageRank,
            onShowInfo = { selectedStageRankDetail = stageRank },
            onPlayStage = { onStartStage(stageRank.stageNumber) },
          )
        }
      }
    }
  }
}

@Composable
private fun CompetitorRankRow(
  entry: LeaderboardCompetitor,
  isAttemptsMode: Boolean,
) {
  val rankBadgeColor =
    when (entry.rank) {
      1 -> GoldPrimary
      2 -> CyanWaveform
      3 -> EmeraldAccent
      else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

  val rankIcon =
    when (entry.rank) {
      1 -> "🥇"
      2 -> "🥈"
      3 -> "🥉"
      else -> "#${entry.rank}"
    }

  Card(
    colors =
      CardDefaults.cardColors(
        containerColor =
          if (entry.isCurrentUser) GoldPrimary.copy(alpha = 0.16f)
          else MaterialTheme.colorScheme.surface
      ),
    shape = RoundedCornerShape(14.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          width = if (entry.isCurrentUser) 1.5.dp else 1.dp,
          color =
            if (entry.isCurrentUser) GoldPrimary
            else MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(14.dp),
        ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.size(38.dp)
              .clip(CircleShape)
              .background(rankBadgeColor.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = rankIcon,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = rankBadgeColor,
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "${entry.avatarEmoji} ${entry.playerName}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color =
                if (entry.isCurrentUser) GoldPrimary else MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (entry.isCurrentUser) {
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "(คุณ)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = EmeraldAccent,
              )
            }
          }
          Text(
            text = "ฉายา: ${entry.funnyTitle} • ผ่าน ${entry.stagesCleared}/25 ด่าน",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Column(horizontalAlignment = Alignment.End) {
        if (isAttemptsMode) {
          Text(
            text = "🔥 ${entry.totalAttempts} ครั้ง",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = CyanWaveform,
          )
          Text(
            text = "แต้มรวม ${entry.highScorePoints}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        } else {
          Text(
            text = "🏆 ${entry.highScorePoints} แต้ม",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = GoldPrimary,
          )
          Text(
            text = "สูงสุด ${entry.bestSingleScore}/100",
            style = MaterialTheme.typography.labelSmall,
            color = EmeraldAccent,
          )
        }
      }
    }
  }
}

@Composable
private fun StageRankCardRow(
  stageRank: StageRankSummary,
  onShowInfo: () -> Unit,
  onPlayStage: () -> Unit,
) {
  val isTop3 = stageRank.yourRank <= 3 && stageRank.yourBestScore > 0
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(14.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          if (isTop3) GoldPrimary.copy(alpha = 0.5f)
          else MaterialTheme.colorScheme.surfaceVariant,
          RoundedCornerShape(14.dp),
        ),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.clip(RoundedCornerShape(10.dp))
              .background(
                if (isTop3) GoldPrimary.copy(alpha = 0.2f)
                else CyanWaveform.copy(alpha = 0.14f)
              )
              .padding(horizontal = 8.dp, vertical = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = "อันดับ",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "#${stageRank.yourRank}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = if (isTop3) GoldPrimary else CyanWaveform,
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stageRank.stageTitle,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = "👤 ${stageRank.customerName} • คุณได้ ${stageRank.yourBestScore}/100 (${stageRank.yourAttempts} ครั้ง)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onShowInfo, modifier = Modifier.size(34.dp)) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "ดูข้อมูลอันดับด่านที่ ${stageRank.stageNumber}",
            tint = CyanWaveform,
            modifier = Modifier.size(18.dp),
          )
        }
        Button(
          onClick = onPlayStage,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
          colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary),
        ) {
          Text("ชิงแชมป์", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
