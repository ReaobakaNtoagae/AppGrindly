const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");

// Initialize Firebase Admin
admin.initializeApp();
const db = admin.firestore();

// Emulator compatibility
if (process.env.FUNCTIONS_EMULATOR) {
  db.settings({ host: "127.0.0.1:8080", ssl: false });
}



const app = express();
app.use(cors({ origin: true }));
app.use(express.json());


app.post("/profile", async (req, res) => {
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
  } = req.body;

  if (!userId) {
    return res.status(400).json({ error: "Missing userId." });
  }

  const docRef = db.collection("profiles").doc(userId);

  try {
    const existingDoc = await docRef.get();

    // Strict validation only for new profiles
    if (!existingDoc.exists) {
      if (
        !title ||
        !category ||
        !description ||
        description.length < 250 ||
        !Array.isArray(workImageURLs) ||
        workImageURLs.length < 1 ||
        !profilePictureURL
      ) {
        return res.status(400).json({
          error:
            "Missing required fields for new profile: description must be ≥250 characters, profile picture is required, and at least one work image must be provided.",
        });
      }
    }

    // Build profileData dynamically for partial updates
    const profileData = {
      ...(title && { title }),
      ...(category && { category }),
      ...(location && { location }),
      ...(price && { price }),
      ...(description && { description }),
      ...(profilePictureURL && { profilePictureURL }),
      ...(Array.isArray(workImageURLs) && { workImageURLs }),
      ...(Array.isArray(documentURLs) && { documentURLs }),
      verifiedBadgeTier: verifiedBadgeTier || "none",
      servicePackages:
        Array.isArray(servicePackages) && servicePackages.length > 0
          ? servicePackages
          : "none",
      updatedAt: new Date(),
    };

    await docRef.set(profileData, { merge: true });
    res.status(200).json({ message: "Profile created/updated successfully" });
  } catch (error) {
    console.error("Error creating/updating profile:", error);
    res.status(500).json({ error: error.message });
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



app.post("/test", (req, res) => {
  res.status(200).json({ status: "Function is alive" });
});


exports.api = functions.https.onRequest(app);
