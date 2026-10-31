package com.example.easylauncher

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Carer-facing settings: button count, per-button setup, PIN and default-home. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var store: ConfigStore
    private lateinit var slotContainer: LinearLayout
    private lateinit var countGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        store = ConfigStore(this)
        slotContainer = findViewById(R.id.slotContainer)
        countGroup = findViewById(R.id.countGroup)

        // On first ever entry there is no PIN yet — set one so settings are protected.
        if (!store.hasPin()) {
            PinManager.createOrChange(this, store)
        }

        if (store.getCount() == 8) {
            findViewById<android.widget.RadioButton>(R.id.countEight).isChecked = true
        } else {
            findViewById<android.widget.RadioButton>(R.id.countSix).isChecked = true
        }
        countGroup.setOnCheckedChangeListener { _, checkedId ->
            store.setCount(if (checkedId == R.id.countEight) 8 else 6)
            buildSlotRows()
        }

        findViewById<Button>(R.id.setHomeButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        }
        findViewById<Button>(R.id.notifBadgesButton).setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        findViewById<Button>(R.id.changePinButton).setOnClickListener {
            PinManager.createOrChange(this, store)
        }
        findViewById<Button>(R.id.doneButton).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        buildSlotRows()
    }

    private fun buildSlotRows() {
        slotContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        store.getButtons().forEach { button ->
            val row = inflater.inflate(R.layout.item_slot_row, slotContainer, false)
            val icon = row.findViewById<ImageView>(R.id.rowIcon)
            val label = row.findViewById<TextView>(R.id.rowLabel)

            val number = button.slot + 1
            if (button.isEmpty) {
                icon.setImageResource(R.drawable.ic_add)
                label.text = getString(R.string.empty_slot).let { "$number. $it" }
            } else {
                icon.setImageDrawable(AppLauncher.iconFor(this, button))
                label.text = "$number. ${button.label}"
            }

            row.findViewById<Button>(R.id.rowEdit).setOnClickListener {
                startActivity(
                    Intent(this, SlotEditActivity::class.java)
                        .putExtra(SlotEditActivity.EXTRA_SLOT, button.slot)
                )
            }
            slotContainer.addView(row)
        }
    }
}
