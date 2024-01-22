package testing.parameters;

public class UpdateParameter implements Parameter {
    private final ParameterType parameterType;
    private final int maxValue;
    private final int step;

    private int value;

    public UpdateParameter(ParameterType parameterType, int startValue, int maxValue, int step) {
        this.parameterType = parameterType;
        this.maxValue = maxValue;
        this.step = step;

        this.value = startValue;
    }

    @Override
    public int getValue() {
        return value;
    }

    @Override
    public ParameterType getType() {
        return parameterType;
    }

    public boolean update() {
        if (value == maxValue)
            return false;

        value = Math.min(value + step, maxValue);
        return true;
    }
}
