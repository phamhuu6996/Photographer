package com.phamhuu.photographer.presentation.common

object AddressTextUtils {
    
    /**
     * Wraps text to fit within specified width, matching the logic used in PhotoCaptureService
     */
    fun wrapTextForPreview(text: String, maxCharactersPerLine: Int = 25): String {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            
            if (testLine.length <= maxCharactersPerLine) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine)
                    currentLine = word
                } else {
                    // Word is too long, add it anyway
                    lines.add(word)
                }
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        // Limit to max lines and join with newlines
        return lines.take(AddressOverlayConstants.MAX_LINES).joinToString("\n")
    }
    
    /**
     * Formats address consistently for both preview and output
     */
    fun formatAddress(address: String): String {
        // Remove extra spaces and normalize
        return address.trim().replace(Regex("\\s+"), " ")
    }
}
