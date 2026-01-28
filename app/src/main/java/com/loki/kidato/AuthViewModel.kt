package com.loki.kidato

import android.app.Application
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.firestore.FieldValue



class AuthViewModel : ViewModel() {

    /* =========================
       AUTH & DATABASE INSTANCES
       ========================= */

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    /* =========================
       AUTH STATE
       ========================= */

    // True if user is logged in
    private val _authState = MutableStateFlow(auth.currentUser != null)
    val authState: StateFlow<Boolean> = _authState

    // Holds auth / Google sign-in errors
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /* =========================
       PROFILE STATE
       ========================= */

    // Firestore listener for profile changes
    private var profileListener: ListenerRegistration? = null

    // True if profileCompleted == true in Firestore
    private val _profileCompleted = MutableStateFlow(false)
    val profileCompleted: StateFlow<Boolean> = _profileCompleted

    /* =========================
       FIRESTORE USER CREATION
       ========================= */

    /**
     * Creates a user document in Firestore
     * ONLY if it does not already exist.
     * Runs silently after login.
     */

    //Expose “isLecturer” to the UI (AuthViewModel)
    private val _role = MutableStateFlow("student")
    val role: StateFlow<String> = _role

    private val _myCourseCodes = MutableStateFlow<List<String>>(emptyList())
    val myCourseCodes: StateFlow<List<String>> = _myCourseCodes


    private fun createUserIfMissing() {
        val user = auth.currentUser ?: return
        val ref = db.collection("users").document(user.uid)

        ref.get().addOnSuccessListener { snap ->
            val updates = mutableMapOf<String, Any?>()
            val defaults = mutableMapOf<String, Any>()

            if (!snap.contains("schoolId")) defaults["schoolId"] = ""
            if (!snap.contains("courseId")) defaults["courseId"] = ""
            if (!snap.contains("year")) defaults["year"] = 0
            if (!snap.contains("semester")) defaults["semester"] = 0
            if (!snap.contains("semesterKey")) defaults["semesterKey"] = ""

            if (defaults.isNotEmpty()) {
                ref.set(defaults, SetOptions.merge())
            }



            if (!snap.exists()) {
                updates["uid"] = user.uid
                updates["email"] = user.email
                updates["displayName"] = user.displayName
                updates["createdAt"] = System.currentTimeMillis()
            }

            //  BACKFILL role for old users
            if (!snap.contains("role")) {
                updates["role"] = "student"
            }

            // BACKFILL profileCompleted if missing
            if (!snap.contains("profileCompleted")) {
                updates["profileCompleted"] = false
            }

            if (updates.isNotEmpty()) {
                ref.set(updates, SetOptions.merge())
            }
        }
    }


    /* =========================
       PROFILE OBSERVER
       ========================= */

    /**
     * Listens to Firestore user document
     * and updates profileCompleted in real time.
     */
    fun observeProfile() {
        val user = auth.currentUser ?: return

        profileListener?.remove()
        profileListener = db.collection("users")
            .document(user.uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    _error.value = e.message
                    return@addSnapshotListener
                }

                if (snap != null && snap.exists()) {
                    _profileCompleted.value = snap.getBoolean("profileCompleted") == true
                    _role.value = snap.getString("role") ?: "student"

                    // OLD system (temporary / optional)
                    _myCourseCodes.value = snap.get("courseCodes") as? List<String> ?: emptyList()

                    // NEW profile fields (for filtering + recommendations)
                    // Keep defaults so old users don’t crash
                    // You can expose these later as StateFlows if you need them in UI.
                    val schoolId = snap.getString("schoolId") ?: ""
                    val courseId = snap.getString("courseId") ?: ""
                    val year = (snap.getLong("year") ?: 0L).toInt()
                    val semester = (snap.getLong("semester") ?: 0L).toInt()
                    val semesterKey = snap.getString("semesterKey") ?: ""

                    android.util.Log.d(
                        "PROFILE",
                        "schoolId=$schoolId courseId=$courseId year=$year semester=$semester semesterKey=$semesterKey"
                    )
                }
            }
    }


    /* =========================
       EMAIL / PASSWORD AUTH
       ========================= */

    fun register(email: String, password: String) {
        _error.value = null
        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                createUserIfMissing()
                observeProfile()
                _authState.value = true
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }

    fun login(email: String, password: String) {
        _error.value = null
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                createUserIfMissing()
                observeProfile()
                _authState.value = true
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }

    /* =========================
       GOOGLE SIGN-IN AUTH
       ========================= */

    fun signInWithGoogle(idToken: String) {
        _error.value = null
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                createUserIfMissing()
                observeProfile()
                _authState.value = true
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }

    /* =========================
       LOGOUT (Firebase + Google)
       ========================= */

    /**
     * Logs out from Firebase AND Google
     * so account chooser appears again.
     */
    fun logout(app: Application) {
        auth.signOut()
        _authState.value = false

        val gso = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .build()

        GoogleSignIn.getClient(app, gso).signOut()
    }

    //saves profile fields
    //flips profileCompleted too true
    //listener auto-updates UI
    //safe to remove this function 28/1/26
    fun saveProfileV2(
        name: String,
        regNo: String,
        schoolId: String,
        courseId: String,
        year: Int,
        semester: Int,
        semesterKey: String,
        profileCompleted: Boolean
    ) {
        val user = auth.currentUser ?: return
        _error.value = null

        val data = mapOf(
            "name" to name,
            "regNo" to regNo,
            "schoolId" to schoolId,
            "courseId" to courseId,
            "year" to year,
            "semester" to semester,
            "semesterKey" to semesterKey,
            "profileCompleted" to profileCompleted
        )

        db.collection("users")
            .document(user.uid)
            .set(data, SetOptions.merge())
            .addOnFailureListener { e -> _error.value = e.message }
    }

    private val storage = FirebaseStorage.getInstance()

    fun uploadFile(
        uri: Uri,
        fileName: String,
        onProgress: (Int) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("Not logged in")
            return
        }

        val ref = storage.reference.child("files/${user.uid}/$fileName")
        val task = ref.putFile(uri)

        task.addOnProgressListener { snap ->
            val pct = ((100.0 * snap.bytesTransferred) / snap.totalByteCount).toInt()
            onProgress(pct)
        }.addOnSuccessListener {
            ref.downloadUrl.addOnSuccessListener { url ->
                onSuccess(url.toString())
            }.addOnFailureListener { e ->
                onError(e.message ?: "Failed to get download URL")
            }
        }.addOnFailureListener { e ->
            onError(e.message ?: "Upload failed")
        }
    }
    fun uploadFileWithMeta(
        uri: android.net.Uri,
        title: String,
        type: String,        // "past_paper" or "marking_scheme"
        courseCode: String,
        unitName: String,
        onProgress: (Int) -> Unit,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onError("Not logged in")
            return
        }

        val safeTitle = title.ifBlank { "file" }
        val storagePath = "files/${user.uid}/$safeTitle"
        val ref = storage.reference.child(storagePath)

        val task = ref.putFile(uri)

        task.addOnProgressListener { snap ->
            val pct = ((100.0 * snap.bytesTransferred) / snap.totalByteCount).toInt()
            onProgress(pct)
        }.addOnSuccessListener { snap ->
            ref.downloadUrl.addOnSuccessListener { url ->
                val meta = hashMapOf(
                    "title" to safeTitle,
                    "type" to type,
                    "courseCode" to courseCode,
                    "unitName" to unitName,
                    "storagePath" to storagePath,
                    "downloadUrl" to url.toString(),
                    "fileName" to ref.name,
                    "sizeBytes" to (snap.metadata?.sizeBytes ?: 0),
                    "uploadedByUid" to user.uid,
                    "uploadedByName" to (user.displayName ?: user.email ?: "Unknown"),
                    "uploadedAt" to FieldValue.serverTimestamp()
                )

                db.collection("files")
                    .add(meta)
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { e ->
                        onError(e.message ?: "Failed to save metadata")
                    }
            }.addOnFailureListener { e ->
                onError(e.message ?: "Failed to get download URL")
            }
        }.addOnFailureListener { e ->
            onError(e.message ?: "Upload failed")
        }
    }

}
