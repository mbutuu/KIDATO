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
    private fun createUserIfMissing() {
        val user = auth.currentUser ?: return
        val ref = db.collection("users").document(user.uid)

        ref.get().addOnSuccessListener { snap ->
            if (!snap.exists()) {
                val data = mapOf(
                    "uid" to user.uid,
                    "email" to user.email,
                    "displayName" to user.displayName,
                    "profileCompleted" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                ref.set(data, SetOptions.merge())
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
            .addSnapshotListener { snap, _ ->
                if (snap != null && snap.exists()) {
                    _profileCompleted.value =
                        snap.getBoolean("profileCompleted") == true
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
    fun saveProfile(
        name: String,
        regNo: String,
        school: String,
        course: String,
        year: String
    ) {
        val user = auth.currentUser ?: return

        val data = mapOf(
            "name" to name,
            "regNo" to regNo,
            "school" to school,
            "course" to course,
            "yearOfStudy" to year,
            "profileCompleted" to true
        )

        db.collection("users")
            .document(user.uid)
            .update(data)
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
