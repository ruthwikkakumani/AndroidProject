const safeId = value => Number.isSafeInteger(value) && value > 0;
const validUid = value => typeof value === 'string' && value.length > 0 && value.length <= 128;

export function participants(thread) {
  if (!thread || !safeId(thread.id) || !safeId(thread.itemId) ||
      !validUid(thread.ownerUid) || !validUid(thread.guestUid) || thread.ownerUid === thread.guestUid ||
      !Array.isArray(thread.participantUids) || thread.participantUids.length !== 2 ||
      thread.participantUids[0] !== thread.ownerUid || thread.participantUids[1] !== thread.guestUid) return [];
  return thread.participantUids;
}

export function messageRecipients(message, thread) {
  const uids = participants(thread);
  if (!message || !safeId(message.id) || message.threadId !== thread?.id ||
      !uids.includes(message.senderUid) || !Array.isArray(message.participantUids) ||
      message.participantUids.length !== 2 || message.participantUids.some((uid, i) => uid !== uids[i])) return [];
  return uids.filter(uid => uid !== message.senderUid);
}

export function handoverRecipients(before, after) {
  const uids = participants(after);
  const previous = participants(before);
  if (uids.length !== 2 || previous.length !== 2 || previous.some((uid, i) => uid !== uids[i]) ||
      before.id !== after.id || before.itemId !== after.itemId) return [];
  const changed = (keys) => keys.some(key => before[key] === false && after[key] === true);
  const recipients = [];
  if (changed(['ownerChecked', 'ownerReturned'])) recipients.push(after.guestUid);
  if (changed(['guestChecked', 'guestReturned'])) recipients.push(after.ownerUid);
  return [...new Set(recipients)];
}

// Only routing metadata goes to the push transport. Photos and messages stay private in Firestore/Storage.
export function pushData(thread, recipientUid, eventId) {
  if (!participants(thread).includes(recipientUid) || typeof eventId !== 'string' ||
      eventId.length === 0 || eventId.length > 200) return null;
  return { recipientUid, threadId: String(thread.id), itemId: String(thread.itemId), eventId };
}

export function invalidToken(error) {
  return ['messaging/invalid-registration-token', 'messaging/registration-token-not-registered'].includes(error?.code);
}
