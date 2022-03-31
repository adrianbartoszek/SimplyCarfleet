package com.simplycarfleet.authentication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.simplycarfleet.R
import com.simplycarfleet.databinding.FragmentRegisterBinding
import com.simplycarfleet.functions.FunctionsAndValues

class RegisterFragment : FunctionsAndValues() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val registerLog = "LOG_REGISTER"
    private val regVm: RegisterViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRegisterButtonClick()
        alreadyHaveAnAccountButtonClick()
    }

    private fun setupRegisterButtonClick() {
        //Dokonaj rejestracji
        binding.buttonRegister.setOnClickListener {
            val email = binding.emailRegisterInput.text?.trim().toString()
            val password = binding.passwordRegisterInput.text?.trim().toString()
            val repeatPassword = binding.repeatPasswordRegisterInput.text?.trim().toString()

            if (isPasswordValid(password) && password == repeatPassword) {
                fbAuth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authRes ->
                        if (authRes.user != null) {
                            //zapisz do bazy
                            val user = com.simplycarfleet.data.User(
                                authRes.user!!.email,
                                "",
                                authRes.user!!.uid
                            )
                            regVm.createNewUser(user)
                            startApp()
                        }
                    }
                    .addOnFailureListener { exc ->
                        Snackbar.make(
                            requireView(),
                            getString(R.string.register_failure_message),
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                        Log.d(registerLog, exc.message.toString())
                    }
            } else if (password != repeatPassword) {
                Snackbar.make(
                    requireView(),
                    getString(R.string.register_password_equal),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            } else {
                Snackbar.make(
                    requireView(),
                    getString(R.string.register_password_regex),
                    Snackbar.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun alreadyHaveAnAccountButtonClick() {
        //Przenieś do LoginFragment
        binding.buttonAlreadyHaveAnAccount.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun isPasswordValid(password: String?): Boolean {
        password?.let {
            //minimum 8 znaków
            //musi wystąpić minimum jedna wielka litera
            //musi wystąpić minimum jedna mała litera
            //musi wystąpić minimum jedna cyfra
            //musi wystąpić minimum jeden znak specjalny
            //dodatkowo nie mogą wystąpić białe znaki
            val passwordPattern =
                "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$"
            val passwordMatcher = Regex(passwordPattern)

            return passwordMatcher.find(password) != null
        } ?: return false
    }
}