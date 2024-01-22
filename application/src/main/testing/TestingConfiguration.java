package testing;

import servers.ServerArchitectureType;
import testing.parameters.TestingParameters;

public record TestingConfiguration(
    ServerArchitectureType architectureType,
    TestingParameters parameters
) { }
