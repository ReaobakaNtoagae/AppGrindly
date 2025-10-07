const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
require("dotenv").config();

admin.initializeApp();
const db = admin.firestore();
const { FieldValue } = require("firebase-admin/firestore");

if (process.env.FUNCTIONS_EMULATOR === "true") {
  process.env.FIRESTORE_EMULATOR_HOST = "localhost:8080";
}

const JWT_SECRET = process.env.JWT_SECRET || "fallback_secret_for_testing";

const app = express();

// ✅ CORS FIX: Allow all origins (or restrict later)
app.use(cors({ origin: true }));

// ✅ Middleware for JSON
app.use(express.json());

// Firestore fix for ignoring undefined fields
db.settings({ ignoreUndefinedProperties: true });

// --------------------
// 🔒 Authentication Middleware
// --------------------
const authenticate = (req, res, next) => {
  const authHeader = req.headers.authorization;
  if (!authHeader)
    return res.status(401).json({ error: "Authorization header missing" });

  const token = authHeader.split(" ")[1];
  try {
    const decoded = jwt.verify(token, JWT_SECRET);
    req.user = decoded;
    next();
  } catch (err) {
    return res.status(401).json({ error: "Invalid or expired token" });
  }
};

// --------------------
// 🧠 Helper Validators
// --------------------
const isStrongPassword = (password) => {
  const regex =
    /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;
  return regex.test(password);
};
const isValidEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
const isValidPhoneNumber = (phone) => /^[0-9]{10,15}$/.test(phone);
const validUserTypes = ["admin", "hustler", "client"];

// --------------------
// 🔹 REGISTER
// --------------------
app.post("/register", async (req, res) => {
  try {
    const { email, password, userType, name, phoneNumber } = req.body;

    if (!email || !password || !userType || !name || !phoneNumber)
      return res.status(400).json({ error: "Missing required fields" });

    if (!isValidEmail(email))
      return res.status(400).json({ error: "Invalid email format" });

    if (!isStrongPassword(password))
      return res.status(400).json({
        error:
          "Password must include uppercase, lowercase, number, and special character",
      });

    if (!validUserTypes.includes(userType.toLowerCase()))
      return res.status(400).json({
        error: `Invalid userType. Must be one of: ${validUserTypes.join(", ")}`,
      });

    const existingSnapshot = await db
      .collection("users")
      .where("email", "==", email)
      .get();

    if (!existingSnapshot.empty)
      return res.status(400).json({ error: "User already exists" });

    const hashedPassword = await bcrypt.hash(password, 10);
    const userRef = db.collection("users").doc();
    const userId = userRef.id;

    await userRef.set({
      email,
      password: hashedPassword,
      userType: userType.toLowerCase(),
      name,
      phoneNumber,
      createdAt: FieldValue.serverTimestamp(),
    });

    const token = jwt.sign(
      { userId, userType: userType.toLowerCase() },
      JWT_SECRET,
      { expiresIn: "1h" }
    );

    return res.status(201).json({
      userId,
      token,
      userType: userType.toLowerCase(),
    });
  } catch (err) {
    console.error("Error in /register:", err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});

// --------------------
// 🔹 LOGIN
// --------------------
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
    console.error("Error in /login:", err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});

const { onRequest } = require("firebase-functions/v2/https");
exports.api = onRequest(
  {
    cors: true,
  },
  app
);
