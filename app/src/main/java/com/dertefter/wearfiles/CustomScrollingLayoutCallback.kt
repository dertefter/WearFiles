package com.dertefter.wearfiles

import android.animation.ObjectAnimator
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import androidx.wear.widget.WearableRecyclerView
import kotlin.math.absoluteValue


class CustomScrollingLayoutCallback : WearableLinearLayoutManager.LayoutCallback() {

    override fun onLayoutFinished(child: View, parent: RecyclerView) {
        child.apply {
            val positionOnScreen = (child.y - parent.height / 2.5f).absoluteValue
            val targetBorder = 130
            if (positionOnScreen >= targetBorder){
                child.alpha = 1 - ((positionOnScreen - targetBorder)/120)
                child.scaleX = 1 - ((positionOnScreen - targetBorder)/300)
                child.scaleY = 1 - ((positionOnScreen - targetBorder)/300)
            }else{
                child.alpha = 1f
                child.scaleX = 1f
                child.scaleY = 1f
            }

        }
    }
}