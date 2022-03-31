package com.simplycarfleet.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter
import androidx.fragment.app.commit
import androidx.viewpager.widget.ViewPager
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.simplycarfleet.R
import com.simplycarfleet.authentication.RegisterViewModel
import com.simplycarfleet.data.User
import com.simplycarfleet.databinding.ActivityMainBinding
import com.simplycarfleet.nav_menu.*

class MainActivity : AppCompatActivity() {
    // Zmienne binding
    private lateinit var binding: ActivityMainBinding

    // Zmienne Firebase
    private val fbAuth: FirebaseAuth = FirebaseAuth.getInstance()

    // Zmienne ViewModel
    private val profileVm by viewModels<RegisterViewModel>()

    // Zmienne DEBUG
    private val profileDebug = "PROFILE_DEBUG"

    // Inne zmienne
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val navView: NavigationView = binding.navView
        drawerLayout = binding.drawerLayout
        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        replaceFragment(HomeFragment(), title.toString())

        profileVm.user.observe(this, { user ->
            bindUserData(user)
        })
        navView.setNavigationItemSelectedListener {
            it.isChecked = true

            when (it.itemId) {
                R.id.nav_main -> replaceFragment(HomeFragment(), getString(R.string.main_home_screen_name))
                R.id.nav_cars -> replaceFragment(CarsFragment(), it.title.toString())
                R.id.nav_reminders -> replaceFragment(RemindersFragment(), it.title.toString())
                R.id.nav_reports -> replaceFragment(ReportsFragment(), it.title.toString())
                R.id.nav_settings -> replaceFragment(SettingsFragment(), it.title.toString())
                R.id.nav_help -> replaceFragment(HelpFragment(), it.title.toString())
                R.id.nav_logout -> logOut()
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun bindUserData(user: User) {
        Log.d(profileDebug, user.toString())
        val navigationView: NavigationView = binding.navView
        val headerView: View = navigationView.getHeaderView(0)
        val currentUser: TextView = headerView.findViewById(R.id.current_user)
        currentUser.text = getString(R.string.main_currently_logged_in, user.email)
    }

    private fun replaceFragment(fragment: Fragment, fragmentTitle: String) {
        supportFragmentManager.commit {
            setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
            replace(R.id.FrameLayoutMain, fragment)
            drawerLayout.closeDrawers()
            title = fragmentTitle
        }
    }

    private fun logOut() {
        fbAuth.signOut()
        Toast.makeText(
            this,
            getString(R.string.main_logout_message),
            Toast.LENGTH_SHORT
        ).show()

        val intent = Intent(this, RegisterActivity::class.java).apply {
            flags = (Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }
}