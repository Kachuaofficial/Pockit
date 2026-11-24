"use strict";
const {onCall,HttpsError}=require("firebase-functions/v2/https");
const logger=require("firebase-functions/logger");
const {getFirestore}=require("firebase-admin/firestore");
const P=require("./policy");
const db=getFirestore();
function callable(handler,consume=false) {
  return onCall({region:"asia-south2",enforceAppCheck:true,consumeAppCheckToken:consume,maxInstances:20,timeoutSeconds:60},async request=>{
    try { return await handler(P.requireUser(request),request.data||{}); }
    catch(error) {
      if(error instanceof P.CommerceError) throw new HttpsError(error.code,error.message);
      logger.error("Offline sync failed",{code:error.code||error.response?.status||"unknown"});
      throw new HttpsError("unavailable","Cloud sync is temporarily unavailable. Your entries are safe and will retry.");
    }
  });
}
const offline = require("./offline").createOfflineService(db);
exports.syncOfflineTransactions=callable((uid,data)=>offline.sync(uid,data),true);
