package com.qivaryn.app.engine

import android.content.Context

/**
 * Keeps a single Python engine bridge alive. Media destinations are supplied
 * by the caller (the UI uses a temporary cache staging folder and then
 * publishes into public Downloads/Qivaryn).
 */
object EngineProvider {
    private var bridge: QivarynEngineBridge? = null

    fun getBridge(context: Context): QivarynEngineBridge {
        return bridge ?: QivarynEngineBridge(context).also { bridge = it }
    }
}
