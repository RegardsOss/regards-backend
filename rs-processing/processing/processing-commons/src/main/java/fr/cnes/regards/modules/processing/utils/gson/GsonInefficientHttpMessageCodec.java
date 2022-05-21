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
package fr.cnes.regards.modules.processing.utils.gson;

import com.google.gson.Gson;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageDecoder;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GSON codec, inefficient because needs to load the whole content of
 * a Flux in order to generate the serialization.
 *
 * @author gandrieu
 */
public class GsonInefficientHttpMessageCodec {

    public static class Co extends GsonInefficientHttpMessageCodec implements HttpMessageEncoder<Object> {

        public Co(Gson gson) {
            super(gson);
        }
    }

    public static class Dec extends GsonInefficientHttpMessageCodec implements HttpMessageDecoder<Object> {

        public Dec(Gson gson) {
            super(gson);
        }
    }

    private static final int MAX_IN_MEMORY_SIZE = 256 * 1024;

    private static final Logger LOGGER = LoggerFactory.getLogger(GsonInefficientHttpMessageCodec.class);

    public static final List<MimeType> MEDIA_TYPES = Arrays.asList(MediaType.APPLICATION_JSON,
                                                                   new MediaType("application",
                                                                                 "json",
                                                                                 StandardCharsets.UTF_8));

    private final Gson gson;

    public GsonInefficientHttpMessageCodec(Gson gson) {
        this.gson = gson;
    }

    public Map<String, Object> getDecodeHints(ResolvableType actualType,
                                              ResolvableType elementType,
                                              ServerHttpRequest request,
                                              ServerHttpResponse response) {
        return Hints.none();
    }

    public Mono<Object> decodeToMono(Publisher<DataBuffer> inputStream,
                                     ResolvableType elementType,
                                     MimeType mimeType,
                                     Map<String, Object> hints) {
        return DataBufferUtils.join(inputStream, MAX_IN_MEMORY_SIZE)
                              .flatMap(dataBuffer -> Mono.justOrEmpty(decode(dataBuffer,
                                                                             elementType,
                                                                             mimeType,
                                                                             hints)));
    }

    public Object decode(DataBuffer dataBuffer,
                         ResolvableType targetType,
                         @Nullable MimeType mimeType,
                         @Nullable Map<String, Object> hints) throws DecodingException {
        try {
            String json = dataBuffer.toString(StandardCharsets.UTF_8);
            return gson.fromJson(json, targetType.getType());
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage(), e);
            throw new DecodingException(e.getMessage());
        } finally {
            DataBufferUtils.release(dataBuffer);
        }
    }

    public Flux<Object> decode(Publisher<DataBuffer> input,
                               ResolvableType elementType,
                               @Nullable MimeType mimeType,
                               @Nullable Map<String, Object> hints) {
        // FIXME: one day, when there are tools to do so, render this horrific method reactive
        ResolvableType listOfElementsType = ResolvableType.forClassWithGenerics(List.class, elementType);
        return DataBufferUtils.join(input).flatMapMany(databuffer -> {
            List<Object> decodedList = (List<Object>) decode(databuffer, listOfElementsType, mimeType, hints);
            return Flux.fromIterable(decodedList);
        });
    }

    public Flux<DataBuffer> encode(Publisher<?> inputStream,
                                   DataBufferFactory bufferFactory,
                                   ResolvableType elementType,
                                   MimeType mimeType,
                                   Map<String, Object> hints) {
        if (inputStream instanceof Mono) {
            return Mono.from(inputStream)
                       .map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
                       .flux();
        } else {
            // FIXME: one day, when there are tools to do so, render this horrific method reactive
            ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
            return Flux.from(inputStream)
                       .collectList()
                       .map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints))
                       .flux();
        }
    }

    public DataBuffer encodeValue(Object value,
                                  DataBufferFactory bufferFactory,
                                  ResolvableType valueType,
                                  @Nullable MimeType mimeType,
                                  @Nullable Map<String, Object> hints) {
        byte[] bytes = value instanceof String ? (((String) value).getBytes()) : gson.toJson(value).getBytes();
        DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length);
        buffer.write(bytes);
        return buffer;
    }

    public boolean canDecode(ResolvableType elementType, MimeType mimeType) {
        return (mimeType == null) || (mimeType.getType().equals("application") && mimeType.getSubtype().equals("json"));
    }

    public boolean canEncode(ResolvableType elementType, MimeType mimeType) {
        return canDecode(elementType, mimeType);
    }

    public List<MediaType> getStreamingMediaTypes() {
        return MEDIA_TYPES.stream().map(x -> (MediaType) x).collect(Collectors.toList());
    }

    public List<MimeType> getDecodableMimeTypes() {
        return MEDIA_TYPES;
    }

    public List<MimeType> getEncodableMimeTypes() {
        return MEDIA_TYPES;
    }
}
