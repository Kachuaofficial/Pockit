/*
 * Server-side Firestore create triggers for friend and space expense notifications.
 * The Admin SDK reads delivery tokens and sends FCM messages after a record exists.
 * These handlers neither calculate wallet balances nor verify external payment receipt.
 */

const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { setGlobalOptions } = require("firebase-functions/v2");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

// Run triggers in the configured asia-south2 region; deployment must match the intended backend environment.
setGlobalOptions({ region: "asia-south2" });

// Account-scoped offline transaction sync.
Object.assign(exports, require("./monetization"));

/**
 * Triggered whenever a new transaction is created in friend_transactions.
 * Sends a high-priority FCM push notification to the friend receiving or involved in the expense.
 */
exports.onFriendTransactionCreated = onDocumentCreated(
  "friend_transactions/{txId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return null;

    const data = snap.data();
    if (!data) return null;

    const creatorId = data.creatorId;
    const friendId = data.friendId;
    const creatorName = data.creatorName || "A friend";
    const amount = Number(data.amount || 0).toLocaleString("en-IN");
    const description = data.description || "Expense";
    const direction = data.direction; // GAVE_UDHAR, TOOK_UDHAR, SETTLE_UP

    // The recipient of the push notification is the other friend
    const targetUserId = friendId;
    if (!targetUserId || targetUserId === creatorId) {
      console.log("No valid recipient found for notification");
      return null;
    }

    try {
      // 1. Fetch friend's FCM token from Firestore
      const userDoc = await getFirestore().collection("users").doc(targetUserId).get();
      if (!userDoc.exists) {
        console.log(`User document not found for target user ${targetUserId}`);
        return null;
      }

      const fcmToken = userDoc.data()?.fcmToken;
      if (!fcmToken) {
        console.log(`No FCM token registered in Firestore for user ${targetUserId}`);
        return null;
      }

      // 2. Format title and body based on transaction direction
      let title = "💸 New Expense in Pockit";
      let body = "";

      if (direction === "GAVE_UDHAR") {
        title = "💸 New Udhar Added";
        body = `${creatorName} added ₹${amount} for '${description}'`;
      } else if (direction === "TOOK_UDHAR") {
        title = "🤝 Udhar Received";
        body = `${creatorName} borrowed ₹${amount} from you for '${description}'`;
      } else if (direction === "SETTLE_UP") {
        title = "✅ Settle Up Update";
        body = `${creatorName} settled up ₹${amount} with you`;
      } else {
        body = `${creatorName} added ₹${amount} for '${description}'`;
      }

      // 3. Construct FCM push payload
      const message = {
        token: fcmToken,
        notification: {
          title: title,
          body: body,
        },
        data: {
          transactionId: String(event.params.txId),
          creatorId: String(creatorId || ""),
          type: "friend_expense",
          amount: String(data.amount || ""),
        },
        android: {
          priority: "high",
          notification: {
            channelId: "pockit_expenses_channel",
            sound: "default",
            defaultSound: true,
            defaultVibrateTimings: true,
          },
        },
      };

      const response = await getMessaging().send(message);
      console.log(`Successfully sent FCM notification to ${targetUserId}:`, response);
      return response;
    } catch (error) {
      console.error(`Error sending push notification to user ${targetUserId}:`, error);
      return null;
    }
  }
);

/**
 * Triggered whenever a new expense is created in a Space.
 * Sends push notification to other space members.
 */
exports.onSpaceExpenseCreated = onDocumentCreated(
  "spaces/{spaceId}/expenses/{expenseId}",
  async (event) => {
    const snap = event.data;
    if (!snap) return null;

    const data = snap.data();
    if (!data) return null;

    const spaceId = event.params.spaceId;
    const payerId = data.paidById;
    const title = data.title || "Expense";
    const amount = Number(data.amount || 0).toLocaleString("en-IN");

    try {
      const spaceDoc = await getFirestore().collection("spaces").doc(spaceId).get();
      if (!spaceDoc.exists) return null;

      const spaceData = spaceDoc.data();
      const spaceName = spaceData?.name || "Space";
      const memberIds = spaceData?.memberIds || [];

      // Send to everyone in the space except the payer
      const targetUserIds = memberIds.filter((id) => id !== payerId);
      if (targetUserIds.length === 0) return null;

      // Use batched getAll to fetch all user documents in a single RPC instead of N individual roundtrips
      const userRefs = targetUserIds.map((uid) => getFirestore().collection("users").doc(uid));
      const userDocs = userRefs.length > 0 ? await getFirestore().getAll(...userRefs) : [];

      const tokens = userDocs
        .filter((doc) => doc.exists && doc.data()?.fcmToken)
        .map((doc) => doc.data().fcmToken);

      if (tokens.length === 0) {
        console.log("No valid FCM tokens found for other space members");
        return null;
      }

      const message = {
        tokens: tokens,
        notification: {
          title: `🚀 ${spaceName}: New Expense`,
          body: `Added ₹${amount} for '${title}'`,
        },
        data: {
          spaceId: String(spaceId),
          type: "space_expense",
        },
        android: {
          priority: "high",
          notification: {
            channelId: "pockit_expenses_channel",
            sound: "default",
          },
        },
      };

      const response = await getMessaging().sendEachForMulticast(message);
      console.log(`Space notification sent: ${response.successCount} success, ${response.failureCount} failed`);
      return response;
    } catch (error) {
      console.error("Error sending space expense notification:", error);
      return null;
    }
  }
);
