package com.example.easylauncher

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/** Configures a single launch button: a preset, or any installed app, plus icon. */
class SlotEditActivity : AppCompatActivity() {

    private lateinit var store: ConfigStore
    private var slot: Int = 0
    private var draft: LauncherButton = LauncherButton(0)

    private lateinit var previewIcon: ImageView
    private lateinit var previewLabel: TextView
    private lateinit var labelInput: EditText

    private val pickApp = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pkg = result.data?.getStringExtra(AppPickerActivity.RESULT_PACKAGE)
            val name = result.data?.getStringExtra(AppPickerActivity.RESULT_LABEL) ?: ""
            if (!pkg.isNullOrEmpty()) {
                draft = draft.copy(
                    packageName = pkg,
                    action = "",
                    iconKey = null,
                    label = name,
                    data = null
                )
                labelInput.setText(name)
                updatePreview()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_slot_edit)

        store = ConfigStore(this)
        slot = intent.getIntExtra(EXTRA_SLOT, 0)
        draft = store.getButton(slot)

        previewIcon = findViewById(R.id.previewIcon)
        previewLabel = findViewById(R.id.previewLabel)
        labelInput = findViewById(R.id.labelInput)

        labelInput.setText(draft.label)
        labelInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                previewLabel.text = s?.toString().orEmpty()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        buildPresetChips()

        findViewById<Button>(R.id.pickAppButton).setOnClickListener {
            pickApp.launch(android.content.Intent(this, AppPickerActivity::class.java))
        }
        findViewById<Button>(R.id.webLinkButton).setOnClickListener { showWebLinkDialog() }
        findViewById<Button>(R.id.youtubeButton).setOnClickListener { showYouTubeDialog() }
        findViewById<Button>(R.id.useAppIconButton).setOnClickListener {
            draft = draft.copy(iconKey = null)
            updatePreview()
        }
        findViewById<Button>(R.id.useGameIconButton).setOnClickListener {
            draft = draft.copy(iconKey = "game")
            updatePreview()
        }
        findViewById<Button>(R.id.clearButton).setOnClickListener {
            store.saveButton(LauncherButton(slot))
            finish()
        }
        findViewById<Button>(R.id.saveButton).setOnClickListener {
            store.saveButton(draft.copy(label = labelInput.text.toString().trim()))
            finish()
        }

        updatePreview()
    }

    private fun buildPresetChips() {
        val group = findViewById<ChipGroup>(R.id.presetGroup)
        Presets.ALL.forEach { preset ->
            val chip = Chip(this).apply {
                text = preset.label
                isCheckable = false
                isClickable = true
                chipIcon = IconCatalog.res(preset.iconKey)?.let {
                    androidx.core.content.ContextCompat.getDrawable(this@SlotEditActivity, it)
                }
                isChipIconVisible = true
                setOnClickListener {
                    draft = LauncherButton(
                        slot = slot,
                        label = preset.label,
                        iconKey = preset.iconKey,
                        action = preset.action,
                        packageName = preset.packageName,
                        data = preset.data
                    )
                    labelInput.setText(preset.label)
                    updatePreview()
                }
            }
            group.addView(chip)
        }
    }

    private fun showWebLinkDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(if (draft.action == "url") draft.data else Presets.WORDLE_URL)
            setSelection(text.length)
        }
        val pad = dp(20)
        val container = android.widget.FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.web_link_title)
            .setMessage(R.string.web_link_message)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                var url = input.text.toString().trim()
                if (url.isEmpty()) return@setPositiveButton
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                val suggested = if (url.contains("wordle", ignoreCase = true)) "Wordle" else ""
                draft = draft.copy(
                    action = "url",
                    data = url,
                    packageName = Presets.CHROME_PACKAGE,
                    iconKey = if (url.contains("wordle", ignoreCase = true)) "wordle" else "web",
                    label = if (draft.label.isBlank()) suggested else draft.label
                )
                if (labelInput.text.isBlank() && suggested.isNotEmpty()) {
                    labelInput.setText(suggested)
                }
                updatePreview()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showYouTubeDialog() {
        val input = android.widget.EditText(this).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            setText(if (draft.packageName == Presets.YOUTUBE_PACKAGE) draft.data else "")
            hint = "https://www.youtube.com/watch?v=…"
            setSelection(text.length)
        }
        val pad = dp(20)
        val container = android.widget.FrameLayout(this).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.youtube_link_title)
            .setMessage(R.string.youtube_link_message)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                var url = input.text.toString().trim()
                if (url.isEmpty()) return@setPositiveButton
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    url = "https://$url"
                }
                draft = draft.copy(
                    action = "url",
                    data = url,
                    packageName = Presets.YOUTUBE_PACKAGE,
                    iconKey = "youtube"
                )
                updatePreview()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun updatePreview() {
        val current = draft.copy(label = labelInput.text.toString())
        val drawable = AppLauncher.iconFor(this, current)
        if (drawable != null) {
            previewIcon.setImageDrawable(drawable)
        } else {
            previewIcon.setImageResource(R.drawable.ic_add)
        }
        previewLabel.text = current.label
    }

    companion object {
        const val EXTRA_SLOT = "slot"
    }
}
