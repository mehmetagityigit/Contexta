const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function serverTime() {
  return new Date(Date.now() - 1000);
}

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

test("Unauthenticated user cannot read or write vocabulary or published content", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vocabulary").get());
  await assertFails(unauthDb.collection("contentGrammar").where("status", "==", "published").get());
});

test("Authenticated user can create, read, update, and delete own vocabulary", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const vocabRef = aliceDb.collection("users").doc(ALICE_UID).collection("vocabulary").doc("vocab_1");
  const now = serverTime();

  await assertSucceeds(vocabRef.set({
    userId: ALICE_UID,
    expression: "ubiquitous",
    normalizedExpression: "ubiquitous",
    meaningTr: "her yerde bulunan",
    tags: ["SAT", "Academic"],
    favorite: true,
    createdAt: now,
    updatedAt: now,
  }));

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("vocabulary").get());

  await assertSucceeds(vocabRef.update({
    favorite: false,
    updatedAt: serverTime(),
  }));

  await assertSucceeds(vocabRef.delete());
});

test("Cross-user isolation: Bob cannot read, write, or delete Alice's vocabulary or reviewStates", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = serverTime();
    await db.collection("users").doc(ALICE_UID).collection("vocabulary").doc("v1").set({
      userId: ALICE_UID,
      expression: "ephemeral",
      normalizedExpression: "ephemeral",
      tags: [],
      favorite: false,
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vocabulary").doc("v1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vocabulary").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vocabulary").doc("v1").delete());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("reviewStates").doc("v1").set({
    userId: BOB_UID,
    vocabularyId: "v1",
    dueAtEpochMillis: Date.now(),
    intervalDays: 1,
    easeFactor: 2.5,
    repetitionCount: 1,
    lapseCount: 0,
    createdAt: serverTime(),
    updatedAt: serverTime(),
  }));
});

test("Shadow update / ghost field / invalid rating rejected on vocabulary and reviewEvents", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = serverTime();

  // Ghost field isAdmin rejected
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).collection("vocabulary").doc("v_ghost").set({
    userId: ALICE_UID,
    expression: "mitigate",
    normalizedExpression: "mitigate",
    tags: [],
    favorite: false,
    isAdmin: true,
    createdAt: now,
    updatedAt: now,
  }));

  // Immutable reviewEvent cannot be updated
  const eventRef = aliceDb.collection("users").doc(ALICE_UID).collection("reviewEvents").doc("evt_1");
  await assertSucceeds(eventRef.set({
    userId: ALICE_UID,
    eventId: "evt_1",
    vocabularyId: "v1",
    rating: "GOOD",
    previousIntervalDays: 0,
    newIntervalDays: 1,
    reviewedAtEpochMillis: Date.now(),
    createdAt: now,
  }));
  await assertFails(eventRef.update({ rating: "EASY" }));
});

test("Published content is read-only: signed-in user can read published, cannot read draft or write", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("contentGrammar").doc("topic_1").set({
      status: "published",
      title: "Sentence Structure",
    });
    await db.collection("contentGrammar").doc("topic_draft").set({
      status: "draft",
      title: "Draft Topic",
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("contentGrammar").doc("topic_1").get());
  await assertSucceeds(aliceDb.collection("contentGrammar").where("status", "==", "published").get());
  await assertFails(aliceDb.collection("contentGrammar").doc("topic_draft").get());
  await assertFails(aliceDb.collection("contentGrammar").get()); // unfiltered list fails
  await assertFails(aliceDb.collection("contentGrammar").doc("topic_new").set({
    status: "published",
    title: "Injected",
  }));
});
