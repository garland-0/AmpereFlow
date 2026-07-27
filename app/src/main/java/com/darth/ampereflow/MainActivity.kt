package com.darth.ampereflow

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private lateinit var gauge: CircularGaugeView
    private lateinit var percentText: TextView
    private lateinit var statusText: TextView
    private lateinit var wattageBadge: TextView
    private lateinit var monitorButton: TextView
    private lateinit var statsContainer: LinearLayout

    private lateinit var voltageValue: TextView
    private lateinit var currentValue: TextView
    private lateinit var wattageValue: TextView
    private lateinit var temperatureValue: TextView
    private lateinit var healthValue: TextView
    private lateinit var pluggedValue: TextView
    private lateinit var capacityValue: TextView
    private lateinit var statusValue: TextView

    private var monitoring = false
    private var lastBatteryIntent: Intent? = null

    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 1000L

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            lastBatteryIntent = intent
        }
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            refreshStats()
            handler.postDelayed(this, pollIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        gauge = findViewById(R.id.gaugeView)
        percentText = findViewById(R.id.percentText)
        statusText = findViewById(R.id.statusText)
        wattageBadge = findViewById(R.id.wattageBadge)
        monitorButton = findViewById(R.id.monitorButton)
        statsContainer = findViewById(R.id.statsContainer)

        buildStatCards()
        buildBottomNav()

        monitorButton.setOnClickListener { toggleMonitoring() }

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStart() {
        super.onStart()
        handler.post(pollRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(pollRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
    }

    private fun buildStatCards() {
        val inflater = LayoutInflater.from(this)
        val rows = listOf(
            listOf("⚡" to "VOLTAGE", "\u3030\uFE0F" to "CURRENT"),
            listOf("\uD83D\uDD0B" to "WATTAGE", "\uD83C\uDF21\uFE0F" to "TEMPERATURE"),
            listOf("\u2764\uFE0F" to "HEALTH", "\uD83D\uDD0C" to "PLUGGED"),
            listOf("\uD83D\uDCDF" to "MAX CAPACITY", "\uD83D\uDCC8" to "CHARGE STATUS")
        )

        val valueRefs = arrayOfNulls<TextView>(8)
        var index = 0

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for ((icon, label) in row) {
                val card = inflater.inflate(R.layout.item_stat_card, rowLayout, false)
                card.findViewById<TextView>(R.id.icon).text = icon
                card.findViewById<TextView>(R.id.label).text = label
                valueRefs[index] = card.findViewById(R.id.value)
                rowLayout.addView(card)
                index++
            }
            statsContainer.addView(rowLayout)
        }

        voltageValue = valueRefs[0]!!
        currentValue = valueRefs[1]!!
        wattageValue = valueRefs[2]!!
        temperatureValue = valueRefs[3]!!
        healthValue = valueRefs[4]!!
        pluggedValue = valueRefs[5]!!
        capacityValue = valueRefs[6]!!
        statusValue = valueRefs[7]!!

        val green = getColorCompat(R.color.accent_green)
        currentValue.setTextColor(green)
        wattageValue.setTextColor(green)
        healthValue.setTextColor(green)
        statusValue.setTextColor(green)
    }

    private fun buildBottomNav() {
        val bottomNav = findViewById<LinearLayout>(R.id.bottomNav)
        val items = listOf(
            "\uD83D\uDD0B" to "Details",
            "\uD83C\uDFA8" to "Design",
            "\uD83D\uDD53" to "History",
            "\uD83D\uDCC8" to "Usage",
            "\u2699\uFE0F" to "Settings"
        )
        items.forEachIndexed { i, pair ->
            val icon = pair.first
            val label = pair.second
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val iconView = TextView(this).apply {
                text = icon
                textSize = 18f
                gravity = Gravity.CENTER
            }
            val labelView = TextView(this).apply {
                text = label
                textSize = 12f
                setTextColor(getColorCompat(if (i == 0) R.color.accent_green else R.color.text_secondary))
                gravity = Gravity.CENTER
                setPadding(0, 4, 0, 0)
            }
            item.addView(iconView)
            item.addView(labelView)
            item.setOnClickListener {
                if (i != 0) Toast.makeText(this, "$label — coming soon", Toast.LENGTH_SHORT).show()
            }
            bottomNav.addView(item)
        }
    }

    private fun getColorCompat(resId: Int) = ContextCompat.getColor(this, resId)

    private fun toggleMonitoring() {
        monitoring = !monitoring
        if (monitoring) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
            monitorButton.text = "\uD83D\uDD0C  Turn off monitoring"
            ContextCompat.startForegroundService(this, Intent(this, BatteryMonitorService::class.java))
        } else {
            monitorButton.text = "\uD83D\uDD0C  Turn on monitoring"
            stopService(Intent(this, BatteryMonitorService::class.java))
        }
    }

    private fun refreshStats() {
        val intent = lastBatteryIntent
            ?: registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val pct = if (level >= 0 && scale > 0) (level * 100f / scale) else 0f

        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)

        val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val currentNowUa = try {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        } catch (e: Exception) { 0L }
        val chargeCounterUah = try {
            bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        } catch (e: Exception) { -1L }

        val currentMa = currentNowUa / 1000.0
        val voltageV = voltageMv / 1000.0
        val wattage = abs(currentMa / 1000.0) * voltageV

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING
        val chargingFast = isCharging && wattage > 10.0

        gauge.setProgress(pct / 100f)
        percentText.text = String.format("%.2f%%", pct)
        statusText.text = when {
            chargingFast -> "FAST CHARGING"
            isCharging -> "CHARGING"
            status == BatteryManager.BATTERY_STATUS_FULL -> "FULLY CHARGED"
            plugged != 0 -> "PLUGGED IN"
            else -> "NOT CHARGING"
        }

        if (isCharging || plugged != 0) {
            wattageBadge.visibility = View.VISIBLE
            wattageBadge.text = "+${String.format("%.0f", wattage * 1000)} mW"
        } else {
            wattageBadge.visibility = View.GONE
        }

        voltageValue.text = "$voltageMv mV"
        val sign = if (currentMa >= 0) "+" else ""
        currentValue.text = "$sign${String.format("%.0f", currentMa)} mA"
        wattageValue.text = "${if (wattage >= 0) "+" else ""}${String.format("%.1f", wattage)} W"
        temperatureValue.text = String.format("%.1f°C", tempTenths / 10.0)

        healthValue.text = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }

        pluggedValue.text = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unplugged"
        }

        val designCapacity = getDesignCapacityMah()
        capacityValue.text = when {
            designCapacity > 0 -> "$designCapacity mAh"
            chargeCounterUah > 0 && pct > 0 -> "${(chargeCounterUah / 1000.0 / (pct / 100.0)).toInt()} mAh (est.)"
            else -> "Unavailable"
        }

        statusValue.text = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
    }

    /** Reads the device's design battery capacity via the hidden PowerProfile API.
     *  Not guaranteed to work on every OEM/Android version — falls back gracefully. */
    private fun getDesignCapacityMah(): Int {
        return try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val powerProfile = constructor.newInstance(this)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            (method.invoke(powerProfile) as Double).toInt()
        } catch (e: Exception) {
            -1
        }
    }
}
