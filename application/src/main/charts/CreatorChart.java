package charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import servers.ServerArchitectureType;
import services.metrics.MetricType;
import services.metrics.GroupMetric;
import services.metrics.Metrics;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import static charts.MyChartUtils.getMetricNameFromMetricType;

public class CreatorChart {
    private final ServerArchitectureType architectureType;
    private final GroupMetric metrics;
    private final MetricType metricType;
    private final XYDataset dataset;

    public CreatorChart(ServerArchitectureType architectureType, GroupMetric metrics, MetricType metricType) {
        this.architectureType = architectureType;
        this.metrics = metrics;
        this.metricType = metricType;
        dataset = createDataset();
    }

    public void drawAndSave(File path) throws IOException {
        var metricName = getMetricNameFromMetricType(metricType);

        String xAxis = MyChartUtils.getTitleAxisFromParameterType(metrics.parameterType());
        String yAxis = "Time in ms";

        JFreeChart chart = ChartFactory.createXYLineChart(
            architectureType.name() + " - " + metricName,
            xAxis, yAxis,
            dataset,
            PlotOrientation.VERTICAL,
            false,
            true,
            false
        );

        XYPlot plot = chart.getXYPlot();

        var renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.BLACK);

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(Color.BLACK);

        var fileName = getMetricNameFromMetricType(metricType);
        ChartUtils.saveChartAsPNG(new File(path, fileName + ".png"), chart, 800, 600);
        saveDataSetFromCSV(chart.getXYPlot().getDataset(), xAxis, yAxis, new File(path, fileName + ".csv"));
    }

    private XYDataset createDataset() {
        var series = new XYSeries(metrics.parameterType().toString());
        for (Metrics metric : metrics.metrics())
            series.add(metric.changedConstant(), metric.valuesInMs().get(metricType));

        var dataset = new XYSeriesCollection();
        dataset.addSeries(series);

        return dataset;
    }

    private void saveDataSetFromCSV(XYDataset dataset, String xAxis, String yAxis, File path) {
        var csv = new ArrayList<String>();

        int seriesCount = dataset.getSeriesCount();
        csv.add(String.format("%s, %s, %s", metricType.name(), xAxis, yAxis));
        for (int i = 0; i < seriesCount; i++) {
            int itemCount = dataset.getItemCount(i);
            for (int j = 0; j < itemCount; j++) {
                Number x = dataset.getX(i, j);
                Number y = dataset.getY(i, j);
                csv.add(String.format("%s, %s, %s", j, x, y));
            }
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path))) {
            for (String line : csv) {
                writer.append(line);
                writer.newLine();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write dataset", e);
        }
    }
}
