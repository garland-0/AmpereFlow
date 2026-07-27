package com.darth.ampereflow

import android.Manifest
import android.content.Context
import android.content.Intent
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

    private lateinit var currentSparkline: LineChartView
    private lateinit var wattageSparkline: LineChartView

    private lateinit var batteryLifeValue: TextView
    private lateinit var batteryLifeSubtitle: TextView
    private lateinit var batteryHealthValue: TextView
    private lateinit var batteryHealthSubtitle: TextView

    private var monitoring = false

    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 1000L

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
        batteryLifeValue = findViewById(R.id.batteryLifeValue)
        batteryLifeSubtitle = findViewById(R.id.batteryLifeSubtitle)
        batteryHealthValue = findViewById(R.id.batteryHealthValue)
        batteryHealthSubtitle = findViewById(R.id.batteryHealthSubtitle)

        buildStatCards()
        buildBottomNav()

        monitorButton.setOnClickListener { toggleMonitoring() }
    }

    override fun onStart() {
        super.onStart()
        handler.post(pollRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(pollRunnable)
    }

    private fun buildStatCards() {
        val inflater = LayoutInflater.from(this)
        val rows = listOf(
            listOf(MetricType.VOLTAGE to "⚡", MetricType.CURRENT to "\u3030\uFE0F"),
            listOf(MetricType.WATTAGE to "\uD83D\uDD0B", MetricType.TEMPERATURE to "\uD83C\uDF21\uFE0F")
        )
        val extraRows = listOf(
            listOf("\u2764\uFE0F" to "HEALTH", "\uD83D\uDD0C" to "PLUGGED"),
            listOf("\uD83D\uDCDF" to "MAX CAPACITY", "\uD83D\uDCC8" to "CHARGE STATUS")
        )

        val metricValueRefs = arrayOfNulls<TextView>(4)
        val metricSparklineRefs = arrayOfNulls<LineChartView>(4)

        // Rows 1-2: tappable metric cards (Voltage, Current, Wattage, Temperature)
        var mi = 0
        for (row in rows) {
            val rowLayout = newRow()
            for ((metric, icon) in row) {
                val card = inflater.inflate(R.layout.item_stat_card, rowLayout, false)
                card.findViewById<TextView>(R.id.icon).text = icon
                card.findViewById<TextView>(R.id.label).text = "${metric.label.uppercase()}"
                metricValueRefs[mi] = card.findViewById(R.id.value)
                metricSparklineRefs[mi] = card.findViewById(R.id.sparkline)
                card.setOnClickListener {
                    val intent = Intent(this, MetricDetailActivity::class.java)
                    intent.putExtra(MetricType.EXTRA_KEY, metric.name)
                    startActivity(intent)
                }
                rowLayout.addView(card)
                mi++
            }
            statsContainer.addView(rowLayout)
        }

        voltageValue = metricValueRefs[0]!!
        currentValue = metricValueRefs[1]!!
        wattageValue = metricValueRefs[2]!!
        temperatureValue = metricValueRefs[3]!!
        currentSparkline = metricSparklineRefs[1]!!
        wattageSparkline = metricSparklineRefs[2]!!
        currentSparkline.visibility = View.VISIBLE
        wattageSparkline.visibility = View.VISIBLE

        // Rows 3-4: informational cards, not tappable
        val infoValueRefs = arrayOfNulls<TextView>(4)
        var ii = 0
        for (row in extraRows) {
            val rowLayout = newRow()
            for ((icon, label) in row) {
                val card = inflater.inflate(R.layout.item_stat_card, rowLayout, false)
                card.findViewById<TextView>(R.id.icon).text = icon
                card.findViewById<TextView>(R.id.label).text = label
                card.isClickable = false
                infoValueRefs[ii] = card.findViewById(R.id.value)
                rowLayout.addView(card)
                ii++
            }
            statsContainer.addView(rowLayout)
        }

        healthValue = infoValueRefs[0]!!
        pluggedValue = infoValueRefs[1]!!
        capacityValue = infoValueRefs[2]!!
        statusValue = infoValueRefs[3]!!
    }

    private fun newRow(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
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
        val reading = BatteryReader.read(this) ?: return
        BatteryHistory.record(reading)

        val isCharging = reading.isCharging
        val dynamicColor = getColorCompat(if (isCharging) R.color.accent_green else R.color.accent_salmon)

        gauge.setProgress(reading.pct / 100f)
        percentText.text = String.format("%.2f%%", reading.pct)
        statusText.text = when {
            isCharging && reading.wattage > 10.0 -> "FAST CHARGING"
            isCharging -> "CHARGING"
            reading.status == BatteryManager.BATTERY_STATUS_FULL -> "FULLY CHARGED"
            reading.plugged != 0 -> "PLUGGED IN"
            else -> "NOT CHARGING"
        }

        val wattageMw = reading.wattage * 1000
        val badgeSign = if (isCharging) "+" else "-"
        wattageBadge.visibility = View.VISIBLE
        wattageBadge.text = "$badgeSign${String.format("%.0f", wattageMw)} mW"
        wattageBadge.setTextColor(dynamicColor)
        wattageBadge.setBackgroundResource(
            if (isCharging) R.drawable.bg_wattage_badge else R.drawable.bg_wattage_badge_negative
        )

        voltageValue.text = "${reading.voltageMv} mV"
        val currentSign = if (reading.currentMa >= 0) "+" else ""
        currentValue.text = "$currentSign${String.format("%.0f", reading.currentMa)} mA"
        currentValue.setTextColor(dynamicColor)
        wattageValue.text = "${if (isCharging) "+" else "-"}${String.format("%.1f", reading.wattage)} W"
        wattageValue.setTextColor(dynamicColor)
        temperatureValue.text = String.format("%.1f°C", reading.tempC)

        currentSparkline.lineColor = dynamicColor
        currentSparkline.setValues(BatteryHistory.current)
        wattageSparkline.lineColor = dynamicColor
        wattageSparkline.setValues(BatteryHistory.wattage)

        healthValue.text = when (reading.health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheating"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        healthValue.setTextColor(getColorCompat(R.color.accent_green))

        pluggedValue.text = when (reading.plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            else -> "Unplugged"
        }

        val designCapacity = BatteryReader.designCapacityMah(this)
        capacityValue.text = when {
            designCapacity > 0 -> "$designCapacity mAh"
            reading.chargeCounterUah > 0 && reading.pct > 0 ->
                "${(reading.chargeCounterUah / 1000.0 / (reading.pct / 100.0)).toInt()} mAh (est.)"
            else -> "Unavailable"
        }

        statusValue.text = when (reading.status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Unknown"
        }
        statusValue.setTextColor(dynamicColor)

        updateInsights(reading, designCapacity)
    }

    private fun updateInsights(reading: BatteryReading, designCapacityMah: Int) {
        val prefs = getSharedPreferences("ampereflow_prefs", Context.MODE_PRIVATE)
        val remainingMah = if (reading.chargeCounterUah > 0) reading.chargeCounterUah / 1000.0 else -1.0

        if (reading.isCharging) {
            batteryLifeSubtitle.text = "Until fully charged"
            val capacityMah = when {
                designCapacityMah > 0 -> designCapacityMah.toDouble()
                reading.pct > 0 && remainingMah > 0 -> remainingMah / (reading.pct / 100.0)
                else -> -1.0
            }
            batteryLifeValue.text = if (remainingMah > 0 && capacityMah > 0 && reading.currentMa > 0) {
                val neededMah = (capacityMah - remainingMah).coerceAtLeast(0.0)
                formatHoursMinutes(neededMah / reading.currentMa)
            } else {
                "Calculating…"
            }
        } else {
            batteryLifeSubtitle.text = "At the current usage"
            batteryLifeValue.text = if (remainingMah > 0 && reading.currentMa < 0) {
                "${formatHoursMinutes(remainingMah / abs(reading.currentMa))} left"
            } else {
                "Calculating…"
            }
        }

        if (reading.pct >= 99f && reading.chargeCounterUah > 0) {
            prefs.edit()
                .putLong("last_full_uah", reading.chargeCounterUah)
                .putFloat("last_full_pct", reading.pct)
                .apply()
        }

        val lastFullUah = prefs.getLong("last_full_uah", -1L)
        val lastFullPct = prefs.getFloat("last_full_pct", 0f)

        if (lastFullUah > 0 && designCapacityMah > 0) {
            val healthPct = ((lastFullUah / 1000.0) / designCapacityMah * 100.0).coerceIn(0.0, 100.0)
            val status = when {
                healthPct >= 80 -> "Good"
                healthPct >= 50 -> "Fair — keep an eye on it"
                else -> "Replace soon"
            }
            batteryHealthValue.text = "~${healthPct.toInt()}%  $status"
            batteryHealthSubtitle.text = if (lastFullPct < 100f) {
                "Measured at ${lastFullPct.toInt()}% · charge to 100% for a more accurate reading"
            } else {
                "Measured at last full charge"
            }
        } else {
            batteryHealthValue.text = "Not yet calibrated"
            batteryHealthSubtitle.text = "Charge to 100% for an accurate reading"
        }
    }

    private fun formatHoursMinutes(hoursDouble: Double): String {
        if (hoursDouble.isNaN() || hoursDouble.isInfinite() || hoursDouble < 0) return "Calculating…"
        val totalMinutes = (hoursDouble * 60).toInt()
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }
}
