package testing;

import servers.ServerArchitectureType;
import services.ServiceLocator;
import services.communication.CommunicationService;
import services.communication.MessageStatus;
import testing.parameters.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;

import static testing.parameters.ParametersConstants.NON_UPDATABLE_PARAMETERS;

public class WaiterTestingConfiguration {
    private final CommunicationService communication =
        ServiceLocator.get(CommunicationService.class);

    private record ParameterCommunicationInfo(ParameterType parameterType, String name, String message) {
    }

    public TestingConfiguration get() {
        communication.println("— Введите цифру типа архитектуры:", MessageStatus.QUESTION);
        communication.println("\t1: Блокирующая;", MessageStatus.QUESTION);
        communication.println("\t2: Неблокирующая;", MessageStatus.QUESTION);
        communication.println("\t3: Асинхронная;", MessageStatus.QUESTION);
        var typeArchitecture = switch (Integer.parseInt(communication.readln())) {
            case 1 -> ServerArchitectureType.BLOCKING;
            case 2 -> ServerArchitectureType.NON_BLOCKING;
            case 3 -> ServerArchitectureType.ASYNCHRONOUS;
            default -> throw new IllegalStateException("Unexpected value: " + Integer.parseInt(communication.readln()));
        };

        var parameterTypes = new ParameterCommunicationInfo[]{
            new ParameterCommunicationInfo(ParameterType.X,
                "X",
                "Суммарное количество запросов, отправляемых каждым клиентом"
            ),
            new ParameterCommunicationInfo(ParameterType.N,
                "N",
                "количество элементов в сортируемых массивах"
            ),
            new ParameterCommunicationInfo(ParameterType.M,
                "M",
                "количество одновременно работающих клиентов"
            ),
            new ParameterCommunicationInfo(ParameterType.DELTA,
                "∆",
                "временной промежуток от получения ответа от сервера на одно сообщение клиента " +
                    "до начала отправки следующего сообщения клиента"
            ),
        };

        var busyParameterTypes = new HashSet<ParameterType>();
        var allParameters = new EnumMap<ParameterType, Parameter>(ParameterType.class);

        var updateParameterCreateInfo = getUpdateParameter(parameterTypes);
        var updateParameterType = updateParameterCreateInfo.parameter.getType();
        busyParameterTypes.add(updateParameterType);
        allParameters.put(updateParameterType, updateParameterCreateInfo.parameter);

        communication.println("— Введите значения для оставшихся параметров:", MessageStatus.QUESTION);
        for (var type : parameterTypes) {
            if (busyParameterTypes.contains(type.parameterType))
                continue;

            communication.println("\t" + type.name + " - " + type.message, MessageStatus.QUESTION);
            communication.println("\t— Введите значение:", MessageStatus.QUESTION);
            var constValue = Integer.parseInt(communication.readln());

            allParameters.put(type.parameterType, new ConstParameter(type.parameterType, constValue));
        }

        return new TestingConfiguration(
            typeArchitecture,
            new TestingParameters(
                updateParameterCreateInfo.parameter(),
                Collections.unmodifiableMap(allParameters)
            )
        );
    }

    private UpdateParameterCreateInfo getUpdateParameter(
        ParameterCommunicationInfo[] parameterTypes
    ) {
        var parameterTypesWithoutUpdatable = Arrays.stream(parameterTypes)
            .filter(type -> !NON_UPDATABLE_PARAMETERS.contains(type.parameterType))
            .toArray(ParameterCommunicationInfo[]::new);

        communication.println("— Выберите параметр, который будет обновляться:", MessageStatus.QUESTION);
        int i = 1;
        for (var type : parameterTypesWithoutUpdatable) {
            communication.println(
                "\t— " + i++ + ": " + type.name + " - " + type.message,
                MessageStatus.QUESTION
            );
        }

        var index = Integer.parseInt(communication.readln());

        communication.println("\t— Введите начальное значение:", MessageStatus.QUESTION);
        var startValue = Integer.parseInt(communication.readln());

        communication.println("\t— Введите максимальное значение:", MessageStatus.QUESTION);
        var maxValue = Integer.parseInt(communication.readln());

        communication.println("\t— Введите шаг:", MessageStatus.QUESTION);
        var step = Integer.parseInt(communication.readln());

        return new UpdateParameterCreateInfo(
            index, new UpdateParameter(parameterTypesWithoutUpdatable[index - 1].parameterType, startValue, maxValue, step)
        );
    }

    private record UpdateParameterCreateInfo(int chooseIndex, UpdateParameter parameter) {
    }
}
