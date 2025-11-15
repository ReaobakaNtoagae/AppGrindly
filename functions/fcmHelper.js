const admin = require("firebase-admin");

async function sendPushNotification(token, title, body, data = {}) {
  console.log("🚀 Attempting to send push notification to:", token);
  console.log("📦 Title:", title, "| Body:", body);

  if (!token) {
    console.log("⚠️ No FCM token provided");
    return;
  }

  const message = {
    token,
    notification: { title: title || "Grindly", body: body || "You have a new notification" },
    data: data,
  };

  try {
    const response = await admin.messaging().send(message);
    console.log("✅ Successfully sent message:", response);
  } catch (error) {
    console.error("❌ Error sending message:", error);
  }
}

module.exports = { sendPushNotification };
