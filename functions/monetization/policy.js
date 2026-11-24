"use strict";

const crypto = require("node:crypto");
const DAY = 86400000;

class CommerceError extends Error {
  constructor(code, message) { super(message); this.code = code; }
}

function fail(code, message) { throw new CommerceError(code, message); }
function hash(value) { return crypto.createHash("sha256").update(value).digest("hex"); }

function requireUser(request) {
  if (!request.auth?.uid || request.auth.token?.firebase?.sign_in_provider === "anonymous")
    fail("unauthenticated", "Sign in to your Pockit account.");
  if (!request.app || request.app.alreadyConsumed)
    fail("unauthenticated", "App verification failed. Please retry.");
  if (request.data?.accountId && request.data.accountId !== request.auth.uid)
    fail("permission-denied", "Your account changed. Sign in again.");
  return request.auth.uid;
}

function operationId(value) {
  if (typeof value !== "string" || !/^[a-zA-Z0-9_-]{16,80}$/.test(value))
    fail("invalid-argument", "Invalid request identifier.");
  return value;
}

module.exports = {DAY, CommerceError, fail, hash, requireUser, operationId};
