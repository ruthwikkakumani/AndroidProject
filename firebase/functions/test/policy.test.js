import test from 'node:test';
import assert from 'node:assert/strict';
import { participants, messageRecipients, handoverRecipients, pushData, invalidToken } from '../src/policy.js';

const thread = { id: 42, itemId: 81, ownerUid: 'owner', guestUid: 'guest', participantUids: ['owner', 'guest'],
  ownerChecked: false, guestChecked: false, ownerReturned: false, guestReturned: false };
const message = { id: 99, threadId: 42, senderUid: 'guest', participantUids: ['owner', 'guest'], body: 'private proof' };

test('only the other participant receives a message alert', () => {
  assert.deepEqual(messageRecipients(message, thread), ['owner']);
  assert.deepEqual(messageRecipients({ ...message, senderUid: 'owner' }, thread), ['guest']);
  assert.deepEqual(messageRecipients({ ...message, senderUid: 'outsider' }, thread), []);
  assert.deepEqual(messageRecipients({ ...message, threadId: 80 }, thread), []);
  assert.deepEqual(messageRecipients({ ...message, participantUids: ['owner', 'outsider'] }, thread), []);
});
test('malformed threads never expose a destination', () => {
  assert.deepEqual(participants({ ...thread, id: Number.MAX_SAFE_INTEGER + 1 }), []);
  assert.deepEqual(participants({ ...thread, guestUid: 'owner' }), []);
  assert.deepEqual(participants({ ...thread, participantUids: ['owner'] }), []);
  assert.equal(pushData(thread, 'outsider', 'event'), null);
  assert.equal(pushData(thread, 'owner', ''), null);
});
test('push payload contains routing only, with no ownership evidence or text', () => {
  assert.deepEqual(pushData({ ...thread, body: 'private', photoPath: 'private.jpg' }, 'owner', 'event'), {
    recipientUid: 'owner', threadId: '42', itemId: '81', eventId: 'event',
  });
});
test('handover only alerts on a new confirmation and respects recipient', () => {
  assert.deepEqual(handoverRecipients(thread, { ...thread, guestChecked: true }), ['owner']);
  assert.deepEqual(handoverRecipients(thread, { ...thread, ownerChecked: true }), ['guest']);
  assert.deepEqual(handoverRecipients(thread, thread), []);
  assert.deepEqual(handoverRecipients(thread, { ...thread, guestUid: 'outsider', guestChecked: true }), []);
  assert.deepEqual(handoverRecipients({ ...thread, guestChecked: true }, thread), []);
});
test('only terminal invalid-token errors remove device registration', () => {
  assert.equal(invalidToken({ code: 'messaging/registration-token-not-registered' }), true);
  assert.equal(invalidToken({ code: 'messaging/server-unavailable' }), false);
});
