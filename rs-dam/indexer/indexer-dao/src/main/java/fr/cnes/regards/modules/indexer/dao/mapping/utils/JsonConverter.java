package fr.cnes.regards.modules.indexer.dao.mapping.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.elasticsearch.core.CheckedFunction;
import org.elasticsearch.common.CheckedSupplier;
import org.elasticsearch.xcontent.XContentBuilder;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.elasticsearch.xcontent.XContentFactory.jsonBuilder;

public class JsonConverter {

    /**
     * This method allows to transform a GSON JsonObject into an ElasticSearch XContentBuilder.
     *
     * @param obj the JsonObject
     * @return a corresponding JSON XContentBuilder
     * @throws IOException whenever constructing the XContentBuilder fails.
     */
    public XContentBuilder toXContentBuilder(JsonObject obj) throws IOException {
        return toXContentBuilder(null, obj, jsonBuilder()).getOrThrow();
    }

    private FineOrNot toXContentBuilder(String key, JsonObject obj, XContentBuilder base) {
        FineOrNot init = tryIt(() -> key == null ? base.startObject() : base.startObject(key));

        return obj.entrySet().stream().reduce(init, this::addObjectField, this::neverCalled)
                .thenTry(XContentBuilder::endObject);

    }

    private FineOrNot addObjectField(FineOrNot acc, Map.Entry<String, JsonElement> entry) {
        return acc.then(builder -> addObjectField(acc, entry.getKey(), entry.getValue(), builder));
    }

    private FineOrNot addObjectField(FineOrNot acc, String key, JsonElement value, XContentBuilder builder) {
        if (value.isJsonPrimitive()) {
            return withPrimitive(value.getAsJsonPrimitive(), bool -> builder.field(key, bool),
                                 aLong -> builder.field(key, aLong), aDouble -> builder.field(key, aDouble), str -> builder.field(key, str));
        } else if (value.isJsonArray()) {
            return toXContentBuilder(key, value.getAsJsonArray(), builder);
        } else if (value.isJsonObject()) {
            return toXContentBuilder(key, value.getAsJsonObject(), builder);
        } else if (value.isJsonNull()) {
            return tryIt(() -> builder.nullField(key));
        } else {
            return acc;
        }
    }

    public FineOrNot toXContentBuilder(String key, JsonArray arr, XContentBuilder base) {
        FineOrNot init = tryIt(() -> key == null ? base.startArray() : base.startArray(key));

        return iteratorToStream(arr.iterator()).reduce(init, this::addArrayValue, this::neverCalled)
                .thenTry(XContentBuilder::endArray);
    }

    private FineOrNot addArrayValue(FineOrNot acc, JsonElement element) {
        return acc.then(builder -> addArrayValue(acc, element, builder));
    }

    private FineOrNot addArrayValue(FineOrNot acc, JsonElement element, XContentBuilder builder) {
        if (element.isJsonPrimitive()) {
            return withPrimitive(element.getAsJsonPrimitive(), builder::value, builder::value, builder::value, builder::value);
        } else if (element.isJsonArray()) {
            return toXContentBuilder(null, element.getAsJsonArray(), builder);
        } else if (element.isJsonObject()) {
            return toXContentBuilder(null, element.getAsJsonObject(), builder);
        } else {
            return acc;
        }
    }

    private <T> Stream<T> iteratorToStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator,
                                                                        Spliterator.DISTINCT | Spliterator.IMMUTABLE
                                                                                | Spliterator.NONNULL), false);
    }

    public FineOrNot tryIt(CheckedSupplier<XContentBuilder, IOException> fn) {
        try {
            return fine(fn.get());
        } catch (IOException e) {
            return error(e);
        }
    }

    public FineOrNot withPrimitive(JsonPrimitive primitive,
                                   CheckedFunction<Boolean, XContentBuilder, IOException> asBoolean,
                                   CheckedFunction<Long, XContentBuilder, IOException> asLong,
                                   CheckedFunction<Double, XContentBuilder, IOException> asDouble, CheckedFunction<String, XContentBuilder, IOException> asString) {
        return tryIt(() -> {
            if (primitive.isBoolean()) {
                return asBoolean.apply(primitive.getAsBoolean());
            } else if (primitive.isNumber()) {
                if(primitive.getAsString().contains(".")) {
                    return asDouble.apply(primitive.getAsDouble());
                } else {
                    return asLong.apply(primitive.getAsLong());
                }
            } else {
                return asString.apply(primitive.getAsString());
            }
        });
    }

    /**
     * A wrapper around either a value or an error. (This is a poor man's {@code Either<E,T>}.)
     *
     * <p>Stream reducing does not permit early exit, so this class allows
     * to detect that an exception has been thrown and ignore all of the
     * remaining fields/parts of the json structure to transform.
     * </p>
     */
    class FineOrNot {

        final IOException error;

        final XContentBuilder value;

        FineOrNot(@NotNull IOException error) {
            this.error = error;
            this.value = null;
        }

        FineOrNot(@NotNull XContentBuilder value) {
            this.value = value;
            this.error = null;
        }

        boolean isFine() {
            return this.value != null;
        }

        boolean isError() {
            return this.error != null;
        }

        XContentBuilder getOrThrow() throws IOException {
            if (isError()) {
                throw error;
            } else {
                return value;
            }
        }

        FineOrNot then(Function<XContentBuilder, FineOrNot> fn) {
            if (isError()) {
                return this;
            } else {
                return fn.apply(this.value);
            }
        }

        FineOrNot thenTry(CheckedFunction<XContentBuilder, XContentBuilder, IOException> fn) {
            if (isError()) {
                return this;
            } else {
                return tryIt(() -> fn.apply(this.value));
            }
        }
    }

    public FineOrNot fine(XContentBuilder value) {
        return new FineOrNot(value);
    }

    public FineOrNot error(IOException e) {
        return new FineOrNot(e);
    }

    /**
     * This method is given when needing to merge two values constructed in parallel
     * while reducing a stream.
     * Since we do not use parallel streams, it is never invoked.
     */
    private <U> U neverCalled(U one, U two) {
        throw new RuntimeException("Should never be called");
    }
}
