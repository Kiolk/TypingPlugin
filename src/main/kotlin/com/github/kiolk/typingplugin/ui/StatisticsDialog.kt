package com.github.kiolk.typingplugin.ui

import com.github.kiolk.typingplugin.model.TypingResult
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.axis.NumberAxis
import org.jfree.chart.plot.XYPlot
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer
import org.jfree.data.xy.XYSeries
import org.jfree.data.xy.XYSeriesCollection
import java.awt.BasicStroke
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class StatisticsDialog(
    project: Project,
    private val results: List<TypingResult>,
    private val currentResult: String,
) : DialogWrapper(project) {
    init {
        title = "Session Summary & Performance"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())

        // Top panel for text statistics
        val statsPanel = JPanel()
        statsPanel.layout = BoxLayout(statsPanel, BoxLayout.Y_AXIS)
        statsPanel.border = JBUI.Borders.empty(10)

        val titleLabel =
            JBLabel("Typing Finished!", SwingConstants.CENTER).apply {
                font = font.deriveFont(Font.BOLD, 16f)
                alignmentX = JComponent.CENTER_ALIGNMENT
            }
        statsPanel.add(titleLabel)
        statsPanel.add(JBUI.Panels.simplePanel(5, 5)) // Spacer

        currentResult.split("\n").forEach { line ->
            if (line.isNotBlank() && !line.contains("Finished")) {
                val label =
                    JBLabel(line, SwingConstants.CENTER).apply {
                        alignmentX = JComponent.CENTER_ALIGNMENT
                    }
                statsPanel.add(label)
            }
        }

        mainPanel.add(statsPanel, BorderLayout.NORTH)

        // Chart setup
        val wpmSeries = XYSeries("Words Per Minute")
        val errorsSeries = XYSeries("Errors Per Minute")
        val accuracySeries = XYSeries("Accuracy")

        results.forEach { result ->
            wpmSeries.add(result.attemptNumber, result.wpm)
            errorsSeries.add(result.attemptNumber, result.errorsPerMinute)
            accuracySeries.add(result.attemptNumber, result.accuracy)
        }

        val dataset =
            XYSeriesCollection().apply {
                addSeries(wpmSeries)
                addSeries(errorsSeries)
            }

        val chart =
            ChartFactory.createXYLineChart(
                "",
                "Attempt",
                "WPM / Errors",
                dataset,
            )

        val plot = chart.plot as XYPlot
        val domainAxis = plot.domainAxis as NumberAxis
        domainAxis.standardTickUnits = NumberAxis.createIntegerTickUnits()

        val renderer = XYLineAndShapeRenderer()
        renderer.setSeriesPaint(0, Color(52, 152, 219)) // Blue
        renderer.setSeriesStroke(0, BasicStroke(2.0f))
        renderer.setSeriesPaint(1, Color(231, 76, 60)) // Red
        renderer.setSeriesStroke(1, BasicStroke(2.0f))

        plot.renderer = renderer
        plot.backgroundPaint = Color.WHITE
        plot.rangeGridlinePaint = Color.LIGHT_GRAY
        plot.domainGridlinePaint = Color.LIGHT_GRAY

        val accuracyDataset = XYSeriesCollection(accuracySeries)
        val axis2 = NumberAxis("Accuracy (%)")
        plot.setRangeAxis(1, axis2)
        plot.setDataset(1, accuracyDataset)
        plot.mapDatasetToRangeAxis(1, 1)

        val renderer2 = XYLineAndShapeRenderer()
        renderer2.setSeriesPaint(0, Color(46, 204, 113)) // Green
        renderer2.setSeriesStroke(0, BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, floatArrayOf(5.0f), 0.0f))
        plot.setRenderer(1, renderer2)

        val chartPanel = ChartPanel(chart)
        chartPanel.preferredSize = java.awt.Dimension(800, 400)
        mainPanel.add(chartPanel, BorderLayout.CENTER)

        return mainPanel
    }
}
