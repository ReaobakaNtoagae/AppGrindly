const functions = require("firebase-functions");
const admin = require("firebase-admin");
const express = require("express");
const cors = require("cors");
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");
require("dotenv").config();
const fetch = require("node-fetch");
const { sendNotificationIfEnabled } = require("./notifications");
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

const { sendPushNotification } = require("./fcmHelper");

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
  try {
    const { email, password, fcmToken } = req.body;

    if (!email || !password) return res.status(400).json({ error: "Email and password required" });

    const snapshot = await db.collection("users").where("email", "==", email).get();
    if (snapshot.empty) return res.status(401).json({ error: "Invalid credentials" });

    const userDoc = snapshot.docs[0];
    const user = userDoc.data();

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) return res.status(401).json({ error: "Invalid credentials" });


    if (fcmToken) {
      await db.collection("users").doc(userDoc.id).set({ fcmToken }, { merge: true });
      console.log(`FCM token for user ${userDoc.id} saved/updated.`);
    }




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

    if (!userId) return res.status(400).json({ error: "Missing userId." });

    const parsedRating = rating ? parseFloat(rating) : 0;

    // Fetch user data
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ error: "User not found." });
    }

    const userData = userDoc.data();
    const name = userData.name;
    const phoneNumber = userData.phoneNumber;

    // Construct profile data
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
      verificationStatus: verificationStatus || "unverified",
      rating: parsedRating || "No ratings yet",
      servicePackages:
        Array.isArray(servicePackages) && servicePackages.length > 0
          ? servicePackages
          : "none",
      updatedAt: new Date(),
    };


    await db.collection("profiles").doc(userId).set(profileData, { merge: true });

    const firstWorkImage =
      Array.isArray(workImageURLs) && workImageURLs.length > 0
        ? workImageURLs[0]
        : null;

    const serviceData = {
      hustlerId: userId,
      name,
      title,
      category,
      location,
      price,
      pricingModel,
      profilePictureURL,
      ...(firstWorkImage && { workImageURL: firstWorkImage }),
      rating: parsedRating || "No ratings yet",
    };

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
    const userId = req.user?.userId;

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
    const userId = req.user?.userId;

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
    const clientId = req.user.userId;
    let { hustlerId, serviceId, date, price, location, paymentMethod, notes } = req.body;

    if (!hustlerId && !serviceId) {
      return res.status(400).json({ success: false, message: "Missing hustlerId or serviceId" });
    }

    // Resolve hustlerId from serviceId if needed
    if (serviceId && !hustlerId) {
      const serviceDoc = await db.collection("services").doc(serviceId).get();
      if (!serviceDoc.exists)
        return res.status(404).json({ success: false, message: "Service not found" });
      hustlerId = serviceDoc.data().hustlerId || serviceDoc.id;
    }

    // Resolve serviceId from hustlerId if needed
    if (!serviceId && hustlerId) {
      const serviceSnap = await db.collection("services").where("hustlerId", "==", hustlerId).limit(1).get();
      if (!serviceSnap.empty) serviceId = serviceSnap.docs[0].id;
    }

    if (!date || !price || !location) {
      return res.status(400).json({ success: false, message: "Missing required booking fields" });
    }

    // Get service title
    let serviceTitle = null;
    if (serviceId) {
      const serviceDoc = await db.collection("services").doc(serviceId).get();
      serviceTitle = serviceDoc.exists ? serviceDoc.data().title : null;
    }

    // Get client details
    const userDoc = await db.collection("users").doc(clientId).get();
    if (!userDoc.exists) {
      return res.status(404).json({ success: false, message: "Client user not found" });
    }
    const userData = userDoc.data();
    const clientName = userData.name || "Unknown";

    // Create booking
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

    // 🔔 Send notification to hustler if enabled
    await sendNotificationIfEnabled(
      hustlerId,
      "New Booking Assigned",
      `You have a new booking from ${clientName} on ${date}.`
    );

    return res.status(201).json({
      success: true,
      message: "Booking created successfully",
      bookingId,
      booking: bookingData,
    });
  } catch (err) {
    console.error("BOOKING ERROR:", err);
    res.status(500).json({ success: false, message: err.message });
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
    if (!bookingDoc.exists) return res.status(404).json({ error: "Booking not found" });

    const bookingData = bookingDoc.data();

    // Update status
    await bookingRef.update({
      status,
      updatedAt: FieldValue.serverTimestamp(),
    });

    // Fetch related data
    const [clientDoc, hustlerDoc, serviceDoc] = await Promise.all([
      db.collection("users").doc(bookingData.clientId).get(),
      db.collection("users").doc(bookingData.hustlerId).get(),
      bookingData.serviceId ? db.collection("services").doc(bookingData.serviceId).get() : Promise.resolve(null),
    ]);

    const clientData = clientDoc.exists ? clientDoc.data() : {};
    const hustlerData = hustlerDoc.exists ? hustlerDoc.data() : {};
    const serviceData = serviceDoc && serviceDoc.exists ? serviceDoc.data() : {};


    await Promise.all([
      sendNotificationIfEnabled(
        bookingData.clientId,
        "Booking Status Updated",
        `Your booking status is now: ${status}`
      ),
      sendNotificationIfEnabled(
        bookingData.hustlerId,
        "Booking Status Updated",
        `A booking assigned to you is now: ${status}`
      ),
    ]);

    // Prepare enriched booking object
    const createdAt = bookingData.createdAt?.toDate ? bookingData.createdAt.toDate().toISOString() : null;
    const updatedAt = new Date().toISOString();

    const enrichedBooking = {
      bookingId: bookingDoc.id,
      ...bookingData,
      status,
      createdAt,
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
        title: serviceData.title || bookingData.serviceTitle || "Untitled Service",
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

app.post("/update-fcm-token", async (req, res) => {
  const { userId, token } = req.body;

  console.log("Received FCM update request:", req.body);

  if (!userId || !token) {
    return res.status(400).json({ error: "Missing userId or token" });
  }

  try {
    const userRef = db.collection("users").doc(userId);
    const userDoc = await userRef.get();

    if (!userDoc.exists) {
      console.log(`User ${userId} does not exist. Creating new document with FCM token.`);
      await userRef.set({ fcmToken: token }, { merge: true });
    } else {
      console.log(`User ${userId} exists. Updating FCM token.`);
      await userRef.update({ fcmToken: token });
    }

    const updatedDoc = await userRef.get();
    console.log("Updated user data:", updatedDoc.data());

    return res.status(200).json({ message: "FCM token updated successfully", data: updatedDoc.data() });
  } catch (err) {
    console.error("Error updating FCM token:", err);
    return res.status(500).json({ error: err.message });
  }
});




const { onRequest } = require("firebase-functions/v2/https");
exports.api = onRequest(app);

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