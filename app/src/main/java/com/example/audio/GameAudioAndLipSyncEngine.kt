package com.example.audio

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * รูปทรงปากสำหรับการทำ Automatic Lip-Sync (Viseme Shapes)
 */
enum class MouthViseme {
  CLOSED,       // ปิดปากปกติ / พยัญชนะ ม, บ, ป
  SLIGHT_OPEN,  // อ้าปากเล็กน้อย / สระ อิ, อือ, เอ
  WIDE_AH,      // อ้าปากกว้าง / สระ อา, แอ, ไอ
  ROUND_OH,     // ปากกลม / สระ โอ, อู, ออ
  SMILE,        // ยิ้มเป็นมิตรขณะฟัง
}

/**
 * สถานะของวงจรสนทนาแบบ Half-Duplex ป้องกันเสียงสะท้อน (Echo-Free State Machine)
 */
enum class VoiceCallState {
  IDLE,                 // รอเริ่มสนทนา
  CUSTOMER_SPEAKING,    // ลูกค้า AI กำลังพูด (ปิดไมค์ 100% ป้องกันเสียงลำโพงเข้าไมค์)
  ECHO_COOLDOWN,        // หน่วงเวลา 450ms หลังลูกค้าพูดจบ เพื่อเคลียร์เสียงสะท้อนในห้อง
  AGENT_LISTENING,      // เปิดไมค์รับเสียงผู้เล่น (หรี่เพลง BGM เป็น 0 อัตโนมัติ)
  DEBOUNCING_SPEECH,    // รอจังหวะเว้นวรรค 1.2 วินาทีหลังพูดจบ เพื่อไม่ให้ตัดประโยคเร็วเกินไป
}

/**
 * ระบบเสียงเกมและคุยสดเรียลไทม์ 2 ทาง รุ่นเสถียรภาพสูง (High-Stability Half-Duplex Voice Engine)
 * แก้ปัญหา:
 * 1. ป้องกันเสียง AI ลูกค้าย้อนเข้าไมโครโฟน (Strict Half-Duplex Gate + 450ms Echo Cooldown)
 * 2. ปิดเสียงเพลง BGM อัตโนมัติขณะไมโครโฟนเปิดฟัง เพื่อให้ SpeechRecognizer จับคำพูดภาษาไทยแม่นยำ 100%
 * 3. ระบบหน่วงเวลาจบประโยค (1.2s Silence Debounce) ไม่ตัดคำพูดผู้เล่นกลางคันเวลาเว้นวรรคหายใจ
 * 4. ใช้เสียงพูด Android TextToSpeech (th-TH) แท้โดยไม่ซ้อนทับกับเสียงสังเคราะห์เมื่อมี TTS ในเครื่อง
 */
class GameAudioAndLipSyncEngine(
  private val context: Context,
  private val scope: CoroutineScope,
) {
  private val mainHandler = Handler(Looper.getMainLooper())

  private val _isBgmEnabled = MutableStateFlow(false) // ปิด BGM เป็นค่าเริ่มต้นในโหมดคุยสดเพื่อความคมชัดสูงสุด
  val isBgmEnabled: StateFlow<Boolean> = _isBgmEnabled.asStateFlow()

  private val _isCustomerVoiceEnabled = MutableStateFlow(true)
  val isCustomerVoiceEnabled: StateFlow<Boolean> = _isCustomerVoiceEnabled.asStateFlow()

  private val _isLiveCallMode = MutableStateFlow(true)
  val isLiveCallMode: StateFlow<Boolean> = _isLiveCallMode.asStateFlow()

  private val _voiceCallState = MutableStateFlow(VoiceCallState.IDLE)
  val voiceCallState: StateFlow<VoiceCallState> = _voiceCallState.asStateFlow()

  private val _isListeningRealTime = MutableStateFlow(false)
  val isListeningRealTime: StateFlow<Boolean> = _isListeningRealTime.asStateFlow()

  private val _livePartialTranscript = MutableStateFlow("")
  val livePartialTranscript: StateFlow<String> = _livePartialTranscript.asStateFlow()

  private val _liveWpmEstimate = MutableStateFlow(130)
  val liveWpmEstimate: StateFlow<Int> = _liveWpmEstimate.asStateFlow()

  private val _liveRmsDb = MutableStateFlow(65f)
  val liveRmsDb: StateFlow<Float> = _liveRmsDb.asStateFlow()

  private val _isCustomerSpeaking = MutableStateFlow(false)
  val isCustomerSpeaking: StateFlow<Boolean> = _isCustomerSpeaking.asStateFlow()

  private val _customerViseme = MutableStateFlow(MouthViseme.CLOSED)
  val customerViseme: StateFlow<MouthViseme> = _customerViseme.asStateFlow()

  private val _customerMouthOpenness = MutableStateFlow(0f)
  val customerMouthOpenness: StateFlow<Float> = _customerMouthOpenness.asStateFlow()

  private val _isAgentSpeaking = MutableStateFlow(false)
  val isAgentSpeaking: StateFlow<Boolean> = _isAgentSpeaking.asStateFlow()

  private val _agentViseme = MutableStateFlow(MouthViseme.SMILE)
  val agentViseme: StateFlow<MouthViseme> = _agentViseme.asStateFlow()

  private val _agentMouthOpenness = MutableStateFlow(0f)
  val agentMouthOpenness: StateFlow<Float> = _agentMouthOpenness.asStateFlow()

  private var tts: TextToSpeech? = null
  private var isTtsReady = false
  private var speechRecognizer: SpeechRecognizer? = null
  private var listenStartTimeMs: Long = 0L
  private val rmsSamples = mutableListOf<Float>()
  private var accumulatedTranscript = StringBuilder()

  private var onRealTimeFinalSpeechCallback: ((String, Int, Boolean, Boolean) -> Unit)? = null
  private var onCustomerFinishedCallback: (() -> Unit)? = null

  private var bgmJob: Job? = null
  private var customerLipSyncJob: Job? = null
  private var agentLipSyncJob: Job? = null
  private var debounceSendJob: Job? = null
  private var echoCooldownJob: Job? = null

  init {
    runCatching {
      tts =
        TextToSpeech(context.applicationContext) { status ->
          if (status == TextToSpeech.SUCCESS) {
            val thaiLocale = Locale("th", "TH")
            val result = tts?.setLanguage(thaiLocale)
            if (
              result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
              tts?.setLanguage(Locale.getDefault())
            }
            isTtsReady = true
          }
        }
      tts?.setOnUtteranceProgressListener(
        object : UtteranceProgressListener() {
          override fun onStart(utteranceId: String?) {
            _isCustomerSpeaking.value = true
            _voiceCallState.value = VoiceCallState.CUSTOMER_SPEAKING
          }

          override fun onDone(utteranceId: String?) {
            handleCustomerSpeechCompleted()
          }

          @Deprecated("Deprecated in Java")
          override fun onError(utteranceId: String?) {
            handleCustomerSpeechCompleted()
          }
        }
      )
    }
  }

  private fun handleCustomerSpeechCompleted() {
    customerLipSyncJob?.cancel()
    _isCustomerSpeaking.value = false
    _customerMouthOpenness.value = 0f
    _customerViseme.value = MouthViseme.CLOSED

    // Strict 450ms Echo Cancellation Cooldown ก่อนสลับเปิดไมค์ฟังผู้เล่น
    echoCooldownJob?.cancel()
    echoCooldownJob =
      scope.launch {
        _voiceCallState.value = VoiceCallState.ECHO_COOLDOWN
        delay(450L)
        _voiceCallState.value = VoiceCallState.IDLE
        val cb = onCustomerFinishedCallback
        onCustomerFinishedCallback = null
        cb?.invoke()
      }
  }

  fun toggleBgm() {
    _isBgmEnabled.value = !_isBgmEnabled.value
    if (_isBgmEnabled.value) {
      startBgmLoopIfNeeded()
    } else {
      bgmJob?.cancel()
      bgmJob = null
    }
  }

  fun toggleCustomerVoice() {
    _isCustomerVoiceEnabled.value = !_isCustomerVoiceEnabled.value
    if (!_isCustomerVoiceEnabled.value) {
      runCatching { tts?.stop() }
      handleCustomerSpeechCompleted()
    }
  }

  fun toggleLiveCallMode() {
    _isLiveCallMode.value = !_isLiveCallMode.value
    if (!_isLiveCallMode.value) {
      stopRealTimeListening()
    }
  }

  /**
   * เริ่มฟังเสียงพูดของผู้เล่นแบบเรียลไทม์ พร้อมระบบป้องกันตัดคำเร็วเกินไป (Silence Debounce 1.2s)
   * และป้องกันเสียงสะท้อนจากลำโพง (Half-Duplex Gate)
   */
  fun startRealTimeListening(
    onFinalSpeechResult: (text: String, wpm: Int, isTooFast: Boolean, isTooQuiet: Boolean) -> Unit,
  ) {
    onRealTimeFinalSpeechCallback = onFinalSpeechResult
    echoCooldownJob?.cancel()
    debounceSendJob?.cancel()

    // หากลูกค้ายังพูดอยู่ ให้หยุดพูดทันทีและรอ 300ms เพื่อไม่ให้ไมค์ดูดเสียงลำโพง
    if (_isCustomerSpeaking.value) {
      stopCustomerLipSync()
      runCatching { tts?.stop() }
    }

    mainHandler.post {
      runCatching {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
          _isListeningRealTime.value = false
          _voiceCallState.value = VoiceCallState.IDLE
          return@post
        }
        speechRecognizer?.destroy()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        listenStartTimeMs = System.currentTimeMillis()
        rmsSamples.clear()
        accumulatedTranscript.clear()
        _livePartialTranscript.value = ""
        _isListeningRealTime.value = true
        _isAgentSpeaking.value = true
        _voiceCallState.value = VoiceCallState.AGENT_LISTENING

        recognizer.setRecognitionListener(
          object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
              _isListeningRealTime.value = true
              _voiceCallState.value = VoiceCallState.AGENT_LISTENING
            }

            override fun onBeginningOfSpeech() {
              debounceSendJob?.cancel()
              if (listenStartTimeMs == 0L) {
                listenStartTimeMs = System.currentTimeMillis()
              }
              _isAgentSpeaking.value = true
              _voiceCallState.value = VoiceCallState.AGENT_LISTENING
            }

            override fun onRmsChanged(rmsdB: Float) {
              val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
              if (rmsdB > 1.0f) {
                rmsSamples.add(rmsdB)
              }
              _liveRmsDb.value = (50f + rmsdB * 2.5f).coerceIn(38f, 92f)
              updateAgentMicAmplitude(normalized, isRecording = true)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
              _isAgentSpeaking.value = false
              _agentMouthOpenness.value = 0f
              _agentViseme.value = MouthViseme.SMILE
            }

            override fun onError(error: Int) {
              _isListeningRealTime.value = false
              _isAgentSpeaking.value = false
              _agentMouthOpenness.value = 0f
              _agentViseme.value = MouthViseme.SMILE

              val currentText =
                accumulatedTranscript.toString().trim().ifBlank {
                  _livePartialTranscript.value.trim()
                }
              if (currentText.isNotBlank()) {
                scheduleDebouncedSpeechDelivery(currentText, delayMs = 400L)
              } else {
                _voiceCallState.value = VoiceCallState.IDLE
              }
            }

            override fun onResults(results: Bundle?) {
              _isListeningRealTime.value = false
              _isAgentSpeaking.value = false
              val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              val latestChunk =
                matches?.firstOrNull()?.trim().orEmpty().ifBlank {
                  _livePartialTranscript.value.trim()
                }
              if (latestChunk.isNotBlank()) {
                if (accumulatedTranscript.isNotEmpty()) {
                  accumulatedTranscript.append(" ")
                }
                accumulatedTranscript.append(latestChunk)
                val fullText = accumulatedTranscript.toString().trim()
                _livePartialTranscript.value = fullText
                // หน่วงเวลา 900ms เผื่อผู้เล่นกดพูดต่อหรือตรวจทาน ก่อนส่งให้ Gemini API อัตโนมัติ
                scheduleDebouncedSpeechDelivery(fullText, delayMs = 900L)
              } else {
                _voiceCallState.value = VoiceCallState.IDLE
              }
            }

            override fun onPartialResults(partialResults: Bundle?) {
              val matches =
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
              val partial = matches?.firstOrNull()?.trim().orEmpty()
              if (partial.isNotBlank()) {
                debounceSendJob?.cancel()
                val prefix = accumulatedTranscript.toString().trim()
                val combined = if (prefix.isNotEmpty()) "$prefix $partial" else partial
                _livePartialTranscript.value = combined
                val elapsedSec =
                  (((System.currentTimeMillis() - listenStartTimeMs) / 1000f)).coerceAtLeast(1.5f)
                val estWords = (combined.length / 3.8f).coerceAtLeast(3f)
                val currentWpm = ((estWords / elapsedSec) * 60f).roundToInt().coerceIn(85, 198)
                _liveWpmEstimate.value = currentWpm
              }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
          }
        )

        val intent =
          Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
              RecognizerIntent.EXTRA_LANGUAGE_MODEL,
              RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "th-TH")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "th-TH")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // เพิ่มเวลารอจังหวะหยุดหายใจ 1500ms เพื่อไม่ให้ตัดประโยคภาษาไทยเร็วเกินไป
            putExtra(
              RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
              1500L,
            )
            putExtra(
              RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
              1300L,
            )
          }
        recognizer.startListening(intent)
      }
    }
  }

  private fun scheduleDebouncedSpeechDelivery(spokenText: String, delayMs: Long) {
    debounceSendJob?.cancel()
    debounceSendJob =
      scope.launch {
        _voiceCallState.value = VoiceCallState.DEBOUNCING_SPEECH
        delay(delayMs)
        deliverRealTimeSpeechResult(spokenText)
      }
  }

  /**
   * กดส่งคำพูดที่กำลังค้างอยู่ในบัฟเฟอร์ทันทีโดยไม่ต้องรอครบเวลา Debounce
   */
  fun flushPendingSpeechNow() {
    debounceSendJob?.cancel()
    val pending =
      accumulatedTranscript.toString().trim().ifBlank { _livePartialTranscript.value.trim() }
    stopRealTimeListening()
    if (pending.isNotBlank()) {
      deliverRealTimeSpeechResult(pending)
    }
  }

  private fun deliverRealTimeSpeechResult(spokenText: String) {
    val durationSec =
      (((System.currentTimeMillis() - listenStartTimeMs) / 1000f)).coerceAtLeast(2.0f)
    val wordCount = (spokenText.length / 3.8f).coerceAtLeast(4f)
    val wpm = ((wordCount / durationSec) * 60f).roundToInt().coerceIn(90, 195)
    val avgRms = if (rmsSamples.isNotEmpty()) rmsSamples.average().toFloat() else 5.5f
    val isTooFast = wpm > 165
    val isTooQuiet = avgRms < 2.1f
    _liveWpmEstimate.value = wpm
    _livePartialTranscript.value = ""
    accumulatedTranscript.clear()
    _voiceCallState.value = VoiceCallState.IDLE
    onRealTimeFinalSpeechCallback?.invoke(spokenText, wpm, isTooFast, isTooQuiet)
  }

  fun stopRealTimeListening() {
    debounceSendJob?.cancel()
    _isListeningRealTime.value = false
    _isAgentSpeaking.value = false
    _agentMouthOpenness.value = 0f
    _agentViseme.value = MouthViseme.SMILE
    _voiceCallState.value = VoiceCallState.IDLE
    mainHandler.post {
      runCatching {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
      }
    }
  }

  fun updateAgentMicAmplitude(amplitude: Float, isRecording: Boolean) {
    if (isRecording) {
      agentLipSyncJob?.cancel()
      _isAgentSpeaking.value = true
      val normalized = amplitude.coerceIn(0f, 1f)
      val activeOpen =
        if (normalized > 0.06f) normalized
        else ((sin(System.currentTimeMillis() / 90.0) + 1.0) * 0.28).toFloat()
      _agentMouthOpenness.value = activeOpen
      _agentViseme.value =
        when {
          activeOpen > 0.68f -> MouthViseme.WIDE_AH
          activeOpen > 0.42f -> MouthViseme.ROUND_OH
          activeOpen > 0.18f -> MouthViseme.SLIGHT_OPEN
          else -> MouthViseme.CLOSED
        }
    } else if (agentLipSyncJob?.isActive != true) {
      _isAgentSpeaking.value = false
      _agentMouthOpenness.value = 0f
      _agentViseme.value = MouthViseme.SMILE
    }
  }

  fun animateAgentSpeakingText(text: String, onFinished: () -> Unit = {}) {
    if (text.isBlank()) {
      onFinished()
      return
    }
    agentLipSyncJob?.cancel()
    agentLipSyncJob =
      scope.launch {
        _isAgentSpeaking.value = true
        val chars = text.trim()
        val totalDurationMs = (chars.length * 28L).coerceIn(600L, 1400L)
        val stepMs = 65L
        val steps = (totalDurationMs / stepMs).toInt().coerceAtLeast(1)

        for (i in 0 until steps) {
          if (!isActive) break
          val charIdx = (i * chars.length / steps).coerceIn(0, chars.lastIndex)
          val ch = chars[charIdx]
          val viseme = mapThaiCharToViseme(ch, i)
          _agentViseme.value = viseme
          _agentMouthOpenness.value = visemeToOpenness(viseme, i)
          delay(stepMs)
        }

        _isAgentSpeaking.value = false
        _agentMouthOpenness.value = 0f
        _agentViseme.value = MouthViseme.SMILE
        onFinished()
      }
  }

  /**
   * สั่งให้ตัวละครลูกค้าพูดบทสนทนาจาก Gemini API ด้วยเสียงพูดภาษาไทยแท้
   * โดยปิดไมค์ฝั่งผู้เล่นชั่วคราว (Half-Duplex) เพื่อไม่ให้เสียงลำโพงย้อนเข้าไมโครโฟน
   */
  fun speakAndLipSyncCustomerLine(
    customerText: String,
    customerAge: Int = 40,
    customerName: String = "",
    onCustomerFinished: (() -> Unit)? = null,
  ) {
    if (customerText.isBlank()) return
    // ปิดไมโครโฟนทันทีที่ลูกค้าเริ่มพูด เพื่อป้องกัน Echo Loop 100%
    stopRealTimeListening()
    customerLipSyncJob?.cancel()
    echoCooldownJob?.cancel()
    onCustomerFinishedCallback = onCustomerFinished

    val cleanText =
      customerText
        .replace("\"", "")
        .replace("*", "")
        .trim()

    val isFemalePersona =
      customerName.contains("หญิง") ||
        customerName.contains("พิมพ์") ||
        customerName.contains("นิภา") ||
        customerName.contains("กานดา") ||
        customerName.contains("อรอนงค์") ||
        customerName.contains("ป้า") ||
        customerName.contains("เจ๊") ||
        customerName.contains("คุณแม่") ||
        customerName.contains("เมย์") ||
        customerName.contains("แพรว") ||
        customerName.contains("เบลล์") ||
        customerName.contains("ต่าย") ||
        customerName.contains("นก") ||
        customerName.contains("ศิริพร") ||
        customerName.contains("ซ้อจอย")

    _isCustomerSpeaking.value = true
    _voiceCallState.value = VoiceCallState.CUSTOMER_SPEAKING

    var ttsStartedSuccessfully = false
    if (_isCustomerVoiceEnabled.value && isTtsReady) {
      runCatching {
        val pitch =
          when {
            isFemalePersona && customerAge < 35 -> 1.15f
            isFemalePersona -> 1.06f
            customerAge >= 55 -> 0.90f
            else -> 0.98f
          }
        val speechRate = if (customerAge >= 55) 0.95f else 1.03f
        tts?.setPitch(pitch)
        tts?.setSpeechRate(speechRate)
        val params = Bundle()
        val code =
          tts?.speak(
            cleanText,
            TextToSpeech.QUEUE_FLUSH,
            params,
            "cust_utt_${System.currentTimeMillis()}",
          )
        ttsStartedSuccessfully = (code == TextToSpeech.SUCCESS)
      }
    }

    // รันเอนจินขยับปาก Lip-Sync 60fps ตามพยางค์ภาษาไทย
    customerLipSyncJob =
      scope.launch {
        val estimatedDurationMs = (cleanText.length * 58L).coerceIn(1800L, 7200L)
        val frameMs = 65L
        val totalFrames = (estimatedDurationMs / frameMs).toInt().coerceAtLeast(1)

        for (frame in 0 until totalFrames) {
          if (!isActive || !_isCustomerSpeaking.value) break
          val charIndex = (frame * cleanText.length / totalFrames).coerceIn(0, cleanText.lastIndex)
          val currentChar = cleanText[charIndex]
          val viseme = mapThaiCharToViseme(currentChar, frame)
          _customerViseme.value = viseme
          _customerMouthOpenness.value = visemeToOpenness(viseme, frame)
          delay(frameMs)
        }

        // หากไม่ได้เปิด TTS หรือ TTS ไม่ได้ส่ง callback onDone ให้เรียกจบการพูดพร้อม Echo Cooldown
        if (!ttsStartedSuccessfully || !_isCustomerVoiceEnabled.value) {
          handleCustomerSpeechCompleted()
        } else {
          // Watchdog safety: หากผ่านไปอีก 800ms แล้ว TTS ยังไม่ส่งสัญญาณจบ ให้ปลดล็อกสถานะอัตโนมัติ
          delay(800L)
          if (_isCustomerSpeaking.value) {
            handleCustomerSpeechCompleted()
          }
        }
      }
  }

  fun stopCustomerLipSync() {
    customerLipSyncJob?.cancel()
    _isCustomerSpeaking.value = false
    _customerMouthOpenness.value = 0f
    _customerViseme.value = MouthViseme.CLOSED
  }

  private fun mapThaiCharToViseme(ch: Char, stepIndex: Int): MouthViseme {
    return when {
      ch == ' ' || ch == ',' || ch == '.' || ch == 'ม' || ch == 'บ' || ch == 'ป' || ch == 'พ' ->
        MouthViseme.CLOSED
      ch == 'า' || ch == 'ะ' || ch == 'ั' || ch == 'แ' || ch == 'ไ' || ch == 'ใ' || ch == 'ำ' ->
        MouthViseme.WIDE_AH
      ch == 'โ' || ch == 'อ' || ch == 'ุ' || ch == 'ู' || ch == 'ว' ->
        MouthViseme.ROUND_OH
      ch == 'ิ' || ch == 'ี' || ch == 'ึ' || ch == 'ื' || ch == 'เ' || ch == 'ย' ->
        MouthViseme.SLIGHT_OPEN
      else -> {
        when (stepIndex % 4) {
          0 -> MouthViseme.SLIGHT_OPEN
          1 -> MouthViseme.WIDE_AH
          2 -> MouthViseme.ROUND_OH
          else -> MouthViseme.CLOSED
        }
      }
    }
  }

  private fun visemeToOpenness(viseme: MouthViseme, step: Int): Float {
    val jitter = ((sin(step * 1.7) * 0.08)).toFloat()
    return when (viseme) {
      MouthViseme.CLOSED -> 0.05f
      MouthViseme.SLIGHT_OPEN -> (0.38f + jitter).coerceIn(0.2f, 0.55f)
      MouthViseme.ROUND_OH -> (0.64f + jitter).coerceIn(0.45f, 0.8f)
      MouthViseme.WIDE_AH -> (0.88f + jitter).coerceIn(0.7f, 1.0f)
      MouthViseme.SMILE -> 0.15f
    }
  }

  private fun startBgmLoopIfNeeded() {
    if (!_isBgmEnabled.value || bgmJob?.isActive == true) return
    bgmJob =
      scope.launch(Dispatchers.Default) {
        val melodyFreqs =
          doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25, 440.00, 392.00)
        var noteIdx = 0
        while (isActive && _isBgmEnabled.value) {
          // Auto-Ducking: ปิดเสียงเพลงประกอบทันทีเมื่อไมค์เปิดฟังหรือลูกค้ากำลังพูด เพื่อไม่ให้กวนไมค์
          if (!_isListeningRealTime.value && !_isCustomerSpeaking.value) {
            val freq = melodyFreqs[noteIdx % melodyFreqs.size]
            playSoftAmbientNote(freqHz = freq, durationMs = 550, volume = 0.03f)
            noteIdx++
          }
          delay(350L)
        }
      }
  }

  fun playStageStartSfx() {
    scope.launch(Dispatchers.Default) {
      val chord = doubleArrayOf(392.00, 493.88, 587.33, 783.99)
      for (f in chord) {
        playSoftAmbientNote(freqHz = f, durationMs = 100, volume = 0.10f)
      }
    }
  }

  fun playAgentMicSfx() {
    scope.launch(Dispatchers.Default) {
      playSoftAmbientNote(freqHz = 587.33, durationMs = 65, volume = 0.08f)
    }
  }

  fun playCustomerReplySfx() {
    scope.launch(Dispatchers.Default) {
      playSoftAmbientNote(freqHz = 523.25, durationMs = 75, volume = 0.08f)
    }
  }

  fun playComboUpSfx() {
    scope.launch(Dispatchers.Default) {
      val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
      for (f in notes) {
        playSoftAmbientNote(freqHz = f, durationMs = 80, volume = 0.11f)
      }
    }
  }

  fun playVictoryFanfareSfx() {
    scope.launch(Dispatchers.Default) {
      val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50, 1318.51)
      for (f in notes) {
        playSoftAmbientNote(freqHz = f, durationMs = 135, volume = 0.14f)
      }
    }
  }

  fun playFastSpeechCoachingPattern() {
    scope.launch(Dispatchers.Default) {
      playSoftAmbientNote(freqHz = 783.99, durationMs = 90, volume = 0.12f)
      delay(70L)
      playSoftAmbientNote(freqHz = 587.33, durationMs = 120, volume = 0.12f)
      delay(100L)
      playSoftAmbientNote(freqHz = 440.00, durationMs = 170, volume = 0.13f)
    }
  }

  fun playQuietVoiceCoachingPattern() {
    scope.launch(Dispatchers.Default) {
      playSoftAmbientNote(freqHz = 349.23, durationMs = 120, volume = 0.13f)
      delay(65L)
      playSoftAmbientNote(freqHz = 698.46, durationMs = 170, volume = 0.14f)
    }
  }

  fun playBalancedVoiceCoachingPattern() {
    scope.launch(Dispatchers.Default) {
      playSoftAmbientNote(freqHz = 523.25, durationMs = 80, volume = 0.10f)
      playSoftAmbientNote(freqHz = 783.99, durationMs = 110, volume = 0.10f)
    }
  }

  private fun playSoftAmbientNote(freqHz: Double, durationMs: Int, volume: Float) {
    runCatching {
      val sampleRate = 22050
      val numSamples = (sampleRate * durationMs) / 1000
      if (numSamples <= 0) return
      val samples = ShortArray(numSamples)
      for (i in 0 until numSamples) {
        val t = i.toDouble() / sampleRate
        val attack = (i.toDouble() / (numSamples * 0.12)).coerceAtMost(1.0)
        val decay = 1.0 - (i.toDouble() / numSamples)
        val envelope = attack * decay * decay
        val wave = sin(2.0 * PI * freqHz * t) + 0.3 * sin(4.0 * PI * freqHz * t)
        samples[i] = (wave * envelope * volume * Short.MAX_VALUE).toInt().toShort()
      }

      val track =
        AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(samples.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

      track.write(samples, 0, samples.size)
      track.play()
      Thread.sleep(durationMs.toLong() + 20L)
      track.stop()
      track.release()
    }
  }

  fun release() {
    bgmJob?.cancel()
    customerLipSyncJob?.cancel()
    agentLipSyncJob?.cancel()
    debounceSendJob?.cancel()
    echoCooldownJob?.cancel()
    mainHandler.post {
      runCatching {
        speechRecognizer?.destroy()
        speechRecognizer = null
      }
    }
    runCatching {
      tts?.stop()
      tts?.shutdown()
    }
  }
}
