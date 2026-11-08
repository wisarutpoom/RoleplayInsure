const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_agent_123";
const BOB_UID = "bob_agent_456";
const ADMIN_UID = "admin_wisarut_001";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function createValidAttemptPayload(userId, now = new Date()) {
  return {
    userId,
    agentName: "Agent Alice",
    teamCode: "TEAM_DRAGON",
    stageNumber: 1,
    stageTitle: "ด่านที่ 1: โทรนัดลูกค้าที่บอกปัดว่าไม่ว่าง",
    category: "COLD_CALL",
    overallScore: 84,
    rapportScore: 85,
    objectionScore: 80,
    factFindingScore: 82,
    preApproachScore: 86,
    voiceToneScore: 88,
    voiceFeedback: "น้ำเสียงนุ่มนวล จังหวะการเว้นวรรคดีมาก ไม่เร่งรัดลูกค้า",
    coachSummary: "เปิดบทสนทนาได้อย่างมืออาชีพและรับมือข้อโต้แย้งเรื่องเวลาได้ดี",
    passed: true,
    createdAt: now,
    updatedAt: now,
  };
}

function createValidTelemetryPayload(userId, now = new Date()) {
  return {
    userId,
    agentName: "Agent Alice",
    teamCode: "TEAM_DRAGON",
    visitCount: 3,
    totalAttemptsCount: 2,
    enteredStagesCsv: "1,2,7",
    completedStagesCsv: "1,2",
    enteredStagesCount: 3,
    completedStagesCount: 2,
    bestOverallScore: 88,
    averageOverallScore: 84,
    averageVoiceScore: 86,
    lastActiveStage: 7,
    lastActiveStageTitle: "ด่านที่ 7: นัดพบครั้งแรกที่ร้านกาแฟ",
    createdAt: now,
    updatedAt: now,
  };
}

function createValidStaffPermissionPayload(userId, status = "APPROVED", now = new Date()) {
  return {
    userId,
    staffEmail: "secretary@gmail.com",
    staffName: "คุณแอน (เลขานุการทีม)",
    role: "SECRETARY",
    status,
    canViewVisitsAndStages: true,
    canViewScores: true,
    canExportCsv: true,
    canManageTeam: false,
    approvedByEmail: "Wisarutpoom@gmail.com",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read stage_attempts, agent_telemetry, or staff_permissions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("stage_attempts").get());
  await assertFails(unauthDb.collection("agent_telemetry").get());
  await assertFails(unauthDb.collection("staff_permissions").get());
});

test("Authenticated user: can create valid stage_attempt and agent_telemetry for themselves", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("stage_attempts").doc("attempt_1").set(createValidAttemptPayload(ALICE_UID))
  );
  await assertSucceeds(
    aliceDb.collection("agent_telemetry").doc(ALICE_UID).set(createValidTelemetryPayload(ALICE_UID))
  );
});

test("Non-admin user: cannot self-approve as APPROVED in staff_permissions (Privilege Escalation Guard)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("staff_permissions").doc("sec_1").set(createValidStaffPermissionPayload(ALICE_UID, "APPROVED"))
  );
  // Can request PENDING status
  await assertSucceeds(
    aliceDb.collection("staff_permissions").doc("sec_1").set(createValidStaffPermissionPayload(ALICE_UID, "PENDING"))
  );
});

test("Verified Admin: can create APPROVED staff_permissions for a Secretary via Gmail", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "Wisarutpoom@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    adminDb.collection("staff_permissions").doc("sec_approved").set(createValidStaffPermissionPayload(ADMIN_UID, "APPROVED"))
  );
});

test("Secretary with matching verified Gmail: can read their own staff_permissions record", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("staff_permissions").doc("sec_approved").set(createValidStaffPermissionPayload(ADMIN_UID, "APPROVED"));
  });

  const secretaryDb = testEnv
    .authenticatedContext("sec_uid_999", {
      email: "secretary@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    secretaryDb.collection("staff_permissions").where("staffEmail", "==", "secretary@gmail.com").get()
  );
});

test("Verified Admin: can list and read all agent_telemetry and stage_attempts for Admin Website", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("agent_telemetry").doc(BOB_UID).set(createValidTelemetryPayload(BOB_UID));
    await context.firestore().collection("stage_attempts").doc("bob_attempt").set(createValidAttemptPayload(BOB_UID));
  });

  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "Wisarutpoom@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(adminDb.collection("agent_telemetry").get());
  await assertSucceeds(adminDb.collection("stage_attempts").get());
});

test("Spoofed Admin (email_verified: false): rejected from reading other users' telemetry", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("agent_telemetry").doc(BOB_UID).set(createValidTelemetryPayload(BOB_UID));
  });

  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "Wisarutpoom@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofAdminDb.collection("agent_telemetry").doc(BOB_UID).get());
});
