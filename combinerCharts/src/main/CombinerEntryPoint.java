import charts.CombinerCharts;
import org.jfree.data.io.CSV;
import servers.ServerArchitectureType;
import services.metrics.MetricType;
import testing.parameters.ParameterType;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;

import static charts.MyChartUtils.RESULTS_DIR;
import static charts.MyChartUtils.getMetricNameFromMetricType;
import static testing.parameters.ParametersConstants.NON_UPDATABLE_PARAMETERS;

class CombinerEntryPoint {
    public static void main(String[] args) throws IOException {
        var csvReader = new CSV();
        var datasets = new ArrayList<CombinerCharts.DatasetWithArchitecture>();

        for (ParameterType parameterType : ParameterType.values()) {
            if (NON_UPDATABLE_PARAMETERS.contains(parameterType))
                continue;

            for (MetricType metricType : MetricType.values()) {

                for (ServerArchitectureType architectureType : ServerArchitectureType.values()) {
                    try (var fileReader = new FileReader(Path.of(
                        RESULTS_DIR, architectureType.name(),
                        parameterType.name(), getMetricNameFromMetricType(metricType) + ".csv").toFile()
                    )) {
                        datasets.add(new CombinerCharts.DatasetWithArchitecture(
                            architectureType, csvReader.readCategoryDataset(fileReader))
                        );
                    }
                }

                CombinerCharts.createOverallArchitecturesChart(
                    Path.of(RESULTS_DIR, "OVERALL"), metricType,
                    parameterType, CombinerCharts.createDataset(datasets)
                );
                datasets.clear();
            }
        }
    }
}
