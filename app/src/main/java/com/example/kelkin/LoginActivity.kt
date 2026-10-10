package com.example.kelkin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kelkin.utils.*
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {
    private lateinit var txtStatus: TextView
    private lateinit var signIn: View
    private lateinit var retry: Button
    private var busy = false
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != RESULT_OK) { busy = false; signIn.isEnabled = true; showStatus("ورود لغو شد؛ دوباره می‌توانید تلاش کنید."); return@registerForActivityResult }
        lifecycleScope.launch {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).awaitResult()
                val token = account.idToken ?: error("Missing token")
                withTimeout(15_000) { FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token, null)).awaitResult() }
                val user = FirebaseAuth.getInstance().currentUser ?: error("No account")
                UserManager.createUserProfileIfNotExist(user.uid, user.email)
                checkActivation()
            } catch (e: TimeoutCancellationException) { showStatus("زمان اتصال تمام شد؛ دوباره تلاش کنید.") }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { showStatus("ورود انجام نشد؛ اتصال اینترنت و حساب Google را بررسی کنید.") }
            finally { busy = false; signIn.isEnabled = true }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if (!BuildConfig.IS_TV) {
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
            val root = findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
                val bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars() or androidx.core.view.WindowInsetsCompat.Type.ime())
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                insets
            }
        }
        txtStatus = findViewById(R.id.txtStatus)
        signIn = findViewById(R.id.materialCardGoogle)
        signIn.setOnClickListener {
            if (busy) return@setOnClickListener
            busy = true; signIn.isEnabled = false
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()
            showStatus("در حال اتصال به Google…")
            launcher.launch(GoogleSignIn.getClient(this, options).signInIntent)
        }
        retry = findViewById(R.id.btnRetryActivation)
        retry.setOnClickListener { if (!busy) lifecycleScope.launch { checkActivation() } }
        signIn.requestFocus()
        if (FirebaseAuth.getInstance().currentUser != null) lifecycleScope.launch { checkActivation() }
    }
    private suspend fun checkActivation() {
        if (FirebaseAuth.getInstance().currentUser == null) { showStatus("ابتدا وارد حساب Google شوید."); return }
        busy = true; retry.isEnabled = false; signIn.isEnabled = false
        try {
            showStatus("در حال بررسی فعال‌سازی…")
            when (UserManager.activation()) {
                UserManager.Activation.ACTIVE -> { startActivity(Intent(this, MainActivity::class.java)); finish() }
                UserManager.Activation.INACTIVE -> showStatus("حساب هنوز فعال نشده است. بعد از فعال‌سازی، «بررسی دوباره» را انتخاب کنید.")
            }
        } catch (e: TimeoutCancellationException) { showStatus("زمان اتصال تمام شد؛ دوباره تلاش کنید.") }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { showStatus("بررسی حساب انجام نشد؛ اینترنت را بررسی کنید و دوباره تلاش کنید.") }
        finally { busy = false; retry.isEnabled = true; signIn.isEnabled = true }
    }
    private fun showStatus(message: String) { txtStatus.text = message; txtStatus.visibility = View.VISIBLE }
}
