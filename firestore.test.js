const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_princess_123";
const BOB_UID = "bob_other_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(offsetMs = 5000) {
  return new Date(Date.now() - offsetMs);
}

function validProfilePayload(uid, ts = pastDate()) {
  return {
    userId: uid,
    displayName: "Princess Aria",
    email: "aria@example.com",
    bio: "Writing my secret dreams every night.",
    avatarKey: "princess",
    selectedThemeId: "pink_princess",
    selectedFontId: "quicksand",
    mascotId: "white_cat",
    animationsEnabled: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validEntryPayload(uid, ts = pastDate()) {
  return {
    userId: uid,
    title: "A Beautiful Day",
    content: "Today was really special and filled with pastel skies.",
    mood: "HAPPY",
    moodScore: 9,
    categoryId: "personal",
    dateKey: "2026-11-19",
    entryTimeMillis: Date.now() - 2000,
    tags: ["grateful", "sunset"],
    photoUris: [],
    locationLabel: "Sakura Park",
    stickerIds: ["heart_pink"],
    backgroundStyle: "midnight_pink",
    fontStyle: "quicksand",
    textColorHex: "#FFFFFF",
    textAlign: "START",
    fontSizeSp: 16,
    boldEnabled: false,
    italicEnabled: false,
    underlineEnabled: false,
    highlightEnabled: false,
    favorite: false,
    privateLocked: false,
    deleted: false,
    deletedAtMillis: 0,
    wordCount: 9,
    charCount: 54,
    reminderTimeMillis: 0,
    createdAt: ts,
    updatedAt: ts,
  };
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("entries").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("e1")
      .set(validEntryPayload(ALICE_UID))
  );
});

test("UserProfile: owner can create/update/read; cross-user and ghost fields denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validProfilePayload(ALICE_UID))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Princess Luna",
      selectedThemeId: "purple_galaxy",
      updatedAt: pastDate(1000),
    })
  );

  // Cross-user read & write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: pastDate(1000),
    })
  );

  // Listing /users root collection denied
  await assertFails(aliceDb.collection("users").get());

  // Ghost field rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validProfilePayload(BOB_UID),
      isAdmin: true,
    })
  );
});

test("DiaryEntry: create, partial updates, recycle bin soft-delete, and negative security checks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const entryRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("entries")
    .doc("entry_1");

  // Empty subcollection list succeeds for owner
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("entries").get()
  );

  // Create valid entry
  await assertSucceeds(entryRef.set(validEntryPayload(ALICE_UID)));

  // Partial update: favorite toggle
  await assertSucceeds(
    entryRef.update({ favorite: true, updatedAt: pastDate(1000) })
  );

  // Partial update: soft delete to recycle bin
  await assertSucceeds(
    entryRef.update({
      deleted: true,
      deletedAtMillis: Date.now() - 1000,
      updatedAt: pastDate(500),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    entryRef.update({
      createdAt: pastDate(100),
      updatedAt: pastDate(100),
    })
  );

  // Reject extra @DocumentId 'id' field on create
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("entry_bad")
      .set({
        id: "entry_bad",
        ...validEntryPayload(ALICE_UID),
      })
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("entry_future")
      .set(validEntryPayload(ALICE_UID, new Date(Date.now() + 60000)))
  );

  // Cross-user isolation
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("entries").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("entry_1")
      .get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("entries")
      .doc("entry_1")
      .delete()
  );
});

test("Moods, Dreams, VoiceNotes, Memories, Categories, and Backups CRUD & isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastDate();

  // Mood
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("moods").doc("m1").set({
      userId: ALICE_UID,
      mood: "EXCITED",
      score: 10,
      note: "Amazing sunrise!",
      dateKey: "2026-11-19",
      loggedAtMillis: Date.now() - 2000,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Dream
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("dreams").doc("d1").set({
      userId: ALICE_UID,
      title: "Castle in the Clouds",
      description: "Floating on lavender clouds with a crown kitty.",
      category: "FANTASY",
      mood: "HAPPY",
      dateKey: "2026-11-19",
      tags: ["magic"],
      imageUris: [],
      favorite: true,
      deleted: false,
      deletedAtMillis: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // VoiceNote
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("voiceNotes").doc("v1").set({
      userId: ALICE_UID,
      title: "Midnight Reflection",
      description: "Voice memo before sleep",
      filePath: "/data/user/0/app/files/voice_1.m4a",
      durationSeconds: 42,
      dateKey: "2026-11-19",
      recordedAtMillis: Date.now() - 2000,
      favorite: false,
      deleted: false,
      deletedAtMillis: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // PhotoMemory
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("memories").doc("p1").set({
      userId: ALICE_UID,
      mediaUri: "file:///data/user/0/app/files/mem_1.jpg",
      mediaType: "PHOTO",
      albumName: "My Memories",
      caption: "Golden hour sky",
      dateKey: "2026-11-19",
      tags: ["sky"],
      favorite: true,
      deleted: false,
      deletedAtMillis: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Category
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("categories").doc("c1").set({
      userId: ALICE_UID,
      name: "Secret Garden",
      iconKey: "flower",
      colorHex: "#FF42C8",
      coverStyle: "pink_glow",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // BackupRecord
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("backups").doc("b1").set({
      userId: ALICE_UID,
      label: "Cloud Snapshot",
      format: "CLOUD",
      entryCount: 5,
      dreamCount: 2,
      moodCount: 7,
      createdAt: ts,
    })
  );

  // Cross-user list denied on all subcollections
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("moods").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("dreams").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("voiceNotes").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("memories").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("categories").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("backups").get());
});
