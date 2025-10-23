package fr.cnes.regards.framework.jsoniter.decoders;

import com.jsoniter.ValueType;
import com.jsoniter.any.Any;
import com.jsoniter.spi.Decoder;
import com.jsoniter.spi.TypeLiteral;
import io.vavr.control.Option;

import java.util.function.Consumer;

public interface SmartDecoder extends Decoder {

    default boolean isNull(Any any) {
        ValueType type = any.valueType();
        return type == ValueType.NULL || type == ValueType.INVALID;
    }

    default <T> Option<T> asOption(Any a, Class<T> asType, Object... keys) {
        Any result = a.get(keys);
        return isNull(result) ? Option.none() : Option.of(result.as(asType));
    }

    default <T> Option<T> asOption(Any a, TypeLiteral<T> asType, Object... keys) {
        Any result = a.get(keys);
        return isNull(result) ? Option.none() : Option.of(result.as(asType));
    }

    default <T> void whenPresent(Any value, Class<T> asType, Consumer<T> action) {
        asOption(value, asType).peek(action);
    }

    default <T> void whenPresent(Any value, TypeLiteral<T> asType, Consumer<T> action) {
        asOption(value, asType).peek(action);
    }

    default String stringOrNull(Any a, Object... keys) {
        Any result = a.get(keys);
        return isNull(result) ? null : result.toString();
    }

    default Long longOrNull(Any a, String field) {
        Any v = a.get(field);
        if (v == null || v.valueType() == ValueType.NULL || v.valueType() == ValueType.INVALID) {
            return null;
        }
        if (v.valueType() == ValueType.NUMBER) {
            return v.toLong();
        }
        try {
            return Long.valueOf(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    default Double doubleOrNull(Any parent, String field) {
        Any v = parent.get(field);
        if (v == null || v.valueType() == ValueType.NULL || v.valueType() == ValueType.INVALID) {
            return null;
        }
        if (v.valueType() == ValueType.NUMBER) {
            return v.toDouble();
        }
        try {
            return Double.valueOf(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    default <T> T asOrNull(Any a, Class<T> asType, Object... keys) {
        Any result = a.get(keys);
        return isNull(result) ? null : result.as(asType);
    }

}
