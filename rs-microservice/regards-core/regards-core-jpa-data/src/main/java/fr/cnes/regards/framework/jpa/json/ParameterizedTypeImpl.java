/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * An {@link ParameterizedType} implementation
 *
 * @author Marc Sordi
 *
 */
public class ParameterizedTypeImpl implements ParameterizedType {

    private final Type[] actualTypeArguments;

    private final Type rawType;

    private final Type ownerType;

    public ParameterizedTypeImpl(Type ownerType, Type rawType, Type... actualTypeArguments) {
        this.ownerType = ownerType;
        this.rawType = rawType;
        this.actualTypeArguments = actualTypeArguments;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return actualTypeArguments;
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

}
