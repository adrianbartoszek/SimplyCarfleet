package com.simplycarfleet.authentication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.simplycarfleet.R
import com.simplycarfleet.databinding.FragmentLoginBinding
import com.simplycarfleet.functions.FunctionsAndValues

class LoginFragment : FunctionsAndValues() {
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val loginLog = "LOG_LOGIN"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLoginButtonClick()
        dontHaveAccountButtonClick()
        forgotPasswordButtonClick()
    }

    private fun dontHaveAccountButtonClick() {
        //Przenieś do RegisterFragment
        binding.buttonDontHaveAccount.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToRegisterFragment().actionId)
        }
    }

    private fun forgotPasswordButtonClick() {
        //Przenieś do ForgotPasswordFragment
        binding.buttonForgotPassword.setOnClickListener {
            findNavController().navigate(LoginFragmentDirections.actionLoginFragmentToForgotPasswordFragment().actionId)
        }
    }

    private fun setupLoginButtonClick() {
        binding.buttonLogin.setOnClickListener {
            //Dokonaj logowania
            val email = binding.emailLoginInput.text?.trim().toString()
            val password = binding.passwordLoginInput.text?.trim().toString()
            if (email.isNotEmpty() && password.isNotEmpty()) {
                fbAuth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authRes ->
                        if (authRes.user != null) startApp()
                    }
                    .addOnFailureListener { exc ->
                        Snackbar.make(requireView(),
                            getString(R.string.login_failure_message),
                            Snackbar.LENGTH_SHORT)
                            .show()
                        Log.d(loginLog, exc.message.toString())
                    }
            } else {
                Toast.makeText(
                    activity,
                    getString(R.string.login_email_password_empty),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }
}