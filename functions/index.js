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

// -------------------
// AUTH MIDDLEWARE
// -------------------
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

// -------------------
// VALIDATION
// -------------------
const isStrongPassword = (password) => {
    const regex =
        /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*(),.?":{}|<>]).{8,}$/;
    return regex.test(password);
};

const isValidEmail = (email) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
const isValidPhoneNumber = (phone) => /^[0-9]{10,15}$/.test(phone);
const validUserTypes = ["admin", "hustler", "client"];

// -------------------
// REGISTER USER
// -------------------
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
                    "Password must be at least 8 characters long, include uppercase, lowercase, number, and special character",
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
            { userId: userId, userType: userType.toLowerCase() },
            JWT_SECRET,
            { expiresIn: "1h" }
        );

        return res.status(201).json({
            userId,
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

// -------------------
// CREATE/UPDATE PROFILE
// -------------------
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
        rating,
    } = req.body;

    if (!userId) return res.status(400).json({ error: "Missing userId." });

    const docRef = db.collection("profiles").doc(userId);

    try {
        const userDoc = await db.collection("users").doc(userId).get();
        if (!userDoc.exists) return res.status(404).json({ error: "User not found." });

        const userData = userDoc.data();
        const name = userData.name;
        const phoneNumber = userData.phoneNumber;

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
            rating: rating || "No ratings yet",
            servicePackages:
                Array.isArray(servicePackages) && servicePackages.length > 0
                    ? servicePackages
                    : "none",
            updatedAt: new Date(),
        };

        await docRef.set(profileData, { merge: true });

        const serviceData = {
            name,
            ...(title && { title }),
            ...(location && { location }),
            ...(category && { category }),
            ...(price && { price }),
            ...(pricingModel && { pricingModel }),
            ...(profilePictureURL && { profilePictureURL }),
            ...(Array.isArray(workImageURLs) && workImageURLs.length > 0 && { workImageURL: workImageURLs[0] }),
            rating: rating || "No ratings yet",
        };

        await db.collection("services").doc(userId).set(serviceData, { merge: true });

        const hustlerData = {
            name,
            phoneNumber,
            ...(title && { title }),
            ...(category && { category }),
            ...(location && { location }),
            ...(price && { price }),
            ...(pricingModel && { pricingModel }),
            ...(description && { description }),
            ...(profilePictureURL && { profilePictureURL }),
            ...(Array.isArray(workImageURLs) && { workImageURLs }),
            verifiedBadgeTier: verifiedBadgeTier || "none",
            rating: rating || "No ratings yet",
            servicePackages:
                Array.isArray(servicePackages) && servicePackages.length > 0
                    ? servicePackages
                    : "none",
        };

        await db.collection("hustlers").doc(userId).set(hustlerData, { merge: true });

        res.status(200).json({ message: "Profile created/updated successfully" });
    } catch (error) {
        console.error("Error creating/updating profile:", error);
        res.status(500).json({ error: error.message });
    }
});

// -------------------
// GET FULL PROFILE
// -------------------
app.get("/profile/:userId", async (req, res) => {
    const { userId } = req.params;

    try {
        const doc = await db.collection("profiles").doc(userId).get();
        if (!doc.exists) return res.status(404).json({ error: "Profile not found" });

        res.status(200).json(doc.data());
    } catch (error) {
        console.error("Error fetching profile:", error);
        res.status(500).json({ error: error.message });
    }
});

// -------------------
// GET PROFILE SUMMARY
// -------------------
app.get("/profile/:userId/summary", async (req, res) => {
    const { userId } = req.params;

    try {
        const doc = await db.collection("profiles").doc(userId).get();
        if (!doc.exists) return res.status(404).json({ error: "Profile not found" });

        const { profilePictureURL, name, workImageURLs, location, category } = doc.data();

        res.status(200).json({
            profilePictureURL: profilePictureURL || null,
            name: name || null,
            workImageURLs: workImageURLs || [],
            location: location || null,
            category: category || null,
        });
    } catch (error) {
        console.error("Error fetching profile summary:", error);
        res.status(500).json({ error: "Failed to fetch profile summary" });
    }
});

// -------------------
// TEST ROUTE
// -------------------
app.post("/test", (req, res) => {
    res.status(200).json({ status: "Function is alive" });
});

// -------------------
// SERVICES ROUTES
// -------------------
app.get("/services", async (req, res) => {
    try {
        const { search, sort, filterCategory } = req.query;

        let query = db.collection("services");
        if (filterCategory) query = query.where("category", "==", filterCategory);

        const servicesSnapshot = await query.get();
        let services = servicesSnapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() }));

        if (search) {
            const s = search.toLowerCase();
            services = services.filter(
                (svc) =>
                    svc.title?.toLowerCase().includes(s) ||
                    svc.category?.toLowerCase().includes(s) ||
                    svc.name?.toLowerCase().includes(s)
            );
        }

        if (sort) {
            if (sort === "price") services.sort((a, b) => (a.price || 0) - (b.price || 0));
            else if (sort === "rating") {
                services.sort((a, b) => {
                    const parseRating = (r) => (r && r !== "No ratings yet" ? parseFloat(r) : 0);
                    return parseRating(b.rating) - parseRating(a.rating);
                });
            }
        }

        res.status(200).json(services);
    } catch (error) {
        console.error("Error fetching services:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

app.get("/services/:id", async (req, res) => {
    const { id } = req.params;

    try {
        const doc = await db.collection("services").doc(id).get();
        if (!doc.exists) return res.status(404).json({ error: "Service not found" });

        const hustlerDoc = await db.collection("hustlers").doc(id).get();
        const hustlerData = hustlerDoc.exists ? hustlerDoc.data() : null;

        res.status(200).json({
            service: { id: doc.id, ...doc.data() },
            hustler: hustlerData,
        });
    } catch (error) {
        console.error("Error fetching service details:", error);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

// -------------------
// FAVOURITES ROUTES
// -------------------
app.post("/favourites", authenticate, async (req, res) => {
    const { serviceId } = req.body;
    const userId = req.user.userId;

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
        console.error(err);
        return res.status(500).json({ error: "Internal Server Error" });
    }
});

app.get("/favourites", authenticate, async (req, res) => {
    const userId = req.user.userId;

    try {
        const favSnapshot = await db.collection("users").doc(userId).collection("favourites").get();
        const favourites = favSnapshot.docs.map(doc => doc.id);
        res.json({ success: true, favourites });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

// -------------------
// REVIEWS ROUTES
// -------------------
app.post("/reviews", authenticate, async (req, res) => {
    const { serviceId, rating, comment } = req.body;
    const userId = req.user.userId;

    if (!serviceId || rating === undefined)
        return res.status(400).json({ error: "Missing serviceId or rating" });

    const parsedRating = Number(rating);
    if (isNaN(parsedRating) || parsedRating < 1 || parsedRating > 5)
        return res.status(400).json({ error: "Rating must be a number between 1 and 5" });

    try {
        const reviewRef = db.collection("services").doc(serviceId).collection("reviews").doc();
        await reviewRef.set({
            userId,
            rating: parsedRating,
            comment: comment || "",
            timestamp: FieldValue.serverTimestamp(),
        });

        res.json({ success: true, message: "Review submitted" });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

app.get("/reviews/:serviceId", async (req, res) => {
    const { serviceId } = req.params;

    try {
        const reviewsSnap = await db.collection("services").doc(serviceId).collection("reviews").get();
        const reviews = reviewsSnap.docs.map(doc => ({ id: doc.id, ...doc.data() }));
        res.json({ success: true, reviews });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: "Internal Server Error" });
    }
});

// -------------------
// EXPORT API
// -------------------
exports.api = functions.https.onRequest(app);

