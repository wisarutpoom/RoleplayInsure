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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.data.OnDeviceRolePlayAiEngine
import com.example.data.PlayerBadge
import com.example.data.StageAttempt
import com.example.data.VoiceRecordingMetrics
import com.example.ui.components.FiveDimensionRadarAndBars
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary

private data class GoldenScriptEntry(
  val id: String,
  val emoji: String,
  val categoryTitle: String,
  val shortHook: String,
  val goldenScript: String,
  val voiceModulationTip: String,
  val recommendedStageNum: Int,
)

private val goldenScriptsCatalog =
  listOf(
    GoldenScriptEntry(
      id = "script_1",
      emoji = "⚡",
      categoryTitle = "คัมภีร์ที่ 1: หยุดลูกค้าสายรีบภายใน 10 วินาที",
      shortHook = "ใช้ปราบผู้จัดการ 5G (ด่าน 1) และมาดาม CFO (ด่าน 6)",
      goldenScript =
        "\"เข้าใจเลยครับว่ากำลังจะเข้าประชุม! วันนี้ผมไม่ได้โทรมาขายทางโทรศัพท์ครับ แต่เห็นว่าพี่เพิ่งเลื่อนตำแหน่ง เลยจะขอส่งสรุปสิทธิ์ลดหย่อนภาษีผู้บริหารให้ดู 15 นาที สะดวกวันอังคารบ่าย หรือ วันพฤหัสเช้าดีครับ?\"",
      voiceModulationTip =
        "พูดด้วยความเร็ว 130-145 WPM (ห้ามเกิน 165 WPM) น้ำเสียงกระฉับกระเฉงและหยุดเว้นจังหวะหลังคำว่า 'วันพฤหัสเช้าดีครับ?'",
      recommendedStageNum = 1,
    ),
    GoldenScriptEntry(
      id = "script_2",
      emoji = "💉",
      categoryTitle = "คัมภีร์ที่ 2: ทลายคำพูด 'มีประกันเต็มตู้เซฟแล้ว'",
      shortHook = "ใช้ปราบเจ๊พิมพ์ โบท็อกซ์ทองคำ (ด่าน 2)",
      goldenScript =
        "\"สุดยอดเลยครับเจ๊พิมพ์ น้อยคนมากที่จะวางแผนไว้ถึง 5 เล่ม! ปกติลูกค้าที่มีหลายเล่มมักจำไม่ได้ว่าค่าห้อง รพ.เอกชนรวมกันได้คืนละเท่าไหร่ ผมมีบริการทำตารางสรุปทั้ง 5 เล่มให้อยู่ในกระดาษแผ่นเดียวฟรี สะดวกให้เข้าไปทำให้ดูวันไหนดีครับ?\"",
      voiceModulationTip =
        "ใช้น้ำเสียงชื่นชมอย่างจริงใจ ทอดเสียงอบอุ่น Неกดดัน และเน้นเสียงที่คำว่า 'สรุปในกระดาษแผ่นเดียวฟรี'",
      recommendedStageNum = 2,
    ),
    GoldenScriptEntry(
      id = "script_3",
      emoji = "🧱",
      categoryTitle = "คัมภีร์ที่ 3: สยบลูกค้าหัวร้อนที่โดนตัวแทนเก่าทิ้งงาน",
      shortHook = "ใช้ปราบเฮียวิชัย หินคลุกเหล็กเส้น (ด่าน 5)",
      goldenScript =
        "\"ผมเข้าใจความรู้สึกเฮียวิชัยเลยครับ ถ้าผมจ่ายเบี้ยแล้วตัวแทนหายหัวผมก็โกรธมากครับ ต้องขออภัยแทนเพื่อนร่วมอาชีพด้วยจริงๆ วันนี้ผมยังไม่ขอเสนอขายอะไรทั้งสิ้น แต่อยากอาสาเข้าไปช่วยเช็กสถานะเล่มเก่าทั้ง 3 เล่มของครอบครัวเฮียให้ฟรีครับ\"",
      voiceModulationTip =
        "ใช้น้ำเสียงทุ้ม สุขุม ความเร็วช้าลงเล็กน้อย (115-125 WPM) เพื่อให้ลูกค้าอารมณ์เย็นลงตามจังหวะเสียงของเรา",
      recommendedStageNum = 5,
    ),
    GoldenScriptEntry(
      id = "script_4",
      emoji = "📈",
      categoryTitle = "คัมภีร์ที่ 4: ปราบเซียนหุ้น 'ประกันผลตอบแทนเต่าคลาน'",
      shortHook = "ใช้ปราบโค้ชเคน กราฟเขียวแอบดอย (ด่าน 9)",
      goldenScript =
        "\"ผมเห็นด้วย 100% เลยครับคุณเคนว่าหุ้นทำกำไรได้สูงกว่าประกันมาก! เหตุผลที่นักลงทุนมืออาชีพแบ่งเงินก้อนเล็กมาไว้ในประกันสุขภาพ ไม่ใช่เพื่อเอากำไรครับ แต่เพื่อเป็น 'กันชน' ไม่ให้ต้องเทขายหุ้นตอนตลาดแดงเพื่อมาจ่ายค่าหมอหลักล้านครับ\"",
      voiceModulationTip =
        "พูดด้วยความมั่นใจ หนักแน่นที่คำว่า 'เห็นด้วย 100%' และคำว่า 'กันชนป้องกันพอร์ตแตก'",
      recommendedStageNum = 9,
    ),
    GoldenScriptEntry(
      id = "script_5",
      emoji = "🕵️",
      categoryTitle = "คัมภีร์ที่ 5: เทคนิคถามรายได้แบบลูกค้าไม่รู้สึกอึดอัด",
      shortHook = "ใช้ปราบคุณแพรว สายลับจัดซื้อ (ด่าน 13)",
      goldenScript =
        "\"เข้าใจเลยครับว่าเป็นข้อมูลส่วนตัว ที่ผมขออนุญาตสอบถามคร่าวๆ เพราะมาตรฐานการวางแผนที่ดีไม่ควรเกิน 10-15% ของรายได้ครับ ไม่ต้องบอกตัวเลขเป๊ะๆ ก็ได้ครับ แค่อยู่ในช่วง 50,000-80,000 หรือเกิน 100,000 บาทต่อเดือนครับ?\"",
      voiceModulationTip =
        "ใช้น้ำเสียงนุ่มนวล สุภาพ เหมือนแพทย์ถามอาการคนไข้ ไม่จ้องจับผิด",
      recommendedStageNum = 13,
    ),
    GoldenScriptEntry(
      id = "script_6",
      emoji = "🐉",
      categoryTitle = "คัมภีร์ที่ 6: สรุป 3 เสาหลักพิชิตเจ้าสัวมังกรทอง",
      shortHook = "ใช้ปราบบอสใหญ่ด่านที่ 25 (เจ้าสัวธนินทร์)",
      goldenScript =
        "\"จากข้อมูลทั้งหมดของท่านเจ้าสัว ผมขอสรุปโครงสร้างเป็น 3 เสาหลักครับ: เสาที่ 1 สุขภาพระดับ VIP ทั่วโลก, เสาที่ 2 ประกัน Keyman ปกป้องเครดิตบริษัท, และเสาที่ 3 กองทุนมรดกเงินสดปลอดภาษีเพื่อให้ทายาททั้ง 2 คนได้รับความเป็นธรรมสูงสุดครับ\"",
      voiceModulationTip =
        "เว้นจังหวะ 1 วินาทีระหว่างการไล่เรียง เสาที่ 1.. เสาที่ 2.. เสาที่ 3.. เพื่อให้ดูเป็นปรมาจารย์ระดับประเทศ",
      recommendedStageNum = 25,
    ),
  )

@Composable
fun TrophyAndScriptVaultScreen(
  equippedFunnyTitle: String,
  attempts: List<StageAttempt>,
  lastVoiceMetrics: VoiceRecordingMetrics?,
  onEquipFunnyTitle: (String) -> Unit,
  onStartStage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedSubTab by remember { mutableIntStateOf(0) } // 0 = เหรียญตรา & ฉายาฮาๆ, 1 = คัมภีร์บทพูดทองคำ, 2 = คะแนน 5 มิติ
  var selectedBadgeDetail by remember { mutableStateOf<PlayerBadge?>(null) }
  var selectedScriptDetail by remember { mutableStateOf<GoldenScriptEntry?>(null) }

  val badges =
    remember(attempts, lastVoiceMetrics) {
      OnDeviceRolePlayAiEngine.evaluatePlayerBadges(attempts, lastVoiceMetrics)
    }
  val unlockedCount = badges.count { it.unlocked }

  val badgeDialog = selectedBadgeDetail
  if (badgeDialog != null) {
    AlertDialog(
      onDismissRequest = { selectedBadgeDetail = null },
      icon = {
        Text(text = badgeDialog.emoji, style = MaterialTheme.typography.headlineMedium)
      },
      title = {
        Text(
          text = badgeDialog.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "👑 ฉายาประจำเหรียญ: \"${badgeDialog.funnyRankName}\"",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
          )
          Text(
            text = "🎯 เงื่อนไขการปลดล็อก: ${badgeDialog.conditionDescription}",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text =
              if (badgeDialog.unlocked) "✅ สถานะ: ปลดล็อกเรียบร้อยแล้ว!"
              else "⏳ ความคืบหน้า: ${badgeDialog.progressText}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = if (badgeDialog.unlocked) EmeraldAccent else CoralAlert,
          )
        }
      },
      confirmButton = {
        if (badgeDialog.unlocked) {
          Button(
            onClick = {
              onEquipFunnyTitle(badgeDialog.funnyRankName)
              selectedBadgeDetail = null
            }
          ) {
            Text("สวมใส่ฉายานี้")
          }
        } else {
          TextButton(onClick = { selectedBadgeDetail = null }) { Text("ปิด") }
        }
      },
      dismissButton = {
        if (badgeDialog.unlocked) {
          TextButton(onClick = { selectedBadgeDetail = null }) { Text("ปิด") }
        }
      },
    )
  }

  val scriptDialog = selectedScriptDetail
  if (scriptDialog != null) {
    AlertDialog(
      onDismissRequest = { selectedScriptDetail = null },
      icon = { Icon(Icons.Default.Lightbulb, contentDescription = null, tint = GoldPrimary) },
      title = {
        Text(
          text = scriptDialog.categoryTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "💬 บทพูดทองคำ (เอาไปพูดตามได้เลย):",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
          )
          Text(
            text = scriptDialog.goldenScript,
            style = MaterialTheme.typography.bodySmall,
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "🎙️ เทคนิคคุมน้ำเสียง (Voice Coaching):",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = CyanWaveform,
          )
          Text(
            text = scriptDialog.voiceModulationTip,
            style = MaterialTheme.typography.bodySmall,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val stageNum = scriptDialog.recommendedStageNum
            selectedScriptDetail = null
            onStartStage(stageNum)
          }
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("ฝึกด่านที่ ${scriptDialog.recommendedStageNum}")
        }
      },
      dismissButton = {
        TextButton(onClick = { selectedScriptDetail = null }) { Text("ปิด") }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Summary Header Card
    item {
      Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        modifier =
          Modifier.fillMaxWidth()
            .border(1.5.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(20.dp)),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth().padding(14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "🏅 ตู้โชว์เหรียญตรา & คัมภีร์ลับยุทธภพ",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.ExtraBold,
              color = GoldPrimary,
            )
            Text(
              text = "ฉายาปัจจุบัน: \"$equippedFunnyTitle\"",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Bold,
              color = EmeraldAccent,
            )
            Text(
              text = "ปลดล็อกแล้ว $unlockedCount / ${badges.size} เหรียญตราสุดฮา",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Box(
            modifier =
              Modifier.size(48.dp)
                .clip(CircleShape)
                .background(GoldPrimary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.EmojiEvents,
              contentDescription = null,
              tint = GoldPrimary,
              modifier = Modifier.size(28.dp),
            )
          }
        }
      }
    }

    // Sub-tabs
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        FilterChip(
          selected = selectedSubTab == 0,
          onClick = { selectedSubTab = 0 },
          label = { Text("🏅 เหรียญฉายา (${unlockedCount}/${badges.size})") },
          modifier = Modifier.testTag("vault_tab_badges"),
        )
        FilterChip(
          selected = selectedSubTab == 1,
          onClick = { selectedSubTab = 1 },
          label = { Text("📜 คัมภีร์บทพูด") },
          modifier = Modifier.testTag("vault_tab_scripts"),
        )
        FilterChip(
          selected = selectedSubTab == 2,
          onClick = { selectedSubTab = 2 },
          label = { Text("📊 สถิติ 5 มิติ") },
          modifier = Modifier.testTag("vault_tab_radar"),
        )
      }
    }

    when (selectedSubTab) {
      0 -> {
        items(badges, key = { it.id }) { badge ->
          val isEquipped = equippedFunnyTitle == badge.funnyRankName
          Card(
            colors =
              CardDefaults.cardColors(
                containerColor =
                  if (badge.unlocked) MaterialTheme.colorScheme.surface
                  else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
              ),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .border(
                  width = if (isEquipped) 1.5.dp else 1.dp,
                  color =
                    if (isEquipped) EmeraldAccent
                    else if (badge.unlocked) GoldPrimary.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(14.dp),
                )
                .clickable { selectedBadgeDetail = badge },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier =
                    Modifier.size(42.dp)
                      .clip(CircleShape)
                      .background(
                        if (badge.unlocked) GoldPrimary.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surface
                      ),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = if (badge.unlocked) badge.emoji else "🔒",
                    style = MaterialTheme.typography.titleMedium,
                  )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = badge.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                      if (badge.unlocked) MaterialTheme.colorScheme.onSurface
                      else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = "ฉายา: \"${badge.funnyRankName}\"",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (badge.unlocked) GoldPrimary else CyanWaveform,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = { selectedBadgeDetail = badge },
                  modifier = Modifier.size(34.dp),
                ) {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "ดูเงื่อนไขเหรียญ",
                    tint = CyanWaveform,
                    modifier = Modifier.size(18.dp),
                  )
                }

                if (badge.unlocked) {
                  OutlinedButton(
                    onClick = { onEquipFunnyTitle(badge.funnyRankName) },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  ) {
                    Text(
                      text = if (isEquipped) "✅ สวมอยู่" else "สวมฉายา",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (isEquipped) EmeraldAccent else GoldPrimary,
                    )
                  }
                } else {
                  Text(
                    text = badge.progressText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          }
        }
      }
      1 -> {
        items(goldenScriptsCatalog, key = { it.id }) { script ->
          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            modifier =
              Modifier.fillMaxWidth()
                .border(1.dp, GoldPrimary.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .clickable { selectedScriptDetail = script },
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text(text = script.emoji, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = script.categoryTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = script.shortHook,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyanWaveform,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
              IconButton(onClick = { selectedScriptDetail = script }) {
                Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = "เปิดอ่านคัมภีร์บทพูด",
                  tint = GoldPrimary,
                )
              }
            }
          }
        }
      }
      else -> {
        item {
          val avgRapport =
            if (attempts.isNotEmpty()) attempts.map { it.rapportScore }.average().toInt() else 85
          val avgObj =
            if (attempts.isNotEmpty()) attempts.map { it.objectionScore }.average().toInt() else 84
          val avgFact =
            if (attempts.isNotEmpty()) attempts.map { it.factFindingScore }.average().toInt()
            else 80
          val avgPre =
            if (attempts.isNotEmpty()) attempts.map { it.preApproachScore }.average().toInt()
            else 84
          val avgVoice =
            if (attempts.isNotEmpty()) attempts.map { it.voiceToneScore }.average().toInt() else 86

          Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Text(
                text = "📊 ค่าเฉลี่ยพลังยุทธ์ 5 มิติของคุณ",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = GoldPrimary,
              )
              Spacer(modifier = Modifier.height(8.dp))
              FiveDimensionRadarAndBars(
                rapportScore = avgRapport,
                objectionScore = avgObj,
                factFindingScore = avgFact,
                preApproachScore = avgPre,
                voiceToneScore = avgVoice,
              )
            }
          }
        }
      }
    }
  }
}
