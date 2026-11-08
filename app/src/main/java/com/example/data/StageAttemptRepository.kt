package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StageAttemptRepository(private val db: FirebaseFirestore) {

  constructor(
    context: Context
  ) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException(
        "User must be signed in with Google before accessing Firestore."
      )
  }

  fun observeUserAttempts(): Flow<List<StageAttempt>> = flow {
    val uid = requireUserId()
    val path = COLLECTION_PATH
    emitAll(
      db
        .collection(COLLECTION_PATH)
        .whereEqualTo("userId", uid)
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            StageAttempt::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  fun observeMyTelemetry(): Flow<AgentTelemetry?> = flow {
    val uid = requireUserId()
    val path = "$TELEMETRY_COLLECTION_PATH/$uid"
    emitAll(
      db
        .collection(TELEMETRY_COLLECTION_PATH)
        .document(uid)
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              AgentTelemetry::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.GET, path)
          }
          throw error
        }
    )
  }

  /**
   * Used by the Admin account (e.g. Wisarutpoom@gmail.com) to observe all agents' telemetry in
   * real-time on the Admin Dashboard. For non-admin users, falls back to observing their own
   * telemetry document via userId filter.
   */
  fun observeAdminTelemetryList(isAdminUser: Boolean): Flow<List<AgentTelemetry>> = flow {
    val uid = requireUserId()
    val path = TELEMETRY_COLLECTION_PATH
    val query =
      if (isAdminUser) {
        db.collection(TELEMETRY_COLLECTION_PATH)
      } else {
        db.collection(TELEMETRY_COLLECTION_PATH).whereEqualTo("userId", uid)
      }
    emitAll(
      query
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            AgentTelemetry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  /**
   * Records a user app visit or stage entry into `/agent_telemetry/{userId}` so the Admin Website
   * and Admin Dashboard can track visit counts, which stages were entered, and scores.
   */
  suspend fun recordTelemetryEvent(
    agentName: String,
    teamCode: String,
    incrementVisit: Boolean = false,
    enteredStageNumber: Int? = null,
    enteredStageTitle: String? = null,
    completedAttempts: List<StageAttempt>? = null,
  ): Result<Unit> {
    val uid =
      try {
        requireUserId()
      } catch (e: Exception) {
        return Result.failure(e)
      }
    val docRef = db.collection(TELEMETRY_COLLECTION_PATH).document(uid)
    return try {
      val existingSnap = docRef.get().await()
      val existing =
        if (existingSnap.exists()) {
          existingSnap.toObject(
            AgentTelemetry::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        } else {
          null
        }

      val currentEntered = existing?.parsedEnteredStages()?.toMutableSet() ?: mutableSetOf()
      if (enteredStageNumber != null && enteredStageNumber in 1..25) {
        currentEntered.add(enteredStageNumber)
      }
      completedAttempts?.forEach { attempt ->
        if (attempt.stageNumber in 1..25) {
          currentEntered.add(attempt.stageNumber)
        }
      }

      val currentCompleted = existing?.parsedCompletedStages()?.toMutableSet() ?: mutableSetOf()
      completedAttempts?.forEach { attempt ->
        if (attempt.passed && attempt.stageNumber in 1..25) {
          currentCompleted.add(attempt.stageNumber)
        }
      }

      val sortedEntered = currentEntered.sorted()
      val sortedCompleted = currentCompleted.sorted()
      val enteredCsv = sortedEntered.joinToString(",").take(200)
      val completedCsv = sortedCompleted.joinToString(",").take(200)

      val newVisitCount =
        if (existing == null) {
          1
        } else if (incrementVisit) {
          (existing.visitCount + 1).coerceAtMost(1_000_000)
        } else {
          existing.visitCount.coerceAtLeast(1)
        }

      val attemptsCount =
        completedAttempts?.size ?: existing?.totalAttemptsCount ?: 0
      val bestScore =
        completedAttempts?.maxOfOrNull { it.overallScore }
          ?: existing?.bestOverallScore
          ?: 0
      val avgScore =
        if (!completedAttempts.isNullOrEmpty()) {
          completedAttempts.map { it.overallScore }.average().toInt().coerceIn(0, 100)
        } else {
          existing?.averageOverallScore ?: 0
        }
      val avgVoice =
        if (!completedAttempts.isNullOrEmpty()) {
          completedAttempts.map { it.voiceToneScore }.average().toInt().coerceIn(0, 100)
        } else {
          existing?.averageVoiceScore ?: 0
        }

      val lastStage =
        enteredStageNumber
          ?: completedAttempts?.maxByOrNull { it.createdAt?.seconds ?: 0L }?.stageNumber
          ?: existing?.lastActiveStage
          ?: 0
      val lastTitle =
        (enteredStageTitle
            ?: completedAttempts?.maxByOrNull { it.createdAt?.seconds ?: 0L }?.stageTitle
            ?: existing?.lastActiveStageTitle
            ?: "เข้าสู่ระบบพร้อมฝึกซ้อม")
          .take(200)

      if (existing == null) {
        val createPayload =
          mapOf(
            "userId" to uid,
            "agentName" to agentName.take(100).ifBlank { "Agent" },
            "teamCode" to teamCode.take(50).ifBlank { "TEAM_ALPHA" },
            "visitCount" to newVisitCount,
            "totalAttemptsCount" to attemptsCount,
            "enteredStagesCsv" to enteredCsv,
            "completedStagesCsv" to completedCsv,
            "enteredStagesCount" to sortedEntered.size.coerceIn(0, 25),
            "completedStagesCount" to sortedCompleted.size.coerceIn(0, 25),
            "bestOverallScore" to bestScore.coerceIn(0, 100),
            "averageOverallScore" to avgScore.coerceIn(0, 100),
            "averageVoiceScore" to avgVoice.coerceIn(0, 100),
            "lastActiveStage" to lastStage.coerceIn(0, 25),
            "lastActiveStageTitle" to lastTitle,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      } else {
        val updatePayload =
          mapOf(
            "agentName" to agentName.take(100).ifBlank { existing.agentName.ifBlank { "Agent" } },
            "teamCode" to teamCode.take(50).ifBlank { existing.teamCode.ifBlank { "TEAM_ALPHA" } },
            "visitCount" to newVisitCount,
            "totalAttemptsCount" to attemptsCount,
            "enteredStagesCsv" to enteredCsv,
            "completedStagesCsv" to completedCsv,
            "enteredStagesCount" to sortedEntered.size.coerceIn(0, 25),
            "completedStagesCount" to sortedCompleted.size.coerceIn(0, 25),
            "bestOverallScore" to bestScore.coerceIn(0, 100),
            "averageOverallScore" to avgScore.coerceIn(0, 100),
            "averageVoiceScore" to avgVoice.coerceIn(0, 100),
            "lastActiveStage" to lastStage.coerceIn(0, 25),
            "lastActiveStageTitle" to lastTitle,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(updatePayload, SetOptions.merge()).await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      Result.failure(e)
    }
  }

  suspend fun createAttempt(
    agentName: String,
    teamCode: String,
    stageNumber: Int,
    stageTitle: String,
    category: String,
    overallScore: Int,
    rapportScore: Int,
    objectionScore: Int,
    factFindingScore: Int,
    preApproachScore: Int,
    voiceToneScore: Int,
    voiceFeedback: String,
    coachSummary: String,
    passed: Boolean,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val collectionRef = db.collection(COLLECTION_PATH)
      val docRef = collectionRef.document()
      val payload =
        mapOf(
          "userId" to uid,
          "agentName" to agentName.take(100).ifBlank { "Agent" },
          "teamCode" to teamCode.take(50).ifBlank { "TEAM_ALPHA" },
          "stageNumber" to stageNumber.coerceIn(1, 25),
          "stageTitle" to stageTitle.take(200).ifBlank { "Stage $stageNumber" },
          "category" to category,
          "overallScore" to overallScore.coerceIn(0, 100),
          "rapportScore" to rapportScore.coerceIn(0, 100),
          "objectionScore" to objectionScore.coerceIn(0, 100),
          "factFindingScore" to factFindingScore.coerceIn(0, 100),
          "preApproachScore" to preApproachScore.coerceIn(0, 100),
          "voiceToneScore" to voiceToneScore.coerceIn(0, 100),
          "voiceFeedback" to voiceFeedback.take(2000),
          "coachSummary" to coachSummary.take(3000),
          "passed" to passed,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, COLLECTION_PATH)
      Result.failure(e)
    }
  }

  suspend fun getAttemptById(attemptId: String): Result<StageAttempt?> {
    val path = "$COLLECTION_PATH/$attemptId"
    return try {
      val snapshot = db.collection(COLLECTION_PATH).document(attemptId).get().await()
      Result.success(
        snapshot.toObject(
          StageAttempt::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
      )
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      Result.failure(e)
    }
  }

  suspend fun deleteAttempt(attemptId: String): Result<Unit> {
    val path = "$COLLECTION_PATH/$attemptId"
    return try {
      db.collection(COLLECTION_PATH).document(attemptId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  /**
   * Used by the Admin account to observe all detailed stage attempts across the team for CSV export
   * and deep analytics. For non-admin users, falls back to observing their own attempts.
   */
  fun observeAllStageAttemptsForAdmin(isAdminUser: Boolean): Flow<List<StageAttempt>> = flow {
    val uid = requireUserId()
    val path = COLLECTION_PATH
    val query =
      if (isAdminUser) {
        db.collection(COLLECTION_PATH)
      } else {
        db.collection(COLLECTION_PATH).whereEqualTo("userId", uid)
      }
    emitAll(
      query
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            StageAttempt::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  /**
   * Observes `/staff_permissions`. Master Admin observes all staff permission records; non-admin
   * users observe records matching their own Gmail (`staffEmail`) or `userId` so Secretaries can
   * see their approved Gmail permissions in real-time.
   */
  fun observeStaffPermissions(
    isAdminUser: Boolean,
    currentUserEmail: String,
  ): Flow<List<StaffPermission>> = flow {
    val uid = requireUserId()
    val path = STAFF_PERMISSIONS_COLLECTION_PATH
    val normalizedEmail = currentUserEmail.trim()
    val query =
      if (isAdminUser) {
        db.collection(STAFF_PERMISSIONS_COLLECTION_PATH)
      } else if (normalizedEmail.isNotBlank()) {
        db.collection(STAFF_PERMISSIONS_COLLECTION_PATH).whereEqualTo("staffEmail", normalizedEmail)
      } else {
        db.collection(STAFF_PERMISSIONS_COLLECTION_PATH).whereEqualTo("userId", uid)
      }
    emitAll(
      query
        .snapshots()
        .map { snapshot ->
          snapshot.toObjects(
            StaffPermission::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        }
        .catch { error ->
          val firestoreError =
            generateSequence(error) { it.cause }
              .filterIsInstance<FirebaseFirestoreException>()
              .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
          }
          throw error
        }
    )
  }

  /**
   * Master Admin adds, updates, or approves/revokes a Secretary or Sub-Admin's Gmail permissions.
   */
  suspend fun saveOrApproveStaffPermission(
    existingPermissionId: String? = null,
    targetUserId: String? = null,
    staffEmail: String,
    staffName: String,
    role: String = "SECRETARY",
    status: String = "APPROVED",
    canViewVisitsAndStages: Boolean = true,
    canViewScores: Boolean = true,
    canExportCsv: Boolean = true,
    canManageTeam: Boolean = false,
    approvedByEmail: String,
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val cleanEmail = staffEmail.trim()
      require(cleanEmail.contains("@") && cleanEmail.length in 5..150) {
        "กรุณาระบุอีเมล Gmail ที่ถูกต้อง"
      }
      val docId =
        existingPermissionId?.takeIf { it.isNotBlank() }
          ?: cleanEmail.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
      val docRef = db.collection(STAFF_PERMISSIONS_COLLECTION_PATH).document(docId)
      val existingSnap = docRef.get().await()

      if (!existingSnap.exists()) {
        val createPayload =
          mapOf(
            "userId" to (targetUserId?.takeIf { it.isNotBlank() } ?: uid),
            "staffEmail" to cleanEmail,
            "staffName" to staffName.trim().take(100).ifBlank { cleanEmail.substringBefore("@") },
            "role" to role,
            "status" to status,
            "canViewVisitsAndStages" to canViewVisitsAndStages,
            "canViewScores" to canViewScores,
            "canExportCsv" to canExportCsv,
            "canManageTeam" to canManageTeam,
            "approvedByEmail" to approvedByEmail.trim().take(150).ifBlank { "Wisarutpoom@gmail.com" },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(createPayload).await()
      } else {
        val existingUserId = existingSnap.getString("userId") ?: uid
        val updatePayload =
          mapOf(
            "userId" to existingUserId,
            "staffEmail" to cleanEmail,
            "staffName" to staffName.trim().take(100).ifBlank { cleanEmail.substringBefore("@") },
            "role" to role,
            "status" to status,
            "canViewVisitsAndStages" to canViewVisitsAndStages,
            "canViewScores" to canViewScores,
            "canExportCsv" to canExportCsv,
            "canManageTeam" to canManageTeam,
            "approvedByEmail" to approvedByEmail.trim().take(150).ifBlank { "Wisarutpoom@gmail.com" },
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(updatePayload, SetOptions.merge()).await()
      }
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, STAFF_PERMISSIONS_COLLECTION_PATH)
      Result.failure(e)
    }
  }

  /**
   * Allows a signed-in Secretary or Staff member to submit a `PENDING` access request with their
   * Gmail so the Master Admin can approve their permissions in 1 click.
   */
  suspend fun requestSecretaryAccess(
    staffEmail: String,
    staffName: String,
    requestedRole: String = "SECRETARY",
  ): Result<String> {
    return try {
      val uid = requireUserId()
      val cleanEmail = staffEmail.trim()
      require(cleanEmail.contains("@") && cleanEmail.length in 5..150) {
        "กรุณาระบุอีเมล Gmail ที่ถูกต้อง"
      }
      val docId = cleanEmail.lowercase().replace(Regex("[^a-z0-9_-]"), "_")
      val docRef = db.collection(STAFF_PERMISSIONS_COLLECTION_PATH).document(docId)
      val payload =
        mapOf(
          "userId" to uid,
          "staffEmail" to cleanEmail,
          "staffName" to staffName.trim().take(100).ifBlank { cleanEmail.substringBefore("@") },
          "role" to requestedRole,
          "status" to "PENDING",
          "canViewVisitsAndStages" to true,
          "canViewScores" to true,
          "canExportCsv" to false,
          "canManageTeam" to false,
          "approvedByEmail" to "รออนุมัติจาก Admin",
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      docRef.set(payload).await()
      Result.success(docRef.id)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, STAFF_PERMISSIONS_COLLECTION_PATH)
      Result.failure(e)
    }
  }

  suspend fun deleteStaffPermission(permissionId: String): Result<Unit> {
    val path = "$STAFF_PERMISSIONS_COLLECTION_PATH/$permissionId"
    return try {
      db.collection(STAFF_PERMISSIONS_COLLECTION_PATH).document(permissionId).delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, path)
      Result.failure(e)
    }
  }

  companion object {
    const val COLLECTION_PATH = "stage_attempts"
    const val TELEMETRY_COLLECTION_PATH = "agent_telemetry"
    const val STAFF_PERMISSIONS_COLLECTION_PATH = "staff_permissions"
  }
}
