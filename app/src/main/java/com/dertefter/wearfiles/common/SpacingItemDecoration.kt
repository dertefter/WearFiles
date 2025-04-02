package com.dertefter.wearfiles.common

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.annotation.DimenRes
import androidx.recyclerview.widget.RecyclerView

class SpacingItemDecoration(@DimenRes private val resId: Int, private val context: Context) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val horizontalSpacing = context.resources.getDimensionPixelSize(resId)
        val verticalSpacing = (horizontalSpacing / 2).toInt()
        outRect.set(horizontalSpacing, verticalSpacing, horizontalSpacing, verticalSpacing)
    }
}