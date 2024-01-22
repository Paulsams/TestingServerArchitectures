package charts;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.io.CSV;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import servers.ServerArchitectureType;
import services.metrics.MetricType;
import testing.parameters.ParameterType;

import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static charts.MyChartUtils.RESULTS_DIR;

class EntryPoint {
    private record DatasetWithArchitecture(ServerArchitectureType architectureType, CategoryDataset dataset) {
    }

    public static void main(String[] args) throws IOException {
        var csvReader = new CSV();
        var datasets = new ArrayList<DatasetWithArchitecture>();

        for (ParameterType parameterType : ParameterType.values()) {
            if (parameterType == ParameterType.DELTA)
                continue;

            for (MetricType metricType : MetricType.values()) {

                for (ServerArchitectureType architectureType : ServerArchitectureType.values()) {
                    try (var fileReader = new FileReader(Path.of(
                        RESULTS_DIR, architectureType.name(),
                        parameterType.name(), metricType.name() + ".csv").toFile()
                    )) {
                        datasets.add(new DatasetWithArchitecture(
                            architectureType, csvReader.readCategoryDataset(fileReader))
                        );
                    }
                }

                createOverallArchitecturesChart(
                    Path.of(RESULTS_DIR, "OVERALL"), metricType,
                    parameterType, createDataset(datasets)
                );
                datasets.clear();
            }
        }
    }

    private static void createOverallArchitecturesChart(
        Path pathToDirectory,
        MetricType metricType,
        ParameterType parameterType,
        XYDataset dataset
    ) throws IOException {
        JFreeChart chart = ChartFactory.createXYLineChart(
            parameterType.name() + " - " + metricType.name(),
            MyChartUtils.getTitleAxisFromParameterType(parameterType),
            "Time in ms",
            dataset,
            PlotOrientation.VERTICAL,
            true,
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

        chart.getLegend().setFrame(BlockBorder.NONE);

        var pathToDirectoryWithParameterPath = Path.of(pathToDirectory.toString(), parameterType.name()).toFile();

        if (!pathToDirectoryWithParameterPath.exists() && !pathToDirectoryWithParameterPath.mkdirs())
            throw new RuntimeException("Creation of folders for saving results failed with an error");

        ChartUtils.saveChartAsPNG(new File(pathToDirectoryWithParameterPath, metricType.name() + ".png"),
            chart, 800, 600
        );
    }

    private static XYDataset createDataset(List<DatasetWithArchitecture> datasets) {
        var xyDataset = new XYSeriesCollection();

        for (var datasetWithArchitectureType : datasets) {
            var dataset = datasetWithArchitectureType.dataset();

            var series = new XYSeries(datasetWithArchitectureType.architectureType);

            for (int i = 0; i < dataset.getRowCount(); i++)
                series.add(dataset.getValue(i, 0), dataset.getValue(i, 1));

            xyDataset.addSeries(series);
        }

        return xyDataset;
    }
}
