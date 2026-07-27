package com.darth.ampereflow

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MetricDetailActivity : AppCompatActivity() {

    private lateinit var type: MetricType
    private lateinit var titleView: TextView
    private lateinit var valueView: TextView
    private lateinit var subtitleView: TextView
    private lateinit var chart: LineChartView
    private lateinit var statMin: TextView
    private lateinit var statAvg: TextView
    private lateinit var statMax: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 1000L

    private val pollRunnable = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, pollIntervalMs)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_metric_detail)

        val typeName = intent.getStringExtra(MetricType.EXTRA_KEY) ?: MetricType.VOLTAGE.name
        type = MetricType.valueOf(typeName)

        titleView = findViewById(R.id.metricTitle)
        valueView = findViewById(R.id.metricValue)
        subtitleView = findViewById(R.id.metricSubtitle)
        chart = findViewById(R.id.detailChart)
        statMin = findViewById(R.id.statMin)
        statAvg = findViewById(R.id.statAvg)
        statMax = findViewById(R.id.statMax)

        findViewById<TextView>(R.id.backArrow).setOnClickListener { finish() }

        titleView.text = type.label
        chart.detailed = true
    }

    override fun onStart() {
        super.onStart()
        handler.post(pollRunnable)
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(pollRunnable)
    }

    private fun refresh() {
        val reading = BatteryReader.read(this) ?: return
        BatteryHistory.record(reading)

        val currentValue: Double = when (type) {
            MetricType.VOLTAGE -> reading.voltageMv.toDouble()
            MetricType.CURRENT -> reading.currentMa
            MetricType.WATTAGE -> reading.wattage
            MetricType.TEMPERATURE -> reading.tempC
        }

        val color = if (reading.isCharging) {
            getColor(R.color.accent_green)
        } else {
            getColor(R.color.accent_salmon)
        }
        val useDynamicColor = type == MetricType.CURRENT || type == MetricType.WATTAGE
        valueView.setTextColor(if (useDynamicColor) color else getColor(R.color.text_primary))
        chart.lineColor = if (useDynamicColor) color else getColor(R.color.accent_green)

        val sign = if (currentValue >= 0 && type != MetricType.VOLTAGE && type != MetricType.TEMPERATURE) "+" else ""
        valueView.text = "$sign${formatValue(currentValue)} ${type.unit}"
        subtitleView.text = if (reading.isCharging) "Charging" else "Discharging"

        val history = BatteryHistory.forMetric(type)
        chart.setValues(history)

        if (history.isNotEmpty()) {
            statMin.text = "${formatValue(history.min().toDouble())} ${type.unit}"
            statMax.text = "${formatValue(history.max().toDouble())} ${type.unit}"
            statAvg.text = "${formatValue(history.average())} ${type.unit}"
        }
    }

    private fun formatValue(value: Double): String = when (type) {
        MetricType.WATTAGE -> String.format("%.1f", value)
        MetricType.TEMPERATURE -> String.format("%.1f", value)
        else -> String.format("%.0f", value)
    }
}
