package com.example.grindlyapp1

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
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
        setContentView(R.layout.activity_main) // uses your drawer layout

        // Toolbar setup
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)

        // Drawer toggle
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Set header info
        val header = navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.tvUsername)?.text = getUserName()
        header.findViewById<TextView>(R.id.tvRole)?.text = getUserType().replaceFirstChar { it.uppercase() }

        // Set menu based on user type
        navView.menu.clear()
        when (getUserType().lowercase()) {
            "admin" -> navView.inflateMenu(R.menu.menu_admin)
            "client" -> navView.inflateMenu(R.menu.menu_client)
            else -> navView.inflateMenu(R.menu.menu_hustler)
        }

        navView.setNavigationItemSelectedListener(this)

        // Load default fragment
        if (savedInstanceState == null) {
            val userType = getUserType().lowercase()
            val defaultFragment: Fragment = when (userType) {
                "admin" -> AdminHomeFragment()
                "client" -> ClientHomeFragment()
                else -> HustlerHomeFragment()
            }
            openFragment(defaultFragment)
            navView.menu.findItem(R.id.navigation_home)?.let { setMenuItemChecked(it) }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        setMenuItemChecked(item)

        when (item.itemId) {
            R.id.navigation_home -> openDefaultFragment()
            R.id.navigation_profile -> openFragment(ProfileFragment())
            R.id.createProfile-> startActivity(Intent(this, CreateProfile::class.java))
            R.id.navigation_favourites -> openFragment(Favourites())
            R.id.navigation_achievements -> openFragment(Achievements())
            R.id.navigation_settings -> openFragment(SettingsFragment())
            R.id.navigation_ratings -> openFragment(RatingsFragment())
            R.id.navigation_report -> openFragment(Report())
            R.id.navigation_package -> openFragment(ServicePackage())
            R.id.navigation_updateservice -> openFragment(UpdateServiceStatus())
            R.id.navigation_services -> openFragment(BrowseServicesFragment())
            R.id.navigation_verification -> openFragment(VerifyDocs())
            R.id.navigation_microacademy -> openFragment(ManageMicroAcademy())
            R.id.navigation_manUsers -> openFragment(ManageUsers())
            R.id.navigation_trackservice -> openFragment(TrackServiceFragment())
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun openDefaultFragment() {
        val userType = getUserType().lowercase()
        val fragment: Fragment = when (userType) {
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
