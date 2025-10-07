package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private var currentMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val header = navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.tvUsername)?.text = getUserName()
        header.findViewById<TextView>(R.id.tvRole)?.text = getUserType().replaceFirstChar { it.uppercase() }

        navView.menu.clear()
        when (getUserType().lowercase()) {
            "admin" -> navView.inflateMenu(R.menu.menu_admin)
            "client" -> navView.inflateMenu(R.menu.menu_client)
            else -> navView.inflateMenu(R.menu.menu_hustler)
        }

        navView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            openDefaultFragment()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setMenuItemChecked(item)

        when (item.itemId) {
            R.id.navigation_home, R.id.nav_home -> openDefaultFragment()
            R.id.navigation_profile, R.id.profileFragment -> openFragment(ProfileFragment())
            R.id.createProfile -> startActivity(Intent(this, CreateProfile::class.java))
            R.id.navigation_favourites, R.id.favouritesFragment -> openFragment(Favourites())
            R.id.navigation_achievements, R.id.achievementsFragment -> openFragment(Achievements())
            R.id.navigation_settings, R.id.settingsFragment -> openFragment(SettingsFragment())
            R.id.navigation_ratings, R.id.ratingsFragment -> openFragment(RatingsFragment())
            R.id.navigation_report, R.id.reportFragment -> openFragment(Report())
            R.id.navigation_updateservice, R.id.updateServiceStatusFragment -> openFragment(UpdateServiceStatus())
            R.id.navigation_services, R.id.browseServicesFragment -> openFragment(BrowseServicesFragment())
            R.id.navigation_verification, R.id.verifyDocsFragment -> openFragment(VerifyDocs())
            R.id.navigation_microacademy, R.id.manageMicroAcademyFragment -> openFragment(ManageMicroAcademy())
            R.id.navigation_manUsers, R.id.manageUsersFragment -> openFragment(ManageUsers())
            R.id.navigation_trackservice, R.id.trackServiceFragment -> openFragment(TrackServiceFragment())
            else -> {
                Log.w("NAV", "Unhandled menu item: ${item.itemId}")
                Toast.makeText(this, "Feature not yet implemented", Toast.LENGTH_SHORT).show()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openDefaultFragment() {
        val fragment: Fragment = when (getUserType().lowercase()) {
            "admin" -> AdminHomeFragment()
            "client" -> ClientHomeFragment()
            else -> HustlerHomeFragment()
        }
        openFragment(fragment)
    }

    private fun setMenuItemChecked(item: MenuItem) {
        currentMenuItem?.isChecked = false
        item.isChecked = true
        currentMenuItem = item
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun getUserType(): String {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("USER_TYPE", "guest") ?: "guest"
    }

    private fun getUserName(): String {
        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return prefs.getString("USER_NAME", "Guest") ?: "Guest"
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
