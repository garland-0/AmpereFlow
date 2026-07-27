package com.darth.ampereflow

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlin.math.abs

data class BatteryReading(
    val pct: Float,
    val voltageMv: Int,
    val tempTenths: Int,
    val status: Int,
    val health: Int,
    val plugged: Int,
    val currentMa: Double,
    val chargeCounterUah: Long
) {
    val voltageV: Double get() = voltageMv / 1000.0
    val tempC: Double get() = tempTenths / 10.0
    val wattage: Double get() = abs(currentMa / 1000.0) * voltageV
    val isCharging: Boolean get() = status == BatteryManager.BATTERY_STATUS_CHARGING
}

/** Central place that knows how to read the current battery snapshot from Android's
 *  BatteryManager APIs. Any screen (MainActivity, MetricDetailActivity, the service)
 *  can call BatteryReader.read(context) to get the same live numbers. */
object BatteryReader {

    fun read(context: Context): BatteryReading? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val pct = if (level >= 0 && scale > 0) (level * 100f / scale) else 0f

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentNowUa = try {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } catch (e: Exception) { 0L }
        val chargeCounterUah = try {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } catch (e: Exception) { -1L }

        return BatteryReading(
            pct = pct,
            voltageMv = voltageMv,
            tempTenths = tempTenths,
            status = status,
            health = health,
            plugged = plugged,
            currentMa = currentNowUa / 1000.0,
            chargeCounterUah = chargeCounterUah
        )
    }

    /** Hidden PowerProfile API — works on most OEMs but not guaranteed. */
    fun designCapacityMah(context: Context): Int {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            (method.invoke(powerProfile) as Double).toInt()
        } catch (e: Exception) {
            -1
        }
    }
}
