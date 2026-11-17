const { test } = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');

// This suite can only send requests to a local Firestore emulator, never a live database.
const host = process.env.FIRESTORE_EMULATOR_HOST;
const project = process.env.GCLOUD_PROJECT;
if (!host || !/^(localhost|127\.0\.0\.1):\d+$/.test(host) || !project?.startsWith('demo-')) {
  throw new Error('Run this suite with firebase emulators:exec using a demo project.');
}
const root = `http://${host}/v1/projects/${project}/databases/(default)/documents`;
function token(uid) {
  const now = Math.floor(Date.now() / 1000);
  const encode = value => Buffer.from(JSON.stringify(value)).toString('base64url');
  return `${encode({ alg: 'none', typ: 'JWT' })}.${encode({ sub: uid, user_id: uid,
    aud: project, iss: `https://securetoken.google.com/${project}`, iat: now, exp: now + 3600,
    firebase: { sign_in_provider: 'custom' } })}.`;
}
function field(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(field) } };
  return { stringValue: value };
}
async function request(uid, path, data) {
  return fetch(`${root}/${path}`, { method: data ? 'PATCH' : 'GET',
    headers: { Authorization: `Bearer ${token(uid)}`, 'Content-Type': 'application/json' },
    body: data ? JSON.stringify({ fields: Object.fromEntries(Object.entries(data).map(([k, v]) => [k, field(v)])) }) : undefined });
}
async function expect(response, status) {
  assert.equal(response.status, status, await response.text());
}

test('new Google profile lookup and a complete private two-student return flow', async () => {
  const suffix = randomUUID();
  const owner = `owner-${suffix}`, guest = `guest-${suffix}`, outsider = `other-${suffix}`;
  const campusKey = `campus-${suffix}`;
  await expect(await request(owner, `profiles/${owner}`), 404);
  for (const [uid, localId] of [[owner, 101], [guest, 102], [outsider, 103]]) {
    await expect(await request(uid, `profiles/${uid}`, { uid, localId,
      displayName: 'Campus Student', campusName: campusKey, campusKey }), 200);
  }
  const itemId = Date.now(), threadId = itemId + 1, messageId = itemId + 2;
  const item = { id: itemId, title: 'Blue backpack', description: 'A private identifying mark inside',
    category: 'Bags', type: 'LOST', status: 'OPEN', latitude: 0, longitude: 0,
    placeLabel: 'Library', zoneName: 'Campus', photoPath: null, scannedText: '', dominantColor: null,
    createdAt: itemId, resolvedAt: null, ownerId: 101, ownerUid: owner, campusKey,
    hasCoordinates: false, deleted: false, returnThreadId: null };
  await expect(await request(owner, `items/${itemId}`, item), 200);
  await expect(await request(guest, `threads/${threadId}`), 404);
  const thread = { id: threadId, itemId, ownerId: 101, guestId: 102, ownerUid: owner, guestUid: guest,
    participantUids: [owner, guest], status: 'OPEN', createdAt: itemId,
    ownerChecked: false, guestChecked: false, ownerReturned: false, guestReturned: false };
  await expect(await request(guest, `threads/${threadId}`, thread), 200);
  await expect(await request(owner, `threads/${threadId}`), 200);
  await expect(await request(outsider, `threads/${threadId}`), 403);
  const message = { id: messageId, threadId, senderId: 102, senderUid: guest,
    body: 'I found it. Let us check private details.', photoPath: null,
    createdAt: itemId, participantUids: [owner, guest] };
  await expect(await request(guest, `messages/${messageId}`, message), 200);
  await expect(await request(owner, `messages/${messageId}`), 200);
  await expect(await request(outsider, `messages/${messageId}`), 403);
  await expect(await request(guest, `threads/${threadId}`, { ...thread, ownerChecked: true }), 403);
  await expect(await request(owner, `threads/${threadId}`, { ...thread, ownerReturned: true }), 403);
  thread.ownerChecked = true;
  await expect(await request(owner, `threads/${threadId}`, thread), 200);
  thread.guestChecked = true;
  await expect(await request(guest, `threads/${threadId}`, thread), 200);
  thread.ownerReturned = true;
  await expect(await request(owner, `threads/${threadId}`, thread), 200);
  thread.guestReturned = true;
  await expect(await request(guest, `threads/${threadId}`, thread), 200);
  await expect(await request(guest, `items/${itemId}`, { ...item, status: 'REUNITED', resolvedAt: Date.now(), returnThreadId: threadId }), 200);
});
