package com.example.data

import com.example.audio.VoiceAnalyzerRecorder

/**
 * 100% On-Device Smart Dialogue, Voice Coaching, Badges & Challenge Leaderboard Engine
 * ทำงานฝังในแอพโดยตรง ไม่ต้องพึ่งโควต้า API เล่นฟรีไม่จำกัดจำนวนครั้ง (Unlimited 0.00฿)
 */
object OnDeviceRolePlayAiEngine {

  data class OnDeviceTurnOutput(
    val customerReply: String,
    val coachTip: String,
    val voiceCoachingAlert: String?,
    val isVoiceAlertWarning: Boolean,
  )

  private val empathyKeywords =
    listOf(
      "เข้าใจ",
      "เห็นด้วย",
      "ขอบคุณ",
      "ยินดี",
      "ไม่รบกวน",
      "ชื่นชม",
      "เพื่อน",
      "ขออภัย",
      "ขอโทษ",
      "สบายใจ",
      "ไม่ซื้อไม่เป็นไร",
      "ให้เกียรติ",
      "สุดยอด",
      "วิสัยทัศน์",
    )

  private val appointmentKeywords =
    listOf(
      "นัด",
      "อังคาร",
      "พฤหัส",
      "พุธ",
      "ศุกร์",
      "สัปดาห์หน้า",
      "15 นาที",
      "10 นาที",
      "กาแฟ",
      "เข้าพบ",
      "สวัสดี",
      "สรุปกรมธรรม์",
      "แผ่นเดียว",
    )

  private val factFindingKeywords =
    listOf(
      "ไหม",
      "หรือเปล่า",
      "เท่าไหร่",
      "กี่ปี",
      "ยังไง",
      "วางแผน",
      "กังวล",
      "ช่วง",
      "ครอบครัว",
      "ลูก",
      "ภาษี",
      "สวัสดิการ",
      "หนี้",
      "เกษียณ",
      "ค่าห้อง",
      "โรงพยาบาล",
      "สำรอง",
      "ที่ดิน",
      "มรดก",
    )

  private val preApproachKeywords =
    listOf(
      "หมวด",
      "โครงสร้าง",
      "สุขภาพ",
      "โรคร้ายแรง",
      "บำนาญ",
      "คุ้มครอง",
      "มรดก",
      "ค่ารักษา",
      "ลดหย่อน",
      "ควบการลงทุน",
      "เสาหลัก",
      "ปิรามิด",
      "รั้วป้องกัน",
      "ตู้เอทีเอ็ม",
      "ATM",
      "Keyman",
    )

  private val hardSellKeywords =
    listOf("ซื้อเลย", "เซ็นเลย", "ต้องทำเดี๋ยวนี้", "โอนเงินเลย", "ห้ามปฏิเสธ")

  fun generateCustomerTurnOnDevice(
    stage: RolePlayStage,
    agentText: String,
    agentTurnIndex: Int,
    voiceMetrics: VoiceRecordingMetrics?,
  ): OnDeviceTurnOutput {
    val clean = agentText.trim()
    val hasEmpathy = empathyKeywords.any { clean.contains(it, ignoreCase = true) }
    val hasAppointment = appointmentKeywords.any { clean.contains(it, ignoreCase = true) }
    val hasQuestion =
      factFindingKeywords.any { clean.contains(it, ignoreCase = true) } || clean.contains("?")
    val hasDirection = preApproachKeywords.any { clean.contains(it, ignoreCase = true) }
    val hasHardSell = hardSellKeywords.any { clean.contains(it, ignoreCase = true) }

    // 1. ตรวจจับ Voice Coaching (พูดเร็ว > 165 WPM หรือ พูดเสียงเบา)
    val wpm =
      voiceMetrics?.estimatedWordsPerMinute
        ?: estimateWpmFromText(clean)
    val isTooFast = voiceMetrics?.isTooFast ?: (wpm > 165)
    val isTooQuiet = voiceMetrics?.isTooQuiet ?: (clean.length in 1..14)

    val voiceAlert =
      VoiceAnalyzerRecorder.buildVoiceCoachingFeedbackTip(
        wpm = wpm,
        isTooFast = isTooFast,
        isTooQuiet = isTooQuiet,
      )
    val isWarning = isTooFast || isTooQuiet

    // 2. สร้างบทพูดโต้ตอบสุดฮาตามคาแรคเตอร์ของลูกค้าในแต่ละด่าน
    val customerReply =
      when {
        hasHardSell ->
          "เดี๋ยวๆ ครับคุณตัวแทน! ยังไม่ทันคุยรายละเอียดเลยจะให้เซ็นใบสมัครแล้วเหรอ แบบนี้${stage.customerName.substringBefore(" ")}ตั้งการ์ดสูงกว่าเดิมนะเนี่ย ฮ่าๆ"
        isTooFast && agentTurnIndex == 1 ->
          "โอ้โห หายใจทันไหมครับเนี่ย พูดรัว 5G แข่งกับผมเลย! ลองพูดช้าลงนิดนึงครับ เมื่อกี้คุณบอกว่าเกี่ยวกับเรื่อง ${stage.targetInsuranceDirection.take(35)} ใช่ไหมครับ?"
        isTooQuiet && agentTurnIndex == 1 ->
          "ฮัลโหลๆ เสียงเบาจังเลยครับ เหมือนกระซิบข้างไมค์เลย! พูดดังๆ มั่นใจได้เลยครับ สรุปว่าที่ติดต่อมามีประเด็นไหนน่าสนใจนะ?"
        agentTurnIndex <= 1 -> buildFirstTurnPersonaReply(stage, hasEmpathy, hasQuestion)
        agentTurnIndex == 2 -> buildSecondTurnHiddenFactReply(stage, hasQuestion, hasDirection)
        else -> buildClosingPersonaReply(stage, hasAppointment, hasDirection)
      }

    // 3. สร้างคำแนะนำโค้ชประจำเทิร์น
    val coachTip =
      when {
        isTooFast ->
          "⚡ โค้ชน้ำเสียง: คุณพูดเร็ว ($wpm WPM > 165) ลองทอดจังหวะให้ช้าลงและเว้นวรรคหลังจบประโยค จะทำให้ลูกค้าฟังทันและเพิ่ม Voice Tone Score!"
        isTooQuiet ->
          "🔉 โค้ชน้ำเสียง: เสียงพูดค่อนข้างเบา ลองเปล่งเสียงให้กังวานและเน้นคำสำคัญเพื่อเพิ่มความน่าเชื่อถือ"
        hasEmpathy && (hasQuestion || hasDirection || hasAppointment) ->
          "🌟 ยอดเยี่ยมมาก! คุณเปิดใจด้วยความเข้าใจและเชื่อมเข้าสู่เป้าหมายด่าน (${stage.missionObjective.take(45)}...) ได้ตรงจุด"
        else ->
          "💡 ทิปส์ด่านที่ ${stage.stageNumber}: ${stage.keyTips.getOrElse((agentTurnIndex - 1) % stage.keyTips.size) { stage.missionObjective }}"
      }

    return OnDeviceTurnOutput(
      customerReply = customerReply,
      coachTip = coachTip,
      voiceCoachingAlert = voiceAlert,
      isVoiceAlertWarning = isWarning,
    )
  }

  private fun buildFirstTurnPersonaReply(
    stage: RolePlayStage,
    hasEmpathy: Boolean,
    hasQuestion: Boolean,
  ): String {
    return when (stage.stageNumber) {
      1 ->
        if (hasEmpathy)
          "เออ ชอบๆ คนพูดตรงประเด็นแบบนี้! พอดีปีนี้ผมเพิ่งเลื่อนตำแหน่ง โดนหักภาษีอ่วมเลย ถ้าคุยแค่ 15 นาทีเรื่องลดภาษี... อาทิตย์หน้าวันไหนว่างล่ะ?"
        else
          "ผมมีเวลาอีกแค่ 30 วินาทีนะ! ถ้าจะนัดคุยเรื่องลดหย่อนภาษีสั้นๆ ลองเสนอวันเวลามาเลยครับว่าวันไหนดี?"
      2 ->
        "ตายจริง ชมกันแบบนี้เจ๊ก็เขินแย่สิคะ! ที่จริง 5 เล่มในตู้เซฟเจ๊ก็ไม่เคยเปิดดูเลยว่าเบิกค่าห้อง รพ.เอกชนได้คืนละเท่าไหร่ สรุปให้ดูในแผ่นเดียวได้จริงเหรอคะ?"
      3 ->
        "อ๋อ! ไอ้เอกมันแนะนำมาเองเหรอครับเนี่ย ค่อยโล่งอก นึกว่ามิจฉาชีพ! พอดีผมเพิ่งแต่งงานกู้ซื้อบ้านใหม่ 6 ล้านด้วย ถ้าจิบกาแฟคุยกันสบายๆ ไม่บังคับขายก็พอได้ครับ"
      4 ->
        "ฮ่าๆ รู้ทันอีกว่าส่งเข้า LINE แล้วเมย์ดองแชท! พอดีรายได้ไลฟ์สดเมย์ขึ้นๆ ลงๆ ไม่มีสวัสดิการด้วย ถ้าคุยออกแบบเฉพาะตัวแค่ 15 นาที นัดวันไหนดีคะ?"
      5 ->
        "เออ! ฟังน้ำเสียงลื้อแล้วดูจริงใจกว่าคนเก่าเยอะ! ที่บ้านเฮียมีกรมธรรม์อยู่ 3 เล่มไม่รู้ยังใช้ได้ไหม ถ้าจะเข้ามาเช็กให้ฟรีที่ร้านวัสดุก่อสร้าง จะเข้ามาวันไหนล่ะ?"
      6 ->
        "น้ำเสียงกระชับดีค่ะ ผ่านเกณฑ์ 60 วินาทีของดิฉัน! ช่วงนี้ดิฉันกำลังดูเรื่องโครงสร้างภาษีมรดกและสวัสดิการผู้บริหารอยู่ ให้เลขาลงนัดวันไหนดีคะ?"
      7 ->
        "ฮ่าๆ โอเคครับ เก็บปากกาใส่กระเป๋าแล้วผมค่อยหายเกร็งหน่อย! ช่วงนี้ผมปั่นงานสถาปนิกดึกทุกคืน แอบกลัวเหมือนกันว่าถ้าล้มป่วยนอน รพ. เงินเก็บจะหายหมด"
      8 ->
        "โหยแก พูดแบบนี้เราก็สบายใจ 100% เลย! ความจริงเรามีเงินนอนนิ่งในออมทรัพย์ตั้ง 2 ล้านแน่ะ แถมต้องดูแลแม่วัยเกษียณด้วย แกช่วยดูหน่อยสิว่าควรวางแผนยังไง?"
      9 ->
        "เฮ้ย คุณพูดถูกใจผม! ตัวแทนส่วนใหญ่ชอบมาเถียงว่าประกันกำไรดีกว่าหุ้น ความจริงปีก่อนพอร์ตผมก็แอบดอยไปเหมือนกัน ถ้าใช้ประกันเป็น 'กันชน' ป้องกันพอร์ตแตกต้องดูตรงไหนครับ?"
      10 ->
        "ซ้อจอย: แหม ชมว่าจอยเป็นขุนคลังของบ้านขนาดนี้ ยอมฟังก็ได้ค่ะ! ที่กังวลเพราะเพิ่งมีลูกวัย 1 ขวบ อยากรู้ว่าวางแผนยังไงไม่ให้กระทบสภาพคล่องรายเดือนคะ?"
      11 ->
        "อ้าว! บัตรประกันกลุ่มบริษัทผมได้ค่าห้องแค่คืนละ 1,500 เองเหรอพี่?! ถ้าผมเข้า รพ.เอกชนคืนละ 6,000 ผมต้องควักเนื้อคืนละ 4,500 เลยเหรอเนี่ย?!"
      12 ->
        "จริงของน้อง! เมื่อ 3 ปีก่อนพี่เคยรีบซื้อตัวลดภาษีแล้วหมุนเงินธุรกิจไม่ทัน ต้องเวนคืนขาดทุนยับเลย งั้นถามข้อมูลกระแสเงินสดพี่ก่อนได้เลย 5 นาที!"
      13 ->
        "อ๋อ... ถ้าถามเป็นช่วงตัวเลขเพื่อคำนวณสิทธิ์ลดหย่อนภาษี แพรวพอตอบได้ค่ะ ตอนนี้รายได้แพรวอยู่ช่วงปีละ 1.8 ล้าน และมีผ่อนคอนโดเดือนละ 25,000 บาทค่ะ"
      14 ->
        "พอน้องถามเรื่องหนี้บ้าน 4.5 ล้านกับค่าใช้จ่ายลูกแฝดเดือนละ 60,000 พี่เริ่มคิดหนักเลย... เพราะตอนนี้พี่มีทุนประกันติดตัวอยู่แค่ 5 แสนบาทเองครับ!"
      15 ->
        "อุ๊ย พอพูดถึงโรคร้ายแรงต่ายนึกขึ้นได้เลยค่ะ คุณลุงต่ายเพิ่งรักษามะเร็งหมดไป 2 ล้าน แต่บัตรออฟฟิศต่ายมีวงเงินแค่ 4 หมื่นบาทเอง แบบนี้ต่ายควรอุดรอยรั่วยังไงดีคะ?"
      16 ->
        "ตายแล้ว! พอลองกดเครื่องคิดเลขตามที่น้องบอก ใช้เงินหลังเกษียณเดือนละ 40,000 นาน 25 ปี ต้องมีถึง 12 ล้าน! แต่กองทุน PVD พี่มีแค่ 1.5 ล้านเอง ทำไงดีคะเนี่ย?!"
      17 ->
        "เฮ้ย ลื้อพูดแทงใจดำเฮียเลย! กู้ 15 ล้านมาลงเครื่องจักรลูกชิ้นหมูเด้ง เฮียเอาบ้านที่เมียกับลูกอยู่ไปจำนองค้ำไว้จริงๆ ถ้าเกิดอะไรขึ้นทำยังไงไม่ให้แบงก์ยึดบ้านเมียล่ะ?!"
      18 ->
        "จริงด้วยครับ! หมอมัวแต่ซื้อหุ้นปันผลเก็บให้ลูกสาวไปเรียนอังกฤษ ลืมคิดเลยว่าถ้าหมอป่วยขึ้นมาใครจะออมต่อ ระบบ Payer Benefit ที่บริษัทจ่ายเบี้ยต่อให้เป็นยังไงครับ?"
      19 ->
        "จริงของหนูจ้ะ... ที่ดิน 80 ล้านของป้ามีแต่แปลงใหญ่ แบ่งให้ลูก 3 คนยากมาก แถมป้ายังไม่ได้เตรียมเงินสดไว้ให้ลูกจ่ายค่าโอนกับภาษีมรดกเลย ควรวางแผนยังไงดีจ๊ะ?"
      20 ->
        "จริงด้วยครับ! ถ้าผมเอาเงินไปลงออมทรัพย์หมด แล้วเกิดป่วยผ่าตัดครั้งเดียว เงินเก็บ 3 แสนผมหายวับแน่ๆ งั้นผมควรล็อกหมวดสุขภาพเหมาจ่ายกับโรคร้ายแรงก่อนใช่ไหมครับ?"
      21 ->
        "เห็นภาพเลยครับ! ค่ารักษาบริษัทพี่จ่ายให้ครบก็จริง แต่หนี้บ้าน 3.5 ล้านกับค่าเทอมลูก 2 คนรวม 6 ล้าน ยังมีทุนประกันแค่ 3 แสน งั้นต้องเน้นหมวดคุ้มครองชีวิตสูงๆ ก่อนเลยใช่ไหมครับ?"
      22 ->
        "อ้าว! สิทธิ์ประกันบำนาญ 2 แสนบาทหลัง พี่ได้เงินคืนภาษีฐาน 25% ถึงปีละ 50,000 บาทเลยเหรอคะ?! เท่ากับได้ทั้งลดภาษีและล็อกเงินบำนาญตอนเกษียณเลยสิคะ!"
      23 ->
        "โห... คำว่า 'พ่อคือตู้ ATM ของลูก' สะเทือนใจพี่มากครับ! ถ้าตู้ ATM พัง ลูกอดเรียนต่อแน่นอน งั้นงบ 2,500 บาทนี้ พี่จัดมาคุ้มครองชีวิตพี่ที่เป็นเสาหลักก่อนดีกว่าครับ!"
      24 ->
        "ตรงสเปกเลยครับ! ถ้าเป็นโครงสร้างประกันชีวิตควบการลงทุน (Unit Linked) ที่ปรับเพิ่ม-ลดความคุ้มครองตามช่วงวัยได้และเติบโตไปกับกองทุนรวม แบบนี้ตอบโจทย์ผมสุดๆ ครับ!"
      25 ->
        "โฮ่ๆ! ลื้อจับประเด็นได้เฉียบขาดสมเป็นมืออาชีพ! อาณาจักรของอั๊วมีทั้งหนี้ธุรกิจ สุขภาพระดับ VIP และมรดกลูก 2 คน ไหนลองฟันธง 3 เสาหลักให้อั๊วฟังอีกทีซิ!"
      else ->
        if (hasEmpathy || hasQuestion) {
          "ที่คุณพูดมาน่าคิดมากเลยครับ ความจริงแล้ว ${stage.hiddenFinancialFact} แล้วแบบนี้ผมควรเริ่มวางโครงสร้างจากจุดไหนก่อนดีครับ?"
        } else {
          "อืม... ประเด็นนี้ก็น่าสนใจนะครับ แต่สำหรับคนที่เป็น${stage.customerOccupation}อย่างผม คุณคิดว่าความเสี่ยงเรื่องไหนสำคัญที่สุดครับ?"
        }
    }
  }

  private fun buildSecondTurnHiddenFactReply(
    stage: RolePlayStage,
    hasQuestion: Boolean,
    hasDirection: Boolean,
  ): String {
    return when (stage.category) {
      StageCategory.COLD_CALL ->
        "ตกลงตามนี้เลย! ชอบน้ำเสียงและวิธีคุยของคุณ${stage.customerName.substringBefore(" ")}ฟังแล้วไม่กดดันเลย งั้นล็อกคิวนัดเจอกันตามที่คุณเสนอมาได้เลยครับ เตรียมข้อมูลเรื่อง ${stage.targetInsuranceDirection} มาคุยกันนะ!"
      StageCategory.FIRST_MEET ->
        "พอได้ฟังมุมมองแบบนี้ผมเปิดใจ 100% เลยครับ สิ่งที่ห่วงลึกๆ คือ ${stage.hiddenFinancialFact} ถ้าเราจะวางโครงสร้าง ${stage.targetInsuranceDirection} ต้องเริ่มยังไงครับ?"
      StageCategory.FACT_FINDING ->
        "ยอมบอกความลับให้หมดเปลือกเลยครับ! จริงๆ แล้ว ${stage.hiddenFinancialFact} พอเห็นตัวเลขแบบนี้แล้ว ถ้าจัดเข้าหมวด ${stage.targetInsuranceDirection} จะช่วยอุดรอยรั่วได้ไหมครับ?"
      StageCategory.NEEDS_ANALYSIS ->
        if (hasDirection)
          "เห็นภาพชัดแจ๋วเลยครับ! การจัดลำดับความสำคัญเข้าหมวด ${stage.targetInsuranceDirection} ตอบโจทย์เรื่อง ${stage.hiddenFinancialFact} ได้ตรงจุดจริงๆ ไม่ต้องหว่านซื้อมั่วเลย!"
        else
          "แล้วจากข้อมูลที่ว่า ${stage.hiddenFinancialFact} ถ้าสรุปให้ตรงจุดที่สุด ผมควรเลือกวางโครงสร้างประกันหมวดไหนเป็นอันดับแรกครับ?"
    }
  }

  private fun buildClosingPersonaReply(
    stage: RolePlayStage,
    hasAppointment: Boolean,
    hasDirection: Boolean,
  ): String {
    return if (hasAppointment || hasDirection) {
      "ประทับใจมากครับ! คุณเป็นตัวแทนที่ฟังลูกค้าจริงจัง น้ำเสียงน่าเชื่อถือ และชี้เป้าหมวด ${stage.targetInsuranceDirection} ได้เคลียร์มาก กดปุ่ม 'ประเมินผล' ด้านบนเพื่อรับคะแนนและปลดล็อกเหรียญรางวัลได้เลย!"
    } else {
      "โอเคเลยครับ ตอนนี้ผมเข้าใจภาพรวมของ ${stage.targetInsuranceDirection} ชัดเจนขึ้นมาก ลองกดปุ่ม 'ประเมินผล' เพื่อดูคะแนน 5 มิติของคุณได้เลยครับ!"
    }
  }

  fun evaluateSessionOnDevice(
    stage: RolePlayStage,
    chatTurns: List<ChatTurn>,
    lastVoiceMetrics: VoiceRecordingMetrics?,
  ): EvaluationResult {
    val agentTurns = chatTurns.filter { it.isAgent }
    val allAgentText = agentTurns.joinToString(" ") { it.text }
    val turnCount = agentTurns.size

    val empathyMatches = empathyKeywords.count { allAgentText.contains(it, ignoreCase = true) }
    val questionMatches =
      factFindingKeywords.count { allAgentText.contains(it, ignoreCase = true) } +
        allAgentText.count { it == '?' }
    val directionMatches = preApproachKeywords.count { allAgentText.contains(it, ignoreCase = true) }
    val appointmentMatches =
      appointmentKeywords.count { allAgentText.contains(it, ignoreCase = true) }
    val hardSellPenalty =
      if (hardSellKeywords.any { allAgentText.contains(it, ignoreCase = true) }) 15 else 0

    val baseBonus = (turnCount * 6).coerceAtMost(18)

    val rapportScore =
      (74 + empathyMatches * 5 + baseBonus - hardSellPenalty).coerceIn(62, 98)
    val objectionScore =
      (72 + (empathyMatches + appointmentMatches) * 4 + baseBonus - hardSellPenalty).coerceIn(60, 97)
    val factFindingScore =
      (70 + questionMatches * 5 + baseBonus).coerceIn(60, 98)
    val preApproachScore =
      (73 + directionMatches * 5 + appointmentMatches * 3 + baseBonus).coerceIn(62, 99)

    val wpm = lastVoiceMetrics?.estimatedWordsPerMinute ?: estimateWpmFromText(allAgentText)
    val isTooFast = lastVoiceMetrics?.isTooFast ?: (wpm > 165)
    val isTooQuiet = lastVoiceMetrics?.isTooQuiet ?: false

    val voiceToneScore =
      when {
        isTooFast && isTooQuiet -> 68
        isTooFast -> 74
        isTooQuiet -> 75
        else -> (lastVoiceMetrics?.confidenceIndex ?: 88).coerceIn(80, 97)
      }

    val overallScore =
      ((rapportScore + objectionScore + factFindingScore + preApproachScore + voiceToneScore) / 5)
        .coerceIn(0, 100)

    val voiceFeedback =
      VoiceAnalyzerRecorder.buildVoiceCoachingFeedbackTip(
        wpm = wpm,
        isTooFast = isTooFast,
        isTooQuiet = isTooQuiet,
      )

    val coachSummary =
      "ประเมินผลด้วย AI ฝังในแอพ (Unlimited 0.00฿): คุณรับมือกับ ${stage.customerName} ได้คะแนนรวม $overallScore/100 สามารถเชื่อมโยงสู่ ${stage.targetInsuranceDirection} ได้อย่างเป็นธรรมชาติ"

    val strengths = buildList {
      add("สร้างความเป็นกันเองและรับฟัง ${stage.customerName} โดยไม่โต้เถียง")
      if (questionMatches > 0 || directionMatches > 0) {
        add("ใช้คำสำคัญในการค้นหาความต้องการและชี้เป้าหมวดความคุ้มครองได้ตรงประเด็น")
      }
      if (!isTooFast && !isTooQuiet) {
        add("ควบคุมจังหวะการพูด ($wpm WPM) และความดังของน้ำเสียงได้สมดุลน่าเชื่อถือ")
      }
    }

    val improvements = buildList {
      if (isTooFast) {
        add("ลดความเร็วในการพูดให้ต่ำกว่า 165 คำ/นาที (ปัจจุบัน $wpm WPM) และเว้นจังหวะท้ายประโยค")
      }
      if (isTooQuiet) {
        add("เพิ่มพลังเสียงพูดจากกระบังลมให้ดังฟังชัดขึ้น เพื่อสร้างความมั่นใจให้ลูกค้า")
      }
      if (turnCount < 2) {
        add("ลองพูดคุยโต้ตอบอย่างน้อย 2-3 ประโยคเพื่อเก็บข้อมูลลึกขึ้นก่อนกดประเมินผล")
      } else {
        add("ฝึกสรุปทวนตัวเลขสำคัญของลูกค้า (${stage.hiddenFinancialFact.take(35)}...) ก่อนชี้เป้าหมวดประกัน")
      }
    }

    return EvaluationResult(
      overallScore = overallScore,
      rapportScore = rapportScore,
      objectionScore = objectionScore,
      factFindingScore = factFindingScore,
      preApproachScore = preApproachScore,
      voiceToneScore = voiceToneScore,
      voiceFeedback = voiceFeedback,
      coachSummary = coachSummary,
      strengths = strengths,
      improvements = improvements,
      recommendedDirectionCheck = "ชี้เป้าตรงตามเป้าหมาย: ${stage.targetInsuranceDirection}",
      passed = overallScore >= 70,
    )
  }

  private fun estimateWpmFromText(text: String): Int {
    return when {
      text.length > 140 -> 172 // จำลองกรณีพิมพ์/พูดรัวประโยคยาวมากเกิน 165 WPM
      text.length in 1..15 -> 115
      else -> 132
    }
  }

  /**
   * ระบบปลดล็อกเหรียญตราและฉายาสุดฮา 18 รายการ จากการวิเคราะห์พฤติกรรมและคะแนนของผู้เล่น
   */
  fun evaluatePlayerBadges(
    attempts: List<StageAttempt>,
    lastVoiceMetrics: VoiceRecordingMetrics?,
  ): List<PlayerBadge> {
    val totalAttempts = attempts.size
    val passedStages = attempts.filter { it.passed }.map { it.stageNumber }.toSet()
    val bestOverall = attempts.maxOfOrNull { it.overallScore } ?: 0
    val bestVoice = attempts.maxOfOrNull { it.voiceToneScore } ?: 0
    val bestRapport = attempts.maxOfOrNull { it.rapportScore } ?: 0
    val bestFactFinding = attempts.maxOfOrNull { it.factFindingScore } ?: 0
    val bestPreApproach = attempts.maxOfOrNull { it.preApproachScore } ?: 0
    val triggeredFastSpeech =
      (lastVoiceMetrics?.isTooFast == true) ||
        attempts.any { it.voiceFeedback.contains("พูดเร็ว") || it.voiceFeedback.contains("165") }
    val triggeredQuietSpeech =
      (lastVoiceMetrics?.isTooQuiet == true) ||
        attempts.any { it.voiceFeedback.contains("เสียงเบา") }

    return listOf(
      PlayerBadge(
        id = "badge_first_blood",
        emoji = "🐣",
        title = "เปิดซิงยุทธภพ",
        funnyRankName = "จอมยุทธ์ฝึกหัดหัดถือแฟ้ม",
        conditionDescription = "ประเดิมฝึกซ้อมจบครบ 1 ครั้งในด่านใดก็ได้",
        unlocked = totalAttempts >= 1,
        progressText = "${totalAttempts.coerceAtMost(1)}/1 ครั้ง",
      ),
      PlayerBadge(
        id = "badge_rap_god",
        emoji = "⚡",
        title = "แร็ปเปอร์ขายประกัน 5G",
        funnyRankName = "จรวดทางเรียบ 170 คำต่อนาที",
        conditionDescription = "พูดเร็วทะลุ 165 WPM จนระบบ Voice Coaching ต้องส่งเสียงเบรก!",
        unlocked = triggeredFastSpeech,
        progressText = if (triggeredFastSpeech) "ปลดล็อกแล้ว!" else "ลองพูดเร็ว > 165 WPM",
      ),
      PlayerBadge(
        id = "badge_whisper_charm",
        emoji = "🤫",
        title = "กระซิบรักข้างใบหู",
        funnyRankName = "นักขายสาย ASMR ง่วงนอน",
        conditionDescription = "พูดเสียงเบาจนระบบ Voice Coaching แจ้งเตือนให้เพิ่มพลังเสียง",
        unlocked = triggeredQuietSpeech,
        progressText = if (triggeredQuietSpeech) "ปลดล็อกแล้ว!" else "ลองทดสอบโหมดเสียงเบา",
      ),
      PlayerBadge(
        id = "badge_golden_voice",
        emoji = "🎙️",
        title = "นักพากย์เสียงทองคำ",
        funnyRankName = "หล่อ/สวยด้วยเสียงระดับวิทยุ",
        conditionDescription = "ทำคะแนนน้ำเสียง (Voice Tone Score) ได้ตั้งแต่ 88 คะแนนขึ้นไป",
        unlocked = bestVoice >= 88,
        progressText = "$bestVoice/88 คะแนน",
      ),
      PlayerBadge(
        id = "badge_botox_slayer",
        emoji = "💉",
        title = "ผู้จัดระเบียบตู้เซฟเจ๊พิมพ์",
        funnyRankName = "เลขาฯ ส่วนตัวคลินิกพันล้าน",
        conditionDescription = "ผ่านด่านที่ 2 (เจ๊พิมพ์ โบท็อกซ์ทองคำ)",
        unlocked = 2 in passedStages,
        progressText = if (2 in passedStages) "ผ่านแล้ว!" else "ชนะด่านที่ 2",
      ),
      PlayerBadge(
        id = "badge_angry_boss_tamer",
        emoji = "🧱",
        title = "คนเหล็กสยบเฮียวิชัย",
        funnyRankName = "ผู้รอดชีวิตจากเสียง 120 เดซิเบล",
        conditionDescription = "ผ่านด่านที่ 5 (เฮียวิชัย หินคลุกเหล็กเส้น) ได้สำเร็จ",
        unlocked = 5 in passedStages,
        progressText = if (5 in passedStages) "ผ่านแล้ว!" else "ชนะด่านที่ 5",
      ),
      PlayerBadge(
        id = "badge_cfo_sniper",
        emoji = "⏱️",
        title = "สไนเปอร์ 60 วินาที",
        funnyRankName = "มือปราบมาดาม CFO หญิงเหล็ก",
        conditionDescription = "ผ่านด่านที่ 6 (มาดามศิริพร เครื่องคิดเลขเดินได้)",
        unlocked = 6 in passedStages,
        progressText = if (6 in passedStages) "ผ่านแล้ว!" else "ชนะด่านที่ 6",
      ),
      PlayerBadge(
        id = "badge_stock_whisperer",
        emoji = "📈",
        title = "เซียนปราบเซียนหุ้น",
        funnyRankName = "กูรูผู้กอบกู้พอร์ตดอย",
        conditionDescription = "ผ่านด่านที่ 9 (โค้ชเคน กราฟเขียวแอบดอย)",
        unlocked = 9 in passedStages,
        progressText = if (9 in passedStages) "ผ่านแล้ว!" else "ชนะด่านที่ 9",
      ),
      PlayerBadge(
        id = "badge_family_minister",
        emoji = "🍼",
        title = "ทูตสันติภาพรมว.คลัง",
        funnyRankName = "ขวัญใจซ้อจอยคุมกระเป๋าตังค์",
        conditionDescription = "ผ่านด่านที่ 10 (พี่ต้น & ซ้อจอย รมว.คลัง)",
        unlocked = 10 in passedStages,
        progressText = if (10 in passedStages) "ผ่านแล้ว!" else "ชนะด่านที่ 10",
      ),
      PlayerBadge(
        id = "badge_detective_conan",
        emoji = "🕵️",
        title = "โคนันนักสืบกระเป๋าตังค์",
        funnyRankName = "สายลับล้วงความลับเงินล้าน",
        conditionDescription = "ทำคะแนน Fact-Finding ได้ตั้งแต่ 85 คะแนนขึ้นไป",
        unlocked = bestFactFinding >= 85,
        progressText = "$bestFactFinding/85 คะแนน",
      ),
      PlayerBadge(
        id = "badge_fortune_teller",
        emoji = "🔮",
        title = "หมอดูชี้เป้าแม่นยังกะตาเห็น",
        funnyRankName = "ปรมาจารย์ชี้เป้าหมวดประกัน",
        conditionDescription = "ทำคะแนน Pre-Approach ชี้เป้าหมวดประกันได้ 85 คะแนนขึ้นไป",
        unlocked = bestPreApproach >= 85,
        progressText = "$bestPreApproach/85 คะแนน",
      ),
      PlayerBadge(
        id = "badge_monk_empathy",
        emoji = "🧘",
        title = "นะเมตตามหานิยม",
        funnyRankName = "ยิ้มสยามละลายใจลูกค้า",
        conditionDescription = "ทำคะแนน Rapport & ความไว้วางใจได้ 88 คะแนนขึ้นไป",
        unlocked = bestRapport >= 88,
        progressText = "$bestRapport/88 คะแนน",
      ),
      PlayerBadge(
        id = "badge_grinder_5",
        emoji = "🔥",
        title = "นักซ้อมสายถึก คึกทุกด่าน",
        funnyRankName = "ขยันซ้อมไม่ยอมพักไมค์",
        conditionDescription = "สะสมจำนวนครั้งการฝึกซ้อมครบ 5 ครั้งขึ้นไป",
        unlocked = totalAttempts >= 5,
        progressText = "${totalAttempts.coerceAtMost(5)}/5 ครั้ง",
      ),
      PlayerBadge(
        id = "badge_grinder_15",
        emoji = "💪",
        title = "ตำนานร้อยสนาม ไม่ถามสุขภาพสักคำ",
        funnyRankName = "เทพเจ้าแห่งความขยันประจำสาขา",
        conditionDescription = "สะสมจำนวนครั้งการฝึกซ้อมครบ 15 ครั้งขึ้นไป",
        unlocked = totalAttempts >= 15,
        progressText = "${totalAttempts.coerceAtMost(15)}/15 ครั้ง",
      ),
      PlayerBadge(
        id = "badge_cold_call_master",
        emoji = "☎️",
        title = "ราชาสายด่วน ห้ามวางสายนะจ๊ะ",
        funnyRankName = "เจ้าพ่อ Cold Call ปิดนัด 100%",
        conditionDescription = "ผ่านด่านหมวดที่ 1 (ด่าน 1-6) ครบทุกด่าน",
        unlocked = (1..6).all { it in passedStages },
        progressText = "${(1..6).count { it in passedStages }}/6 ด่าน",
      ),
      PlayerBadge(
        id = "badge_high_scorer_90",
        emoji = "👑",
        title = "คะแนนทะลุฟ้า ท้าชิงแชมป์",
        funnyRankName = "อัจฉริยะนักขายระดับ S-Class",
        conditionDescription = "ทำคะแนนรวม (Overall Score) ในด่านใดก็ได้ตั้งแต่ 90 คะแนนขึ้นไป",
        unlocked = bestOverall >= 90,
        progressText = "$bestOverall/90 คะแนน",
      ),
      PlayerBadge(
        id = "badge_dragon_slayer",
        emoji = "🐉",
        title = "ผู้พิชิตเจ้าสัวมังกรทอง",
        funnyRankName = "ที่ปรึกษาคู่ใจมหาเศรษฐีพันล้าน",
        conditionDescription = "ผ่านด่านบอสใหญ่ ด่านที่ 25 (เจ้าสัวธนินทร์ มังกรทองคำ)",
        unlocked = 25 in passedStages,
        progressText = if (25 in passedStages) "ปราบบอสสำเร็จ!" else "ชนะด่านที่ 25",
      ),
      PlayerBadge(
        id = "badge_mdrt_grandmaster",
        emoji = "💎",
        title = "ปรมาจารย์ยุทธภพ 25 ด่าน",
        funnyRankName = "ตำนาน MDRT สยามอคาเดมี",
        conditionDescription = "พิชิตครบทั้ง 25 ด่านในยุทธภพนักขาย",
        unlocked = passedStages.size >= 25,
        progressText = "${passedStages.size}/25 ด่าน",
      ),
    )
  }

  private data class RivalNpc(
    val id: String,
    val name: String,
    val funnyTitle: String,
    val emoji: String,
    val basePoints: Int,
    val baseBestScore: Int,
    val baseAttempts: Int,
    val stagesCleared: Int,
  )

  private val rivalCompetitors =
    listOf(
      RivalNpc("npc_1", "พี่เอก ซอยอารีย์", "แชมป์เก่าสายพูดหวาน", "🦁", 1890, 95, 28, 22),
      RivalNpc("npc_2", "เจ๊บุ๋ม ทองหล่อ", "นักปิดการขายความเร็วแสง", "🦊", 1740, 93, 25, 20),
      RivalNpc("npc_3", "โค้ชบาส สาทร", "ปรมาจารย์ Fact-Finding", "🦅", 1580, 91, 22, 18),
      RivalNpc("npc_4", "น้องมายด์ รัชดา", "ดาวรุ่งเสียงใสใจเกินร้อย", "🐰", 1390, 89, 19, 16),
      RivalNpc("npc_5", "เฮียตี๋ เยาวราช", "ผู้ปราบเจ้าสัวร้อยล้าน", "🐼", 1210, 87, 16, 14),
      RivalNpc("npc_6", "คุณก้อง สีลม", "สไนเปอร์ Cold Call", "🐯", 980, 85, 13, 11),
      RivalNpc("npc_7", "ป้าหน่อย บางนา", "นักซ้อมสายถึกประจำทีม", "🐨", 790, 83, 10, 9),
      RivalNpc("npc_8", "น้องท็อป ลาดพร้าว", "จอมยุทธ์ไมค์ทองคำ", "🐸", 560, 81, 7, 6),
      RivalNpc("npc_9", "คุณแนน พระราม 9", "มือใหม่ไฟแรงแซงทางโค้ง", "🐱", 320, 78, 4, 4),
    )

  /**
   * จัดอันดับ Top 1-10 สำหรับ "รางวัลคะแนนสูงสุด (High Score Award)" และ "รางวัลจำนวนครั้งสูงสุด (Most Attempts Award)"
   */
  fun buildChallengeLeaderboards(
    currentPlayerName: String,
    currentFunnyTitle: String,
    attempts: List<StageAttempt>,
  ): Pair<List<LeaderboardCompetitor>, List<LeaderboardCompetitor>> {
    val bestByStage =
      attempts.groupBy { it.stageNumber }.mapValues { entry ->
        entry.value.maxOfOrNull { it.overallScore } ?: 0
      }
    val userTotalPoints = bestByStage.values.sum() + (attempts.size * 25)
    val userBestSingle = attempts.maxOfOrNull { it.overallScore } ?: 0
    val userAttemptsCount = attempts.size
    val userClearedCount = bestByStage.count { it.value >= 70 }

    val userEntry =
      LeaderboardCompetitor(
        rank = 1,
        playerId = "current_user",
        playerName = currentPlayerName,
        funnyTitle = currentFunnyTitle,
        avatarEmoji = "👑",
        highScorePoints = userTotalPoints,
        bestSingleScore = userBestSingle,
        totalAttempts = userAttemptsCount,
        stagesCleared = userClearedCount,
        isCurrentUser = true,
      )

    val allEntries =
      rivalCompetitors.map { npc ->
        LeaderboardCompetitor(
          rank = 0,
          playerId = npc.id,
          playerName = npc.name,
          funnyTitle = npc.funnyTitle,
          avatarEmoji = npc.emoji,
          highScorePoints = npc.basePoints,
          bestSingleScore = npc.baseBestScore,
          totalAttempts = npc.baseAttempts,
          stagesCleared = npc.stagesCleared,
          isCurrentUser = false,
        )
      } + userEntry

    val topByHighScore =
      allEntries
        .sortedWith(
          compareByDescending<LeaderboardCompetitor> { it.highScorePoints }
            .thenByDescending { it.bestSingleScore }
        )
        .take(10)
        .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

    val topByAttempts =
      allEntries
        .sortedWith(
          compareByDescending<LeaderboardCompetitor> { it.totalAttempts }
            .thenByDescending { it.highScorePoints }
        )
        .take(10)
        .mapIndexed { idx, item -> item.copy(rank = idx + 1) }

    return topByHighScore to topByAttempts
  }

  /**
   * คำนวณว่าคุณอยู่อันดับที่เท่าไร (Rank 1-10) ในแต่ละด่านครบทั้ง 25 ด่าน
   */
  fun buildStageRanksForAll25Stages(
    attempts: List<StageAttempt>,
  ): List<StageRankSummary> {
    val attemptsByStage = attempts.groupBy { it.stageNumber }
    return RolePlayCurriculum.allStages.map { stage ->
      val stageAttempts = attemptsByStage[stage.stageNumber].orEmpty()
      val myBest = stageAttempts.maxOfOrNull { it.overallScore } ?: 0
      val myCount = stageAttempts.size

      // สร้างคะแนนคู่แข่งทั้ง 9 คนในด่านนั้นๆ เพื่อเปรียบเทียบอันดับจริง 1-10
      val rivalScores =
        rivalCompetitors.mapIndexed { idx, npc ->
          val deterministicScore =
            (npc.baseBestScore - ((stage.stageNumber * 3 + idx * 4) % 24)).coerceIn(64, 96)
          npc.name to deterministicScore
        }

      val rank =
        if (myBest == 0) {
          10
        } else {
          1 + rivalScores.count { it.second > myBest }
        }

      val topRival = rivalScores.maxByOrNull { it.second } ?: ("พี่เอก ซอยอารีย์" to 92)
      val topScore = maxOf(myBest, topRival.second)
      val topHolder = if (myBest >= topRival.second && myBest > 0) "คุณ (แชมป์ด่านนี้! 🏆)" else topRival.first

      StageRankSummary(
        stageNumber = stage.stageNumber,
        stageTitle = stage.title,
        customerName = stage.customerName,
        yourRank = rank,
        yourBestScore = myBest,
        yourAttempts = myCount,
        topScoreOnStage = topScore,
        topHolderName = topHolder,
      )
    }
  }
}
