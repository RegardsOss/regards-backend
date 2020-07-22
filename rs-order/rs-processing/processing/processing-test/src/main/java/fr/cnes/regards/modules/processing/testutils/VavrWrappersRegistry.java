package fr.cnes.regards.modules.processing.testutils;

import com.google.common.reflect.TypeToken;
import io.github.xshadov.easyrandom.vavr.factory.VavrRandomizerFactory;
import io.vavr.control.Either;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.api.RandomizerRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class VavrWrappersRegistry implements RandomizerRegistry {
    private VavrRandomizerFactory factory;
    private EasyRandomParameters easyRandomParameters;
    private EasyRandom easyRandom;

    @Override
    public void init(EasyRandomParameters parameters) {
        this.easyRandomParameters = parameters;
        this.factory = VavrRandomizerFactory.builder().easyRandom(easyRandom).parameters(parameters).build();
    }

    @Override public Randomizer<?> getRandomizer(Field field) {
        final Class<?> fieldType = field.getType();
        final Type genericType = field.getGenericType();

        if (fieldType.equals(Either.class)) {
            final Type leftType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            final Class<?> leftClass = rawType(leftType);
            final Type rightType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
            final Class<?> rightClass = rawType(rightType);
            return new VavrEitherRandomizer(
                    () -> easyRandom.nextBoolean(),
                    getRandomizer(leftType, leftClass),
                    getRandomizer(rightType, rightClass)
            );
        }
        else if (fieldType.equals(Option.class)) {
            final Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            final Class<?> valueClass = rawType(valueType);
            return new VavrOptionRandomizer(
                    () -> easyRandom.nextBoolean(),
                    getRandomizer(valueType, valueClass)
            );
        }
        else if (fieldType.equals(Try.class)) {
            final Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            final Class<?> valueClass = rawType(valueType);
            return new VavrTryRandomizer(
                    () -> easyRandom.nextBoolean(),
                    getRandomizer(valueType, valueClass)
            );
        }
        else {
            return null;
        }
    }

    public Randomizer<?> getRandomizer(Type leftType, Class<?> leftClass) {
        Randomizer<?> vavrRandomizer = factory.fromTypes(leftClass, leftType);
        return vavrRandomizer != null
                ? vavrRandomizer
                : () -> easyRandom.nextObject(leftClass);
    }

    public Class<?> rawType(Type genericType) {
        return TypeToken.of(genericType).getRawType();
    }

    @Override public Randomizer<?> getRandomizer(Class<?> type) {
        return null;
    }

    public EasyRandomParameters getEasyRandomParameters() {
        return easyRandomParameters;
    }

    public void setEasyRandomParameters(EasyRandomParameters easyRandomParameters) {
        this.easyRandomParameters = easyRandomParameters;
    }

    public EasyRandom getEasyRandom() {
        return easyRandom;
    }

    public void setEasyRandom(EasyRandom easyRandom) {
        this.easyRandom = easyRandom;
    }
}
