package charts;

import testing.parameters.ParameterType;

public class MyChartUtils {
    public static final String RESULTS_DIR = "RESULTS";

    public static String getTitleAxisFromParameterType(ParameterType parameterType) {
        return switch (parameterType) {
            case X -> "Total number of requests sent by each client";
            case N -> "Number of elements in sorted arrays";
            case M -> "Number of simultaneously working clients";
            case DELTA -> "Time interval from receiving a response ";
        };
    }
}
