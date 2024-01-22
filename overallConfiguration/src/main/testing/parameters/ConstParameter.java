package testing.parameters;

public record ConstParameter(ParameterType type, int value) implements Parameter {
    @Override
    public int getValue() {
        return value;
    }

    @Override
    public ParameterType getType() {
        return type;
    }
}
