package com.dertefter.wearfiles.common

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.wear.widget.WearableLinearLayoutManager
import kotlin.math.absoluteValue

class CustomScrollingLayoutCallback : WearableLinearLayoutManager.LayoutCallback() {

    override fun onLayoutFinished(child: View, parent: RecyclerView) {
        child.apply {
            val positionOnScreen = (child.y - parent.height / 2.5f).absoluteValue
            val targetBorder = 140
            if (positionOnScreen >= targetBorder){
                child.alpha = 1 - ((positionOnScreen - targetBorder)/200)
                child.scaleX = 1 - ((positionOnScreen - targetBorder)/400)
                child.scaleY = 1 - ((positionOnScreen - targetBorder)/400)
            }else{
                child.alpha = 1f
                child.scaleX = 1f
                child.scaleY = 1f
            }

        }
    }
}