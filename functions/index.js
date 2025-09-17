const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");

// Initialize Firebase Admin SDK
admin.initializeApp();
const db = admin.firestore();


const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

app.post("/profile", async (req, res) => {
  try {
    const {
      userId,
      title,
      category,
      location,
      price,
      description,
      profilePictureURL,
      workImageURLs,
      documentURLs,
      verifiedBadgeTier,
      servicePackages,
      packageStatus,
    } = req.body;

    if (!userId) {
      return res.status(400).json({ message: "Missing userId" });
    }

    const docRef = db.collection("profiles").doc(userId);

    const profileData = {
updatedAt: new Date(),
    };

    if (title) profileData.title = title;
    if (category) profileData.category = category;
    if (location) profileData.location = location;
    if (price) profileData.price = price;
    if (description) profileData.description = description;
    if (profilePictureURL) profileData.profilePictureURL = profilePictureURL;
    if (Array.isArray(workImageURLs)) profileData.workImageURLs = workImageURLs;
    if (Array.isArray(documentURLs)) profileData.documentURLs = documentURLs;
    if (verifiedBadgeTier) profileData.verifiedBadgeTier = verifiedBadgeTier;

    if (servicePackages === "none") {
      profileData.servicePackages = "none";
    } else if (Array.isArray(servicePackages)) {
      profileData.servicePackages = servicePackages;
    }

    if (packageStatus) {
      profileData.packageStatus = packageStatus;
    }

    await docRef.set(profileData, { merge: true });

    return res.status(200).json({ message: "Profile updated successfully" });
  } catch (error) {
    console.error("Error updating profile:", error);
    return res.status(500).json({ message: "Internal server error" });
  }
});

app.get("/profile/:userId", async (req, res) => {
  const { userId } = req.params;

  try {
    const doc = await db.collection("profiles").doc(userId).get();
    if (!doc.exists) {
      return res.status(404).json({ error: "Profile not found" });
    }
    res.status(200).json(doc.data());
  } catch (error) {
    console.error("Error fetching profile:", error);
    res.status(500).json({ error: error.message });
  }
});


exports.api = functions.https.onRequest(app);