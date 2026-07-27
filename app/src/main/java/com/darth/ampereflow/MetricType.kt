package com.darth.ampereflow

/** Identifies which metric a stat card / detail screen represents. */
enum class MetricType(val label: String, val unit: String) {
    VOLTAGE("Voltage", "mV"),
    CURRENT("Current", "mA"),
    WATTAGE("Wattage", "W"),
    TEMPERATURE("Temperature", "°C");

    companion object {
        const val EXTRA_KEY = "metric_type"
    }
}
