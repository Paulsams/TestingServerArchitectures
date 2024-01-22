package testing.parameters;

import java.util.Map;

public record TestingParameters(
    UpdateParameter updateParameter,
    Map<ParameterType, Parameter> all
) { }
