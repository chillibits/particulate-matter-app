/*
 * Copyright © Marc Auberer 2020. All rights reserved
 */

package com.chillibits.pmapp.model

class ScrapingResult {

    // Variables
    val name: String
    val chipID: String
    val macAddress: String
    val sendToUsEnabled: Boolean

    constructor(id: String, name: String, macAddress: String, sendToUsEnabled: Boolean) {
        this.chipID = id
        this.name = name
        this.macAddress = macAddress
        this.sendToUsEnabled = sendToUsEnabled
    }
}