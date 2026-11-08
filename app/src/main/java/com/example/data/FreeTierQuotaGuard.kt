package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * สถานะการควบคุมโควต้าฟรีของ Gemini API (Free-Tier Quota Guard)
 * พร้อมคำนวณเวลารีเซ็ตโควต้าฟรีตามเวลาจริงของ Google (Midnight Pacific Time = 14:00 / 15:00 น. เวลาไทย)
 */
data class FreeQuotaStatus(
  val modelName: String = "gemini-3.5-flash-lite (Flash-Lite รุ่นต่ำสุด ใช้ได้เยอะสุด)",
  val rpmUsed: Int = 0,
  val rpmLimit: Int = FreeTierQuotaGuard.MAX_FREE_RPM,
  val rpdUsed: Int = 0,
  val rpdLimit: Int = FreeTierQuotaGuard.MAX_FREE_RPD,
  val cachedResponsesCount: Int = 0,
  val isTemporarilyCapped: Boolean = false,
  val isDailyQuotaExhausted: Boolean = false,
  val nextResetTimeThaiLabel: String = "15:00 น. (เวลาประเทศไทย)",
  val countdownToResetText: String = "",
  val quotaExhaustedTitle: String = "",
  val quotaExhaustedDetailMessage: String = "",
  val statusMessage: String = "🛡️ โควต้าฟรี Gemini Flash-Lite 100% (0.00฿)",
)

/**
 * ระบบควบคุมและล็อกโควต้าฟรีสำหรับ Gemini API รุ่นต่ำสุด/ประหยัดสุด (gemini-3.5-flash-lite)
 * - ใช้โมเดล `gemini-3.5-flash-lite` (Flash-Lite Tier) ซึ่งกิน Token น้อยที่สุด เร็วที่สุด และให้โควต้าฟรีสูงสุดถึง 1,000 ครั้ง/วัน (เล่นได้ ~200-250 เกมเต็มต่อวัน)
 * - Google รีเซ็ตโควต้ารายวัน (RPD) ทุกเวลาเที่ยงคืนแปซิฟิก (00:00 PT - America/Los_Angeles)
 *   ซึ่งตรงกับเวลาประเทศไทย (Asia/Bangkok, UTC+7):
 *   • ช่วง พ.ย. – มี.ค. (PST UTC-8): รีเซ็ตเวลา 15:00 น. (บ่าย 3 โมงตรง)
 *   • ช่วง มี.ค. – พ.ย. (PDT UTC-7): รีเซ็ตเวลา 14:00 น. (บ่าย 2 โมงตรง)
 */
object FreeTierQuotaGuard {
  const val MAX_FREE_RPM = 15
  const val MAX_FREE_RPD = 1000
  const val MAX_FREE_OUTPUT_TOKENS = 260

  private val requestTimestampsMs = mutableListOf<Long>()
  private var nextPacificMidnightEpochMs: Long = computeNextPacificMidnightEpochMs(System.currentTimeMillis())
  private var dailyCount: Int = 0
  private val responseCache = ConcurrentHashMap<String, String>()

  private val _quotaStatus = MutableStateFlow(buildQuotaStatus(isCapped = false, isDailyExhausted = false))
  val quotaStatus: StateFlow<FreeQuotaStatus> = _quotaStatus.asStateFlow()

  /**
   * คำนวณเวลาเที่ยงคืนถัดไปของโซน America/Los_Angeles (00:00 PT) ที่ Google ใช้รีเซ็ตโควต้าฟรีรายวัน
   */
  fun computeNextPacificMidnightEpochMs(nowMs: Long = System.currentTimeMillis()): Long {
    val ptZone = TimeZone.getTimeZone("America/Los_Angeles")
    val ptCal = Calendar.getInstance(ptZone).apply {
      timeInMillis = nowMs
      add(Calendar.DAY_OF_YEAR, 1)
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return ptCal.timeInMillis
  }

  /**
   * แปลงเวลารีเซ็ตถัดไปเป็นข้อความเวลาประเทศไทย (Asia/Bangkok) พร้อมนับถอยหลังชั่วโมง/นาที
   */
  fun formatNextResetThaiSummary(nowMs: Long = System.currentTimeMillis()): Triple<String, String, String> {
    val resetMs =
      if (nowMs >= nextPacificMidnightEpochMs) computeNextPacificMidnightEpochMs(nowMs)
      else nextPacificMidnightEpochMs

    val thaiZone = TimeZone.getTimeZone("Asia/Bangkok")
    val nowThai = Calendar.getInstance(thaiZone).apply { timeInMillis = nowMs }
    val resetThai = Calendar.getInstance(thaiZone).apply { timeInMillis = resetMs }

    val timeFmt = SimpleDateFormat("HH:mm", Locale("th", "TH")).apply { timeZone = thaiZone }
    val dateFmt = SimpleDateFormat("d MMM", Locale("th", "TH")).apply { timeZone = thaiZone }

    val resetClockStr = "${timeFmt.format(Date(resetMs))} น."
    val isTomorrowInThai =
      resetThai.get(Calendar.DAY_OF_YEAR) != nowThai.get(Calendar.DAY_OF_YEAR) ||
        resetThai.get(Calendar.YEAR) != nowThai.get(Calendar.YEAR)
    val dayWord = if (isTomorrowInThai) "วันพรุ่งนี้ (${dateFmt.format(Date(resetMs))})" else "วันนี้"

    val diffMs = (resetMs - nowMs).coerceAtLeast(60_000L)
    val totalMinutes = (diffMs / 60_000L).toInt()
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val countdownStr =
      if (hours > 0) "อีก $hours ชั่วโมง $mins นาที" else "อีก $mins นาที"

    val fullResetHeadline = "$dayWord เวลา $resetClockStr (เวลาไทย)"
    return Triple(resetClockStr, countdownStr, fullResetHeadline)
  }

  @Synchronized
  fun getCachedResponse(cacheKey: String): String? {
    val cached = responseCache[cacheKey]
    if (cached != null) {
      refreshState(
        isCapped = false,
        isDailyExhausted = dailyCount >= MAX_FREE_RPD,
        note = "⚡ ใช้คำตอบจากหน่วยความจำประหยัดโควต้าฟรี (0.00฿)",
      )
    }
    return cached
  }

  @Synchronized
  fun putCachedResponse(cacheKey: String, jsonResponse: String) {
    if (responseCache.size > 200) {
      val firstKey = responseCache.keys().nextElement()
      if (firstKey != null) responseCache.remove(firstKey)
    }
    responseCache[cacheKey] = jsonResponse
    refreshState(isCapped = false, isDailyExhausted = dailyCount >= MAX_FREE_RPD)
  }

  /**
   * ตรวจสอบและจองสิทธิ์การเรียกใช้ Gemini API ภายในโควต้าฟรีเท่านั้น
   */
  @Synchronized
  fun tryAcquireFreeQuota(): Boolean {
    val now = System.currentTimeMillis()

    // หากถึงเวลา 00:00 PT (14:00 หรือ 15:00 น. เวลาไทย) ให้รีเซ็ตโควต้ารายวันอัตโนมัติ
    if (now >= nextPacificMidnightEpochMs) {
      nextPacificMidnightEpochMs = computeNextPacificMidnightEpochMs(now)
      dailyCount = 0
    }

    // ล้างประวัติที่เก่าเกิน 60 วินาที
    requestTimestampsMs.removeAll { now - it >= 60_000L }

    if (dailyCount >= MAX_FREE_RPD) {
      val (_, countdownStr, fullHeadline) = formatNextResetThaiSummary(now)
      refreshState(
        isCapped = true,
        isDailyExhausted = true,
        note = "🛑 โควต้าฟรี Gemini API วันนี้ครบแล้ว ($MAX_FREE_RPD ครั้ง) • รีเซ็ต $fullHeadline ($countdownStr)",
      )
      return false
    }

    if (requestTimestampsMs.size >= MAX_FREE_RPM) {
      val oldest = requestTimestampsMs.firstOrNull() ?: now
      val waitSec = (((60_000L - (now - oldest)) / 1000L).toInt()).coerceIn(1, 60)
      refreshState(
        isCapped = true,
        isDailyExhausted = false,
        note = "⏳ ครบโควต้าฟรีรายนาที ($MAX_FREE_RPM ครั้ง/นาที) กรุณารอ $waitSec วินาที (หรือคุยต่อด้วยระบบสำรองฟรี)",
      )
      return false
    }

    requestTimestampsMs.add(now)
    dailyCount += 1
    refreshState(isCapped = false, isDailyExhausted = false)
    return true
  }

  /**
   * เรียกเมื่อ Gemini API แจ้ง 429 / Quota Exhausted จากฝั่งเซิร์ฟเวอร์โดยตรง
   */
  @Synchronized
  fun markServerQuotaExhausted() {
    dailyCount = MAX_FREE_RPD
    val now = System.currentTimeMillis()
    val (_, countdownStr, fullHeadline) = formatNextResetThaiSummary(now)
    refreshState(
      isCapped = true,
      isDailyExhausted = true,
      note = "🛑 โควต้าฟรี Gemini API วันนี้เต็มแล้ว! กลับมาต่อได้ใน$fullHeadline ($countdownStr)",
    )
  }

  /**
   * สำหรับทดสอบดูหน้าต่างแจ้งเตือนเมื่อโควต้าฟรีหมด และกดรีเซ็ตกลับได้
   */
  @Synchronized
  fun simulateQuotaExhaustedForPreview() {
    dailyCount = MAX_FREE_RPD
    val now = System.currentTimeMillis()
    val (_, countdownStr, fullHeadline) = formatNextResetThaiSummary(now)
    refreshState(
      isCapped = true,
      isDailyExhausted = true,
      note = "🛑 โควต้าฟรี Gemini API วันนี้ครบแล้ว • กลับมาต่อได้ใน$fullHeadline ($countdownStr)",
    )
  }

  @Synchronized
  fun resetQuotaCounter() {
    dailyCount = 0
    requestTimestampsMs.clear()
    refreshState(isCapped = false, isDailyExhausted = false)
  }

  @Synchronized
  private fun refreshState(
    isCapped: Boolean,
    isDailyExhausted: Boolean,
    note: String? = null,
  ) {
    _quotaStatus.value =
      buildQuotaStatus(
        isCapped = isCapped,
        isDailyExhausted = isDailyExhausted,
        customNote = note,
      )
  }

  private fun buildQuotaStatus(
    isCapped: Boolean,
    isDailyExhausted: Boolean,
    customNote: String? = null,
  ): FreeQuotaStatus {
    val now = System.currentTimeMillis()
    requestTimestampsMs.removeAll { now - it >= 60_000L }
    val rpm = requestTimestampsMs.size
    val rpd = dailyCount
    val (resetClockStr, countdownStr, fullHeadline) = formatNextResetThaiSummary(now)

    val exhaustedTitle =
      if (isDailyExhausted) {
        "🛑 โควต้าฟรี Gemini API วันนี้ครบแล้ว (0.00฿)"
      } else if (isCapped) {
        "⏳ ครบโควต้าฟรีรายนาที ($MAX_FREE_RPM ครั้ง/นาที)"
      } else {
        ""
      }

    val exhaustedDetail =
      if (isDailyExhausted) {
        "คุณใช้โควต้าฟรีของ Gemini API ครบ $MAX_FREE_RPD ครั้งแล้วในวันนี้ (ระบบตัดอัตโนมัติ ไม่มีการคิดเงินเพิ่ม 100%)\n\n" +
          "🕒 เวลาที่รีเซ็ตโควต้าฟรีรอบถัดไป:\n" +
          "👉 กลับมาเล่นด้วย Gemini API ต่อได้ใน$fullHeadline ($countdownStr)\n" +
          "(Google รีเซ็ตโควต้าฟรีทุกวันเวลา 00:00 PT แปซิฟิก = $resetClockStr เวลาไทย)"
      } else if (isCapped) {
        "คุณส่งคำพูดเร็วเกินโควต้าฟรีรายนาที ($MAX_FREE_RPM ครั้ง/นาที) กรุณารอประมาณ 15-30 วินาทีแล้วพูดต่อได้เลยครับ (โควต้ารายวันรีเซ็ตเวลา $resetClockStr)"
      } else {
        "โควต้าฟรีคงเหลือวันนี้: ${MAX_FREE_RPD - rpd}/$MAX_FREE_RPD ครั้ง • รีเซ็ตโควต้าฟรีรอบถัดไป: $fullHeadline ($countdownStr)"
      }

    val msg =
      customNote
        ?: "🛡️ โควต้าฟรี Gemini: $rpd/$MAX_FREE_RPD ต่อวัน • รีเซ็ต $resetClockStr ($countdownStr)"

    return FreeQuotaStatus(
      rpmUsed = rpm,
      rpmLimit = MAX_FREE_RPM,
      rpdUsed = rpd,
      rpdLimit = MAX_FREE_RPD,
      cachedResponsesCount = responseCache.size,
      isTemporarilyCapped = isCapped,
      isDailyQuotaExhausted = isDailyExhausted,
      nextResetTimeThaiLabel = "$fullHeadline ($countdownStr)",
      countdownToResetText = countdownStr,
      quotaExhaustedTitle = exhaustedTitle,
      quotaExhaustedDetailMessage = exhaustedDetail,
      statusMessage = msg,
    )
  }
}
