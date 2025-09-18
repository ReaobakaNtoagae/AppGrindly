const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
require("dotenv").config();

// Initialize Firebase Admin
admin.initializeApp();
const db = admin.firestore();
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

// Emulator compatibility
if (process.env.FUNCTIONS_EMULATOR === "true") {
  process.env.FIRESTORE_EMULATOR_HOST = "localhost:8080";
}

// JWT secret (fallback for local testing)
const JWT_SECRET = process.env.JWT_SECRET || "fallback_secret_for_testing";

const app = express();
app.use(cors({ origin: true }));
app.use(express.json());

// -------------------
// Middleware: JWT authentication
// -------------------
const authenticate = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader)
    return res.status(401).json({ error: "Authorization header missing" });

  const token = authHeader.split(" ")[1]; // Expect "Bearer <token>"
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded; // attach user info to request
    next();
  } catch (err) {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
};

// -------------------
// Validators
// -------------------
const isStrongPassword = (password) => {
  const regex =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;
  return regex.test(password);
};

const isValidEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);

const validUserTypes = ["admin", "hustler", "client"];


app.post("/register", async (req, res) => {
  try {
    const { email, password, userType, name } = req.body;
    if (!email || !password || !userType || !name)
      return res.status(400).json({ error: "Missing required fields" });

    if (!isValidEmail(email))
      return res.status(400).json({ error: "Invalid email format" });

    if (!isStrongPassword(password))
      return res.status(400).json({
        error:
          "Password must be at least 8 characters long, include uppercase, lowercase, number, and special character",
      });

    if (!validUserTypes.includes(userType.toLowerCase()))
      return res.status(400).json({
        error: `Invalid userType. Must be one of: ${validUserTypes.join(
          ", "
        )}`,
      });

    const snapshot = await db
      .collection("users")
      .where("email", "==", email)
      .get();
    if (!snapshot.empty)
      return res.status(400).json({ error: "User already exists" });

    const hashedPassword = await bcrypt.hash(password, 10);

    const userRef = await db.collection("users").add({
      email,
      password: hashedPassword,
      userType: userType.toLowerCase(),
      name,
      createdAt: FieldValue.serverTimestamp(),
    });

    const token = jwt.sign(
      { userId: userRef.id, userType: userType.toLowerCase() },
      JWT_SECRET,
      { expiresIn: "1h" }
    );

    return res.status(201).json({
      userId: userRef.id,
      token,
      userType: userType.toLowerCase(),
    });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});

// -------------------
// LOGIN USER
// POST /api/login
// -------------------
app.post("/login", async (req, res) => {
  try {
    const { email, password } = req.body;
    if (!email || !password)
      return res.status(400).json({ error: "Email and password required" });

    const snapshot = await db
      .collection("users")
      .where("email", "==", email)
      .get();
    if (snapshot.empty)
      return res.status(401).json({ error: "Invalid credentials" });

    const userDoc = snapshot.docs[0];
    const user = userDoc.data();

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch)
      return res.status(401).json({ error: "Invalid credentials" });

    const token = jwt.sign(
      { userId: userDoc.id, userType: user.userType },
      JWT_SECRET,
      { expiresIn: "1h" }
    );

    return res
      .status(200)
      .json({ userId: userDoc.id, token, userType: user.userType });
  } catch (err) {
    console.error(err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});


app.post("/profile", async (req, res) => {
  const {
    userId,
    title,
    category,
    location,
    price,
    pricingModel,
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


    const profileData = {
      ...(title && { title }),
      ...(category && { category }),
      ...(location && { location }),
      ...(price && { price }),
      ...(pricingModel && { pricingModel}),
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
