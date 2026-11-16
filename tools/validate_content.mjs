#!/usr/bin/env node
/**
 * Standalone CLI validator for Contexta curriculum content packs.
 * Checks required metadata, unique IDs, CEFR levels, valid correctOptionId,
 * Turkish explanations, and Command of Evidence paragraph references.
 *
 * Usage: node tools/validate_content.mjs
 */

import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const CONTENT_DIR = path.resolve(__dirname, "../app/src/main/assets/content");
const VALID_LEVELS = new Set(["B1", "B2", "C1", "C2", "B1-B2", "C1-C2"]);
const VALID_STATUSES = new Set(["draft", "published", "archived"]);
const VALID_GENRES = new Set([
  "Natural Sciences",
  "Social Sciences & History",
  "Humanities",
  "Technology & AI",
]);
const VALID_LISTENING_TYPES = new Set(["ACADEMIC_LECTURE", "DIALOGUE", "SHORT_STORY"]);

const errors = [];

function checkQuestions(questions, parentId, validEvidenceIds, requireEvidenceForCommand, seenQuestionIds) {
  if (!Array.isArray(questions) || questions.length === 0) {
    errors.push(`${parentId}: missing questions array`);
    return;
  }
  for (const q of questions) {
    if (!q.id || typeof q.id !== "string") errors.push(`${parentId}: question missing id`);
    if (seenQuestionIds.has(q.id)) errors.push(`${parentId}: duplicate question id '${q.id}'`);
    seenQuestionIds.add(q.id);
    if (!q.prompt || !q.prompt.trim()) errors.push(`${q.id}: empty prompt`);
    if (!q.explanationTr || !q.explanationTr.trim()) errors.push(`${q.id}: empty explanationTr`);
    if (!Array.isArray(q.options) || q.options.length < 2) {
      errors.push(`${q.id}: requires at least 2 options`);
      continue;
    }
    const optIds = new Set();
    for (const opt of q.options) {
      if (!opt.id) errors.push(`${q.id}: option missing id`);
      if (optIds.has(opt.id)) errors.push(`${q.id}: duplicate option id '${opt.id}'`);
      optIds.add(opt.id);
      if (!opt.text || !opt.text.trim()) errors.push(`${q.id}/${opt.id}: empty option text`);
    }
    if (!q.correctOptionId || !optIds.has(q.correctOptionId)) {
      errors.push(`${q.id}: invalid correctOptionId '${q.correctOptionId}'`);
    }
    if (requireEvidenceForCommand && q.questionType === "Command of Evidence" && !q.evidenceRefId) {
      errors.push(`${q.id}: Command of Evidence missing evidenceRefId`);
    }
    if (q.evidenceRefId && validEvidenceIds.size > 0 && !validEvidenceIds.has(q.evidenceRefId)) {
      errors.push(`${q.id}: unknown evidenceRefId '${q.evidenceRefId}'`);
    }
  }
}

// 1. Grammar
const grammarPath = path.join(CONTENT_DIR, "grammar_pack.json");
const grammarPack = JSON.parse(fs.readFileSync(grammarPath, "utf8"));
const gIds = new Set();
const gQIds = new Set();
for (const t of grammarPack.topics || []) {
  if (!t.id || gIds.has(t.id)) errors.push(`grammar: invalid/duplicate id '${t.id}'`);
  gIds.add(t.id);
  if (!VALID_LEVELS.has(t.level)) errors.push(`grammar ${t.id}: invalid level '${t.level}'`);
  if (!VALID_STATUSES.has(t.status)) errors.push(`grammar ${t.id}: invalid status '${t.status}'`);
  if (!t.source || !t.license || !t.reviewedAt) errors.push(`grammar ${t.id}: missing attribution metadata`);
  if (!t.summaryTr || !t.summaryTr.trim()) errors.push(`grammar ${t.id}: missing summaryTr`);
  if (!Array.isArray(t.examples) || t.examples.length === 0) errors.push(`grammar ${t.id}: missing examples`);
  checkQuestions(t.questions, t.id, new Set(), false, gQIds);
}

// 2. Reading
const readingPath = path.join(CONTENT_DIR, "reading_pack.json");
const readingPack = JSON.parse(fs.readFileSync(readingPath, "utf8"));
const rIds = new Set();
const rQIds = new Set();
for (const p of readingPack.passages || []) {
  if (!p.id || rIds.has(p.id)) errors.push(`reading: invalid/duplicate id '${p.id}'`);
  rIds.add(p.id);
  if (!VALID_LEVELS.has(p.level)) errors.push(`reading ${p.id}: invalid level '${p.level}'`);
  if (!VALID_GENRES.has(p.genre)) errors.push(`reading ${p.id}: invalid genre '${p.genre}'`);
  if (!VALID_STATUSES.has(p.status)) errors.push(`reading ${p.id}: invalid status '${p.status}'`);
  if (!p.source || !p.license || !p.reviewedAt) errors.push(`reading ${p.id}: missing attribution metadata`);
  const paraIds = new Set();
  for (const para of p.paragraphs || []) {
    if (!para.id || paraIds.has(para.id)) errors.push(`reading ${p.id}: invalid/duplicate paragraph '${para.id}'`);
    paraIds.add(para.id);
    if (!para.text || !para.text.trim()) errors.push(`reading ${p.id}/${para.id}: empty text`);
  }
  checkQuestions(p.questions, p.id, paraIds, true, rQIds);
}

// 3. Listening
const listeningPath = path.join(CONTENT_DIR, "listening_pack.json");
const listeningPack = JSON.parse(fs.readFileSync(listeningPath, "utf8"));
const lIds = new Set();
const lQIds = new Set();
for (const l of listeningPack.lessons || []) {
  if (!l.id || lIds.has(l.id)) errors.push(`listening: invalid/duplicate id '${l.id}'`);
  lIds.add(l.id);
  if (!VALID_LEVELS.has(l.level)) errors.push(`listening ${l.id}: invalid level '${l.level}'`);
  if (!VALID_LISTENING_TYPES.has(l.contentType)) errors.push(`listening ${l.id}: invalid contentType '${l.contentType}'`);
  if (!VALID_STATUSES.has(l.status)) errors.push(`listening ${l.id}: invalid status '${l.status}'`);
  if (!l.source || !l.license || !l.reviewedAt) errors.push(`listening ${l.id}: missing attribution metadata`);
  if (l.audioMode === "MEDIA3" && !l.audioUri) errors.push(`listening ${l.id}: MEDIA3 missing audioUri`);
  const lineIds = new Set();
  for (const line of l.transcript || []) {
    if (!line.id || lineIds.has(line.id)) errors.push(`listening ${l.id}: invalid/duplicate line '${line.id}'`);
    lineIds.add(line.id);
  }
  checkQuestions(l.questions, l.id, lineIds, false, lQIds);
}

if (errors.length > 0) {
  console.error("Content validation FAILED:\n" + errors.map((e) => " - " + e).join("\n"));
  process.exit(1);
}

console.log(
  `Content validation PASSED: ${grammarPack.topics.length} grammar topics, ` +
    `${readingPack.passages.length} reading passages, ${listeningPack.lessons.length} listening lessons.`
);
