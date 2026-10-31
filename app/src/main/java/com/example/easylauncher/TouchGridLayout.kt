package com.example.easylauncher

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.GridLayout
import kotlin.math.abs

/**
 * A grid that treats the whole surface as one touch area and resolves taps by
 * coordinate, tracking each finger independently.
 *
 * Why: with ordinary child buttons, a thumb resting on the screen (common when
 * the tablet is picked up) can capture the gesture and leave the other buttons
 * unresponsive. Here every pointer is handled separately, so a resting finger is
 * simply ignored and taps anywhere else still register. Hit areas extend into
 * the gaps between buttons so slightly-off taps still land on the nearest button.
 */
class TouchGridLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : GridLayout(context, attrs) {

    /** Invoked when a finger taps within a cell. */
    var onCellTap: ((View) -> Unit)? = null

    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val hitPadding = (resources.displayMetrics.density * 12).toInt()

    private val startX = HashMap<Int, Float>()
    private val startY = HashMap<Int, Float>()
    private val downCell = HashMap<Int, View?>()
    private val moved = HashMap<Int, Boolean>()

    // We own all touch handling; never let children intercept.
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val index = event.actionIndex
                val id = event.getPointerId(index)
                val x = event.getX(index)
                val y = event.getY(index)
                val cell = cellAt(x, y)
                startX[id] = x
                startY[id] = y
                downCell[id] = cell
                moved[id] = false
                cell?.takeIf { it.isClickable }?.isPressed = true
            }

            MotionEvent.ACTION_MOVE -> {
                for (index in 0 until event.pointerCount) {
                    val id = event.getPointerId(index)
                    val sx = startX[id] ?: continue
                    val sy = startY[id] ?: continue
                    if (abs(event.getX(index) - sx) > touchSlop ||
                        abs(event.getY(index) - sy) > touchSlop
                    ) {
                        if (moved[id] != true) {
                            moved[id] = true
                            downCell[id]?.isPressed = false
                        }
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val index = event.actionIndex
                val id = event.getPointerId(index)
                val cell = downCell[id]
                cell?.isPressed = false
                if (cell != null && moved[id] != true) {
                    val x = event.getX(index)
                    val y = event.getY(index)
                    // Only fire if the finger lifted over the same cell it started on.
                    if (cellAt(x, y) === cell) {
                        onCellTap?.invoke(cell)
                    }
                }
                clearPointer(id)
            }

            MotionEvent.ACTION_CANCEL -> {
                downCell.values.forEach { it?.isPressed = false }
                startX.clear(); startY.clear(); downCell.clear(); moved.clear()
            }
        }
        return true
    }

    private fun clearPointer(id: Int) {
        startX.remove(id); startY.remove(id); downCell.remove(id); moved.remove(id)
    }

    /** Returns the cell containing the point, allowing some slack into the gaps. */
    private fun cellAt(x: Float, y: Float): View? {
        val rect = Rect()
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            rect.set(
                child.left - hitPadding,
                child.top - hitPadding,
                child.right + hitPadding,
                child.bottom + hitPadding
            )
            if (rect.contains(x.toInt(), y.toInt())) return child
        }
        return null
    }
}
