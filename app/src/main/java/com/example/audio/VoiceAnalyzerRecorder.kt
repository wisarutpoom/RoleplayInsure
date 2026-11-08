package com.example.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import com.example.data.VoiceRecordingMetrics
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class VoiceAnalyzerRecorder(private val context: Context) {

  private var mediaRecorder: MediaRecorder? = null
  private var outputFile: File? = null
  private var startTimeMs: Long = 0L
  private var amplitudeJob: Job? = null
  private val amplitudeSamples = mutableListOf<Int>()

  private val _isRecording = MutableStateFlow(false)
  val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

  private val _currentAmplitude = MutableStateFlow(0f) // Normalized 0f..1f
  val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

  private val _elapsedSeconds = MutableStateFlow(0)
  val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()

  fun startRecording(scope: CoroutineScope): Boolean {
    stopRecordingInternal()
    return try {
      val file = File(context.cacheDir, "agent_roleplay_voice.m4a")
      if (file.exists()) {
        file.delete()
      }
      outputFile = file

      val recorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          MediaRecorder(context)
        } else {
          @Suppress("DEPRECATION") MediaRecorder()
        }

      recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
      recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
      recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
      recorder.setAudioSamplingRate(16000)
      recorder.setAudioEncodingBitRate(64000)
      recorder.setOutputFile(file.absolutePath)
      recorder.prepare()
      recorder.start()

      mediaRecorder = recorder
      startTimeMs = System.currentTimeMillis()
      amplitudeSamples.clear()
      _isRecording.value = true
      _elapsedSeconds.value = 0

      amplitudeJob =
        scope.launch(Dispatchers.Default) {
          while (isActive && _isRecording.value) {
            val rawAmp =
              try {
                mediaRecorder?.maxAmplitude ?: 0
              } catch (e: Exception) {
                0
              }
            amplitudeSamples.add(rawAmp)
            val normalized = (rawAmp / 22000f).coerceIn(0.05f, 1.0f)
            _currentAmplitude.value = normalized
            _elapsedSeconds.value = ((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()
            delay(120L)
          }
        }
      true
    } catch (e: Exception) {
      Log.e("VoiceRecorder", "Failed to start MediaRecorder", e)
      stopRecordingInternal()
      false
    }
  }

  fun stopAndAnalyze(spokenTextHint: String = ""): Pair<ByteArray?, VoiceRecordingMetrics> {
    val durationSec =
      (((System.currentTimeMillis() - startTimeMs) / 1000L).toInt()).coerceAtLeast(1)
    stopRecordingInternal()

    val audioBytes =
      try {
        val file = outputFile
        if (file != null && file.exists() && file.length() > 0 && file.length() < 15 * 1024 * 1024) {
          file.readBytes()
        } else {
          null
        }
      } catch (e: Exception) {
        Log.e("VoiceRecorder", "Failed to read recorded audio bytes", e)
        null
      }

    val avgAmp =
      if (amplitudeSamples.isNotEmpty()) amplitudeSamples.average().roundToInt() else 6500
    val activeFrames = amplitudeSamples.count { it > 1200 }
    val silenceFrames = (amplitudeSamples.size - activeFrames).coerceAtLeast(0)
    val pauseRatio =
      if (amplitudeSamples.isNotEmpty()) silenceFrames.toFloat() / amplitudeSamples.size else 0.25f

    val wordCount =
      if (spokenTextHint.isNotBlank()) {
        (spokenTextHint.length / 4).coerceAtLeast(6)
      } else {
        (durationSec * 2.4).roundToInt().coerceAtLeast(8)
      }
    val wpm =
      ((wordCount.toFloat() / durationSec.coerceAtLeast(3)) * 60f).roundToInt().coerceIn(85, 195)

    val isTooFast = wpm > 165
    val isTooQuiet = avgAmp < 2500

    val speechRateLabel =
      when {
        isTooFast -> "พูดเร็วเกินไป ($wpm WPM > 165) ควรลดสปีดและเว้นจังหวะ"
        wpm < 100 -> "ค่อนข้างช้า ($wpm WPM) เพิ่มความกระฉับกระเฉงอีกนิด"
        else -> "จังหวะพอดีเป็นธรรมชาติ ($wpm WPM)"
      }

    val energyLevelLabel =
      when {
        avgAmp > 14000 -> "พลังเสียงดังฟังชัด แต่อาจฟังดูกดดันลูกค้าในบางช่วง"
        isTooQuiet -> "เสียงเบาเกินไป ควรเพิ่มพลังเสียงจากกระบังลม"
        else -> "ระดับเสียงอบอุ่น มั่นคง น่าเชื่อถือ"
      }

    val pauseBalanceLabel =
      when {
        pauseRatio < 0.12f -> "พูดรัวติดกัน ควรเว้นวรรคท้ายประโยค 1 วินาที"
        pauseRatio > 0.55f -> "มีจังหวะลังเลหรือหยุดคิดนานในบางช่วง"
        else -> "การเว้นจังหวะหายใจและเน้นคำสมดุลดีมาก"
      }

    val confidenceIndex =
      (84 -
          (if (isTooFast) 12 else if (wpm < 95) 6 else 0) -
          (if (isTooQuiet) 12 else 0) +
          (if (pauseRatio in 0.15f..0.42f) 10 else 0))
        .coerceIn(55, 98)

    val voiceCoachingTip = buildVoiceCoachingFeedbackTip(wpm = wpm, isTooFast = isTooFast, isTooQuiet = isTooQuiet)

    val metrics =
      VoiceRecordingMetrics(
        durationSeconds = durationSec,
        speechRateLabel = speechRateLabel,
        energyLevelLabel = energyLevelLabel,
        pauseBalanceLabel = pauseBalanceLabel,
        estimatedWordsPerMinute = wpm,
        confidenceIndex = confidenceIndex,
        avgAmplitude = avgAmp,
        isTooFast = isTooFast,
        isTooQuiet = isTooQuiet,
        voiceCoachingTip = voiceCoachingTip,
      )
    return audioBytes to metrics
  }

  private fun stopRecordingInternal() {
    _isRecording.value = false
    amplitudeJob?.cancel()
    amplitudeJob = null
    _currentAmplitude.value = 0f
    try {
      mediaRecorder?.stop()
    } catch (@Suppress("SwallowedException") e: Exception) {}
    try {
      mediaRecorder?.release()
    } catch (@Suppress("SwallowedException") e: Exception) {}
    mediaRecorder = null
  }

  companion object {
    fun buildVoiceCoachingFeedbackTip(
      wpm: Int,
      isTooFast: Boolean,
      isTooQuiet: Boolean,
    ): String {
      return when {
        isTooFast && isTooQuiet ->
          "⚠️ Voice Coaching Alert ($wpm WPM & เสียงเบา): คุณพูดเร็วเกิน 165 คำ/นาที และเสียงค่อนข้างเบา! แนะนำให้สูดหายใจลึกๆ พูดให้ดังขึ้นจากกระบังลม และเว้นจังหวะท้ายประโยค 1 วินาที เพื่ออัพคะแนน Voice Tone Score (+15 แต้ม)"
        isTooFast ->
          "⚡ Voice Coaching Alert (พูดเร็ว $wpm WPM > 165): จังหวะการพูดเร็วเกินไปอาจทำให้ดูตื่นเต้นหรือเร่งรัดลูกค้า แนะนำให้ลดความเร็วลง 15% ทอดเสียงคำลงท้ายให้นุ่มนวล และหยุดเว้นจังหวะหลังถามคำถาม เพื่อเพิ่ม Voice Tone Score (+12 แต้ม)"
        isTooQuiet ->
          "🔉 Voice Coaching Alert (เสียงเบาเกินไป): น้ำเสียงฟังดูไม่มั่นใจ แนะนำให้ยืดอก พูดเต็มเสียงให้กังวานขึ้น และเน้นน้ำเสียงที่คำสำคัญ (เช่น ผลประโยชน์/ความคุ้มครอง) เพื่อเพิ่มความน่าเชื่อถือและ Voice Tone Score (+12 แต้ม)"
        else ->
          "✅ Voice Coaching ($wpm WPM • น้ำเสียงสมดุล): จังหวะการพูดและความดังอยู่ในเกณฑ์มืออาชีพ (110–160 WPM) น้ำเสียงอบอุ่นมั่นคง ช่วยดันคะแนน Voice Tone Score ขึ้นสู่ระดับดีเยี่ยม!"
      }
    }
  }
}
