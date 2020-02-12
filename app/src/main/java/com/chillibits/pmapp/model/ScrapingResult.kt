/*
 * Copyright © Marc Auberer 2017 - 2020. All rights reserved
 */

package com.chillibits.pmapp.model

class ScrapingResult(
    val chipID: String,
    val name: String,
    val ipAddress: String,
    val macAddress: String,
    val firmwareVersion: String,
    val sendToUsEnabled: Boolean
)