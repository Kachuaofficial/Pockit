"use strict";
const P = require("./policy");
const {Timestamp, FieldValue} = require("firebase-admin/firestore");

function validateEntries(rows, now) {
  if (!Array.isArray(rows) || rows.length < 1 || rows.length > 100)
    P.fail("invalid-argument", "Sync between 1 and 100 entries at a time.");
  const ids = new Set();
  return rows.map(row => {
    P.operationId(row?.id);
    if (ids.has(row.id)) P.fail("invalid-argument", "Duplicate record in sync batch.");
    ids.add(row.id);
    if (typeof row.title !== "string" || !row.title.trim() || row.title.length > 200 ||
        typeof row.category !== "string" || !row.category || row.category.length > 100 ||
        !["INCOME", "EXPENSE"].includes(row.type) || !Number.isSafeInteger(row.amountPaise) ||
        row.amountPaise < 1 || row.amountPaise > 100000000000 ||
        !Number.isSafeInteger(row.createdAtMillis) || row.createdAtMillis < 946684800000 || row.createdAtMillis > now + 300000)
      P.fail("invalid-argument", "An offline entry has invalid details. Review it before syncing.");
    return {id:row.id, title:row.title.trim(), amountPaise:row.amountPaise, type:row.type,
      category:row.category, createdAtMillis:row.createdAtMillis};
  }).sort((a,b) => a.id.localeCompare(b.id));
}

function createOfflineService(db, now = Date.now) {
  async function sync(uid, data) {
    const timestamp = now(), rows = validateEntries(data.entries, timestamp);
    P.operationId(data.requestId);
    const fingerprint = P.hash(JSON.stringify(rows));
    const receipt = db.collection("offlineSyncReceipts").doc(P.hash(uid + ":" + data.requestId));
    const wallet = db.collection("users").doc(uid).collection("wallets").doc("main");
    const refs = rows.map(row => db.collection("users").doc(uid).collection("transactions").doc(row.id));
    return db.runTransaction(async tx => {
      const [prior, walletRow, ...existing] = await Promise.all([
        tx.get(receipt), tx.get(wallet), ...refs.map(ref => tx.get(ref))]);
      if (prior.exists) {
        if (prior.data().fingerprint !== fingerprint) P.fail("already-exists", "This sync request was already used for different records.");
        return {alreadyProcessed:true, balancePaise:walletRow.get("balancePaise") || 0, ids:rows.map(r=>r.id)};
      }
      let balance = walletRow.get("balancePaise") || 0;
      for (let i=0; i<rows.length; i++) {
        const row=rows[i], old=existing[i];
        if (old.exists) {
          if (old.get("title")!==row.title || old.get("amountPaise")!==row.amountPaise || old.get("type")!==row.type || old.get("category")!==row.category)
            P.fail("already-exists", "An offline record conflicts with a saved record. Your local entries have been kept.");
        } else balance += (row.type==="INCOME"?1:-1)*row.amountPaise;
      }
      if (!Number.isSafeInteger(balance) || Math.abs(balance)>100000000000)
        P.fail("failed-precondition", "The resulting balance exceeds the supported range.");
      rows.forEach((row,i) => { if (!existing[i].exists) tx.create(refs[i], {
        title:row.title, amountPaise:row.amountPaise, type:row.type, category:row.category,
        createdAt:Timestamp.fromMillis(row.createdAtMillis), recordedAt:FieldValue.serverTimestamp(), source:"OFFLINE_SYNC"}); });
      tx.set(wallet, {balancePaise:balance, updatedAt:FieldValue.serverTimestamp()}, {merge:true});
      tx.create(receipt, {uid, fingerprint, count:rows.length, createdAtMs:timestamp, source:"FREE_SYNC"});
      return {balancePaise:balance, ids:rows.map(r=>r.id)};
    });
  }
  return {sync};
}
module.exports = {createOfflineService, validateEntries};
