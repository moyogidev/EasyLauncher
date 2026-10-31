package com.example.easylauncher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** The home screen. Shows the configured grid of large launch buttons. */
class MainActivity : AppCompatActivity() {

    private lateinit var store: ConfigStore
    private lateinit var grid: TouchGridLayout

    private val handler = Handler(Looper.getMainLooper())

    // While there's a call (ringing/in progress) we drop immersive so the
    // incoming-call answer UI isn't fighting the hidden status-bar region.
    private var inCall = false
    private var phoneListener: PhoneStateListener? = null

    // Refresh badges live while the home screen is visible.
    private val notifListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == ConfigStore.KEY_NOTIFIED_PACKAGES) buildGrid()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applyBars()

        store = ConfigStore(this)
        grid = findViewById(R.id.grid)
        grid.onCellTap = { cell -> onCellTapped(cell) }

        // Settings opens only on a deliberate ~1 second hold of the gear.
        setupGearHold(findViewById(R.id.gear))

        setupCallStateWatch()
    }

    override fun onResume() {
        super.onResume()
        store.registerListener(notifListener)
        buildGrid()
    }

    override fun onPause() {
        super.onPause()
        store.unregisterListener(notifListener)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-apply our preference after a transient swipe or returning from an app.
            applyBars()
        } else {
            // Something else (a dialog, heads-up, the call screen) took over —
            // show the bars so it isn't competing with our hidden-bar regions.
            showSystemBars()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Reflow the grid for the new orientation without recreating the screen.
        buildGrid()
    }

    /** Applies our bar preference: hidden normally, visible during a call. */
    private fun applyBars() {
        if (inCall) showSystemBars() else hideSystemBars()
    }

    /** Hides the status and navigation bars; they reappear transiently on swipe. */
    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /** Shows the status and navigation bars (used during calls / when unfocused). */
    private fun showSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    /** Watches phone call state so the bars return while a call is active. */
    private fun setupCallStateWatch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.READ_PHONE_STATE), REQ_PHONE_STATE)
            return
        }
        registerPhoneListener()
    }

    private fun registerPhoneListener() {
        if (phoneListener != null) return
        val tm = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager ?: return
        val listener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                inCall = state != TelephonyManager.CALL_STATE_IDLE
                applyBars()
            }
        }
        phoneListener = listener
        @Suppress("DEPRECATION")
        tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PHONE_STATE &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            registerPhoneListener()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneListener?.let {
            val tm = getSystemService(TELEPHONY_SERVICE) as? TelephonyManager
            @Suppress("DEPRECATION")
            tm?.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        phoneListener = null
    }

    private fun buildGrid() {
        grid.removeAllViews()

        val buttons = store.getButtons()
        val count = buttons.size

        // Wide grid in landscape, tall grid in portrait.
        val portrait =
            resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        val columns = when {
            portrait -> 2
            count == 8 -> 4
            else -> 3
        }
        val rows = (count + columns - 1) / columns

        grid.columnCount = columns
        grid.rowCount = rows

        val margin = dp(10)
        val notified = store.getNotifiedPackages()

        buttons.forEachIndexed { index, button ->
            val row = index / columns
            val col = index % columns
            val cell = createCell(button, hasNotification(button, notified))

            val params = GridLayout.LayoutParams(
                GridLayout.spec(row, 1, 1f),
                GridLayout.spec(col, 1, 1f)
            ).apply {
                width = 0
                height = 0
                setMargins(margin, margin, margin, margin)
            }
            grid.addView(cell, params)
        }
    }

    private fun createCell(button: LauncherButton, showBadge: Boolean): View {
        val cell = LayoutInflater.from(this).inflate(R.layout.item_button, grid, false)
        val icon = cell.findViewById<ImageView>(R.id.icon)
        val label = cell.findViewById<TextView>(R.id.label)
        val badge = cell.findViewById<View>(R.id.badge)

        if (button.isEmpty) {
            // Unassigned slots are completely blank but still reserve their place
            // in the grid so the other buttons keep their positions.
            cell.visibility = View.INVISIBLE
        } else {
            icon.setImageDrawable(AppLauncher.iconFor(this, button))
            label.text = button.label
            badge.visibility = if (showBadge) View.VISIBLE else View.GONE
        }
        // Touch handling lives in TouchGridLayout; the tag identifies the button.
        cell.tag = button
        return cell
    }

    private fun onCellTapped(cell: View) {
        val button = cell.tag as? LauncherButton ?: return
        if (!button.isEmpty) AppLauncher.launch(this, button)
    }

    /** True if this button's app currently has a notification. */
    private fun hasNotification(button: LauncherButton, notified: Set<String>): Boolean = when {
        button.action == "dial" -> notified.any { it in PHONE_PACKAGES }
        button.action == "url" -> false
        !button.packageName.isNullOrBlank() -> notified.contains(button.packageName)
        else -> false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGearHold(gear: View) {
        val open = Runnable {
            gear.isPressed = false
            openSettings()
        }
        gear.setOnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    v.isPressed = true
                    handler.postDelayed(open, HOLD_MILLIS)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.isPressed = false
                    handler.removeCallbacks(open)
                }
            }
            true
        }
    }

    private fun openSettings() {
        PinManager.verify(this, store) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    /** As the home app, the Back button should stay on the home screen. */
    @Suppress("MissingSuperCall", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // Intentionally do nothing.
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val HOLD_MILLIS = 1000L
        private const val REQ_PHONE_STATE = 101

        // Packages that post missed-call notifications across common devices.
        private val PHONE_PACKAGES = setOf(
            "com.android.server.telecom",
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.samsung.android.incallui",
            "com.samsung.android.app.telephonyui"
        )
    }
}
