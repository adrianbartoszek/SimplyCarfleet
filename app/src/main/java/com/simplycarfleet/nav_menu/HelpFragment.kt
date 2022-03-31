package com.simplycarfleet.nav_menu

import android.content.ActivityNotFoundException
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.simplycarfleet.databinding.FragmentHelpBinding
import android.widget.Toast

import android.content.Intent
import android.net.Uri
import com.simplycarfleet.R
import java.io.File


class HelpFragment : Fragment() {
    private var _binding: FragmentHelpBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fragmentHelpSendEmailButton.setOnClickListener {
            // Wysyłanie maila za pomocą Intent -> otwiera klienta poczty zainstalowanego na komputerze
            val i = Intent(Intent.ACTION_SENDTO).apply {
                // Typ i data, aby można było wybrać tylko klienta poczty, a nie inną aplikację
                type = "message/rfc822"
                data = Uri.parse("mailto:")
                putExtra(
                    Intent.EXTRA_EMAIL,
                    arrayOf(getString(R.string.fragment_help_mail_address))
                )
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fragment_help_mail_subject))
                putExtra(Intent.EXTRA_TEXT, getString(R.string.fragment_help_mail_text))
            }
            try {
                startActivity(Intent.createChooser(i, getString(R.string.fragment_help_mail_send)))
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(
                    activity,
                    getString(R.string.fragment_help_mail_error),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

}