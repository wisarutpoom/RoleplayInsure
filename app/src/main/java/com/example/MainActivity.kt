package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.GoogleSignInButton
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.configureAppCheck
import com.example.auth.signOut
import com.example.data.OnDeviceRolePlayAiEngine
import com.example.data.RolePlayCurriculum
import com.example.data.RolePlayStage
import com.example.data.StageAttempt
import com.example.data.StageAttemptRepository
import com.example.data.StageCategory
import com.example.ui.screens.ChallengeLeaderboardScreen
import com.example.ui.screens.RolePlayStageScreen
import com.example.ui.screens.TrophyAndScriptVaultScreen
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.CyanWaveform
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AttemptsUiState
import com.example.viewmodel.InsureQuestViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Mandatory Step 1: Configure Firebase App Check with launch intent before any UI or Firebase calls
    configureAppCheck(this, intent)

    enableEdgeToEdge()
    setContent {
      var isDarkTheme by rememberSaveable { mutableStateOf(true) }

      MyApplicationTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          AppNavigation(
            isDarkTheme = isDarkTheme,
            onToggleTheme = { isDarkTheme = !isDarkTheme },
          )
        }
      }
    }
  }
}

@Composable
fun AppNavigation(
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  auth: FirebaseAuth = Firebase.auth,
) {
  val currentUser by
    auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
  val user = currentUser
  // Internal testing mode: bypass login screen by default so the team can test all features immediately
  var bypassLoginForInternalTest by rememberSaveable { mutableStateOf(true) }

  if (user == null && !bypassLoginForInternalTest) {
    AuthWelcomeScreen(
      isDarkTheme = isDarkTheme,
      onToggleTheme = onToggleTheme,
      onBypassLogin = { bypassLoginForInternalTest = true },
    )
  } else {
    val userEmail = user?.email ?: "Wisarutpoom@gmail.com"
    val isMasterAdmin =
      user == null || userEmail.equals("Wisarutpoom@gmail.com", ignoreCase = true)
    MainFeatureScreen(
      currentUserId = user?.uid ?: "internal_test_admin",
      currentUserEmail = userEmail,
      initialDisplayName =
        user?.displayName
          ?: if (user == null) "คุณวิศรุต"
          else userEmail.substringBefore("@").ifBlank { "Agent" },
      isMasterAdmin = isMasterAdmin,
      isInternalTestBypass = user == null,
      isDarkTheme = isDarkTheme,
      onToggleTheme = onToggleTheme,
      onShowLoginScreen = { bypassLoginForInternalTest = false },
    )
  }
}

@Composable
fun ThaiHeritageGoldOrnament(modifier: Modifier = Modifier) {
  val goldColor = MaterialTheme.colorScheme.primary
  Canvas(modifier = modifier.fillMaxWidth().height(16.dp)) {
    val w = size.width
    val h = size.height
    val cy = h / 2f
    val cx = w / 2f

    drawLine(
      color = goldColor.copy(alpha = 0.35f),
      start = Offset(24f, cy),
      end = Offset(cx - 36f, cy),
      strokeWidth = 2f,
    )
    drawLine(
      color = goldColor.copy(alpha = 0.35f),
      start = Offset(cx + 36f, cy),
      end = Offset(w - 24f, cy),
      strokeWidth = 2f,
    )

    val centerDiamond =
      Path().apply {
        moveTo(cx, cy - 7f)
        lineTo(cx + 9f, cy)
        lineTo(cx, cy + 7f)
        lineTo(cx - 9f, cy)
        close()
      }
    drawPath(path = centerDiamond, color = goldColor)

    for (dir in listOf(-1f, 1f)) {
      val sx = cx + dir * 20f
      val sideDiamond =
        Path().apply {
          moveTo(sx, cy - 4f)
          lineTo(sx + 5f, cy)
          lineTo(sx, cy + 4f)
          lineTo(sx - 5f, cy)
          close()
        }
      drawPath(path = sideDiamond, color = goldColor.copy(alpha = 0.7f))
    }
  }
}

@Composable
fun AuthWelcomeScreen(
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onBypassLogin: () -> Unit = {},
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }
  var authError by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {},
      onUnauthenticated = {},
      scope = scope,
    )
  }

  Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { innerPadding ->
    LazyColumn(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      contentPadding = PaddingValues(vertical = 20.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column {
            Text(
              text = "⚜️ INSUREQUEST สยามอคาเดมี",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = "ยุทธภพนักขายประกัน 25 ด่าน • คุยสดเรียลไทม์",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          OutlinedButton(
            onClick = onToggleTheme,
            shape = RoundedCornerShape(99.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("welcome_theme_toggle_button"),
          ) {
            Icon(
              imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = "สลับโหมดมืด-สว่าง",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isDarkTheme) "โหมดสว่าง" else "โหมดมืด",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
            )
          }
        }
      }

      item { ThaiHeritageGoldOrnament() }

      item {
        Image(
          painter = painterResource(id = R.drawable.img_hero_coach_1794022212140),
          contentDescription = "InsureQuest AI Role-Play Coach Banner",
          modifier =
            Modifier.fillMaxWidth()
              .height(190.dp)
              .clip(RoundedCornerShape(22.dp))
              .border(
                1.5.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                RoundedCornerShape(22.dp),
              ),
          contentScale = ContentScale.Crop,
        )
      }

      item {
        Column {
          Text(
            text = "ยุทธภพนักขาย 25 ด่าน • คุยสดเรียลไทม์กับ AI ฝังในแอพ (เล่นไม่จำกัดครั้ง)",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text =
              "ฝึกพูดโต้ตอบเรียลไทม์กับลูกค้าสุดฮา 25 คาแรคเตอร์ พร้อมระบบ Voice Coaching จับความเร็วการพูด (>165 WPM) แดชบอร์ดศึกชิงแชมป์ Top 1-10 และปลดล็อกเหรียญตราฉายาสุดฮา!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        Button(
          onClick = onBypassLogin,
          shape = RoundedCornerShape(14.dp),
          colors =
            ButtonDefaults.buttonColors(
              containerColor = EmeraldAccent,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
          modifier = Modifier.fillMaxWidth().height(54.dp).testTag("bypass_login_test_button"),
        ) {
          Text(
            text = "🚀 เข้าสู่ยุทธภพนักขายทันที (ไม่ต้อง Login)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
          )
        }
      }

      if (authError != null) {
        item {
          Card(
            colors = CardDefaults.cardColors(containerColor = CoralAlert.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Text(
              text = authError!!,
              style = MaterialTheme.typography.bodySmall,
              color = CoralAlert,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
      }

      item {
        GoogleSignInButton(
          onAuthSuccess = { authError = null },
          onAuthError = { msg -> authError = msg },
        )
      }
    }
  }
}

@Composable
fun MainFeatureScreen(
  currentUserId: String,
  currentUserEmail: String,
  initialDisplayName: String,
  isMasterAdmin: Boolean,
  isInternalTestBypass: Boolean = false,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onShowLoginScreen: () -> Unit = {},
) {
  val viewModel: InsureQuestViewModel =
    viewModel(
      key = "${currentUserId}_$isMasterAdmin",
      factory =
        viewModelFactory {
          initializer {
            val app =
              checkNotNull(this[APPLICATION_KEY]) {
                "APPLICATION_KEY missing from CreationExtras"
              }
            val databaseId = app.getString(R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(databaseId)
            InsureQuestViewModel(
              repository = StageAttemptRepository(db),
              currentUserId = currentUserId,
              initialAgentName = initialDisplayName,
              currentUserEmail = currentUserEmail,
              isAdminUser = isMasterAdmin,
            )
          }
        },
    )

  val activeStage by viewModel.activeStage.collectAsStateWithLifecycle()
  val chatTurns by viewModel.chatTurns.collectAsStateWithLifecycle()
  val aiState by viewModel.aiState.collectAsStateWithLifecycle()
  val evaluationResult by viewModel.evaluationResult.collectAsStateWithLifecycle()
  val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
  val attemptsState by viewModel.attemptsState.collectAsStateWithLifecycle()
  val lastVoiceMetrics by viewModel.lastVoiceMetrics.collectAsStateWithLifecycle()
  val agentName by viewModel.agentName.collectAsStateWithLifecycle()
  val equippedFunnyTitle by viewModel.equippedFunnyTitle.collectAsStateWithLifecycle()
  val useBuiltInOnDeviceAi by viewModel.useBuiltInOnDeviceAi.collectAsStateWithLifecycle()
  val quotaExhaustedPopupMessage by viewModel.quotaExhaustedPopupMessage.collectAsStateWithLifecycle()

  var currentTab by rememberSaveable { mutableIntStateOf(0) }
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  Scaffold(
    contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
      if (activeStage == null) {
        NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
          NavigationBarItem(
            selected = currentTab == 0,
            onClick = { currentTab = 0 },
            icon = { Icon(Icons.Default.Map, contentDescription = "ยุทธภพนักขาย 25 ด่าน") },
            label = { Text("ยุทธภพนักขาย") },
            modifier = Modifier.testTag("nav_stages_tab"),
          )
          NavigationBarItem(
            selected = currentTab == 1,
            onClick = { currentTab = 1 },
            icon = {
              Icon(
                Icons.Default.EmojiEvents,
                contentDescription = "ศึกชิงแชมป์ Top 1-10",
              )
            },
            label = { Text("ศึกชิงแชมป์") },
            modifier = Modifier.testTag("nav_challenge_tab"),
          )
          NavigationBarItem(
            selected = currentTab == 2,
            onClick = { currentTab = 2 },
            icon = {
              Icon(
                Icons.Default.WorkspacePremium,
                contentDescription = "คัมภีร์ลับ & เหรียญเกียรติยศ",
              )
            },
            label = { Text("คัมภีร์ & เหรียญ") },
            modifier = Modifier.testTag("nav_trophy_vault_tab"),
          )
        }
      }
    },
  ) { innerPadding ->
    val stage = activeStage
    if (stage != null) {
      RolePlayStageScreen(
        stage = stage,
        chatTurns = chatTurns,
        aiState = aiState,
        evaluationResult = evaluationResult,
        agentName = agentName,
        useBuiltInOnDeviceAi = useBuiltInOnDeviceAi,
        quotaExhaustedPopupMessage = quotaExhaustedPopupMessage,
        onDismissQuotaExhaustedPopup = { viewModel.dismissQuotaExhaustedPopup() },
        onSimulateQuotaExhausted = { viewModel.triggerQuotaExhaustedPreview() },
        onResetQuotaCounter = { viewModel.resetQuotaGuardForTesting() },
        onToggleAiMode = { viewModel.toggleAiEngineMode() },
        onSendTurn = { text, audioBytes, metrics ->
          viewModel.sendAgentTurn(text, audioBytes, metrics)
        },
        onSimulateVoicePattern = { pattern ->
          viewModel.sendSimulatedVoicePatternTurn(pattern)
        },
        onEvaluateStage = { viewModel.evaluateCurrentSession() },
        onReplayStage = { viewModel.startStage(stage.stageNumber) },
        onNextStage = {
          val nextNum = (stage.stageNumber + 1).coerceAtMost(25)
          viewModel.startStage(nextNum)
        },
        onBack = { viewModel.exitStage() },
        onDismissError = { viewModel.clearAiError() },
        modifier = Modifier.padding(innerPadding),
      )
    } else {
      val attemptsList =
        (attemptsState as? AttemptsUiState.Success)?.attempts ?: emptyList()

      Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        // Top Player Header with Equipped Funny Title + 1-Tap Dark/Light Mode Switch
        Surface(
          color = MaterialTheme.colorScheme.surface,
          tonalElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "⚜️ $agentName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Box(
                    modifier =
                      Modifier.clip(RoundedCornerShape(99.dp))
                        .background(EmeraldAccent.copy(alpha = 0.2f))
                        .clickable { currentTab = 2 }
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                  ) {
                    Text(
                      text = "🏅 $equippedFunnyTitle",
                      style = MaterialTheme.typography.labelSmall,
                      fontWeight = FontWeight.Bold,
                      color = EmeraldAccent,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  }
                }
                val passedStagesCount =
                  attemptsList.filter { it.passed }.map { it.stageNumber }.distinct().size
                val engineLabel =
                  if (useBuiltInOnDeviceAi) "🧠 AI สำรองในแอพ (ไม่จำกัด)"
                  else "✨ Gemini 3.5 Flash-Lite (Free 1,000 ครั้ง/วัน)"
                Text(
                  text =
                    "$engineLabel • พิชิต $passedStagesCount/25 ด่าน • ซ้อม ${attemptsList.size} ครั้ง",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                  onClick = onToggleTheme,
                  modifier = Modifier.testTag("theme_toggle_button"),
                ) {
                  Icon(
                    imageVector =
                      if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription =
                      if (isDarkTheme) "สลับเป็นโหมดสว่าง" else "สลับเป็นโหมดมืด",
                    tint = MaterialTheme.colorScheme.primary,
                  )
                }

                IconButton(
                  onClick = {
                    if (isInternalTestBypass) {
                      onShowLoginScreen()
                    } else {
                      signOut(
                        context = context,
                        credentialManager = credentialManager,
                        onSignOutComplete = { onShowLoginScreen() },
                        scope = scope,
                      )
                    }
                  },
                  modifier = Modifier.testTag("sign_out_button"),
                ) {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "สลับไปหน้าตั้งค่า Login / ออกจากระบบ",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            ThaiHeritageGoldOrnament()
          }
        }

        when (currentTab) {
          0 ->
            StagesCampaignMapTab(
              selectedCategory = selectedCategory,
              attempts = attemptsList,
              onSelectCategory = { viewModel.selectCategory(it) },
              onStartStage = { stageNum -> viewModel.startStage(stageNum) },
              modifier = Modifier.weight(1f),
            )
          1 ->
            ChallengeLeaderboardScreen(
              playerName = agentName,
              equippedFunnyTitle = equippedFunnyTitle,
              attempts = attemptsList,
              onStartStage = { stageNum -> viewModel.startStage(stageNum) },
              modifier = Modifier.weight(1f),
            )
          else ->
            TrophyAndScriptVaultScreen(
              equippedFunnyTitle = equippedFunnyTitle,
              attempts = attemptsList,
              lastVoiceMetrics = lastVoiceMetrics,
              onEquipFunnyTitle = { newTitle -> viewModel.equipFunnyTitle(newTitle) },
              onStartStage = { stageNum -> viewModel.startStage(stageNum) },
              modifier = Modifier.weight(1f),
            )
        }
      }
    }
  }
}

@Composable
private fun StagesCampaignMapTab(
  selectedCategory: StageCategory?,
  attempts: List<StageAttempt>,
  onSelectCategory: (StageCategory?) -> Unit,
  onStartStage: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var detailStageForDialog by remember { mutableStateOf<RolePlayStage?>(null) }

  val bestScoreByStage =
    remember(attempts) {
      attempts.groupBy { it.stageNumber }.mapValues { entry ->
        entry.value.maxOfOrNull { it.overallScore } ?: 0
      }
    }

  val rankByStage =
    remember(attempts) {
      OnDeviceRolePlayAiEngine.buildStageRanksForAll25Stages(attempts)
        .associate { it.stageNumber to it.yourRank }
    }

  val recommendedStage =
    remember(bestScoreByStage) {
      val nextUnpassed =
        (1..25).firstOrNull { stageNum -> (bestScoreByStage[stageNum] ?: 0) < 70 } ?: 1
      RolePlayCurriculum.getStageByNumber(nextUnpassed)
    }

  val filteredStages =
    remember(selectedCategory) {
      if (selectedCategory == null) {
        RolePlayCurriculum.allStages
      } else {
        RolePlayCurriculum.allStages.filter { it.category == selectedCategory }
      }
    }

  // แฟ้มลับเตรียมตัวก่อนคุย (แสดงเมื่อกดปุ่ม ℹ️ บนการ์ดแต่ละด่าน)
  val dialogStage = detailStageForDialog
  if (dialogStage != null) {
    val myStageRank = rankByStage[dialogStage.stageNumber] ?: 10
    AlertDialog(
      onDismissRequest = { detailStageForDialog = null },
      icon = {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
      },
      title = {
        Text(
          text = "📂 แฟ้มข้อมูลเตรียมตัวก่อนคุย\n${dialogStage.title}",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "🏅 อันดับปัจจุบันของคุณในด่านนี้: อันดับที่ #$myStageRank / 10",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = GoldPrimary,
          )
          Text(
            text = "👤 ตัวละครลูกค้า: ${dialogStage.customerName} (${dialogStage.customerAge} ปี • ${dialogStage.customerOccupation})",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = "🎭 นิสัยประจำตัว: ${dialogStage.customerPersona}",
            style = MaterialTheme.typography.bodySmall,
          )
          Text(
            text = "🔍 ข้อมูลเบื้องหลังที่ซ่อนอยู่ (ใช้เตรียมตัว): ${dialogStage.hiddenFinancialFact}",
            style = MaterialTheme.typography.bodySmall,
            color = CoralAlert,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "🎯 ภารกิจที่ต้องทำให้สำเร็จ: ${dialogStage.missionObjective}",
            style = MaterialTheme.typography.bodySmall,
            color = EmeraldAccent,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = "🧭 หมวดประกันที่ต้องชี้เป้า: ${dialogStage.targetInsuranceDirection}",
            style = MaterialTheme.typography.bodySmall,
            color = CyanWaveform,
          )
          Text(
            text = "💬 ประโยคเปิดฉากของลูกค้า: \"${dialogStage.openingCustomerLine}\"",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val num = dialogStage.stageNumber
            detailStageForDialog = null
            onStartStage(num)
          }
        ) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("เข้าคุยสดเลย!")
        }
      },
      dismissButton = {
        TextButton(onClick = { detailStageForDialog = null }) { Text("ปิด") }
      },
    )
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // 1-Tap Quick-Start Hero Card
    item {
      Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier =
          Modifier.fillMaxWidth()
            .border(
              1.5.dp,
              MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
              RoundedCornerShape(20.dp),
            ),
      ) {
        Column {
          Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            Image(
              painter = painterResource(id = R.drawable.img_hero_coach_1794022212140),
              contentDescription = "Role-Play Hero Banner",
              modifier = Modifier.fillMaxSize(),
              contentScale = ContentScale.Crop,
            )
            IconButton(
              onClick = { detailStageForDialog = recommendedStage },
              modifier =
                Modifier.align(Alignment.TopEnd)
                  .padding(8.dp)
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
            ) {
              Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "ดูแฟ้มข้อมูลเตรียมตัวด่านแนะนำ",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "⚡ ด่านแนะนำ • คุยสดเรียลไทม์ + Lip-Sync",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
              Text(
                text = recommendedStage.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              Text(
                text = "👤 ${recommendedStage.customerName} • อันดับคุณ #${rankByStage[recommendedStage.stageNumber] ?: 10}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Button(
              onClick = { onStartStage(recommendedStage.stageNumber) },
              shape = RoundedCornerShape(12.dp),
              colors =
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              modifier = Modifier.testTag("quick_start_stage_button"),
            ) {
              Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "ลุยด่าน ${recommendedStage.stageNumber}",
                fontWeight = FontWeight.Bold,
              )
            }
          }
        }
      }
    }

    // Category Filter Chips
    item {
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
          FilterChip(
            selected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            label = { Text("ทั้งหมด (25 ด่าน)") },
          )
        }
        items(StageCategory.entries) { category ->
          FilterChip(
            selected = selectedCategory == category,
            onClick = { onSelectCategory(category) },
            label = { Text("${category.stageRange}: ${category.titleTh.substringAfter(": ")}") },
          )
        }
      }
    }

    // 25 Hilarious Stages List (Minimalist Cards + Your Rank + ℹ️ Prep Dossier Button)
    items(filteredStages, key = { it.stageNumber }) { stage ->
      val bestScore = bestScoreByStage[stage.stageNumber]
      val myRank = rankByStage[stage.stageNumber] ?: 10
      StageCardItem(
        stage = stage,
        bestScore = bestScore,
        yourRank = myRank,
        onClick = { onStartStage(stage.stageNumber) },
        onShowDetails = { detailStageForDialog = stage },
      )
    }
  }
}

@Composable
private fun StageCardItem(
  stage: RolePlayStage,
  bestScore: Int?,
  yourRank: Int,
  onClick: () -> Unit,
  onShowDetails: () -> Unit,
) {
  val passed = (bestScore ?: 0) >= 70
  Card(
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    shape = RoundedCornerShape(16.dp),
    modifier =
      Modifier.fillMaxWidth()
        .border(
          1.dp,
          if (passed) EmeraldAccent.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.surfaceVariant,
          RoundedCornerShape(16.dp),
        )
        .clickable(onClick = onClick)
        .testTag("stage_card_${stage.stageNumber}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        Box(
          modifier =
            Modifier.size(42.dp)
              .clip(CircleShape)
              .background(
                if (passed) EmeraldAccent.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
              ),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = "${stage.stageNumber}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = if (passed) EmeraldAccent else MaterialTheme.colorScheme.primary,
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = stage.title.substringAfter(": "),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = "👤 ${stage.customerName} • อันดับ #$yourRank",
              style = MaterialTheme.typography.labelSmall,
              color = if (yourRank <= 3 && passed) GoldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = if (yourRank <= 3 && passed) FontWeight.Bold else FontWeight.Normal,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (bestScore != null) {
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "🏆 $bestScore",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (passed) EmeraldAccent else MaterialTheme.colorScheme.primary,
              )
            }
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // ปุ่มไอคอน ℹ️ สำหรับกดดูแฟ้มข้อมูลเตรียมตัวก่อนคุย
        IconButton(
          onClick = onShowDetails,
          modifier = Modifier.size(36.dp).testTag("stage_info_btn_${stage.stageNumber}"),
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "ดูข้อมูลเตรียมตัวด่านที่ ${stage.stageNumber}",
            tint = CyanWaveform,
            modifier = Modifier.size(20.dp),
          )
        }

        Button(
          onClick = onClick,
          shape = RoundedCornerShape(10.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
          colors =
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
          Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(2.dp))
          Text(
            text = if (bestScore == null) "ลุย" else "ซ้ำ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
