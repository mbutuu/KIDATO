package com.loki.kidato

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthViewModel : ViewModel() {

    /* =========================
       FIREBASE INSTANCES
       ========================= */
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    /* =========================
       AUTH STATE
       ========================= */

    // True if user is logged in
    private val _authState = MutableStateFlow(auth.currentUser != null)
    val authState: StateFlow<Boolean> = _authState

    // Holds auth errors
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    /* =========================
       PROFILE STATE
       ========================= */

    private var profileListener: ListenerRegistration? = null

    // True if profileCompleted == true in Firestore
    private val _profileCompleted = MutableStateFlow(false)
    val profileCompleted: StateFlow<Boolean> = _profileCompleted

    // Used in HomeScreen drawer header
    private val _profileName = MutableStateFlow<String?>(null)
    val profileName: StateFlow<String?> = _profileName

    // Optional: used if you want role-based UI later
    private val _role = MutableStateFlow("student")
    val role: StateFlow<String> = _role

    /* =========================
       INTERNAL HELPERS
       ========================= */

    /**
     * Create user document in Firestore ONLY if it doesn't exist.
     */
    private fun createUserIfMissing() {
        val user = auth.currentUser ?: return
        val ref = db.collection("users").document(user.uid)

        ref.get()
            .addOnSuccessListener { snap ->
                if (!snap.exists()) {
                    val data = mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: ""),
                        "name" to (user.displayName ?: ""),
                        "role" to "student",
                        "profileCompleted" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                    ref.set(data, SetOptions.merge())
                }
            }
    }

    /**
     * Listen to profile changes and keep local flows updated.
     * Call after login/register.
     */
    fun observeProfile() {
        val user = auth.currentUser ?: return

        profileListener?.remove()
        profileListener = db.collection("users")
            .document(user.uid)
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    _profileCompleted.value = snap.getBoolean("profileCompleted") == true

                    // Prefer Firestore name, fallback to auth displayName/email
                    _profileName.value =
                        snap.getString("name")
                            ?.takeIf { it.isNotBlank() }
                            ?: user.displayName
                                    ?: user.email

                    _role.value = snap.getString("role") ?: "student"
                } else {
                    // If doc missing for some reason, fallback
                    _profileCompleted.value = false
                    _profileName.value = user.displayName ?: user.email
                    _role.value = "student"
                }
            }
    }

    /**
     * Optional: call this once on app start if you want state consistent.
     */
    fun refreshAuthState() {
        val loggedIn = auth.currentUser != null
        _authState.value = loggedIn
        if (loggedIn) {
            createUserIfMissing()
            observeProfile()
        }
    }

    /* =========================
       EMAIL/PASSWORD AUTH
       ========================= */

    fun register(email: String, password: String) {
        _error.value = null

        auth.createUserWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                _authState.value = true
                createUserIfMissing()
                observeProfile()
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }

    fun login(email: String, password: String) {
        _error.value = null

        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener {
                _authState.value = true
                createUserIfMissing()
                observeProfile()
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }

    /* =========================
       GOOGLE AUTH
       ========================= */

    fun signInWithGoogle(idToken: String) {
        _error.value = null

        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener {
                _authState.value = true
                createUserIfMissing()
                observeProfile()
            }
            .addOnFailureListener { e ->
                _error.value = e.message
            }
    }

    /* =========================
       LOGOUT
       ========================= */

    /**
     * Logs out from Firebase and also signs out Google so the chooser appears next time.
     */
    fun logout(app: Application) {
        // Stop listening to user doc
        profileListener?.remove()
        profileListener = null

        auth.signOut()

        // Reset local state
        _authState.value = false
        _profileCompleted.value = false
        _profileName.value = null
        _role.value = "student"

        // Google sign out (safe even if user didn't use Google)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(app, gso).signOut()
    }

    /* =========================
       SAVE PROFILE (DETAILS SCREEN)
       ========================= */

    fun saveProfile(
        name: String,
        regNo: String,
        school: String,
        course: String,
        year: String
    ) {
        val user = auth.currentUser ?: return

        // HARD validation (don't allow blanks)
        if (
            name.isBlank() ||
            regNo.isBlank() ||
            school.isBlank() ||
            course.isBlank() ||
            year.isBlank()
        ) {
            _error.value = "Please fill in all fields."
            return
        }

        val data = mapOf(
            "name" to name.trim(),
            "regNo" to regNo.trim(),
            "school" to school.trim(),
            "course" to course.trim(),
            "yearOfStudy" to year.trim(),
            "profileCompleted" to true,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("users")
            .document(user.uid)
            .set(data, SetOptions.merge())
            .addOnSuccessListener {
                // The snapshot listener will update flows automatically,
                // but we can also update locally for immediate UI response.
                _profileCompleted.value = true
                _profileName.value = name.trim()
            }
            .addOnFailureListener { e ->
                _error.value = e.message ?: "Failed to save profile."
            }
    }

    /* =========================
       FILE UPLOAD (STORAGE + METADATA)
       ========================= */

    /**
     * Simple upload (just gives back download URL).
     */
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

        val safeName = fileName.ifBlank { "file" }
        val ref = storage.reference.child("files/${user.uid}/$safeName")
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

    /**
     * Upload + writes metadata to Firestore "files" collection
     * type = "past_paper" or "marking_scheme"
     */
    fun uploadFileWithMeta(
        uri: Uri,
        title: String,
        type: String,
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

        val safeTitle = title.trim().ifBlank { "file" }

        // If you want safe filenames always, you can replace spaces:
        val safeFilename = safeTitle.replace(Regex("[^a-zA-Z0-9._-]"), "_")

        val storagePath = "files/${user.uid}/$safeFilename"
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
                    "courseCode" to courseCode.trim(),
                    "unitName" to unitName.trim(),
                    "storagePath" to storagePath,
                    "downloadUrl" to url.toString(),
                    "fileName" to ref.name,
                    "sizeBytes" to (snap.metadata?.sizeBytes ?: 0),
                    "uploadedByUid" to user.uid,
                    "uploadedByName" to (_profileName.value ?: user.displayName ?: user.email ?: "Unknown"),
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

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
        profileListener = null
    }
}
