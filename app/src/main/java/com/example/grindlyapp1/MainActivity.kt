package com.example.grindlyapp1

import android.content.Context
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
import androidx.customview.widget.ViewDragHelper

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var toggle: ActionBarDrawerToggle
    private var currentMenuItem: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Toolbar & drawer setup
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


        increaseDrawerEdgeSize(drawerLayout, 0.2f)

        // Load user info
        val userType = getUserType()
        navView.menu.clear()
        when (userType.lowercase()) {
            "admin" -> navView.inflateMenu(R.menu.menu_admin)
            "client" -> navView.inflateMenu(R.menu.menu_client)
            else -> navView.inflateMenu(R.menu.menu_hustler)
        }

        // Set header info
        val header = navView.getHeaderView(0)
        header.findViewById<TextView>(R.id.tvUsername).text = getUserName()
        header.findViewById<TextView>(R.id.tvRole).text = userType.replaceFirstChar { it.uppercase() }

        navView.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            when (userType.lowercase()) {
                "admin" -> {
                    openFragment(AdminHomeFragment())
                    setMenuItemChecked(navView.menu.findItem(R.id.navigation_home))
                }
                "client" -> {
                    openFragment(ClientHomeFragment())
                    setMenuItemChecked(navView.menu.findItem(R.id.navigation_home))
                }
                else -> {
                    openFragment(HustlerHomeFragment())
                    setMenuItemChecked(navView.menu.findItem(R.id.navigation_home))
                }
            }
        }
    }

    // Increase drawer swipe area (edge sensitivity)
    private fun increaseDrawerEdgeSize(drawerLayout: DrawerLayout, displayWidthPercentage: Float) {
        try {
            val leftDraggerField = DrawerLayout::class.java.getDeclaredField("mLeftDragger")
            leftDraggerField.isAccessible = true
            val leftDragger = leftDraggerField.get(drawerLayout) as ViewDragHelper

            val edgeSizeField = ViewDragHelper::class.java.getDeclaredField("mEdgeSize")
            edgeSizeField.isAccessible = true

            val displayWidth = resources.displayMetrics.widthPixels
            edgeSizeField.setInt(leftDragger, (displayWidth * displayWidthPercentage).toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Highlight selected item
        setMenuItemChecked(item)

        val userType = getUserType()
        when (item.itemId) {
            R.id.navigation_home -> {
                when (userType.lowercase()) {
                    "admin" -> openFragment(AdminHomeFragment())
                    "client" -> openFragment(ClientHomeFragment())
                    else -> openFragment(HustlerHomeFragment())
                }
            }
            R.id.navigation_favourites -> openFragment(Favourites())
            R.id.navigation_achievements -> openFragment(Achievements())
            R.id.navigation_settings -> openFragment(SettingsFragment())
            R.id.navigation_ratings -> openFragment(RatingsFragment())
            R.id.navigation_report -> openFragment(Report())
            R.id.navigation_package -> openFragment(UpdateServiceStatus())
            R.id.navigation_microacademy -> openFragment(ManageMicroAcademy())
            R.id.navigation_profile -> openFragment(ProfileFragment())
            R.id.navigation_manUsers -> openFragment(ManageUsers())
            R.id.navigation_trackservice -> openFragment(TrackServiceFragment())
            R.id.navigation_package -> openFragment(ServicePackage())
            R.id.navigation_updateservice -> openFragment(UpdateServiceStatus())
            R.id.navigation_services -> openFragment(BrowseServicesFragment())
            R.id.navigation_verification -> openFragment(VerifyDocs())
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setMenuItemChecked(item: MenuItem) {
        currentMenuItem?.isChecked = false
        item.isChecked = true
        currentMenuItem = item
    }

    private fun openFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
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
