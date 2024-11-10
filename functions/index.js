const functions = require("firebase-functions");
const admin = require("firebase-admin");

// Initialize the Firebase Admin SDK if not already initialized
if (admin.apps.length === 0) {
  admin.initializeApp();
}

exports.myFunction = functions.https.onRequest(async (req, res) => {
  functions.logger.log("Function started");

  // Check if it's a GET request
  if (req.method === "GET") {
    const userId = req.query.userId;

    if (!userId) {
      return res.status(400).json({
        error: "Missing userId parameter",
        received: req.query,
      });
    }

    // Access Firestore or Realtime Database
    const db = admin.database(); // For Realtime Database
    // const db = admin.firestore(); // Uncomment for Firestore

    try {
      // Retrieve data for the given userId from the Realtime Database
      const snapshot = await db.ref(`/users/${userId}`).once("value");

      if (!snapshot.exists()) {
        return res.status(404).json({message: "User not found"});
      }

      const userData = snapshot.val();

      // Return the data as JSON
      return res.json({message: "User data retrieved successfully",
        data: userData});
    } catch (error) {
      functions.logger.error("Error retrieving data", error);
      return res.status(500).json({error: "Error retrieving data",
        details: error.message});
    }
  } else {
    return res.status(405).json({error:
      "Method Not Allowed. Only GET is allowed."});
  }
});
