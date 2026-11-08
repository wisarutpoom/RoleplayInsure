package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.VoiceAnalyzerRecorder
import com.example.data.AgentTelemetry
import com.example.data.ChatTurn
import com.example.data.EvaluationResult
import com.example.data.FreeTierQuotaGuard
import com.example.data.OnDeviceRolePlayAiEngine
import com.example.data.RolePlayCurriculum
import com.example.data.RolePlayStage
import com.example.data.StageAttempt
import com.example.data.StageAttemptRepository
import com.example.data.StageCategory
import com.example.data.VoiceRecordingMetrics
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "InsureQuestVM"

sealed interface AttemptsUiState {
  data object Loading : AttemptsUiState
  data class Success(val attempts: List<StageAttempt>) : AttemptsUiState
  data class Error(val message: String) : AttemptsUiState
}

sealed interface AdminTelemetryUiState {
  data object Loading : AdminTelemetryUiState
  data class Success(val telemetryList: List<AgentTelemetry>) : AdminTelemetryUiState
  data class Error(val message: String) : AdminTelemetryUiState
}

sealed interface StaffPermissionsUiState {
  data object Loading : StaffPermissionsUiState
  data class Success(val permissions: List<com.example.data.StaffPermission>) :
    StaffPermissionsUiState
  data class Error(val message: String) : StaffPermissionsUiState
}

sealed interface RolePlayAiState {
  data object Idle : RolePlayAiState
  data object Thinking : RolePlayAiState
  data class Evaluating(val progressText: String) : RolePlayAiState
  data class Error(val message: String) : RolePlayAiState
}

class InsureQuestViewModel(
  private val repository: StageAttemptRepository,
  val currentUserId: String,
  initialAgentName: String,
  val currentUserEmail: String = "",
  val isAdminUser: Boolean = false,
) : ViewModel() {

  private val _agentName = MutableStateFlow(initialAgentName.ifBlank { "จอมยุทธ์นักขาย" })
  val agentName: StateFlow<String> = _agentName.asStateFlow()

  private val _equippedFunnyTitle = MutableStateFlow("จอมยุทธ์ไมค์ทองคำ")
  val equippedFunnyTitle: StateFlow<String> = _equippedFunnyTitle.asStateFlow()

  // เชื่อมต่อ Gemini API (Free Tier) เป็นค่าเริ่มต้นหลักตามที่ผู้ใช้ต้องการ!
  private val _useBuiltInOnDeviceAi = MutableStateFlow(false)
  val useBuiltInOnDeviceAi: StateFlow<Boolean> = _useBuiltInOnDeviceAi.asStateFlow()

  private val _quotaExhaustedPopupMessage = MutableStateFlow<String?>(null)
  val quotaExhaustedPopupMessage: StateFlow<String?> = _quotaExhaustedPopupMessage.asStateFlow()

  private val _teamCode = MutableStateFlow("TEAM_MDRT_TH")
  val teamCode: StateFlow<String> = _teamCode.asStateFlow()

  private val _customDomain = MutableStateFlow("roleplay.smartnexus.vip")
  val customDomain: StateFlow<String> = _customDomain.asStateFlow()

  private val _selectedCategory = MutableStateFlow<StageCategory?>(null)
  val selectedCategory: StateFlow<StageCategory?> = _selectedCategory.asStateFlow()

  private val _activeStage = MutableStateFlow<RolePlayStage?>(null)
  val activeStage: StateFlow<RolePlayStage?> = _activeStage.asStateFlow()

  private val _chatTurns = MutableStateFlow<List<ChatTurn>>(emptyList())
  val chatTurns: StateFlow<List<ChatTurn>> = _chatTurns.asStateFlow()

  private val _lastVoiceMetrics = MutableStateFlow<VoiceRecordingMetrics?>(null)
  val lastVoiceMetrics: StateFlow<VoiceRecordingMetrics?> = _lastVoiceMetrics.asStateFlow()

  private val _evaluationResult = MutableStateFlow<EvaluationResult?>(null)
  val evaluationResult: StateFlow<EvaluationResult?> = _evaluationResult.asStateFlow()

  private val _aiState = MutableStateFlow<RolePlayAiState>(RolePlayAiState.Idle)
  val aiState: StateFlow<RolePlayAiState> = _aiState.asStateFlow()

  private val _adminActionMessage = MutableStateFlow<String?>(null)
  val adminActionMessage: StateFlow<String?> = _adminActionMessage.asStateFlow()

  private val _localAttempts =
    MutableStateFlow<List<StageAttempt>>(
      listOf(
        StageAttempt(
          id = "seed_stage_1",
          userId = currentUserId,
          agentName = initialAgentName.ifBlank { "จอมยุทธ์นักขาย" },
          teamCode = "TEAM_MDRT_TH",
          stageNumber = 1,
          stageTitle = RolePlayCurriculum.getStageByNumber(1).title,
          category = StageCategory.COLD_CALL.code,
          overallScore = 86,
          rapportScore = 88,
          objectionScore = 85,
          factFindingScore = 82,
          preApproachScore = 86,
          voiceToneScore = 89,
          voiceFeedback = "✅ จังหวะการพูด 135 WPM กระชับ มั่นใจ เหมาะกับการคุยกับผู้บริหาร",
          coachSummary = "เปิดใจคุณกานต์ สายฟ้าแลบ ได้ภายใน 45 วินาที และปิดนัดหมายวันอังคารบ่ายสำเร็จ",
          passed = true,
        ),
        StageAttempt(
          id = "seed_stage_2",
          userId = currentUserId,
          agentName = initialAgentName.ifBlank { "จอมยุทธ์นักขาย" },
          teamCode = "TEAM_MDRT_TH",
          stageNumber = 2,
          stageTitle = RolePlayCurriculum.getStageByNumber(2).title,
          category = StageCategory.COLD_CALL.code,
          overallScore = 84,
          rapportScore = 86,
          objectionScore = 84,
          factFindingScore = 80,
          preApproachScore = 85,
          voiceToneScore = 85,
          voiceFeedback = "✅ น้ำเสียงอบอุ่น สุภาพ ชื่นชมเจ๊พิมพ์ก่อนเสนอบริการสรุปกรมธรรม์",
          coachSummary = "เจ๊พิมพ์ยอมให้นัดเข้าไปทำ Policy Review สรุป 5 เล่มในแผ่นเดียวเรียบร้อย",
          passed = true,
        ),
      )
    )
  private val _localEnteredStages = MutableStateFlow<Set<Int>>(setOf(1, 2))
  private val _localVisitCount = MutableStateFlow(1)
  private val _localLastStageTitle = MutableStateFlow("เข้าสู่ยุทธภพนักขาย 25 ด่าน")
  private val _localStaffPermissions =
    MutableStateFlow(
      listOf(
        com.example.data.StaffPermission(
          id = "wisarutpoom_gmail_com",
          userId = currentUserId,
          staffEmail = "Wisarutpoom@gmail.com",
          staffName = "คุณวิศรุต (Master Admin)",
          role = "ADMIN",
          status = "APPROVED",
          canViewVisitsAndStages = true,
          canViewScores = true,
          canExportCsv = true,
          canManageTeam = true,
          approvedByEmail = "System Master Admin",
        )
      )
    )

  val attemptsState: StateFlow<AttemptsUiState> =
    kotlinx.coroutines.flow
      .combine(
        repository.observeUserAttempts().catch { emit(emptyList()) },
        _localAttempts,
      ) { remote, local ->
        val merged =
          (remote + local).distinctBy {
            it.id.ifBlank { "${it.stageNumber}_${it.overallScore}" }
          }
        val sorted = merged.sortedByDescending { it.createdAt?.seconds ?: 0L }
        AttemptsUiState.Success(sorted)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AttemptsUiState.Success(_localAttempts.value),
      )

  val myTelemetryState: StateFlow<AgentTelemetry?> =
    repository
      .observeMyTelemetry()
      .catch { emit(null) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null,
      )

  val adminTelemetryState: StateFlow<AdminTelemetryUiState> =
    kotlinx.coroutines.flow
      .combine(
        repository.observeAdminTelemetryList(isAdminUser = isAdminUser).catch { emit(emptyList()) },
        _localAttempts,
        _localEnteredStages,
        _agentName,
        _teamCode,
      ) { remoteList, localAttempts, enteredSet, currentName, currentTeam ->
        val passedSet = localAttempts.filter { it.passed }.map { it.stageNumber }.toSet()
        val allEntered = (enteredSet + localAttempts.map { it.stageNumber }).sorted()
        val allPassed = passedSet.sorted()
        val bestScore = localAttempts.maxOfOrNull { it.overallScore } ?: 0
        val avgScore =
          if (localAttempts.isNotEmpty()) localAttempts.map { it.overallScore }.average().toInt()
          else 0
        val avgVoice =
          if (localAttempts.isNotEmpty()) localAttempts.map { it.voiceToneScore }.average().toInt()
          else 0

        val localSelfTelemetry =
          AgentTelemetry(
            id = currentUserId,
            userId = currentUserId,
            agentName = currentName,
            teamCode = currentTeam,
            visitCount = _localVisitCount.value,
            totalAttemptsCount = localAttempts.size,
            enteredStagesCsv = allEntered.joinToString(","),
            completedStagesCsv = allPassed.joinToString(","),
            enteredStagesCount = allEntered.size,
            completedStagesCount = allPassed.size,
            bestOverallScore = bestScore,
            averageOverallScore = avgScore,
            averageVoiceScore = avgVoice,
            lastActiveStage = allEntered.lastOrNull() ?: 0,
            lastActiveStageTitle = _localLastStageTitle.value,
          )

        val combined =
          if (remoteList.any { it.userId == currentUserId }) {
            remoteList
          } else {
            listOf(localSelfTelemetry) + remoteList
          }
        AdminTelemetryUiState.Success(combined)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = AdminTelemetryUiState.Success(emptyList()),
      )

  val allAttemptsForAdminState: StateFlow<List<StageAttempt>> =
    kotlinx.coroutines.flow
      .combine(
        repository.observeAllStageAttemptsForAdmin(isAdminUser = isAdminUser).catch {
          emit(emptyList())
        },
        _localAttempts,
      ) { remote, local ->
        (remote + local).distinctBy { it.id.ifBlank { "${it.stageNumber}_${it.overallScore}" } }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList(),
      )

  val staffPermissionsState: StateFlow<StaffPermissionsUiState> =
    kotlinx.coroutines.flow
      .combine(
        repository
          .observeStaffPermissions(isAdminUser = isAdminUser, currentUserEmail = currentUserEmail)
          .catch { emit(emptyList()) },
        _localStaffPermissions,
      ) { remote, local ->
        val merged = (remote + local).distinctBy { it.staffEmail.lowercase() }
        StaffPermissionsUiState.Success(merged)
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = StaffPermissionsUiState.Success(_localStaffPermissions.value),
      )

  init {
    viewModelScope.launch {
      repository.recordTelemetryEvent(
        agentName = _agentName.value,
        teamCode = _teamCode.value,
        incrementVisit = true,
      )
    }
  }

  private val turnSchema =
    Schema.obj(
      properties =
        mapOf(
          "transcribedAgentSpeech" to
            Schema.string(
              description = "Exact or inferred Thai speech said by the insurance agent"
            ),
          "customerReply" to
            Schema.string(
              description = "Natural spoken Thai reply from the customer persona (1-2 concise sentences for real-time voice TTS)"
            ),
          "voiceCoachTip" to
            Schema.string(
              description =
                "Short constructive Thai feedback on the agent's vocal tone, warmth, pacing, and empathy"
            ),
        )
    )

  private val evaluationSchema =
    Schema.obj(
      properties =
        mapOf(
          "overallScore" to Schema.integer(description = "Overall weighted score 0-100"),
          "rapportScore" to Schema.integer(description = "Rapport & trust score 0-100"),
          "objectionScore" to Schema.integer(description = "Objection handling score 0-100"),
          "factFindingScore" to Schema.integer(description = "Fact-finding depth score 0-100"),
          "preApproachScore" to
            Schema.integer(
              description = "Pre-approach needs analysis & insurance direction score 0-100"
            ),
          "voiceToneScore" to
            Schema.integer(description = "Vocal tone, warmth, confidence, and pacing score 0-100"),
          "voiceFeedback" to
            Schema.string(
              description = "Detailed Thai analysis of the agent's voice tone, rhythm, and empathy"
            ),
          "coachSummary" to
            Schema.string(description = "Executive coaching summary in Thai for the agent"),
          "strengths" to
            Schema.array(
              items = Schema.string(),
              description = "2-3 key strengths demonstrated in Thai",
            ),
          "improvements" to
            Schema.array(
              items = Schema.string(),
              description = "2-3 actionable areas to improve in Thai",
            ),
          "recommendedDirectionCheck" to
            Schema.string(
              description =
                "Assessment of whether the agent identified the right insurance category without prematurely pitching product details"
            ),
        )
    )

  val quotaStatus: StateFlow<com.example.data.FreeQuotaStatus> =
    FreeTierQuotaGuard.quotaStatus

  private val turnModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = turnSchema
          maxOutputTokens = FreeTierQuotaGuard.MAX_FREE_OUTPUT_TOKENS
        },
    )
  }

  private val evaluationModel by lazy {
    Firebase.ai.generativeModel(
      modelName = "gemini-3.5-flash-lite",
      generationConfig =
        generationConfig {
          responseMimeType = "application/json"
          responseSchema = evaluationSchema
          maxOutputTokens = FreeTierQuotaGuard.MAX_FREE_OUTPUT_TOKENS
        },
    )
  }

  fun toggleAiEngineMode() {
    _useBuiltInOnDeviceAi.value = !_useBuiltInOnDeviceAi.value
  }

  fun dismissQuotaExhaustedPopup() {
    _quotaExhaustedPopupMessage.value = null
  }

  fun triggerQuotaExhaustedPreview() {
    FreeTierQuotaGuard.simulateQuotaExhaustedForPreview()
    val status = FreeTierQuotaGuard.quotaStatus.value
    _quotaExhaustedPopupMessage.value = status.quotaExhaustedDetailMessage
  }

  fun resetQuotaGuardForTesting() {
    FreeTierQuotaGuard.resetQuotaCounter()
    _quotaExhaustedPopupMessage.value = null
  }

  fun equipFunnyTitle(newTitle: String) {
    if (newTitle.isNotBlank()) {
      _equippedFunnyTitle.value = newTitle
    }
  }

  fun selectCategory(category: StageCategory?) {
    _selectedCategory.value = category
  }

  fun startStage(stageNumber: Int) {
    val stage = RolePlayCurriculum.getStageByNumber(stageNumber)
    _activeStage.value = stage
    _evaluationResult.value = null
    _lastVoiceMetrics.value = null
    _aiState.value = RolePlayAiState.Idle
    _localEnteredStages.value = _localEnteredStages.value + stage.stageNumber
    _localLastStageTitle.value = stage.title
    _chatTurns.value =
      listOf(
        ChatTurn(
          id = UUID.randomUUID().toString(),
          isAgent = false,
          text = stage.openingCustomerLine,
          coachTip = "🎯 ภารกิจด่านนี้: ${stage.missionObjective}",
        )
      )

    viewModelScope.launch {
      val currentAttempts = (attemptsState.value as? AttemptsUiState.Success)?.attempts
      repository.recordTelemetryEvent(
        agentName = _agentName.value,
        teamCode = _teamCode.value,
        incrementVisit = false,
        enteredStageNumber = stage.stageNumber,
        enteredStageTitle = stage.title,
        completedAttempts = currentAttempts,
      )
    }
  }

  fun exitStage() {
    _activeStage.value = null
    _evaluationResult.value = null
    _aiState.value = RolePlayAiState.Idle
  }

  fun sendAgentTurn(
    agentText: String,
    audioBytes: ByteArray? = null,
    voiceMetrics: VoiceRecordingMetrics? = null,
  ) {
    val stage = _activeStage.value ?: return
    if (agentText.isBlank() && audioBytes == null) return

    val effectiveMetrics =
      voiceMetrics
        ?: run {
          val clean = agentText.trim()
          val wpm =
            when {
              clean.length > 135 -> 174
              clean.length in 1..14 -> 112
              else -> 134
            }
          val isFast = wpm > 165
          val isQuiet = clean.length in 1..12
          val avgAmp = if (isQuiet) 1800 else 7200
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
            confidenceIndex = if (isFast || isQuiet) 72 else 90,
            avgAmplitude = avgAmp,
            isTooFast = isFast,
            isTooQuiet = isQuiet,
            voiceCoachingTip =
              VoiceAnalyzerRecorder.buildVoiceCoachingFeedbackTip(
                wpm = wpm,
                isTooFast = isFast,
                isTooQuiet = isQuiet,
              ),
          )
        }

    _lastVoiceMetrics.value = effectiveMetrics

    val metricsDesc =
      "ความเร็ว ${effectiveMetrics.estimatedWordsPerMinute} WPM | ${effectiveMetrics.speechRateLabel} | ${effectiveMetrics.energyLevelLabel} (ความมั่นใจเสียง ${effectiveMetrics.confidenceIndex}/100)"

    val optimisticText =
      agentText.ifBlank {
        "เข้าใจเลยครับคุณ${stage.customerName.substringBefore(" ")} วันนี้ผมขออนุญาตแลกเปลี่ยนมุมมองสั้นๆ เรื่อง ${stage.targetInsuranceDirection.take(30)} ครับ"
      }

    val agentTurnId = UUID.randomUUID().toString()
    val currentHistory = _chatTurns.value
    val agentTurnIndex = currentHistory.count { it.isAgent } + 1

    val onDeviceResult =
      OnDeviceRolePlayAiEngine.generateCustomerTurnOnDevice(
        stage = stage,
        agentText = optimisticText,
        agentTurnIndex = agentTurnIndex,
        voiceMetrics = effectiveMetrics,
      )

    _chatTurns.value =
      currentHistory +
        ChatTurn(
          id = agentTurnId,
          isAgent = true,
          text = optimisticText,
          voiceMetricsSummary = metricsDesc,
          voiceCoachingAlert = onDeviceResult.voiceCoachingAlert,
          isVoiceAlertWarning = onDeviceResult.isVoiceAlertWarning,
        )

    viewModelScope.launch {
      _aiState.value = RolePlayAiState.Thinking
      try {
        if (_useBuiltInOnDeviceAi.value) {
          delay(350L)
          _chatTurns.value =
            _chatTurns.value +
              ChatTurn(
                id = UUID.randomUUID().toString(),
                isAgent = false,
                text = onDeviceResult.customerReply,
                coachTip = onDeviceResult.coachTip,
              )
          _aiState.value = RolePlayAiState.Idle
          return@launch
        }

        // เชื่อมต่อ Gemini API (gemini-3.8-flash Free Tier) พร้อมตรวจสอบโควต้าฟรีและเวลารีเซ็ต
        val conversationContext =
          _chatTurns.value.takeLast(6).joinToString("\n") { turn ->
            if (turn.isAgent) "ตัวแทนประกัน: ${turn.text}"
            else "ลูกค้า (${stage.customerName}): ${turn.text}"
          }

        val promptText =
          """
          สวมบทบาทเป็นลูกค้าในเกมฝึกซ้อมคุยสดตัวแทนประกันภัย (Real-Time Voice Role-Play)
          ด่านที่ ${stage.stageNumber}: ${stage.title}
          ชื่อลูกค้า: ${stage.customerName} (อายุ ${stage.customerAge} ปี, อาชีพ ${stage.customerOccupation})
          นิสัยและคาแรคเตอร์สุดฮา: ${stage.customerPersona}
          ข้อมูลลับทางการเงิน (เปิดเผยเมื่อตัวแทนถามดี): ${stage.hiddenFinancialFact}
          เป้าหมายด่าน: ${stage.missionObjective}
          หมวดความคุ้มครองเป้าหมาย: ${stage.targetInsuranceDirection}

          บทสนทนาล่าสุด:
          $conversationContext

          ผลวิเคราะห์น้ำเสียงตัวแทนรอบนี้: $metricsDesc
          คำพูดของตัวแทนล่าสุด: "$optimisticText"

          คำสั่ง:
          1. ตอบกลับเป็นภาษาพูดภาษาไทยที่สมจริงตามคาแรคเตอร์ ${stage.customerName} สั้นกระชับ 1-2 ประโยคใน customerReply เพื่อให้ระบบอ่านออกเสียง TTS โต้ตอบเรียลไทม์ได้ทันที
          2. ให้คำแนะนำโค้ชสั้นๆ 1 ประโยคใน voiceCoachTip
          """
            .trimIndent()

        val cacheKey =
          "turn_${stage.stageNumber}_${_chatTurns.value.size}_${optimisticText.trim().lowercase()}"
        val cachedJson =
          if (audioBytes == null) FreeTierQuotaGuard.getCachedResponse(cacheKey) else null

        val jsonText =
          if (cachedJson != null) {
            cachedJson
          } else if (!FreeTierQuotaGuard.tryAcquireFreeQuota()) {
            // เมื่อโควต้าฟรีหมด ให้แจ้งเตือนผู้เล่นทันทีว่าจะรีเซ็ตวันพรุ่งนี้กี่โมง!
            val status = FreeTierQuotaGuard.quotaStatus.value
            _quotaExhaustedPopupMessage.value = status.quotaExhaustedDetailMessage
            JSONObject()
              .apply {
                put("transcribedAgentSpeech", optimisticText)
                put("customerReply", onDeviceResult.customerReply)
                put(
                  "voiceCoachTip",
                  "🛑 โควต้าฟรี Gemini API ครบแล้ว (รีเซ็ต ${status.nextResetTimeThaiLabel}) • ระบบสลับใช้ AI สำรองในแอพให้คุยต่อได้ฟรี",
                )
              }
              .toString()
          } else {
            val response =
              withTimeout(22_000L) {
                turnModel.generateContent(
                  content {
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                      inlineData(audioBytes, "audio/mp4")
                    }
                    text(promptText)
                  }
                )
              }
            val raw =
              response.text ?: throw IllegalStateException("Gemini returned an empty response.")
            if (audioBytes == null) {
              FreeTierQuotaGuard.putCachedResponse(cacheKey, raw)
            }
            raw
          }

        val json = JSONObject(jsonText)
        val transcribed = json.optString("transcribedAgentSpeech", "").trim()
        val customerReply =
          json.optString("customerReply", onDeviceResult.customerReply).trim()
        val coachTip = json.optString("voiceCoachTip", onDeviceResult.coachTip).trim()

        val updatedTurns =
          _chatTurns.value.map { turn ->
            if (turn.id == agentTurnId && agentText.isBlank() && transcribed.isNotBlank()) {
              turn.copy(text = transcribed)
            } else {
              turn
            }
          }

        _chatTurns.value =
          updatedTurns +
            ChatTurn(
              id = UUID.randomUUID().toString(),
              isAgent = false,
              text = customerReply,
              coachTip = coachTip,
            )
        _aiState.value = RolePlayAiState.Idle
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.w(TAG, "Gemini API error or quota reached; checking if quota exhausted", e)
        val errMsg = e.localizedMessage?.lowercase().orEmpty()
        if (errMsg.contains("429") || errMsg.contains("quota") || errMsg.contains("exhausted")) {
          FreeTierQuotaGuard.markServerQuotaExhausted()
          _quotaExhaustedPopupMessage.value =
            FreeTierQuotaGuard.quotaStatus.value.quotaExhaustedDetailMessage
        }
        _chatTurns.value =
          _chatTurns.value +
            ChatTurn(
              id = UUID.randomUUID().toString(),
              isAgent = false,
              text = onDeviceResult.customerReply,
              coachTip = onDeviceResult.coachTip,
            )
        _aiState.value = RolePlayAiState.Idle
      }
    }
  }

  fun sendSimulatedVoicePatternTurn(patternType: String) {
    val stage = _activeStage.value ?: return
    when (patternType) {
      "FAST_WPM" -> {
        val fastMetrics =
          VoiceRecordingMetrics(
            durationSeconds = 3,
            speechRateLabel = "พูดเร็วเกินไป (178 WPM > 165) ควรลดสปีดและเว้นจังหวะ",
            energyLevelLabel = "ระดับเสียงดังชัดเจน",
            pauseBalanceLabel = "พูดรัวติดกันแทบไม่เว้นวรรค",
            estimatedWordsPerMinute = 178,
            confidenceIndex = 70,
            avgAmplitude = 8500,
            isTooFast = true,
            isTooQuiet = false,
            voiceCoachingTip =
              VoiceAnalyzerRecorder.buildVoiceCoachingFeedbackTip(
                wpm = 178,
                isTooFast = true,
                isTooQuiet = false,
              ),
          )
        sendAgentTurn(
          agentText =
            "สวัสดีครับคุณ${stage.customerName.substringBefore(" ")}ผมขอเวลาสั้นๆพูดเร็วๆเรื่องการวางแผน${stage.targetInsuranceDirection.take(28)}ให้ฟังเดี๋ยวนี้เลยครับ!",
          audioBytes = null,
          voiceMetrics = fastMetrics,
        )
      }
      "QUIET_VOICE" -> {
        val quietMetrics =
          VoiceRecordingMetrics(
            durationSeconds = 4,
            speechRateLabel = "จังหวะปกติ (118 WPM)",
            energyLevelLabel = "เสียงเบาเกินไป ควรเพิ่มพลังเสียงจากกระบังลม",
            pauseBalanceLabel = "มีจังหวะลังเลและเสียงแผ่วปลายประโยค",
            estimatedWordsPerMinute = 118,
            confidenceIndex = 68,
            avgAmplitude = 1650,
            isTooFast = false,
            isTooQuiet = true,
            voiceCoachingTip =
              VoiceAnalyzerRecorder.buildVoiceCoachingFeedbackTip(
                wpm = 118,
                isTooFast = false,
                isTooQuiet = true,
              ),
          )
        sendAgentTurn(
          agentText = "เอ่อ... ขออนุญาตสอบถามข้อมูลเรื่องความคุ้มครองนิดนึงนะครับ...",
          audioBytes = null,
          voiceMetrics = quietMetrics,
        )
      }
      else -> {
        val proMetrics =
          VoiceRecordingMetrics(
            durationSeconds = 5,
            speechRateLabel = "จังหวะพอดีเป็นธรรมชาติ (132 WPM)",
            energyLevelLabel = "ระดับเสียงอบอุ่น มั่นคง น่าเชื่อถือ",
            pauseBalanceLabel = "การเว้นจังหวะหายใจและเน้นคำสมดุลดีมาก",
            estimatedWordsPerMinute = 132,
            confidenceIndex = 94,
            avgAmplitude = 7800,
            isTooFast = false,
            isTooQuiet = false,
            voiceCoachingTip =
              VoiceAnalyzerRecorder.buildVoiceCoachingFeedbackTip(
                wpm = 132,
                isTooFast = false,
                isTooQuiet = false,
              ),
          )
        sendAgentTurn(
          agentText =
            "เข้าใจเลยครับคุณ${stage.customerName.substringBefore(" ")} วันนี้ผมไม่ได้มาเสนอขาย แต่ขออนุญาตชวนวางโครงสร้าง ${stage.targetInsuranceDirection} ให้ตอบโจทย์ครอบครัวครับ",
          audioBytes = null,
          voiceMetrics = proMetrics,
        )
      }
    }
  }

  fun evaluateCurrentSession() {
    val stage = _activeStage.value ?: return
    val turns = _chatTurns.value
    val agentTurnsCount = turns.count { it.isAgent }
    if (agentTurnsCount == 0) {
      _aiState.value =
        RolePlayAiState.Error("กรุณาพูดหรือพิมพ์โต้ตอบกับลูกค้าอย่างน้อย 1 รอบก่อนกดประเมินผล")
      return
    }

    viewModelScope.launch {
      _aiState.value =
        RolePlayAiState.Evaluating(
          "กำลังประเมินคะแนน 5 มิติด้วย Gemini API (Free Tier) และตรวจสอบเหรียญรางวัล..."
        )
      try {
        val previousAttempts =
          (attemptsState.value as? AttemptsUiState.Success)?.attempts ?: _localAttempts.value
        val previousUnlockedIds =
          OnDeviceRolePlayAiEngine.evaluatePlayerBadges(previousAttempts, null)
            .filter { it.unlocked }
            .map { it.id }
            .toSet()

        val fallbackEval =
          OnDeviceRolePlayAiEngine.evaluateSessionOnDevice(
            stage = stage,
            chatTurns = turns,
            lastVoiceMetrics = _lastVoiceMetrics.value,
          )

        val baseEval =
          if (_useBuiltInOnDeviceAi.value) {
            delay(350L)
            fallbackEval
          } else if (!FreeTierQuotaGuard.tryAcquireFreeQuota()) {
            val status = FreeTierQuotaGuard.quotaStatus.value
            _quotaExhaustedPopupMessage.value = status.quotaExhaustedDetailMessage
            fallbackEval.copy(
              coachSummary =
                "🛑 โควต้าฟรี Gemini API วันนี้ครบแล้ว (กลับมาต่อได้ใน${status.nextResetTimeThaiLabel}) • ประเมินผลด้วยระบบสำรองในแอพ: ${fallbackEval.coachSummary}"
            )
          } else {
            runCatching {
              val transcript =
                turns.joinToString("\n") { turn ->
                  val role =
                    if (turn.isAgent) "ตัวแทนประกัน" else "ลูกค้า (${stage.customerName})"
                  val voiceNote =
                    turn.voiceMetricsSummary?.let { " [น้ำเสียง: $it]" } ?: ""
                  "$role: ${turn.text}$voiceNote"
                }
              val evalPrompt =
                """
                ประเมินผลการ Role-Play ของตัวแทนประกันในด่านที่ ${stage.stageNumber}: ${stage.title}
                ลูกค้า: ${stage.customerName} (${stage.customerPersona})
                ข้อมูลลับ: ${stage.hiddenFinancialFact}
                ภารกิจ: ${stage.missionObjective}
                หมวดความคุ้มครองเป้าหมาย: ${stage.targetInsuranceDirection}

                บทสนทนา:
                $transcript
                """
                  .trimIndent()

              val response =
                withTimeout(22_000L) { evaluationModel.generateContent(evalPrompt) }
              val raw =
                response.text ?: throw IllegalStateException("Empty evaluation response")
              val json = JSONObject(raw)
              val rapport = json.optInt("rapportScore", fallbackEval.rapportScore).coerceIn(0, 100)
              val objection =
                json.optInt("objectionScore", fallbackEval.objectionScore).coerceIn(0, 100)
              val factFinding =
                json.optInt("factFindingScore", fallbackEval.factFindingScore).coerceIn(0, 100)
              val preApproach =
                json.optInt("preApproachScore", fallbackEval.preApproachScore).coerceIn(0, 100)
              val voiceTone =
                json.optInt("voiceToneScore", fallbackEval.voiceToneScore).coerceIn(0, 100)
              val overall =
                json
                  .optInt(
                    "overallScore",
                    (rapport + objection + factFinding + preApproach + voiceTone) / 5,
                  )
                  .coerceIn(0, 100)
              EvaluationResult(
                overallScore = overall,
                rapportScore = rapport,
                objectionScore = objection,
                factFindingScore = factFinding,
                preApproachScore = preApproach,
                voiceToneScore = voiceTone,
                voiceFeedback =
                  json.optString("voiceFeedback", fallbackEval.voiceFeedback).trim(),
                coachSummary =
                  json.optString("coachSummary", fallbackEval.coachSummary).trim(),
                strengths = fallbackEval.strengths,
                improvements = fallbackEval.improvements,
                recommendedDirectionCheck =
                  json
                    .optString(
                      "recommendedDirectionCheck",
                      fallbackEval.recommendedDirectionCheck,
                    )
                    .trim(),
                passed = overall >= 70,
              )
            }.getOrElse { e ->
              val errMsg = e.localizedMessage?.lowercase().orEmpty()
              if (
                errMsg.contains("429") ||
                  errMsg.contains("quota") ||
                  errMsg.contains("exhausted")
              ) {
                FreeTierQuotaGuard.markServerQuotaExhausted()
                _quotaExhaustedPopupMessage.value =
                  FreeTierQuotaGuard.quotaStatus.value.quotaExhaustedDetailMessage
              }
              fallbackEval
            }
          }

        val newSyntheticAttempt =
          StageAttempt(
            id = "local_${UUID.randomUUID()}",
            userId = currentUserId,
            agentName = _agentName.value,
            teamCode = _teamCode.value,
            stageNumber = stage.stageNumber,
            stageTitle = stage.title,
            category = stage.category.code,
            overallScore = baseEval.overallScore,
            rapportScore = baseEval.rapportScore,
            objectionScore = baseEval.objectionScore,
            factFindingScore = baseEval.factFindingScore,
            preApproachScore = baseEval.preApproachScore,
            voiceToneScore = baseEval.voiceToneScore,
            voiceFeedback = baseEval.voiceFeedback,
            coachSummary = baseEval.coachSummary,
            passed = baseEval.passed,
            createdAt = com.google.firebase.Timestamp.now(),
            updatedAt = com.google.firebase.Timestamp.now(),
          )

        val updatedAttempts = listOf(newSyntheticAttempt) + _localAttempts.value
        _localAttempts.value = updatedAttempts

        val updatedBadges =
          OnDeviceRolePlayAiEngine.evaluatePlayerBadges(
            attempts = updatedAttempts,
            lastVoiceMetrics = _lastVoiceMetrics.value,
          )
        val newlyUnlocked =
          updatedBadges.filter { it.unlocked && it.id !in previousUnlockedIds }

        val finalEval = baseEval.copy(newlyUnlockedBadges = newlyUnlocked)
        _evaluationResult.value = finalEval
        _aiState.value = RolePlayAiState.Idle

        repository.createAttempt(
          agentName = _agentName.value,
          teamCode = _teamCode.value,
          stageNumber = stage.stageNumber,
          stageTitle = stage.title,
          category = stage.category.code,
          overallScore = finalEval.overallScore,
          rapportScore = finalEval.rapportScore,
          objectionScore = finalEval.objectionScore,
          factFindingScore = finalEval.factFindingScore,
          preApproachScore = finalEval.preApproachScore,
          voiceToneScore = finalEval.voiceToneScore,
          voiceFeedback = finalEval.voiceFeedback,
          coachSummary = finalEval.coachSummary,
          passed = finalEval.passed,
        )

        repository.recordTelemetryEvent(
          agentName = _agentName.value,
          teamCode = _teamCode.value,
          incrementVisit = false,
          enteredStageNumber = stage.stageNumber,
          enteredStageTitle = stage.title,
          completedAttempts = updatedAttempts,
        )
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Log.e(TAG, "Evaluation failed", e)
        _aiState.value =
          RolePlayAiState.Error(e.localizedMessage ?: "ไม่สามารถประเมินผลได้ กรุณาลองอีกครั้ง")
      }
    }
  }

  fun clearAiError() {
    if (_aiState.value is RolePlayAiState.Error) {
      _aiState.value = RolePlayAiState.Idle
    }
  }
}
