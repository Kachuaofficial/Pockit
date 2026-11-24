"use strict";
const {test}=require("node:test");
const assert=require("node:assert/strict");
const crypto=require("node:crypto");
const P=require("../monetization/policy");
const {validateEntries,createOfflineService}=require("../monetization/offline");
const emulator=!!process.env.FIRESTORE_EMULATOR_HOST;

test("offline sync validation rejects malformed and duplicate entries",()=>{
  const now=1795392000000;
  const row={id:crypto.randomUUID(),title:"Lunch",amountPaise:4900,type:"EXPENSE",category:"Food",createdAtMillis:now};
  assert.equal(validateEntries([row],now)[0].amountPaise,4900);
  for(const patch of [{amountPaise:-1},{amountPaise:1.1},{type:"CASH"},{title:""},{id:"../bad"},{createdAtMillis:now+P.DAY}])
    assert.throws(()=>validateEntries([{...row,...patch}],now),{code:"invalid-argument"});
  assert.throws(()=>validateEntries([row,row],now),{code:"invalid-argument"});
  assert.throws(()=>validateEntries([],now),{code:"invalid-argument"});
});

test("offline sync is free, account-scoped and idempotent",{skip:!emulator},async()=>{
  const {initializeApp,deleteApp}=require("firebase-admin/app"),{getFirestore}=require("firebase-admin/firestore");
  const app=initializeApp({projectId:"demo-pockit-commerce"},"offline-"+crypto.randomUUID()),db=getFirestore(app);
  const now=1795392000000,uid="offline_"+crypto.randomUUID(),offline=createOfflineService(db,()=>now);
  const row={id:crypto.randomUUID(),title:"Offline lunch",amountPaise:4900,type:"EXPENSE",category:"Food",createdAtMillis:now};
  const data={requestId:crypto.randomUUID(),entries:[row]};
  try {
    const results=await Promise.all([offline.sync(uid,data),offline.sync(uid,data),offline.sync(uid,data)]);
    assert.ok(results.every(x=>x.balancePaise===-4900));
    assert.equal((await db.collection("users").doc(uid).collection("transactions").get()).size,1);
    await assert.rejects(offline.sync(uid,{...data,entries:[{...row,amountPaise:5000}]}),{code:"already-exists"});
    const next={requestId:crypto.randomUUID(),entries:[{...row,id:crypto.randomUUID(),type:"INCOME",amountPaise:10000}]};
    assert.equal((await offline.sync(uid,next)).balancePaise,5100);
  } finally {await deleteApp(app)}
});
