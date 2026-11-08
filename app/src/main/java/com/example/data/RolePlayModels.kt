package com.example.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class StageAttempt(
  @DocumentId val id: String = "",
  val userId: String = "",
  val agentName: String = "",
  val teamCode: String = "TEAM_ALPHA",
  val stageNumber: Int = 1,
  val stageTitle: String = "",
  val category: String = "COLD_CALL",
  val overallScore: Int = 0,
  val rapportScore: Int = 0,
  val objectionScore: Int = 0,
  val factFindingScore: Int = 0,
  val preApproachScore: Int = 0,
  val voiceToneScore: Int = 0,
  val voiceFeedback: String = "",
  val coachSummary: String = "",
  val passed: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AgentTelemetry(
  @DocumentId val id: String = "",
  val userId: String = "",
  val agentName: String = "",
  val teamCode: String = "TEAM_ALPHA",
  val visitCount: Int = 1,
  val totalAttemptsCount: Int = 0,
  val enteredStagesCsv: String = "",
  val completedStagesCsv: String = "",
  val enteredStagesCount: Int = 0,
  val completedStagesCount: Int = 0,
  val bestOverallScore: Int = 0,
  val averageOverallScore: Int = 0,
  val averageVoiceScore: Int = 0,
  val lastActiveStage: Int = 0,
  val lastActiveStageTitle: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun parsedEnteredStages(): List<Int> {
    if (enteredStagesCsv.isBlank()) return emptyList()
    return enteredStagesCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()
  }

  fun parsedCompletedStages(): List<Int> {
    if (completedStagesCsv.isBlank()) return emptyList()
    return completedStagesCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.distinct().sorted()
  }
}

@IgnoreExtraProperties
data class StaffPermission(
  @DocumentId val id: String = "",
  val userId: String = "",
  val staffEmail: String = "",
  val staffName: String = "",
  val role: String = "SECRETARY", // SECRETARY, SUB_ADMIN, VIEWER
  val status: String = "APPROVED", // APPROVED, PENDING, REVOKED
  val canViewVisitsAndStages: Boolean = true,
  val canViewScores: Boolean = true,
  val canExportCsv: Boolean = true,
  val canManageTeam: Boolean = false,
  val approvedByEmail: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class StageCategory(
  val code: String,
  val titleTh: String,
  val subtitleTh: String,
  val stageRange: String,
) {
  COLD_CALL(
    code = "COLD_CALL",
    titleTh = "หมวดที่ 1: โทรนัดสายฟ้าแลบ (Cold Call)",
    subtitleTh = "ฝึกเปิดใจทางโทรศัพท์ ฝ่าข้อโต้แย้งสุดฮาและปิดนัดหมาย (ด่าน 1-6)",
    stageRange = "ด่าน 1 - 6",
  ),
  FIRST_MEET(
    code = "FIRST_MEET",
    titleTh = "หมวดที่ 2: ปะทะซึ่งหน้าครั้งแรก (First Impression)",
    subtitleTh = "สร้างความไว้วางใจ ทลายกำแพงอคติ และปูทางสู่การพูดคุยการเงิน (ด่าน 7-12)",
    stageRange = "ด่าน 7 - 12",
  ),
  FACT_FINDING(
    code = "FACT_FINDING",
    titleTh = "หมวดที่ 3: นักสืบกระเป๋าตังค์ (Fact-Finding)",
    subtitleTh = "ตั้งคำถามทรงพลัง ค้นหารายได้ ภาระหนี้ ความกังวล และเป้าหมายชีวิต (ด่าน 13-19)",
    stageRange = "ด่าน 13 - 19",
  ),
  NEEDS_ANALYSIS(
    code = "NEEDS_ANALYSIS",
    titleTh = "หมวดที่ 4: ชี้เป้าหมวดประกันหน้างาน (Pre-Approach)",
    subtitleTh = "สรุปภาพรวมการเงินและชี้เป้าหมวดความคุ้มครองที่เหมาะสมก่อนเข้าแบบประกัน (ด่าน 20-25)",
    stageRange = "ด่าน 20 - 25",
  ),
}

data class RolePlayStage(
  val stageNumber: Int,
  val title: String,
  val category: StageCategory,
  val difficulty: Int, // 1 to 5 stars
  val customerName: String,
  val customerAge: Int,
  val customerOccupation: String,
  val customerPersona: String,
  val hiddenFinancialFact: String,
  val missionObjective: String,
  val openingCustomerLine: String,
  val targetInsuranceDirection: String,
  val keyTips: List<String>,
)

data class ChatTurn(
  val id: String,
  val isAgent: Boolean,
  val text: String,
  val voiceMetricsSummary: String? = null,
  val coachTip: String? = null,
  val voiceCoachingAlert: String? = null,
  val isVoiceAlertWarning: Boolean = false,
)

data class VoiceRecordingMetrics(
  val durationSeconds: Int,
  val speechRateLabel: String,
  val energyLevelLabel: String,
  val pauseBalanceLabel: String,
  val estimatedWordsPerMinute: Int,
  val confidenceIndex: Int,
  val avgAmplitude: Int = 6500,
  val isTooFast: Boolean = estimatedWordsPerMinute > 165,
  val isTooQuiet: Boolean = avgAmplitude < 2500,
  val voiceCoachingTip: String = "",
)

data class EvaluationResult(
  val overallScore: Int,
  val rapportScore: Int,
  val objectionScore: Int,
  val factFindingScore: Int,
  val preApproachScore: Int,
  val voiceToneScore: Int,
  val voiceFeedback: String,
  val coachSummary: String,
  val strengths: List<String>,
  val improvements: List<String>,
  val recommendedDirectionCheck: String,
  val passed: Boolean,
  val newlyUnlockedBadges: List<PlayerBadge> = emptyList(),
)

data class LeaderboardCompetitor(
  val rank: Int,
  val playerId: String,
  val playerName: String,
  val funnyTitle: String,
  val avatarEmoji: String,
  val highScorePoints: Int,
  val bestSingleScore: Int,
  val totalAttempts: Int,
  val stagesCleared: Int,
  val isCurrentUser: Boolean = false,
)

data class StageRankSummary(
  val stageNumber: Int,
  val stageTitle: String,
  val customerName: String,
  val yourRank: Int, // 1..10+
  val yourBestScore: Int,
  val yourAttempts: Int,
  val topScoreOnStage: Int,
  val topHolderName: String,
)

data class PlayerBadge(
  val id: String,
  val emoji: String,
  val title: String,
  val funnyRankName: String,
  val conditionDescription: String,
  val unlocked: Boolean,
  val progressText: String,
)
