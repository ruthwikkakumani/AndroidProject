import { initializeApp } from 'firebase-admin/app';
import { getFirestore } from 'firebase-admin/firestore';
import { getMessaging } from 'firebase-admin/messaging';
import { onDocumentCreated, onDocumentUpdated } from 'firebase-functions/v2/firestore';
import { participants, messageRecipients, handoverRecipients, pushData, invalidToken } from './policy.js';

initializeApp();
const store = getFirestore();

async function notify(thread, recipients, eventId) {
  for (const uid of recipients) {
    const data = pushData(thread, uid, eventId);
    if (!data) continue;
    const devices = await store.collection('profiles').doc(uid).collection('devices').get();
    const eligible = devices.docs.filter(doc => {
      const token = doc.get('token');
      return typeof token === 'string' && token.length > 0 && token.length <= 4096;
    });
    for (let start = 0; start < eligible.length; start += 500) {
      const batch = eligible.slice(start, start + 500);
      const response = await getMessaging().sendEachForMulticast({
        tokens: batch.map(doc => doc.get('token')), data,
        android: { priority: 'high', ttl: 60 * 60 * 1000, collapseKey: String(thread.id) },
      });
      await Promise.all(response.responses.map((result, index) =>
        !result.success && invalidToken(result.error) ? batch[index].ref.delete() : undefined));
    }
  }
}

export const conversationStarted = onDocumentCreated('threads/{threadId}', async event => {
  const thread = event.data?.data();
  if (participants(thread).length === 2) await notify(thread, [thread.ownerUid], event.id);
});

export const privateMessageCreated = onDocumentCreated('messages/{messageId}', async event => {
  const message = event.data?.data();
  if (!Number.isSafeInteger(message?.threadId) || message.threadId <= 0) return;
  const thread = (await store.collection('threads').doc(String(message.threadId)).get()).data();
  await notify(thread, messageRecipients(message, thread), event.id);
});

export const returnProgressChanged = onDocumentUpdated('threads/{threadId}', async event => {
  const before = event.data?.before.data(); const after = event.data?.after.data();
  await notify(after, handoverRecipients(before, after), event.id);
});
