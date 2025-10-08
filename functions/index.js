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
app.use(cors({ origin: true }));
app.use(express.json());
db.settings({ ignoreUndefinedProperties: true });


const authenticate = (req, res, next) => {
    const authHeader = req.headers.authorization;
    if (!authHeader) {
        console.log("AUTH ERROR: No Authorization header");
        return res.status(401).json({ error: "Authorization header missing" });
    }
    const token = authHeader.split(" ")[1];
    try {
        const decoded = jwt.verify(token, JWT_SECRET);
        req.user = decoded;
        next();
    } catch (err) {
        console.error("AUTH ERROR: Invalid token", err);
        return res.status(401).json({ error: "Invalid or expired token" });
    }
};

const isStrongPassword = (password) => /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/.test(password);
const isValidEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
const isValidPhoneNumber = (phone) => /^[0-9]{10,15}$/.test(phone);
const validUserTypes = ["admin", "hustler", "client"];

// --------------------
// Test endpoint
// --------------------
app.get("/test", async (req, res) => {
    try {
        console.log("Test endpoint hit");
        await db.collection("firestore-test").doc("test").set({ timestamp: FieldValue.serverTimestamp() });
        res.status(200).json({ message: "Test data created successfully" });
    } catch (err) {
        console.error("TEST ERROR:", err);
        res.status(500).json({ error: err.message });
    }
});

// --------------------
// Auth Endpoints
// --------------------
app.post("/register", async (req, res) => {
    console.log("Register endpoint hit:", req.body);
    try {
        const { email, password, userType, name, phoneNumber } = req.body;

        if (!email || !password || !userType || !name || !phoneNumber) {
            console.log("REGISTER VALIDATION FAILED: Missing fields");
            return res.status(400).json({ error: "Missing required fields" });
        }

        if (!isValidEmail(email)) return res.status(400).json({ error: "Invalid email format" });
        if (!isStrongPassword(password)) return res.status(400).json({ error: "Weak password" });
        if (!validUserTypes.includes(userType.toLowerCase())) return res.status(400).json({ error: "Invalid userType" });

        const existingSnapshot = await db.collection("users").where("email", "==", email).get();
        if (!existingSnapshot.empty) return res.status(400).json({ error: "User already exists" });

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

        const token = jwt.sign({ userId, userType: userType.toLowerCase() }, JWT_SECRET, { expiresIn: "1h" });

        console.log("REGISTER SUCCESS:", { userId, userType: userType.toLowerCase() });
        return res.status(201).json({ userId, token, userType: userType.toLowerCase() });
    } catch (err) {
        console.error("REGISTER ERROR:", err);
        return res.status(500).json({ error: err.message });
    }
});

app.post("/login", async (req, res) => {
    console.log("Login endpoint hit:", req.body);
    try {
        const { email, password } = req.body;
        if (!email || !password) return res.status(400).json({ error: "Email and password required" });

        const snapshot = await db.collection("users").where("email", "==", email).get();
        if (snapshot.empty) return res.status(401).json({ error: "Invalid credentials" });

        const userDoc = snapshot.docs[0];
        const user = userDoc.data();
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) return res.status(401).json({ error: "Invalid credentials" });

        const token = jwt.sign({ userId: userDoc.id, userType: user.userType }, JWT_SECRET, { expiresIn: "1h" });
        console.log("LOGIN SUCCESS:", { userId: userDoc.id, userType: user.userType });
        return res.status(200).json({ userId: userDoc.id, token, userType: user.userType });
    } catch (err) {
        console.error("LOGIN ERROR:", err);
        return res.status(500).json({ error: err.message });
    }
});


app.post("/profile", authenticate, async (req, res) => {
    console.log("Profile endpoint hit:", req.body);
    try {
        const { userId, title, category, location, price, pricingModel, description, profilePictureURL, workImageURLs, documentURLs, verifiedBadgeTier, servicePackages, rating } = req.body;
        if (!userId) return res.status(400).json({ error: "Missing userId." });

        const parsedRating = rating ? parseFloat(rating) : 0;
        const docRef = db.collection("profiles").doc(userId);
        const userDoc = await db.collection("users").doc(userId).get();
        if (!userDoc.exists) return res.status(404).json({ error: "User not found." });

        const userData = userDoc.data();
        const name = userData.name;
        const phoneNumber = userData.phoneNumber;

        const profileData = {
            name,
            ...(title && { title }),
            ...(category && { category }),
            ...(location && { location }),
            ...(price && { price }),
            ...(pricingModel && { pricingModel }),
            ...(description && { description }),
            ...(profilePictureURL && { profilePictureURL }),
            ...(Array.isArray(workImageURLs) && { workImageURLs }),
            ...(Array.isArray(documentURLs) && { documentURLs }),
            verifiedBadgeTier: verifiedBadgeTier || "none",
            rating: parsedRating || "No ratings yet",
            servicePackages: Array.isArray(servicePackages) && servicePackages.length > 0 ? servicePackages : "none",
            updatedAt: new Date(),
        };

        await docRef.set(profileData, { merge: true });

        let firstWorkImage = Array.isArray(workImageURLs) && workImageURLs.length > 0 ? workImageURLs[0] : null;
        const serviceData = { name, title, category, location, price, pricingModel, profilePictureURL, ...(firstWorkImage && { workImageURL: firstWorkImage }), rating: parsedRating || "No ratings yet" };
        await db.collection("services").doc(userId).set(serviceData, { merge: true });

        const hustlerData = { name, phoneNumber, ...profileData };
        await db.collection("hustlers").doc(userId).set(hustlerData, { merge: true });

        res.status(200).json({ message: "Profile created/updated successfully" });
    } catch (err) {
        console.error("PROFILE ERROR:", err);
        res.status(500).json({ error: err.message });
    }
});

app.get("/profile/:userId", authenticate, async (req, res) => {
    const { userId } = req.params;
    console.log("Get Profile:", userId);
    try {
        const doc = await db.collection("profiles").doc(userId).get();
        if (!doc.exists) return res.status(404).json({ error: "Profile not found" });
        res.status(200).json(doc.data());
    } catch (err) {
        console.error("PROFILE GET ERROR:", err);
        res.status(500).json({ error: err.message });
    }
});

// --------------------
// Services Endpoints
// --------------------
app.get("/services", async (req, res) => {
    const { search, sort, filterCategory } = req.query;
    console.log("Get Services:", req.query);
    try {
        let query = db.collection("services");
        if (filterCategory) query = query.where("category", "==", filterCategory);
        const servicesSnapshot = await query.get();
        let services = servicesSnapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));

        if (search) {
            const s = search.toLowerCase();
            services = services.filter(svc => svc.title?.toLowerCase().includes(s) || svc.category?.toLowerCase().includes(s) || svc.name?.toLowerCase().includes(s));
        }
        if (sort === "price") services.sort((a, b) => (a.price || 0) - (b.price || 0));
        else if (sort === "rating") services.sort((a, b) => (parseFloat(b.rating) || 0) - (parseFloat(a.rating) || 0));

        res.status(200).json(services);
    } catch (err) {
        console.error("SERVICES ERROR:", err);
        res.status(500).json({ error: err.message });
    }
});

app.get("/services/:id", async (req, res) => {
    const { id } = req.params;
    console.log("Get Service by ID:", id);
    try {
        const doc = await db.collection("services").doc(id).get();
        if (!doc.exists) return res.status(404).json({ error: "Service not found" });

        const hustlerDoc = await db.collection("hustlers").doc(id).get();
        const hustlerData = hustlerDoc.exists ? hustlerDoc.data() : null;

        res.status(200).json({ service: { id: doc.id, ...doc.data() }, hustler: hustlerData });
    } catch (err) {
        console.error("SERVICE DETAIL ERROR:", err);
        res.status(500).json({ error: err.message });
    }
});

// --------------------
// Favourites
// --------------------
app.post("/favourites", authenticate, async (req, res) => {
    const { serviceId } = req.body;
    const userId = req.user.userId;
    console.log("Favourites hit:", { userId, serviceId });

    if (!serviceId) return res.status(400).json({ error: "Missing serviceId" });

    try {
        const favRef = db.collection("users").doc(userId).collection("favourites").doc(serviceId);
        const doc = await favRef.get();

        if (doc.exists) {
            await favRef.delete();
            return res.json({ success: true, message: "Removed from favourites" });
        } else {
            await favRef.set({ timestamp: FieldValue.serverTimestamp() });
            return res.json({ success: true, message: "Added to favourites" });
        }
    } catch (err) {
        console.error("FAVOURITES ERROR:", err);
        res.status(500).json({ error: err.message });
    }
});

app.get("/favourites", authenticate, async (req, res) => {
    const userId = req.user.userId;
    console.log("Get Favourites for:", userId);

    try {
        const favSnapshot = await db.collection("users").doc(userId).collection("favourites").get();
        const favourites = favSnapshot.docs.map(doc => doc.id);
        res.json({ success: true, favourites });
    } catch (err) {
        console.error("GET FAVOURITES ERROR:", err);
        res.status(500).json({ error: err.message });
    }
});

app.post("/reviews", authenticate, async (req, res) => {
    const { serviceId, rating, comment } = req.body;
    const userId = req.user.userId;



    if (!serviceId || rating === undefined)
        return res.status(400).json({ error: "Missing serviceId or rating" });

    const parsedRating = Number(rating);
    if (isNaN(parsedRating) || parsedRating < 1 || parsedRating > 5)
        return res.status(400).json({ error: "Rating must be between 1 and 5" });

    try {
        const reviewRef = db.collection("services").doc(serviceId).collection("reviews").doc(userId);


        const existing = await reviewRef.get();
        if (existing.exists) {
            await reviewRef.update({
                rating: parsedRating,
                comment: comment || "",
                timestamp: FieldValue.serverTimestamp(),
            });
        } else {
            await reviewRef.set({
                userId,
                rating: parsedRating,
                comment: comment || "",
                timestamp: FieldValue.serverTimestamp(),
            });
        }

        // update average
        const reviewsSnap = await db.collection("services").doc(serviceId).collection("reviews").get();
        const allRatings = reviewsSnap.docs.map(d => d.data().rating);
        const avgRating = allRatings.reduce((a, b) => a + b, 0) / allRatings.length;
        const avg = parseFloat(avgRating.toFixed(1));

        await db.collection("services").doc(serviceId).set({ rating: avg }, { merge: true });
        await db.collection("hustlers").doc(serviceId).set({ rating: avg }, { merge: true });

        res.json({ success: true, message: "Review submitted", averageRating: avg });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

app.get("/reviews/:serviceId", async (req, res) => {
    const { serviceId } = req.params;
    try {
        // Get the reviews
        const reviewsSnap = await db.collection("services")
            .doc(serviceId)
            .collection("reviews")
            .orderBy("timestamp", "desc")
            .get();


        const serviceDoc = await db.collection("services").doc(serviceId).get();
        const serviceData = serviceDoc.exists ? serviceDoc.data() : {};
        const averageRating = serviceData.rating || 0;

        const reviews = await Promise.all(
            reviewsSnap.docs.map(async (doc) => {
                const data = doc.data();
                const userDoc = await db.collection("users").doc(data.userId).get();
                const userData = userDoc.exists ? userDoc.data() : {};
                return {
                    id: doc.id,
                    rating: data.rating,
                    comment: data.comment,
                    timestamp: data.timestamp,
                    reviewerName: userData.name || "Anonymous",
                    reviewerType: userData.userType || null,
                };
            })
        );

        res.json({
            success: true,
            averageRating,
            reviewCount: reviews.length,
            reviews
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: "Internal Server Error" });
    }
});


app.post("/user/change-password", authenticate, async (req, res) => {
  try {
    const { userId, oldPassword, newPassword } = req.body;

    if (!userId || !oldPassword || !newPassword) {
      return res.status(400).json({ error: "Missing required fields" });
    }

    if (!isStrongPassword(newPassword)) {
      return res.status(400).json({
        error:
          "New password must be at least 8 characters long, include uppercase, lowercase, number, and special character",
      });
    }

    app.delete("/user/account", authenticate, async (req, res) => {
      try {
        const userId = req.query.userId;
        if (!userId) {
          return res.status(400).json({ error: "Missing userId" });
        }

        // Delete user document
        await db.collection("users").doc(userId).delete();

        // Delete associated profile
        await db.collection("profiles").doc(userId).delete();

        return res.status(200).json({ message: "Account deleted successfully" });
      } catch (err) {
        console.error("Error deleting account:", err);
        return res.status(500).json({ error: "Internal Server Error" });
      }
    });


    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: "User not found" });
    }

    const userData = userDoc.data();
    const isMatch = await bcrypt.compare(oldPassword, userData.password);
    if (!isMatch) {
      return res.status(401).json({ error: "Old password is incorrect" });
    }

    const hashedNewPassword = await bcrypt.hash(newPassword, 10);
    await db.collection("users").doc(userId).update({
      password: hashedNewPassword,
      updatedAt: FieldValue.serverTimestamp(),
    });

    return res.status(200).json({ message: "Password updated successfully" });
  } catch (err) {
    console.error("Error changing password:", err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});


const { onRequest } = require("firebase-functions/v2/https");
exports.api = onRequest(app);
