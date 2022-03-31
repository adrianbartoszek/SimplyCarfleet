package com.simplycarfleet.activities

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.simplycarfleet.R
import com.simplycarfleet.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    // Zmienne binding
    private lateinit var binding: ActivityRegisterBinding

    // Zmienne Firebase
    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root

        // Domyślnie widok splash screena
        setContentView(R.layout.splash_screen)

        // Ukrycie ActionBara
        supportActionBar?.hide()

        // Handler który ustawia prawidłowy widok po upływie 2 sekund
        Handler(Looper.getMainLooper()).postDelayed({
            setContentView(view)
        }, 2000)
    }

    override fun onStart() {
        super.onStart()
        isCurrentUser()
    }

    //Jezeli uzytkownik jest zalogowany, przeniesie do MainActivity
    //Jezeli nie jest -> przeniesie do okna logowania/rejestracji
    private fun isCurrentUser() {
        fbAuth.currentUser?.let { _ ->
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)
        }
    }
}