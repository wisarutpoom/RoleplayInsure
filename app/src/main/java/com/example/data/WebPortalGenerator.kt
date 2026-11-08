package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WebPortalGenerator {

  const val SHARED_WEB_APP_URL =
    "https://ais-dev-jdmpizsj2544irqik5aigg-882878485935.asia-southeast1.run.app"
  const val DEFAULT_CUSTOM_DOMAIN = "roleplay.smartnexus.vip"
  const val ROOT_DOMAIN = "smartnexus.vip"
  const val EXISTING_ROOT_IP = "104.18.26.246"
  const val FIREBASE_HOSTING_IP = "199.36.158.100"
  const val NAMESERVER_1 = "ns1.globaldomaingroup.com"
  const val NAMESERVER_2 = "ns2.globaldomaingroup.com"
  const val FIREBASE_PROJECT_ID = "gen-lang-client-0907040926"
  const val FIRESTORE_DATABASE_ID = "ai-studio-android-d6ba137f-ef3e-49ff-875a-52e937311e63"

  fun buildCsvReport(
    telemetryList: List<AgentTelemetry>,
    attemptsList: List<StageAttempt>,
  ): String {
    val sb = StringBuilder()
    // UTF-8 BOM so Excel opens Thai characters cleanly
    sb.append("\uFEFF")
    sb.appendLine(
      "ชื่อตัวแทน,รหัสทีม,จำนวนครั้งที่เข้าแอพ (Visits),จำนวนรอบที่ส่งประเมิน,จำนวนด่านที่เข้าเล่นแล้ว,รายชื่อด่านที่เข้าเล่นแล้ว (1-25),จำนวนด่านที่สอบผ่าน,รายชื่อด่านที่สอบผ่าน,คะแนนสูงสุด,คะแนนเฉลี่ยรวม,คะแนนน้ำเสียงเฉลี่ย,ด่านล่าสุดที่เข้าเล่น,เวลาใช้งานล่าสุด"
    )
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    telemetryList.forEach { t ->
      val dateStr =
        t.updatedAt?.toDate()?.let { sdf.format(it) }
          ?: t.createdAt?.toDate()?.let { sdf.format(it) }
          ?: "-"
      val enteredStr = t.enteredStagesCsv.ifBlank { "-" }
      val completedStr = t.completedStagesCsv.ifBlank { "-" }
      sb.appendLine(
        "\"${t.agentName}\",\"${t.teamCode}\",${t.visitCount},${t.totalAttemptsCount},${t.enteredStagesCount},\"$enteredStr\",${t.completedStagesCount},\"$completedStr\",${t.bestOverallScore},${t.averageOverallScore},${t.averageVoiceScore},\"${t.lastActiveStageTitle}\",\"$dateStr\""
      )
    }

    if (attemptsList.isNotEmpty()) {
      sb.appendLine()
      sb.appendLine("--- ประวัติการประเมินรายด่าน (5 มิติ & วิเคราะห์น้ำเสียง) ---")
      sb.appendLine(
        "ชื่อตัวแทน,รหัสทีม,ด่านที่,ชื่อด่าน,หมวดหมู่,คะแนนรวม,การสร้างความไว้วางใจ (Rapport),การรับมือข้อโต้แย้ง (Objection),การเก็บข้อมูล (Fact-Finding),การวิเคราะห์หน้างาน (Pre-Approach),คะแนนน้ำเสียง (Voice Tone),ผลการประเมิน,วิเคราะห์น้ำเสียง,เวลาที่ประเมิน"
      )
      attemptsList.forEach { a ->
        val dateStr = a.createdAt?.toDate()?.let { sdf.format(it) } ?: sdf.format(Date())
        val passLabel = if (a.passed) "ผ่านเกณฑ์" else "ควรฝึกซ้ำ"
        sb.appendLine(
          "\"${a.agentName}\",\"${a.teamCode}\",${a.stageNumber},\"${a.stageTitle}\",\"${a.category}\",${a.overallScore},${a.rapportScore},${a.objectionScore},${a.factFindingScore},${a.preApproachScore},${a.voiceToneScore},\"$passLabel\",\"${a.voiceFeedback.replace("\"", "'")}\",\"$dateStr\""
        )
      }
    }
    return sb.toString()
  }

  fun buildSmartNexusDnsGuide(customDomain: String): String {
    val subPrefix =
      customDomain.removeSuffix(".$ROOT_DOMAIN").ifBlank { "roleplay" }
    return """
=== ตารางตั้งค่า DNS สำหรับ $customDomain ===
Nameserver ปัจจุบันของคุณ:
• $NAMESERVER_1
• $NAMESERVER_2

[1] ค่าเดิมที่ต้องคงไว้ (ห้ามลบหรือแก้ไข เพื่อไม่ให้กระทบเว็บหลัก):
• Host: @ ($ROOT_DOMAIN)       | Type: A | Value: $EXISTING_ROOT_IP (คงเดิม)
• Host: www (www.$ROOT_DOMAIN) | Type: A | Value: $EXISTING_ROOT_IP (คงเดิม)

[2] ค่าใหม่ที่ต้องกด "เพิ่ม (Add Record)" สำหรับ Subdomain $customDomain:
• Record ที่ 1 (ชี้ Subdomain มายัง Google Firebase Hosting ฟรี + ฟรี SSL):
  - Type: A
  - Host / Name: $subPrefix
  - Value / Points to: $FIREBASE_HOSTING_IP
  - TTL: 3600 (หรือ Automatic)

• Record ที่ 2 (ยืนยันความเป็นเจ้าของโดเมนใน Firebase Console):
  - ไปที่ https://console.firebase.google.com/project/$FIREBASE_PROJECT_ID/hosting/sites
  - กด "Add custom domain" -> กรอก "$customDomain"
  - นำค่า TXT ที่ระบบแจ้ง มาเพิ่มเป็น Type: TXT, Host: $subPrefix
    """.trimIndent()
  }

  fun buildFirebaseHostingCommands(customDomain: String): String {
    return """
# 1. ล็อกอินและเชื่อมต่อโปรเจกต์ Google Firebase ของคุณ (ฟรี 100% แผน Spark)
firebase login
firebase use $FIREBASE_PROJECT_ID

# 2. สร้างโฟลเดอร์ public สำหรับวางเว็บแอพรวม (25 ด่าน + เมนู Admin ในโดเมนเดียว)
mkdir -p public
# นำโค้ด "เว็บแอพรวมทุกฟีเจอร์ (index.html)" ไปบันทึกที่ public/index.html

# 3. สั่ง Deploy ขึ้น Google Firebase Hosting ฟรีทันที
firebase deploy --only hosting --project $FIREBASE_PROJECT_ID

# 4. ผูก Subdomain "$customDomain" ในหน้า Firebase Console:
# เปิด https://console.firebase.google.com/project/$FIREBASE_PROJECT_ID/hosting/sites
# กด "Add custom domain" -> พิมพ์ "$customDomain"
# แล้วเพิ่ม A Record (Host: roleplay -> Value: $FIREBASE_HOSTING_IP) ที่ระบบจัดการ DNS ของ $ROOT_DOMAIN
    """.trimIndent()
  }

  /**
   * Generates a unified single-domain web application (`index.html`) for `roleplay.smartnexus.vip`
   * where both Agents (25 Role-Play Stages) and Admin/Secretaries (Admin Menu via Gmail) access
   * everything from the exact same domain (`https://roleplay.smartnexus.vip`).
   */
  fun buildStandaloneWebPortalHtml(teamCode: String, customDomain: String): String {
    val activeDomain = customDomain.ifBlank { DEFAULT_CUSTOM_DOMAIN }
    return """
<!DOCTYPE html>
<html lang="th" data-theme="dark">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <meta name="apple-mobile-web-app-capable" content="yes" />
  <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
  <title>InsureQuest สยามอคาเดมี ($activeDomain) - 25 ด่านฝึกซ้อม & เมนู Admin ครบในที่เดียว</title>
  <link href="https://fonts.googleapis.com/css2?family=Prompt:wght@500;600;700&family=Sarabun:wght@400;600&display=swap" rel="stylesheet">
  <style>
    :root[data-theme="dark"] { --bg: #090E1A; --card: #131D31; --border: #2B3B55; --gold: #F59E0B; --emerald: #10B981; --cyan: #06B6D4; --text: #F8FAFC; --muted: #94A3B8; }
    :root[data-theme="light"] { --bg: #FAF6EE; --card: #FFFFFF; --border: #E5DEC9; --gold: #B45309; --emerald: #047857; --cyan: #0E7490; --text: #1C1917; --muted: #57534E; }
    * { box-sizing: border-box; transition: background 0.2s, color 0.2s; }
    body { margin: 0; font-family: 'Sarabun', sans-serif; background: var(--bg); color: var(--text); display: flex; flex-direction: column; min-height: 100vh; }
    h1, h2, h3, .badge, .btn, .tab-btn { font-family: 'Prompt', sans-serif; }
    header { padding: 14px 20px; background: var(--card); border-bottom: 2px solid var(--gold); display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
    .badge { display: inline-block; padding: 4px 12px; border-radius: 99px; background: rgba(245,158,11,0.18); color: var(--gold); font-weight: 600; font-size: 12px; }
    .toolbar { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .btn { display: inline-block; background: var(--gold); color: #FFFFFF; padding: 10px 18px; border-radius: 10px; font-weight: 600; text-decoration: none; border: none; cursor: pointer; font-size: 14px; }
    .btn-outline { background: transparent; color: var(--cyan); border: 1.5px solid var(--cyan); }
    .app-frame { flex: 1; width: 100%; border: none; min-height: calc(100vh - 76px); background: var(--bg); }
  </style>
  <script>
    function toggleTheme() {
      const root = document.documentElement;
      const next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
      root.setAttribute('data-theme', next);
      document.getElementById('themeBtn').innerText = next === 'dark' ? '☀️ โหมดสว่าง' : '🌙 โหมดมืด';
    }
  </script>
</head>
<body>
  <header>
    <div>
      <span class="badge">⚜️ $activeDomain • ทีม: $teamCode (iOS / Android / PC)</span>
      <h1 style="margin:4px 0 0;font-size:18px;">InsureQuest: 25 ด่านฝึกซ้อมตัวแทนประกัน & เมนู Admin/เลขาฯ (Gmail)</h1>
    </div>
    <div class="toolbar">
      <button id="themeBtn" class="btn btn-outline" onclick="toggleTheme()">☀️ โหมดสว่าง</button>
      <a class="btn" href="$SHARED_WEB_APP_URL" target="_blank">🚀 เปิดจอเต็ม (Full Screen)</a>
    </div>
  </header>
  <iframe class="app-frame" src="$SHARED_WEB_APP_URL" allow="microphone; clipboard-write" title="InsureQuest Role-Play & Admin Web App"></iframe>
</body>
</html>
    """.trimIndent()
  }

  fun buildAdminAnalyticsWebHtml(teamCode: String, customDomain: String): String {
    return buildStandaloneWebPortalHtml(teamCode, customDomain)
  }
}
