package com.hungerexpress.config;

import org.springframework.context.annotation.Configuration;

/**
 * Currency Configuration
 * All prices in the system are stored and processed in INR (Indian Rupees)
 */
@Configuration
public class CurrencyConfig {
    
    public static final String CURRENCY_CODE = "INR";
    public static final String CURRENCY_SYMBOL = "₹";
    
    /**
     * Format price for display
     * @param price Price in INR
     * @return Formatted string with currency symbol
     */
    public static String formatPrice(Double price) {
        if (price == null) return CURRENCY_SYMBOL + "0";
        return CURRENCY_SYMBOL + String.format("%.2f", price);
    }
    
    /**
     * Format price without decimals for display
     * @param price Price in INR
     * @return Formatted string with currency symbol, no decimals
     */
    public static String formatPriceNoDecimals(Double price) {
        if (price == null) return CURRENCY_SYMBOL + "0";
        return CURRENCY_SYMBOL + String.format("%.0f", price);
    }
}
