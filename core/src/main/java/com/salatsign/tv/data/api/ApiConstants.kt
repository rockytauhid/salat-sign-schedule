// SPDX-License-Identifier: GPL-3.0-or-later
// Copyright (C) 2026 Rocky Adam Tauhid and SalatSign contributors

package com.salatsign.tv.data.api

/**
 * Constants for the SalatSign Local Controller and Remote Management API.
 */
object ApiConstants {
    /**
     * API Protocol Version for remote TV administration and phone controller communication.
     */
    const val API_VERSION: Int = 1

    /**
     * Default network port for the local TV management service (planned).
     */
    const val DEFAULT_HTTP_PORT: Int = 8088

    /**
     * mDNS service discovery type for pairing with phone controllers.
     */
    const val MDNS_SERVICE_TYPE: String = "_salatsign._tcp."
}
