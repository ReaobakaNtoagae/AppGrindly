// index.js
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
require("dotenv").config(); // Load .env variables

// Initialize Firebase Admin
admin.initializeApp();

// Use Firestore emulator locally
if (process.env.FUNCTIONS_EMULATOR === "true") {
  process.env.FIRESTORE_EMULATOR_HOST = "localhost:8080";
}

// Import Firestore functions from Admin SDK
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const db = getFirestore();

// JWT secret (fallback for local testing)
const JWT_SECRET = process.env.JWT_SECRET || "fallback_secret_for_testing";

const app = express();
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

// -------------------
// REGISTER USER
// POST /api/register
// -------------------
app.post("/api/register", async (req, res) => {
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
app.post("/api/login", async (req, res) => {
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


exports.api = functions.https.onRequest(app);
