package com.example.easylauncher

import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/** Dialog helpers for creating and verifying the settings PIN. */
object PinManager {

    private const val MIN_LENGTH = 4

    private fun pinInput(context: Context): Pair<FrameLayout, EditText> {
        val edit = EditText(context).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 22f
        }
        val pad = (context.resources.displayMetrics.density * 20).toInt()
        val container = FrameLayout(context).apply {
            setPadding(pad, pad / 2, pad, 0)
            addView(edit)
        }
        return container to edit
    }

    /** Verifies the existing PIN (or runs [onSuccess] immediately if none set). */
    fun verify(context: Context, store: ConfigStore, onSuccess: () -> Unit) {
        if (!store.hasPin()) {
            onSuccess()
            return
        }
        val (container, edit) = pinInput(context)
        AlertDialog.Builder(context)
            .setTitle(R.string.enter_pin)
            .setMessage(R.string.enter_pin_message)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (store.checkPin(edit.text.toString())) {
                    onSuccess()
                } else {
                    Toast.makeText(context, R.string.pin_wrong, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Creates (or changes) the PIN, prompting twice for confirmation. */
    fun createOrChange(context: Context, store: ConfigStore, onDone: (() -> Unit)? = null) {
        val (container, edit) = pinInput(context)
        AlertDialog.Builder(context)
            .setTitle(R.string.create_pin)
            .setMessage(R.string.create_pin_message)
            .setView(container)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                val first = edit.text.toString()
                if (first.length < MIN_LENGTH) {
                    Toast.makeText(context, R.string.pin_too_short, Toast.LENGTH_SHORT).show()
                    createOrChange(context, store, onDone)
                } else {
                    confirm(context, store, first, onDone)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }

    private fun confirm(
        context: Context,
        store: ConfigStore,
        first: String,
        onDone: (() -> Unit)?
    ) {
        val (container, edit) = pinInput(context)
        AlertDialog.Builder(context)
            .setTitle(R.string.confirm_pin)
            .setMessage(R.string.confirm_pin_message)
            .setView(container)
            .setCancelable(false)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (edit.text.toString() == first) {
                    store.setPin(first)
                    onDone?.invoke()
                } else {
                    Toast.makeText(context, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                    createOrChange(context, store, onDone)
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> }
            .show()
    }
}
