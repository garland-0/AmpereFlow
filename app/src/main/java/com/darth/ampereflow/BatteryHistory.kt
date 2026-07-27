package com.darth.ampereflow

/** Keeps the last few minutes of readings in memory so cards can show a live
 *  sparkline and the detail screen can show a bigger trend graph. Resets when
 *  the app process is killed — this is a lightweight in-memory history, not a
 *  persisted log (see README for how to extend this into real charge-cycle history). */
object BatteryHistory {

    private const val MAX_SAMPLES = 180 // ~3 minutes at 1s polling

    val voltage = ArrayDeque<Float>()
    val current = ArrayDeque<Float>()
    val wattage = ArrayDeque<Float>()
    val temperature = ArrayDeque<Float>()

    fun record(reading: BatteryReading) {
        addBounded(voltage, reading.voltageMv.toFloat())
        addBounded(current, reading.currentMa.toFloat())
        addBounded(wattage, reading.wattage.toFloat())
        addBounded(temperature, reading.tempC.toFloat())
    }

    fun forMetric(type: MetricType): List<Float> = when (type) {
        MetricType.VOLTAGE -> voltage
        MetricType.CURRENT -> current
        MetricType.WATTAGE -> wattage
        MetricType.TEMPERATURE -> temperature
    }

    private fun addBounded(deque: ArrayDeque<Float>, value: Float) {
        deque.addLast(value)
        if (deque.size > MAX_SAMPLES) deque.removeFirst()
    }
}
