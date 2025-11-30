package com.phamhuu.photographer.services.android

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TimeService {
    private val dateTimeFormat = SimpleDateFormat(DATE_TIME_FORMAT, Locale.getDefault())
    
    companion object {
        private const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss"
    }
    
    /**
     * Format current date and time
     * @return Formatted date time string (e.g., "25/12/2024 14:30:45")
     */
    fun getCurrentDateTime(): String {
        return dateTimeFormat.format(Date())
    }
    
    /**
     * Format location text with date time
     * @param address The location address
     * @return Formatted string with address and date time on separate lines
     */
    fun formatLocationWithDateTime(address: String): String {
        val dateTime = getCurrentDateTime()
        return "$address\n$dateTime"
    }
}

