package com.example.kelkin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.kelkin.utils.UserManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        txtStatus = findViewById(R.id.txtStatus)

        findViewById<View>(R.id.materialCardGoogle).setOnClickListener {
            signInWithGoogle()
        }
    }

    private fun showStatus(message: String) {
        txtStatus.text = message
        txtStatus.visibility = View.VISIBLE
    }

    private fun signInWithGoogle() {
        showStatus(getString(R.string.status_connecting_google))
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(this, gso)
        startActivityForResult(client.signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: Exception) {
                showStatus(getString(R.string.status_google_error, e.message ?: ""))
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        showStatus(getString(R.string.status_validating))
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val credential = GoogleAuthProvider.getCredential(idToken, null)

        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser!!
                UserManager.createUserProfileIfNotExist(user.uid, user.email)
                UserManager.checkUserActivationStatus { isActive ->
                    if (isActive) {
                        showStatus(getString(R.string.status_login_success))
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        showStatus(getString(R.string.status_account_inactive))
                    }
                }
            } else {
                showStatus(getString(R.string.status_firebase_error))
            }
        }
    }
}