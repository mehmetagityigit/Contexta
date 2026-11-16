#!/usr/bin/env node
/**
 * Admin CLI script for upserting validated Contexta curriculum content
 * into Firestore collections (`contentGrammar`, `contentReading`, `contentListening`).
 *
 * SECURITY:
 * - Never embeds service account credentials.
 * - Uses GOOGLE_APPLICATION_CREDENTIALS or Firebase Emulator (`FIRESTORE_EMULATOR_HOST`).
 * - Supports `--dry-run` (default unless `--apply` is passed).
 *
 * Example dry-run:
 *   node tools/seed_firestore_content.mjs
 *
 * Example emulator upsert:
 *   FIRESTORE_EMULATOR_HOST=127.0.0.1:8085 node tools/seed_firestore_content.mjs --apply
 */

const fs = require("node:fs");
const path = require("node:path");
const { execSync } = require("node:child_process");

const CONTENT_DIR = path.resolve(__dirname, "../app/src/main/assets/content");
const apply = process.argv.includes("--apply");

// Always run schema validation first before touching Firestore
execSync("node " + path.resolve(__dirname, "validate_content.mjs"), { stdio: "inherit" });

const grammarPack = JSON.parse(fs.readFileSync(path.join(CONTENT_DIR, "grammar_pack.json"), "utf8"));
const readingPack = JSON.parse(fs.readFileSync(path.join(CONTENT_DIR, "reading_pack.json"), "utf8"));
const listeningPack = JSON.parse(fs.readFileSync(path.join(CONTENT_DIR, "listening_pack.json"), "utf8"));

if (!apply) {
  console.log(
    `[DRY RUN] Would upsert ${grammarPack.topics.length} contentGrammar, ` +
      `${readingPack.passages.length} contentReading, ${listeningPack.lessons.length} contentListening docs.`
  );
  console.log("Pass --apply with GOOGLE_APPLICATION_CREDENTIALS or FIRESTORE_EMULATOR_HOST to execute.");
  process.exit(0);
}

async function runUpsert() {
  const { default: admin } = await import("firebase-admin");
  const { getFirestore } = await import("firebase-admin/firestore");
  const databaseId = process.env.FIRESTORE_DATABASE_ID || "ai-studio-android-contexta-09e7b9fe-5b09-4658-a887-ca9d67b8c68b";
  if (admin.apps.length === 0) {
    admin.initializeApp({
      projectId: process.env.GCP_PROJECT || "demo-no-project",
    });
  }
  const db = getFirestore(admin.app(), databaseId);

  const batch = db.batch();
  for (const topic of grammarPack.topics) {
    batch.set(db.collection("contentGrammar").doc(topic.id), topic, { merge: true });
  }
  for (const passage of readingPack.passages) {
    batch.set(db.collection("contentReading").doc(passage.id), passage, { merge: true });
  }
  for (const lesson of listeningPack.lessons) {
    batch.set(db.collection("contentListening").doc(lesson.id), lesson, { merge: true });
  }
  await batch.commit();
  console.log("Successfully upserted validated curriculum content to Firestore.");
}

runUpsert().catch((err) => {
  console.error("Failed to upsert curriculum content:", err);
  process.exit(1);
});
