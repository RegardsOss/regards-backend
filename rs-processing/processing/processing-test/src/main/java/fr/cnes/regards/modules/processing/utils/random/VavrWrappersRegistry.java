/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.processing.utils.random;

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

/**
 * This class provides random generation for vavr collection wrappers.
 *
 * @author gandrieu
 */
public class VavrWrappersRegistry implements RandomizerRegistry {

    private VavrRandomizerFactory factory;

    private EasyRandomParameters easyRandomParameters;

    private EasyRandom easyRandom;

    @Override
    public void init(EasyRandomParameters parameters) {
        this.easyRandomParameters = parameters;
        this.factory = VavrRandomizerFactory.builder().easyRandom(easyRandom).parameters(parameters).build();
    }

    @Override
    public Randomizer<?> getRandomizer(Field field) {
        final Class<?> fieldType = field.getType();
        final Type genericType = field.getGenericType();

        if (fieldType.equals(Either.class)) {
            final Type leftType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            final Class<?> leftClass = rawType(leftType);
            final Type rightType = ((ParameterizedType) genericType).getActualTypeArguments()[1];
            final Class<?> rightClass = rawType(rightType);
            return new VavrEitherRandomizer(() -> easyRandom.nextBoolean(),
                                            getRandomizer(leftType, leftClass),
                                            getRandomizer(rightType, rightClass));
        } else if (fieldType.equals(Option.class)) {
            final Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            final Class<?> valueClass = rawType(valueType);
            return new VavrOptionRandomizer(() -> easyRandom.nextBoolean(), getRandomizer(valueType, valueClass));
        } else if (fieldType.equals(Try.class)) {
            final Type valueType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
            final Class<?> valueClass = rawType(valueType);
            return new VavrTryRandomizer(() -> easyRandom.nextBoolean(), getRandomizer(valueType, valueClass));
        } else {
            return null;
        }
    }

    public Randomizer<?> getRandomizer(Type leftType, Class<?> leftClass) {
        Randomizer<?> vavrRandomizer = factory.fromTypes(leftClass, leftType);
        return vavrRandomizer != null ? vavrRandomizer : () -> easyRandom.nextObject(leftClass);
    }

    public Class<?> rawType(Type genericType) {
        return TypeToken.of(genericType).getRawType();
    }

    @Override
    public Randomizer<?> getRandomizer(Class<?> type) {
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
