package com.invatech.pockit.glance

/*
 * Android AppWidget receiver declared in the manifest. Connects launcher update events to PockitHomeWidget.
 */

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class PockitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PockitHomeWidget()
}
