package com.simplycarfleet.authentication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.simplycarfleet.R
import com.simplycarfleet.databinding.FragmentForgotPasswordBinding
import com.simplycarfleet.functions.FunctionsAndValues

class ForgotPasswordFragment : FunctionsAndValues() {
    private var _binding: FragmentForgotPasswordBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentForgotPasswordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        resetPasswordButtonClick()
    }

    private fun resetPasswordButtonClick() {
        binding.buttonResetPassword.setOnClickListener {
            val emailForgotPass = binding.forgotPasswordInput.text?.trim().toString()
            if (emailForgotPass.isEmpty()) {
                Toast.makeText(
                    activity,
                    getString(R.string.forgot_password_email_empty),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                //Wyslij maila resetującego hasło
                FirebaseAuth.getInstance().sendPasswordResetEmail(emailForgotPass)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Snackbar.make(
                                requireView(),
                                getString(R.string.forgot_password_success_message),
                                Snackbar.LENGTH_SHORT
                            )
                                .show()
                            findNavController().navigateUp()
                        }
                    }
                    .addOnFailureListener {
                        Snackbar.make(
                            requireView(),
                            getString(R.string.forgot_password_failure_message),
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                    }
            }
        }
    }
}