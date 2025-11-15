const admin = require("firebase-admin");

// Initialize Firebase Admin **if not already initialized**
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

async function sendNotificationIfEnabled(userId, title, body) {
  try {
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
      console.log(`⚠️ No user found for ID: ${userId}`);
      return;
    }

    const userData = userDoc.data();

    // Check notifications toggle
    if (userData.notificationsEnabled === false) {
      console.log(`🔕 Notifications disabled for user ${userId}. Skipping FCM.`);
      return;
    }

    // Check if FCM token exists
    if (!userData.fcmToken) {
      console.log(`⚠️ No FCM token found for user ${userId}`);
      return;
    }

    // Build notification payload
    const message = {
      token: userData.fcmToken,
      notification: {
        title,
        body,
      },
      data: {
        userId,
        click_action: "FLUTTER_NOTIFICATION_CLICK",
      },
    };

    // Send notification via FCM
    await admin.messaging().send(message);
    console.log(`✅ Notification sent to ${userId}: ${title} - ${body}`);
  } catch (err) {
    console.error(`❌ Failed to send notification to ${userId}:`, err);
  }
}

module.exports = { sendNotificationIfEnabled };
