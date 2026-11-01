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
const OUTSIDER_UID = "mallory_999";

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

function validQuotePayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    text: "We suffer more often in imagination than in reality.",
    author: "Seneca",
    tags: ["Stoicism", "Mindfulness", "Resilience"],
    timestampInfo: {
      location: "San Francisco, CA",
      date: "Nov 1, 2026",
      time: "9:07 AM",
      capturedAt: "Nov 1, 2026 at 9:07 AM",
    },
    sourceWork: "Letters from a Stoic",
    category: "Stoicism",
    onlinePopularityScore: 95,
    personalNote: "Timeless reminder for busy mornings.",
    createdAt: now,
    updatedAt: now,
  };
}

function validPrefsPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    selectedInterests: ["Stoicism", "Literature", "Philosophy"],
    priorityMode: "Balanced",
    recentQuoteIds: ["q_seneca_1"],
    createdAt: now,
    updatedAt: now,
  };
}

function validFamilyCirclePayload(ownerUid) {
  const now = new Date();
  return {
    name: "The Seneca Family Circle",
    inviteCode: "FAM-SENECA",
    ownerUid,
    ownerEmail: "alice@commonplace.test",
    memberUids: [ownerUid, BOB_UID],
    memberEmails: ["alice@commonplace.test", "bob@commonplace.test"],
    createdAt: now,
    updatedAt: now,
  };
}

function validFamilySharedQuotePayload(senderUid) {
  const now = new Date();
  return {
    circleId: "fc_1",
    circleName: "The Seneca Family Circle",
    senderUid,
    senderName: "Alice",
    senderEmail: "alice@commonplace.test",
    allowedMemberUids: [ALICE_UID, BOB_UID],
    allowedEmails: ["alice@commonplace.test", "bob@commonplace.test"],
    text: "We suffer more often in imagination than in reality.",
    author: "Seneca",
    tags: ["Stoicism", "Resilience"],
    timestampInfo: {
      location: "San Francisco, CA",
      date: "Nov 1, 2026",
      time: "9:30 AM",
      capturedAt: "Nov 1, 2026 at 9:30 AM",
    },
    personalNote: "Sharing with the family today.",
    createdAt: now,
    updatedAt: now,
  };
}

test("Unauthenticated user: cannot read or create saved_quotes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("saved_quotes").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_quotes")
      .doc("q1")
      .set(validQuotePayload(ALICE_UID))
  );
});

test("Authenticated owner: can create, get, list, update, and delete their own saved_quotes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_quotes")
    .doc("q_1");

  await assertSucceeds(docRef.set(validQuotePayload(ALICE_UID)));
  await assertSucceeds(docRef.get());
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_quotes")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    docRef.update({
      personalNote: "Updated marginalia note.",
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(docRef.delete());
});

test("Cross-user isolation: Bob cannot read, update, or delete Alice's saved_quotes", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_quotes")
      .doc("q_alice")
      .set(validQuotePayload(ALICE_UID));
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const aliceDocFromBob = bobDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_quotes")
    .doc("q_alice");

  await assertFails(aliceDocFromBob.get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_quotes").get()
  );
  await assertFails(
    aliceDocFromBob.update({ personalNote: "Bob tampering", updatedAt: new Date() })
  );
  await assertFails(aliceDocFromBob.delete());
});

test("Shadow field and value poisoning: rejects ghost fields and invalid types", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_quotes")
    .doc("q_shadow");

  await assertFails(
    docRef.set({
      ...validQuotePayload(ALICE_UID),
      isAdmin: true,
    })
  );

  await assertFails(
    docRef.set({
      ...validQuotePayload(ALICE_UID),
      onlinePopularityScore: 500,
    })
  );
});

test("Authenticated owner: can create and update user preferences", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const prefRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("preferences")
    .doc("main");

  await assertSucceeds(prefRef.set(validPrefsPayload(ALICE_UID)));
  await assertSucceeds(
    prefRef.update({
      priorityMode: "Popular Online",
      selectedInterests: ["Stoicism", "Science"],
      updatedAt: new Date(),
    })
  );
  await assertFails(
    prefRef.update({
      priorityMode: "InvalidMode",
      updatedAt: new Date(),
    })
  );
});

test("Zero-Trust Family Social Circle: members can read and share; outsiders are denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@commonplace.test" }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@commonplace.test" }).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID, { email: "mallory@outsider.test" }).firestore();

  const circleRefAlice = aliceDb.collection("family_circles").doc("fc_1");
  await assertSucceeds(circleRefAlice.set(validFamilyCirclePayload(ALICE_UID)));

  // Bob (authorized member) can read the Family Circle
  await assertSucceeds(bobDb.collection("family_circles").doc("fc_1").get());

  // Outsider (Mallory) cannot read or list the Family Circle
  await assertFails(outsiderDb.collection("family_circles").doc("fc_1").get());
  await assertFails(
    outsiderDb.collection("family_circles").where("memberUids", "array-contains", OUTSIDER_UID).get().then(snap => {
      if (!snap.empty) throw new Error("Outsider saw circles");
      return outsiderDb.collection("family_circles").doc("fc_1").get();
    })
  );

  // Alice shares a quote with TimestampInfo to the Family Circle
  const sharedRefAlice = aliceDb.collection("family_shared_quotes").doc("fsq_1");
  await assertSucceeds(sharedRefAlice.set(validFamilySharedQuotePayload(ALICE_UID)));

  // Bob (member) can read the shared quote
  await assertSucceeds(bobDb.collection("family_shared_quotes").doc("fsq_1").get());

  // Outsider (Mallory) is strictly denied access to the shared quote
  await assertFails(outsiderDb.collection("family_shared_quotes").doc("fsq_1").get());
});

test("Tier-B Cloud Salon Relay: authenticated readers can create/join symposium_rooms and post marginalia; unauthenticated denied", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  const roomCode = "LFS-7K9M2P";
  const now = new Date();
  const validRoom = {
    roomCode,
    title: "Stoic Dawn Co-Reading Folio",
    hostUid: ALICE_UID,
    hostName: "Alice",
    gameMode: "CO_READING_SALON",
    lifecyclePolicy: "ON_DEMAND_WAKE",
    currentQuoteId: "q_seneca_imagination",
    currentQuoteText: "We suffer more often in imagination than in reality.",
    currentQuoteAuthor: "Seneca the Younger",
    participantUids: [ALICE_UID],
    participantNames: ["Alice"],
    activeScholars: ["@archivist", "@willow", "@mote"],
    roundNumber: 1,
    createdAt: now,
    updatedAt: now,
  };

  await assertFails(unauthDb.collection("symposium_rooms").doc(roomCode).set(validRoom));
  await assertSucceeds(aliceDb.collection("symposium_rooms").doc(roomCode).set(validRoom));

  // Bob can join the room by adding his UID to participantUids without mutating hostUid
  await assertSucceeds(
    bobDb.collection("symposium_rooms").doc(roomCode).update({
      participantUids: [ALICE_UID, BOB_UID],
      participantNames: ["Alice", "Bob"],
      updatedAt: new Date(),
    })
  );

  // Bob cannot hijack hostUid
  await assertFails(
    bobDb.collection("symposium_rooms").doc(roomCode).update({
      hostUid: BOB_UID,
      updatedAt: new Date(),
    })
  );

  // Alice and Bob can post synchronized marginalia messages
  const msgRef = aliceDb
    .collection("symposium_rooms")
    .doc(roomCode)
    .collection("messages")
    .doc("smsg_1");
  await assertSucceeds(
    msgRef.set({
      roomCode,
      senderUid: ALICE_UID,
      senderName: "Alice",
      messageType: "MARGINALIA",
      text: "Notice how Seneca contrasts imagination with reality.",
      quoteSnippet: "imagination than in reality",
      scholarHandle: "@archivist",
      triviaDeltaScore: 0,
      createdAt: new Date(),
    })
  );
});

test("Quote Highlight Annotations: owner can create, read, and delete; cross-user denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const annRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("saved_quotes")
    .doc("q_1")
    .collection("annotations")
    .doc("hl_1");

  await assertSucceeds(
    annRef.set({
      quoteId: "q_1",
      userId: ALICE_UID,
      highlightedText: "more often in imagination",
      startCharIndex: 10,
      endCharIndex: 35,
      inkColor: "AMBER_GOLD",
      noteText: "Crucial Stoic distinction.",
      timestampLabel: "Nov 1, 2026 • 10:15 AM",
      createdAt: new Date(),
    })
  );

  await assertSucceeds(annRef.get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_quotes")
      .doc("q_1")
      .collection("annotations")
      .doc("hl_1")
      .get()
  );
  await assertSucceeds(annRef.delete());
});

test("Zero-Trust World Map (/map_quote_pins & /map_pin_identities): enforces 1/day deterministic ID and protects identity ACL", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@commonplace.test" }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@commonplace.test" }).firestore();
  const outsiderDb = testEnv.authenticatedContext(OUTSIDER_UID, { email: "mallory@outsider.test" }).firestore();

  const dayKey = "2026_11_01";
  const deterministicPinId = `${ALICE_UID}_${dayKey}`;
  const now = new Date();

  const validPublicPin = {
    ownerUid: ALICE_UID,
    dayKey,
    quoteId: "q_seneca_1",
    quoteText: "We suffer more often in imagination than in reality.",
    quoteAuthor: "Seneca",
    sourceWork: "Letters from a Stoic",
    category: "Stoicism",
    latitude: 37.78,
    longitude: -122.42,
    regionLabel: "San Francisco Bay Area",
    anonymousAlias: "Wandering Stoic #402",
    createdAt: now,
  };

  // Rejects non-deterministic pinId (enforces 1 pin per user per day)
  await assertFails(
    aliceDb.collection("map_quote_pins").doc("random_second_pin_today").set(validPublicPin)
  );

  // Accepts deterministic {uid}_{dayKey} pinId
  await assertSucceeds(
    aliceDb.collection("map_quote_pins").doc(deterministicPinId).set(validPublicPin)
  );

  // Both connected friend (Bob) and unconnected user (Mallory) can read the anonymous public pin
  await assertSucceeds(bobDb.collection("map_quote_pins").doc(deterministicPinId).get());
  await assertSucceeds(outsiderDb.collection("map_quote_pins").doc(deterministicPinId).get());

  // Zero-Trust Identity envelope in /map_pin_identities/{pinId}
  const validIdentity = {
    pinId: deterministicPinId,
    ownerUid: ALICE_UID,
    ownerDisplayName: "Alice",
    ownerEmail: "alice@commonplace.test",
    circleNames: ["The Seneca Family Circle", "PREC:bob@commonplace.test:EXACT_GPS"],
    personalMarginalia: "Pinned from the conservatory.",
    exactLocationLabel: "San Francisco Conservatory||37.7749,-122.4194||PUB||q_seneca_1||Seneca||We suffer more often",
    connectedUserUids: [ALICE_UID, BOB_UID],
    connectedUserEmails: ["alice@commonplace.test", "bob@commonplace.test"],
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(
    aliceDb.collection("map_pin_identities").doc(deterministicPinId).set(validIdentity)
  );

  // Connected friend (Bob) can read Alice's identity & precision envelope
  await assertSucceeds(
    bobDb.collection("map_pin_identities").doc(deterministicPinId).get()
  );

  // Unconnected user (Mallory) is strictly denied access to Alice's identity envelope
  await assertFails(
    outsiderDb.collection("map_pin_identities").doc(deterministicPinId).get()
  );
});


