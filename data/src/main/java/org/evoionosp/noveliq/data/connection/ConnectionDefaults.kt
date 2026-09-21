package org.evoionosp.noveliq.data.connection

import android.os.Build

/**
 * Default User-Agent sent when the user has not set a custom one. Mimics a
 * mobile browser so reverse proxies and WAFs that filter unknown agents
 * treat API traffic as ordinary browser traffic. Value adapted from
 * lissen-android (MIT, GrakovNe/lissen-android).
 */
internal fun defaultUserAgent(): String =
    "Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}; K) " +
        "AppleWebKit/537.36 (KHTML, like Gecko) " +
        "Chrome/130.0.6723.106 Mobile Safari/537.36"
