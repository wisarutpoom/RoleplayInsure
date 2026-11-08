package com.example.data

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StageAttemptRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createAttempt_validPayload_createsDocumentAndEmitsInFlow() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val repository = StageAttemptRepository(firestore)

    val createResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createAttempt(
          agentName = "Agent Alice",
          teamCode = "TEAM_ALPHA",
          stageNumber = 1,
          stageTitle = "ด่านที่ 1: โทรนัดลูกค้าที่บอกปัดว่าไม่ว่าง",
          category = "COLD_CALL",
          overallScore = 85,
          rapportScore = 88,
          objectionScore = 82,
          factFindingScore = 80,
          preApproachScore = 84,
          voiceToneScore = 90,
          voiceFeedback = "น้ำเสียงมั่นใจและอบอุ่น",
          coachSummary = "ทำนัดหมายสำเร็จด้วยเทคนิคตัวเลือกเวลา",
          passed = true,
        )
      }

    assertTrue(
      "Expected createAttempt to succeed: ${createResult.exceptionOrNull()}",
      createResult.isSuccess,
    )
    val attemptId = createResult.getOrThrow()
    assertTrue(attemptId.isNotEmpty())

    val emitted =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeUserAttempts().first { list -> list.any { it.id == attemptId } }
      }
    assertTrue(emitted.any { it.id == attemptId && it.overallScore == 85 })
  }

  @Test
  fun recordTelemetryEvent_tracksVisitCountAndEnteredStages() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repository = StageAttemptRepository(firestore)

    // Ensure clean state for Alice's telemetry document across repeated emulator test runs
    runCatching {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(StageAttemptRepository.TELEMETRY_COLLECTION_PATH)
          .document(uid)
          .delete()
          .await()
      }
    }

    val firstVisit =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.recordTelemetryEvent(
          agentName = "Agent Alice",
          teamCode = "TEAM_ALPHA",
          incrementVisit = true,
          enteredStageNumber = 1,
          enteredStageTitle = "ด่านที่ 1: โทรนัดลูกค้าที่บอกปัดว่าไม่ว่าง",
        )
      }
    assertTrue(
      "Expected first telemetry write to succeed: ${firstVisit.exceptionOrNull()}",
      firstVisit.isSuccess,
    )

    val secondStage =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.recordTelemetryEvent(
          agentName = "Agent Alice",
          teamCode = "TEAM_ALPHA",
          incrementVisit = true,
          enteredStageNumber = 7,
          enteredStageTitle = "ด่านที่ 7: นัดพบครั้งแรกที่ร้านกาแฟ",
        )
      }
    assertTrue(
      "Expected telemetry update to succeed: ${secondStage.exceptionOrNull()}",
      secondStage.isSuccess,
    )

    val telemetry =
      withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeMyTelemetry().first { it != null && it.visitCount >= 2 }
      }
    assertNotNull(telemetry)
    assertTrue((telemetry?.visitCount ?: 0) >= 2)
    assertTrue(telemetry?.parsedEnteredStages()?.containsAll(listOf(1, 7)) == true)
  }

  @Test
  fun getAttemptById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
    signInTestUser(ALICE_EMAIL)
    val aliceRepo = StageAttemptRepository(firestore)
    val attemptId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .createAttempt(
            agentName = "Agent Alice",
            teamCode = "TEAM_ALPHA",
            stageNumber = 2,
            stageTitle = "ด่านที่ 2: โทรหาลูกค้าที่บอกว่ามีประกันเยอะแล้ว",
            category = "COLD_CALL",
            overallScore = 78,
            rapportScore = 80,
            objectionScore = 75,
            factFindingScore = 76,
            preApproachScore = 80,
            voiceToneScore = 79,
            voiceFeedback = "จังหวะการพูดชัดเจน",
            coachSummary = "ผ่านเกณฑ์",
            passed = true,
          )
          .getOrThrow()
      }

    signInTestUser(BOB_EMAIL)
    val bobRepo = StageAttemptRepository(firestore)
    val bobGetResult = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getAttemptById(attemptId) }
    assertTrue(bobGetResult.isFailure)
    val firestoreException = bobGetResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  @Test
  fun observeUserAttempts_unauthenticatedUser_fails() = runBlocking {
    auth.signOut()
    val repository = StageAttemptRepository(firestore)

    var caughtException: Throwable? = null
    try {
      withTimeout(FLOW_TIMEOUT_MS) { repository.observeUserAttempts().first() }
    } catch (e: Throwable) {
      caughtException = e
    }
    assertNotNull("Expected unauthenticated observeUserAttempts to throw", caughtException)
  }

  @Test
  fun requestSecretaryAccess_createsPendingAndBlocksSelfApproval() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = StageAttemptRepository(firestore)
    val docId = ALICE_EMAIL.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
    runCatching {
      withTimeout(DEFAULT_TIMEOUT_MS) {
        firestore
          .collection(StageAttemptRepository.STAFF_PERMISSIONS_COLLECTION_PATH)
          .document(docId)
          .delete()
          .await()
      }
    }

    // 1. Alice can submit a PENDING Secretary request for her Gmail
    val pendingResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.requestSecretaryAccess(
          staffEmail = ALICE_EMAIL,
          staffName = "เลขาฯ อลิซ",
          requestedRole = "SECRETARY",
        )
      }
    assertTrue(
      "Expected PENDING secretary request to succeed (`uid=$uid`): ${pendingResult.exceptionOrNull()}",
      pendingResult.isSuccess,
    )

    // 2. Non-admin Alice cannot self-approve as APPROVED (privilege escalation blocked by rules)
    val selfApproveResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.saveOrApproveStaffPermission(
          staffEmail = ALICE_EMAIL,
          staffName = "เลขาฯ อลิซ",
          role = "SUB_ADMIN",
          status = "APPROVED",
          approvedByEmail = ALICE_EMAIL,
        )
      }
    assertTrue("Expected non-admin self-approval to fail", selfApproveResult.isFailure)
    val firestoreException = selfApproveResult.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(firestoreException)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
  }

  private companion object {
    const val ALICE_EMAIL = "alice@insurequest.com"
    const val BOB_EMAIL = "bob@insurequest.com"
    const val DEFAULT_TIMEOUT_MS = 10000L
    const val FLOW_TIMEOUT_MS = 8000L
  }
}
