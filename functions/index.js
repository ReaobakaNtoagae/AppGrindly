require("dotenv").config();

const { onRequest } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
const bodyParser = require("body-parser");
const { OAuth2Client } = require("google-auth-library");

const { sendNotificationIfEnabled } = require("./notifications");


if (process.env.FUNCTIONS_EMULATOR === "true") {
    process.env.FIRESTORE_EMULATOR_HOST = "localhost:8080";
}

const jwtSecret = defineSecret("JWT_SECRET");
const webClientId = defineSecret("WEB_CLIENT_ID");

admin.initializeApp();
const db = admin.firestore();
const { FieldValue } = require("firebase-admin/firestore");

const app = express();
app.use(cors());
app.use(bodyParser.json());
db.settings({ ignoreUndefinedProperties: true });


// 🔐 Access secrets from environment
const JWT_SECRET = process.env.JWT_SECRET;
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || "1h";
const WEB_CLIENT_ID = process.env.WEB_CLIENT_ID;

const googleClient = new OAuth2Client(WEB_CLIENT_ID);

function generateToken(email) {
  return jwt.sign({ email }, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN });
}


function authenticate(req, res, next) {
  const header = req.headers["authorization"];
  if (!header) return res.status(401).json({ message: "No token provided" });

  const token = header.split(" ")[1];
  if (!token) return res.status(401).json({ message: "Invalid token format" });

  jwt.verify(token, JWT_SECRET, (err, decoded) => {
    if (err) return res.status(403).json({ message: "Token invalid or expired" });
    req.userId = decoded.email;
    next();
  });
}

async function verifyGoogleIdToken(idToken) {
  const ticket = await googleClient.verifyIdToken({
    idToken,
    audience: WEB_CLIENT_ID,
  });
  return ticket.getPayload();
}

// 🧾 Register
app.post("/register", async (req, res) => {
  try {
    const { email, password, fullName, role, phoneNumber } = req.body;

    if (!email || !password || !fullName || !role || !phoneNumber) {
      return res.status(400).json({ message: "Missing required fields" });
    }

    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return res.status(400).json({ message: "Invalid email format" });
    }

    if (!/^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*?&]).{8,}$/.test(password)) {
      return res.status(400).json({
        message: "Password must be at least 8 characters long and include a letter, number, and special character",
      });
    }

    if (!/^0\d{9}$/.test(phoneNumber)) {
      return res.status(400).json({ message: "Phone number must start with 0 and be exactly 10 digits" });
    }

    const existing = await db.collection("users").doc(email).get();
    if (existing.exists) {
      return res.status(400).json({ message: "Account already exists" });
    }

    const hashedPassword = await bcrypt.hash(password, 12);

    await db.collection("users").doc(email).set({
      email,
      fullName,
      phoneNumber,
      password: hashedPassword,
      role,
      createdAt: FieldValue.serverTimestamp(),
      google: false,
    });

    return res.status(201).json({ message: "Account created successfully" });
  } catch (e) {
    return res.status(500).json({ message: e.message });
  }
});

app.post("/login", async (req, res) => {
  try {
    const { email, password, fcmToken } = req.body;

    if (!email || !password) {
      return res.status(400).json({ message: "Missing credentials" });
    }

    const userRef = db.collection("users").doc(email);
    const userDoc = await userRef.get();
    if (!userDoc.exists) {
      return res.status(404).json({ message: "User not found" });
    }

    const user = userDoc.data();

    if (user.google) {
      return res.status(400).json({ message: "Use Google Sign-In instead" });
    }

    const match = await bcrypt.compare(password, user.password);
    if (!match) {
      return res.status(401).json({ message: "Incorrect password" });
    }

    const token = generateToken(email);

    // Update FCM token in users table
    const updateData = {
      updatedAt: FieldValue.serverTimestamp(),
    };

    if (fcmToken) {
      updateData.fcmToken = fcmToken;
    }

    await userRef.update(updateData);

    return res.json({
      message: "Login successful",
      token,
      role: user.role,
      firstTime: !user.profileCreated,
    });
  } catch (e) {
    console.error("Login error:", e);
    return res.status(500).json({ message: e.message });
  }
});

app.post("/google-login", async (req, res) => {
  try {
    const { idToken, fcmToken } = req.body;

    if (!idToken) {
      return res.status(400).json({ message: "Missing Google ID token" });
    }

    const payload = await verifyGoogleIdToken(idToken);
    const { email, name, picture } = payload;

    if (!email) {
      return res.status(400).json({ message: "Google email missing" });
    }

    const userRef = db.collection("users").doc(email);
    const userDoc = await userRef.get();

    let firstTime = false;
    let userData = {};

    if (!userDoc.exists) {
      firstTime = true;
      userData = {
        email,
        fullName: name || "",
        role: null,
        google: true,
        profileCreated: false,
        createdAt: FieldValue.serverTimestamp(),
        picture,
        ...(fcmToken && { fcmToken: fcmToken }),
        updatedAt: FieldValue.serverTimestamp(),
      };
      await userRef.set(userData);
    } else {
      // Update existing user with FCM token
      const updateData = {
        updatedAt: FieldValue.serverTimestamp(),
      };

      if (fcmToken) {
        updateData.fcmToken = fcmToken;
      }

      await userRef.update(updateData);
      userData = userDoc.data();
    }

    const token = generateToken(email);

    return res.json({
      message: "Google Sign-In successful",
      token,
      firstTime,
      role: userDoc.exists ? userData.role : null,
    });
  } catch (e) {
    console.error("Google login error:", e);
    return res.status(500).json({ message: "Google Sign-In failed", error: e.message });
  }
});


app.post("/set-role", async (req, res) => {
  try {
    const { email, role, phoneNumber } = req.body;

    if (!email || !role) {
      return res.status(400).json({ message: "Missing email or role" });
    }

    const userRef = db.collection("users").doc(email);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      return res.status(404).json({ message: "User not found" });
    }


    const updateData = { role };


    if (role === "hustler") {
      if (!phoneNumber) {
        return res.status(400).json({ message: "Phone number required for hustlers" });
      }
      updateData.phoneNumber = phoneNumber;
    }

    await userRef.update(updateData);

    return res.json({
      message: "Role updated successfully",
      role,
      phoneNumber: phoneNumber || null
    });

  } catch (e) {
    return res.status(500).json({ message: "Failed to update role", error: e.message });
  }
});



// Rewritten /profile endpoint — preserves original logic and adds robust validation and consistency checks.
// Assumes same file context: `db`, `FieldValue`, and `authenticate` are already defined above.
// Rewritten /profile endpoint — preserves original logic and adds robust validation and consistency checks.
// Assumes same file context: `db`, `FieldValue`, and `authenticate` are already defined above.

app.post("/profile", authenticate, async (req, res) => {
  console.log("Profile endpoint hit:", req.body);

  try {
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
      verificationStatus,

      servicePackages,
      rating,
    } = req.body;

    // Basic required param
    if (!userId) return res.status(400).json({ error: "Missing userId." });

    // Fetch user data
    const userRef = db.collection("users").doc(userId);
    const userDoc = await userRef.get();
    if (!userDoc.exists) return res.status(404).json({ error: "User not found." });

    const userData = userDoc.data();

    // Ensure user has a role and is a hustler
    if (!userData.role) {
      return res.status(400).json({ error: "Role must be set before creating a profile." });
    }
    if (userData.role !== "hustler") {
      return res.status(403).json({ error: "Only hustlers can create or update profiles." });
    }

    // Validate required profile fields (keep original behavior but enforce minimum requirements)
    const required = { title, category, location, price, pricingModel, description };
    for (const [key, value] of Object.entries(required)) {
      if (value === undefined || value === null || (typeof value === "string" && value.trim() === "")) {
        return res.status(400).json({ error: `${key} is required.` });
      }
    }

    // Price numeric validation (original code did not validate type)
    const numericPrice = typeof price === "number" ? price : parseFloat(price);
    if (isNaN(numericPrice)) {
      return res.status(400).json({ error: "Price must be a valid number." });
    }

    // Rating handling: keep original idea but make it consistent and safe
    const parsedRating = rating !== undefined && rating !== null && rating !== "" && !isNaN(parseFloat(rating))
      ? parseFloat(rating)
      : null; // null means no rating yet

    // Ensure servicePackages is always an array for front-end consistency
    const normalizedServicePackages = Array.isArray(servicePackages) ? servicePackages : [];

    // Prepare profile data (preserve original optional merging behavior)
    const name = userData.fullName || "";
    const phoneNumber = userData.phoneNumber || null;

    const profileData = {
      name,
      ...(title && { title }),
      ...(category && { category }),
      ...(location && { location }),
      ...(numericPrice !== undefined && { price: numericPrice }),
      ...(pricingModel && { pricingModel }),
      ...(description && { description }),
      ...(profilePictureURL && { profilePictureURL }),
      ...(Array.isArray(workImageURLs) && { workImageURLs }),
      ...(Array.isArray(documentURLs) && { documentURLs }),
      verificationStatus: verificationStatus || "unverified",
      rating: parsedRating !== null ? parsedRating : "No ratings yet",
      servicePackages: normalizedServicePackages,
      hasProfile: true,
      updatedAt: new Date(),
    };

    // Write to profiles collection (merge to preserve existing fields)
    await db.collection("profiles").doc(userId).set(profileData, { merge: true });

    // Prepare service summary data (preserve original fields and merging)
    const firstWorkImage = Array.isArray(workImageURLs) && workImageURLs.length > 0 ? workImageURLs[0] : null;

    const serviceData = {
      hustlerId: userId,
      name,
      title,
      category,
      location,
      price: numericPrice,
      pricingModel,
      profilePictureURL,
      ...(firstWorkImage && { workImageURL: firstWorkImage }),
      rating: parsedRating !== null ? parsedRating : "No ratings yet",
    };

    await db.collection("services").doc(userId).set(serviceData, { merge: true });

    // Prepare hustler entry (keeps same structure as original - note: this duplicates some profile fields)
    const hustlerData = { name, phoneNumber, ...profileData };
    await db.collection("hustlers").doc(userId).set(hustlerData, { merge: true });

    // Mark user's profileCreated flag if not already set (keeps clients' original first-time logic working)
    if (!userData.profileCreated) {
      try {
        await userRef.update({ profileCreated: true });
      } catch (e) {
        // Non-fatal: log but don't block successful response
        console.warn("Failed to update user's profileCreated flag:", e.message);
      }
    }

    return res.status(200).json({ message: "Profile created/updated successfully" });
  } catch (err) {
    console.error("PROFILE ERROR:", err);
    return res.status(500).json({ error: err.message });
  }
});

// Add this as a separate endpoint - make sure it's added to your Express app
app.post("/service-packages", authenticate, async (req, res) => {
  console.log("Service Packages endpoint hit:", req.body);

  try {
    const userId = req.body.userId;
    const servicePackages = req.body.servicePackages;
    const packageStatus = req.body.packageStatus;

    // Basic validation
    if (!userId) {
      return res.status(400).json({ error: "Missing userId." });
    }

    // Fetch user data
    const userRef = db.collection("users").doc(userId);
    const userDoc = await userRef.get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: "User not found." });
    }

    const userData = userDoc.data();

    // Check if user is a hustler
    if (userData.role !== "hustler") {
      return res.status(403).json({ error: "Only hustlers can manage service packages." });
    }

    let normalizedServicePackages = [];

    // Handle skipped packages
    if (packageStatus === "skipped") {
      normalizedServicePackages = [];
      console.log("User skipped service packages");
    }
    // Handle submitted packages
    else if (packageStatus === "submitted" && Array.isArray(servicePackages)) {
      console.log("Processing service packages:", servicePackages);

      // Validate each service package
      for (let i = 0; i < servicePackages.length; i++) {
        const pkg = servicePackages[i];

        // Check for title
        if (!pkg.title || pkg.title.trim() === "") {
          return res.status(400).json({ error: "Service package title is required." });
        }

        // Check for price
        if (pkg.price === undefined || pkg.price === null) {
          return res.status(400).json({ error: "Service package price is required." });
        }

        const priceNum = parseFloat(pkg.price);
        if (isNaN(priceNum)) {
          return res.status(400).json({ error: "Service package price must be a valid number." });
        }

        // Check for services
        if (!pkg.services || pkg.services.trim() === "") {
          return res.status(400).json({ error: "Service package services description is required." });
        }

        // Normalize the package data
        normalizedServicePackages.push({
          title: pkg.title.trim(),
          services: pkg.services.trim(),
          price: priceNum,
          sampleImageURLs: Array.isArray(pkg.sampleImageURLs) ? pkg.sampleImageURLs : []
        });
      }
    } else {
      return res.status(400).json({ error: "Invalid package status or service packages format." });
    }

    // Update profile with service packages
    const profileUpdate = {
      servicePackages: normalizedServicePackages,
      packageStatus: packageStatus,
      updatedAt: new Date()
    };

    await db.collection("profiles").doc(userId).set(profileUpdate, { merge: true });
    await db.collection("hustlers").doc(userId).set(profileUpdate, { merge: true });

    return res.status(200).json({
      message: "Service packages updated successfully",
      servicePackages: normalizedServicePackages,
      packageStatus: packageStatus
    });

  } catch (err) {
    console.error("SERVICE PACKAGES ERROR:", err);
    return res.status(500).json({ error: err.message });
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


app.get("/admin/verifications", authenticate, async (req, res) => {
  try {
    if (req.user.userType !== "admin") {
      return res.status(403).json({ error: "Access denied" });
    }

    const snapshot = await db
      .collection("profiles")
      .where("verificationStatus", "==", "unverified")
      .get();

    if (snapshot.empty) {
      return res.status(200).json({ success: true, hustlers: [] });
    }

    const hustlers = snapshot.docs
      .map((doc) => {
        const data = doc.data();
        return {
          hustlerId: doc.id,
          ...data,
        };
      })

      .filter((hustler) =>
        hustler.documentURLs && Array.isArray(hustler.documentURLs) && hustler.documentURLs.length > 0
      );

    res.status(200).json({ success: true, hustlers });
  } catch (err) {
    console.error("ADMIN VERIFICATION FETCH ERROR:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
});



app.post("/admin/verify-hustler", authenticate, async (req, res) => {
  try {
    if (req.user.userType !== "admin") {
      return res.status(403).json({ error: "Access denied" });
    }

    const { hustlerId, action } = req.body;
    if (!hustlerId || !action)
      return res.status(400).json({ error: "Missing hustlerId or action" });

    if (!["verify", "reject"].includes(action.toLowerCase())) {
      return res.status(400).json({ error: "Invalid action" });
    }

    const hustlerProfileRef = db.collection("profiles").doc(hustlerId);
    const hustlerDoc = await hustlerProfileRef.get();

    if (!hustlerDoc.exists)
      return res.status(404).json({ error: "Hustler profile not found" });

    let verificationStatus;
    if (action === "verify") {
      verificationStatus = "verified";
      await hustlerProfileRef.update({
        verificationStatus: "verified",
        verifiedAt: FieldValue.serverTimestamp(),
      });
      await db.collection("hustlers").doc(hustlerId).update({
        verificationStatus: "verified",
      });
      await db.collection("services").doc(hustlerId).update({
        verificationStatus: "verified",
      });

    }
     else

    {
      verificationStatus = "rejected";
      await hustlerProfileRef.update({
        verificationStatus: "none",
        rejectedAt: FieldValue.serverTimestamp(),
      });
    }

    res.status(200).json({
      success: true,
      message: `Hustler ${action.toUpperCase()} successful.`,
      verificationStatus,
    });
  } catch (err) {
    console.error("ADMIN VERIFICATION ERROR:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
});

app.get("/admin/hustler-docs/:hustlerId", authenticate, async (req, res) => {
  try {
    if (req.user.userType !== "admin") {
      return res.status(403).json({ error: "Access denied" });
    }

    const { hustlerId } = req.params;
    const profileDoc = await db.collection("profiles").doc(hustlerId).get();

    if (!profileDoc.exists) {
      return res.status(404).json({ error: "Hustler profile not found" });
    }

    const profileData = profileDoc.data();
    const documentURLs = Array.isArray(profileData.documentURLs) ? profileData.documentURLs : [];

    res.status(200).json({
      success: true,
      hustlerId,
      documents: documentURLs,
    });
  } catch (err) {
    console.error("FETCH HUSTLER DOCS ERROR:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
});

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
  const serviceId = req.params.id;

  try {
    const serviceDoc = await db.collection("services").doc(serviceId).get();

    if (!serviceDoc.exists) {
      return res.status(404).json({ error: "Service not found" });
    }

    const serviceData = { id: serviceDoc.id, ...serviceDoc.data() };
    let hustlerData = null;

    // ✅ Get hustler details if hustlerId exists
    if (serviceData.hustlerId) {
      const hustlerDoc = await db.collection("hustlers").doc(serviceData.hustlerId).get();

      if (hustlerDoc.exists) {
        hustlerData = { hustlerId: hustlerDoc.id, ...hustlerDoc.data() };
      }
    }

    const response = {
      service: serviceData,
      hustler: hustlerData,
    };

    console.log("Fetched service:", response);
    res.json(response);

  } catch (error) {
    console.error("Error fetching service:", error);
    res.status(500).json({ error: "Failed to fetch service details" });
  }
});




app.post("/favourites", authenticate, async (req, res) => {
    const { serviceId } = req.body;
    const userId = req.userId;

    if (!serviceId) return res.status(400).json({ error: "Missing serviceId" });
    if (!userId) return res.status(401).json({ error: "User not authenticated" });

    try {
        const favRef = db.collection("users").doc(userId).collection("favourites").doc(serviceId);

        // Add or update favourite
        await favRef.set({
            serviceId,
            timestamp: FieldValue.serverTimestamp()
        }, { merge: true });

        return res.json({ success: true, message: "Added to favourites" });
    } catch (err) {
        console.error("ADD FAVOURITE ERROR:", err);
        return res.status(500).json({ error: err.message });
    }
});

// -------------------- REMOVE FAVOURITE --------------------
app.delete("/favourites/:serviceId", authenticate, async (req, res) => {
    const { serviceId } = req.params;
    const userId = req.userId;

    if (!serviceId) return res.status(400).json({ error: "Missing serviceId" });
    if (!userId) return res.status(401).json({ error: "User not authenticated" });

    try {
        const favRef = db.collection("users").doc(userId).collection("favourites").doc(serviceId);
        await favRef.delete();

        return res.json({ success: true, message: "Removed from favourites" });
    } catch (err) {
        console.error("REMOVE FAVOURITE ERROR:", err);
        return res.status(500).json({ error: err.message });
    }
});


app.get("/favourites", authenticate, async (req, res) => {
  const userId = req.user?.userId;

  if (!userId) {
    return res.status(401).json({ error: "User not authenticated" });
  }

  try {
    const favSnapshot = await db
      .collection("users")
      .doc(userId)
      .collection("favourites")
      .get();

    const favourites = await Promise.all(
      favSnapshot.docs.map(async (doc) => {
        const serviceId = doc.id;
        const serviceDoc = await db.collection("services").doc(serviceId).get();

        return {
          serviceId,
          timestamp: doc.data().timestamp,
          service: serviceDoc.exists ? serviceDoc.data() : null,
        };
      })
    );

    return res.json({ success: true, favourites });
  } catch (err) {
    console.error("GET FAVOURITES ERROR:", err);
    return res.status(500).json({ error: err.message });
  }
});

app.post("/reviews", authenticate, async (req, res) => {
  const { serviceId, rating, comment } = req.body;
  const userId = req.userId;

  if (!userId) return res.status(401).json({ error: "User ID missing from token" });
  if (!serviceId || typeof serviceId !== "string" || serviceId.trim() === "")
    return res.status(400).json({ error: "Invalid serviceId" });
  if (rating === undefined) return res.status(400).json({ error: "Missing rating" });

  const parsedRating = Number(rating);
  if (isNaN(parsedRating) || parsedRating < 1 || parsedRating > 5)
    return res.status(400).json({ error: "Rating must be between 1 and 5" });

  try {
    const reviewRef = db.collection("services").doc(serviceId).collection("reviews").doc(userId);
    const reviewData = {
      userId,
      rating: parsedRating,
      comment: comment || "",
      timestamp: FieldValue.serverTimestamp(),
    };

    const existing = await reviewRef.get();
    existing.exists ? await reviewRef.update(reviewData) : await reviewRef.set(reviewData);

    const reviewsSnap = await db.collection("services").doc(serviceId).collection("reviews").get();
    const allRatings = reviewsSnap.docs.map(d => d.data().rating);
    const avgRating = allRatings.reduce((a, b) => a + b, 0) / allRatings.length;
    const avg = parseFloat(avgRating.toFixed(1));

    await db.collection("services").doc(serviceId).set({ rating: avg }, { merge: true });
    await db.collection("hustlers").doc(serviceId).set({ rating: avg }, { merge: true });

    res.json({ success: true, message: "Review submitted", averageRating: avg });
  } catch (err) {
    console.error("Review submission error:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
});

app.get("/reviews/:serviceId", async (req, res) => {
  const { serviceId } = req.params;

  if (!serviceId || typeof serviceId !== "string" || serviceId.trim() === "")
    return res.status(400).json({ error: "Invalid serviceId" });

  try {
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
        const userDoc = data.userId
          ? await db.collection("users").doc(data.userId).get()
          : null;
        const userData = userDoc?.exists ? userDoc.data() : {};
        return {
          id: doc.id,
          rating: data.rating,
          comment: data.comment,
          timestamp: data.timestamp,
          reviewerName: userData.fullName || "Anonymous",
          reviewerType: userData.role || null,
        };
      })
    );

    res.json({
      success: true,
      averageRating,
      reviewCount: reviews.length,
      reviews,
    });
  } catch (err) {
    console.error("Review fetch error:", err);
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

app.delete("/user/account", authenticate, async (req, res) => {
  try {
    const userId = req.query.userId;
    if (!userId) {
      return res.status(400).json({ error: "Missing userId" });
    }


    await db.collection("users").doc(userId).delete();
    await db.collection("profiles").doc(userId).delete();
    await db.collection("services").doc(userId).delete();


    const favouritesSnapshot = await db.collection("users").doc(userId).collection("favourites").get();
    favouritesSnapshot.forEach(async doc => await doc.ref.delete());

    return res.status(200).json({ message: "Account deleted successfully" });
  } catch (err) {
    console.error("Error deleting account:", err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});

// Enable/Disable Notifications
app.post("/user/notifications", authenticate, async (req, res) => {
  try {
    const { enable } = req.body; // boolean
    const userId = req.user.userId;

    if (typeof enable !== "boolean") {
      return res.status(400).json({ error: "Missing or invalid 'enable' field" });
    }

    await db.collection("users").doc(userId).update({
      notificationsEnabled: enable,
      updatedAt: FieldValue.serverTimestamp(),
    });

    return res.json({ success: true, enabled: enable });
  } catch (err) {
    console.error("Notifications toggle error:", err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});


app.post("/user/biometrics", authenticate, async (req, res) => {
  try {
    const { enable } = req.body;
    const userId = req.user.userId;

    if (typeof enable !== "boolean") {
      return res.status(400).json({ error: "Missing or invalid 'enable' field" });
    }

    await db.collection("users").doc(userId).update({
      biometricsEnabled: enable,
      updatedAt: FieldValue.serverTimestamp(),
    });

    return res.json({ success: true, enabled: enable });
  } catch (err) {
    console.error("Biometrics toggle error:", err);
    return res.status(500).json({ error: "Internal Server Error" });
  }
});


app.post("/bookings", authenticate, async (req, res) => {
  try {
    const clientId = req.userId;
    let { hustlerId, serviceId, date, price, location, paymentMethod, notes } = req.body;

    if (!hustlerId && !serviceId) {
      return res.status(400).json({ success: false, message: "Missing hustlerId or serviceId" });
    }

    // 🔍 Resolve hustlerId from serviceId if needed
    if (serviceId && !hustlerId) {
      const serviceDoc = await db.collection("services").doc(serviceId).get();
      if (!serviceDoc.exists)
        return res.status(404).json({ success: false, message: "Service not found" });

      hustlerId = serviceDoc.data().hustlerId || serviceDoc.id;
    }

    // 🔍 Resolve serviceId from hustlerId if needed
    if (!serviceId && hustlerId) {
      const serviceSnap = await db.collection("services")
        .where("hustlerId", "==", hustlerId)
        .limit(1)
        .get();

      if (!serviceSnap.empty) serviceId = serviceSnap.docs[0].id;
    }

    // 🧪 Validate required fields
    if (!date || !price || !location) {
      return res.status(400).json({
        success: false,
        message: "Missing required booking fields",
      });
    }

    // 🧩 Get service title
    let serviceTitle = null;
    if (serviceId) {
      const serviceDoc = await db.collection("services").doc(serviceId).get();
      serviceTitle = serviceDoc.exists ? serviceDoc.data().title : null;
    }

    // 👤 Get client details
    const userDoc = await db.collection("users").doc(clientId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ success: false, message: "Client user not found" });
    }

    const userData = userDoc.data();
    const clientName = userData.fullName || "Unknown";

    // 🏗️ Create booking
    const bookingRef = db.collection("bookings").doc();
    const bookingId = bookingRef.id;

    const bookingData = {
      bookingId,
      clientId,
      clientName,
      hustlerId,
      serviceId: serviceId || null,
      serviceTitle: serviceTitle || null,
      date,
      price,
      location,
      paymentMethod: paymentMethod || "Not specified",
      notes: notes || "",
      status: "Pending",
      timestamp: FieldValue.serverTimestamp(),
    };

    await bookingRef.set(bookingData);

    // 🔔 More descriptive notification to hustler
    await sendNotificationIfEnabled(
      hustlerId,
      "✨ New Booking Request",
      `${clientName} has requested your service "${
        serviceTitle || "Service"
      }" for ${date} at ${location}.`
    );

    return res.status(201).json({
      success: true,
      message: "Booking created successfully",
      bookingId,
      booking: bookingData,
    });

  } catch (err) {
    console.error("BOOKING ERROR:", err);
    return res.status(500).json({ success: false, message: err.message });
  }
});



app.get("/bookings/client/:clientId", authenticate, async (req, res) => {
  const { clientId } = req.params;

  try {
    const snapshot = await db.collection("bookings").where("clientId", "==", clientId).get();

    const bookings = await Promise.all(snapshot.docs.map(async doc => {
      const bookingData = doc.data();

      // Convert timestamps to ISO strings
      const createdAt = bookingData.createdAt?.toDate ? bookingData.createdAt.toDate().toISOString() : null;
      const updatedAt = bookingData.updatedAt?.toDate ? bookingData.updatedAt.toDate().toISOString() : null;

      // Fetch related data
      const [clientDoc, hustlerDoc, serviceDoc] = await Promise.all([
        db.collection("users").doc(bookingData.clientId).get(),
        db.collection("users").doc(bookingData.hustlerId).get(),
        bookingData.serviceId ? db.collection("services").doc(bookingData.serviceId).get() : Promise.resolve(null)
      ]);

      const clientData = clientDoc.exists ? clientDoc.data() : {};
      const hustlerData = hustlerDoc.exists ? hustlerDoc.data() : {};
      const serviceData = serviceDoc && serviceDoc.exists ? serviceDoc.data() : {};

      return {
        bookingId: doc.id,
        ...bookingData,
        createdAt,
        updatedAt,
        client: {
          id: bookingData.clientId,
          name: clientData.name || "Unknown Client",
          phoneNumber: clientData.phoneNumber || "N/A"
        },
        hustler: {
          id: bookingData.hustlerId,
          name: hustlerData.name || "Unknown Hustler",
          phoneNumber: hustlerData.phoneNumber || "N/A",
          rating: hustlerData.rating || "No ratings"
        },
        service: {
          id: bookingData.serviceId,
          title: serviceData.title || bookingData.serviceTitle || "Untitled Service"
        }
      };
    }));

    return res.status(200).json(bookings);

  } catch (err) {
    console.error("BOOKINGS CLIENT ERROR:", err);
    res.status(500).json({ error: err.message });
  }
});


app.get("/bookings/hustler/:hustlerId", authenticate, async (req, res) => {
  const { hustlerId } = req.params;

  try {
    const snapshot = await db.collection("bookings").where("hustlerId", "==", hustlerId).get();

    const bookings = await Promise.all(snapshot.docs.map(async doc => {
      const bookingData = doc.data();

      // Convert timestamps to ISO strings
      const createdAt = bookingData.createdAt?.toDate ? bookingData.createdAt.toDate().toISOString() : null;
      const updatedAt = bookingData.updatedAt?.toDate ? bookingData.updatedAt.toDate().toISOString() : null;

      // Fetch related data
      const [clientDoc, hustlerDoc, serviceDoc] = await Promise.all([
        db.collection("users").doc(bookingData.clientId).get(),
        db.collection("users").doc(bookingData.hustlerId).get(),
        bookingData.serviceId ? db.collection("services").doc(bookingData.serviceId).get() : Promise.resolve(null)
      ]);

      const clientData = clientDoc.exists ? clientDoc.data() : {};
      const hustlerData = hustlerDoc.exists ? hustlerDoc.data() : {};
      const serviceData = serviceDoc && serviceDoc.exists ? serviceDoc.data() : {};

      return {
        bookingId: doc.id,
        ...bookingData,
        createdAt,
        updatedAt,
        client: {
          id: bookingData.clientId,
          name: clientData.name || "Unknown Client",
          phoneNumber: clientData.phoneNumber || "N/A"
        },
        hustler: {
          id: bookingData.hustlerId,
          name: hustlerData.name || "Unknown Hustler",
          phoneNumber: hustlerData.phoneNumber || "N/A",
          rating: hustlerData.rating || "No ratings"
        },
        service: {
          id: bookingData.serviceId,
          title: serviceData.title || bookingData.serviceTitle || "Untitled Service"
        }
      };
    }));

    return res.status(200).json(bookings);

  } catch (err) {
    console.error("BOOKINGS HUSTLER ERROR:", err);
    res.status(500).json({ error: err.message });
  }
});

app.patch("/bookings/:bookingId/status", authenticate, async (req, res) => {
  const { bookingId } = req.params;
  const { status } = req.body;

  if (!status) return res.status(400).json({ error: "Missing status" });

  try {
    const bookingRef = db.collection("bookings").doc(bookingId);
    const bookingDoc = await bookingRef.get();

    if (!bookingDoc.exists)
      return res.status(404).json({ error: "Booking not found" });

    const bookingData = bookingDoc.data();
    const previousStatus = bookingData.status;

    // Update status
    await bookingRef.update({
      status,
      updatedAt: FieldValue.serverTimestamp(),
    });

    // Fetch related data
    const [clientDoc, hustlerDoc, serviceDoc] = await Promise.all([
      db.collection("users").doc(bookingData.clientId).get(),
      db.collection("users").doc(bookingData.hustlerId).get(),
      bookingData.serviceId
        ? db.collection("services").doc(bookingData.serviceId).get()
        : Promise.resolve(null),
    ]);

    const clientData = clientDoc.exists ? clientDoc.data() : {};
    const hustlerData = hustlerDoc.exists ? hustlerDoc.data() : {};
    const serviceData = serviceDoc?.exists ? serviceDoc.data() : {};

    const serviceTitle =
      serviceData.title || bookingData.serviceTitle || "Service";

    const createdAt = bookingData.createdAt?.toDate
      ? bookingData.createdAt.toDate()
      : null;

    const formattedDate = createdAt
      ? createdAt.toLocaleDateString("en-US", {
          year: "numeric",
          month: "short",
          day: "numeric",
        })
      : "Unknown date";

    // ---------------------------------------------
    // 🔔 More Descriptive Notifications
    // ---------------------------------------------

    const clientMessage = `
Your booking for "${serviceTitle}" has been updated.
Status: ${previousStatus} → ${status}
Hustler: ${hustlerData.name || "Unknown Hustler"}
Date: ${formattedDate}
    `.trim();

    const hustlerMessage = `
A booking assigned to you has been updated.
Service: "${serviceTitle}"
Client: ${clientData.name || "Unknown Client"}
Status: ${previousStatus} → ${status}
Date: ${formattedDate}
    `.trim();

    await Promise.all([
      sendNotificationIfEnabled(
        bookingData.clientId,
        "Booking Update",
        clientMessage
      ),
      sendNotificationIfEnabled(
        bookingData.hustlerId,
        "Booking Assigned Update",
        hustlerMessage
      ),
    ]);

    // Prepare enriched booking object
    const updatedAt = new Date().toISOString();

    const enrichedBooking = {
      bookingId: bookingDoc.id,
      ...bookingData,
      status,
      createdAt: createdAt ? createdAt.toISOString() : null,
      updatedAt,
      client: {
        id: bookingData.clientId,
        name: clientData.name || "Unknown Client",
        phoneNumber: clientData.phoneNumber || "N/A",
      },
      hustler: {
        id: bookingData.hustlerId,
        name: hustlerData.name || "Unknown Hustler",
        phoneNumber: hustlerData.phoneNumber || "N/A",
        rating: hustlerData.rating || "No ratings",
      },
      service: {
        id: bookingData.serviceId,
        title: serviceTitle,
      },
    };

    return res.status(200).json({
      message: "Status updated successfully",
      booking: enrichedBooking,
    });

  } catch (err) {
    console.error("❌ UPDATE STATUS ERROR:", err);
    return res.status(500).json({ error: err.message });
  }
});


app.get("/bookings/:bookingId", authenticate, async (req, res) => {
  const { bookingId } = req.params;

  try {
    const bookingDoc = await db.collection("bookings").doc(bookingId).get();

    if (!bookingDoc.exists) {
      return res.status(404).json({ error: "Booking not found" });
    }

    const bookingData = bookingDoc.data();

    const [clientDoc, hustlerDoc, serviceDoc] = await Promise.all([
      db.collection("users").doc(bookingData.clientId).get(),
      db.collection("users").doc(bookingData.hustlerId).get(),
      bookingData.serviceId
        ? db.collection("services").doc(bookingData.serviceId).get()
        : Promise.resolve(null),
    ]);

    const clientData = clientDoc.exists ? clientDoc.data() : {};
    const hustlerData = hustlerDoc.exists ? hustlerDoc.data() : {};
    const serviceData = serviceDoc && serviceDoc.exists ? serviceDoc.data() : {};

    const enrichedBooking = {
      bookingId: bookingDoc.id,
      ...bookingData,
      createdAt: bookingData.createdAt?.toDate ? bookingData.createdAt.toDate().toISOString() : null,
      updatedAt: bookingData.updatedAt?.toDate ? bookingData.updatedAt.toDate().toISOString() : null,
      client: {
        id: bookingData.clientId,
        name: clientData.name || "Unknown Client",
        phoneNumber: clientData.phoneNumber || "N/A",
      },
      hustler: {
        id: bookingData.hustlerId,
        name: hustlerData.name || "Unknown Hustler",
        phoneNumber: hustlerData.phoneNumber || "N/A",
        rating: hustlerData.rating || "No ratings",
      },
      service: {
        id: bookingData.serviceId,
        title: serviceData.title || bookingData.serviceTitle || "Untitled Service",
      },
    };

    return res.status(200).json({ booking: enrichedBooking });
  } catch (err) {
    console.error("FETCH BOOKING ERROR:", err);
    res.status(500).json({ error: err.message });
  }
});

app.post("/update-fcm-token", authenticate, async (req, res) => {
  try {
    const { userId, fcmToken } = req.body;

    if (!userId || !fcmToken) {
      return res.status(400).json({ message: "Missing userId or fcmToken" });
    }

    const userRef = db.collection("users").doc(userId);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      return res.status(404).json({ message: "User not found" });
    }

    await userRef.update({
      fcmToken: fcmToken,
      updatedAt: FieldValue.serverTimestamp(),
    });

    return res.json({ message: "FCM token updated successfully" });
  } catch (e) {
    console.error("FCM token update error:", e);
    return res.status(500).json({ message: e.message });
  }
});




exports.api = onRequest({ secrets: [jwtSecret, webClientId] }, app);

/*
-----------------------------------------
References / Sources:

1. Firebase Functions + Express setup
   - Firebase Docs: https://firebase.google.com/docs/functions/http-events
   - Medium Tutorial: https://dev.to/firebase/creating-a-rest-api-with-firebase-functions-2b4f

2. Firestore Admin SDK usage
   - Firestore Quickstart: https://firebase.google.com/docs/firestore/quickstart
   - CRUD operations in Firestore: https://firebase.google.com/docs/firestore/manage-data/add-data

3. JWT Authentication
   - JWT npm package: https://www.npmjs.com/package/jsonwebtoken
   - DigitalOcean Tutorial: https://www.digitalocean.com/community/tutorials/nodejs-jwt-expressjs

4. Password Hashing with bcryptjs
   - bcryptjs npm package: https://www.npmjs.com/package/bcryptjs
   - Node.js Authentication Tutorial: https://www.toptal.com/nodejs/nodejs-user-authentication-with-jwt

5. CORS + Express Middleware
   - CORS npm package: https://www.npmjs.com/package/cors
   - Express Middleware Docs: https://expressjs.com/en/guide/using-middleware.html

6. Regex for Password, Email, and Phone Validation
   - Password validation: https://stackoverflow.com/questions/19605150/regular-expression-for-password-strength
   - Email regex: https://emailregex.com/
   - Phone number regex patterns (StackOverflow): https://stackoverflow.com/questions/123559/a-comprehensive-regex-for-phone-number-validation

7. User Registration / Login Flow with Firestore + JWT
   - Node.js Firebase Auth Tutorial: https://www.youtube.com/watch?v=UjXR0qYXK5k
   - Building REST API with Firebase Functions: https://medium.com/firebase-tips-tricks/how-to-create-a-rest-api-with-firebase-functions-and-firestore-42330fa93d8d

8. Firestore Subcollections (Favourites / Reviews)
   - Firestore Data Model: https://firebase.google.com/docs/firestore/data-model
   - Nested collections: https://firebase.google.com/docs/firestore/query-data/queries#subcollections

9. Password Change / Delete Account Flow
   - Updating / deleting Firestore documents: https://firebase.google.com/docs/firestore/manage-data/delete-data
   - Secure password update flow (bcrypt + Firestore): https://dev.to/firebase/changing-passwords-in-firebase-1d4h

-----------------------------------------
*/