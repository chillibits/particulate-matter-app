/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.model

class ScrapingResult {

    // Variables
    val name: String
    val chipID: String
    val ipAddress: String
    val macAddress: String
    val firmwareVersion: String
    val sendToUsEnabled: Boolean

    constructor(id: String, name: String, ipAddress: String, macAddress: String, firmwareVersion: String, sendToUsEnabled: Boolean) {
        this.chipID = id
        this.name = name
        this.ipAddress = ipAddress
        this.macAddress = macAddress
        this.firmwareVersion = firmwareVersion
        this.sendToUsEnabled = sendToUsEnabled
    }
}