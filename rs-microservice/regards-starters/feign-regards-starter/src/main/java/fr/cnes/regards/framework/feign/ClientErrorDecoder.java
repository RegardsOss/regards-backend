/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.feign;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.feign.support.SpringDecoder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.common.io.ByteStreams;
import com.google.common.reflect.ClassPath;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Intercept Feign error to write custom log and decode the body into an object
 * in case the return type is defined as a {@link ResponseEntity}&#60;SOMETHING&#62;.
 * It will deserialize the body, using {@link SpringDecoder}, into a SOMETHING instance accessible
 * into the {@link FeignResponseDecodedException} thrown.
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class ClientErrorDecoder extends ErrorDecoder.Default implements ErrorDecoder {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientErrorDecoder.class);

    /**
     * Spring decoder
     */
    private SpringDecoder springDecoder;

    /**
     * Constructor setting the sprind decoder as attribute
     * @param springDecoder
     */
    public ClientErrorDecoder(SpringDecoder springDecoder) {
        this.springDecoder = springDecoder;
    }

    @Override
    public Exception decode(final String methodKey, final Response response) {
        LOGGER.error(String.format("Remote call to %s. Response is : %d - %s",
                                   methodKey,
                                   response.status(),
                                   response.reason()));
        try {
            // we want to use SpringDecoder, for that we need to know the type of the response body
            // to get it we need to get the method by introspection thanks to methodKey
            String[] classNmethod = methodKey.split("#");
            // first lets get the client class
            // TODO: warning, we are considering that all feign clients are uniquely simple named in the system
            Class<?> clientClass = Class
                    .forName(ClassPath.from(this.getClass().getClassLoader()).getAllClasses().stream()
                                     .filter(classInfo -> classInfo.getSimpleName().equals(classNmethod[0])).findFirst()
                                     .get().getName());
            // now lets reconstruct the method parameters class
            String methodNParameters = classNmethod[1];
            String methodName = methodNParameters.substring(0, methodNParameters.indexOf('('));
            String methodParametersString = methodNParameters
                    .substring(methodNParameters.indexOf('(') + 1, methodNParameters.indexOf(')'));
            Class[] methodParametersType;
            if (methodParametersString.trim().isEmpty()) {
                //There is no parameters
                methodParametersType = new Class[0];
            } else {
                String[] methodParameters = methodParametersString.split(",");
                int parameterNumber = methodParameters.length;
                methodParametersType = new Class[parameterNumber];
                for (int i = 0; i < parameterNumber; i++) {
                    methodParametersType[i] = Class.forName(methodParameters[i].trim());
                }
            }
            // now that we have the method name and the method parameters class, we can retrieve the Method object
            Method method = clientClass.getMethod(methodName, methodParametersType);
            // We decide to only handle ResponseEntity<?> return types
            Type bodyType = method.getGenericReturnType();
            if (bodyType instanceof ParameterizedType && ((Class) ((ParameterizedType) bodyType).getRawType())
                    .isAssignableFrom(ResponseEntity.class)) {
                bodyType = ((ParameterizedType) bodyType).getActualTypeArguments()[0];
            } else {
                //TODO: test with no changes on body type as if it is not a ResponseEntity, there is no apparent reasons the body is not what the interface defines
                return super.decode(methodKey, response);
            }
            Object responseEntity = springDecoder.decode(response, bodyType);
            // now that we have the response body deserialize, lets get the other things we need to construct a proper FeignResponseDecodedException
            HttpHeaders responseHeaders = new HttpHeaders();
            response.headers().entrySet().stream()
                    .forEach(entry -> responseHeaders.put(entry.getKey(), new ArrayList<>(entry.getValue())));
            Charset responseCharset = null;
            if (responseHeaders.getContentType() != null) {
                // if we find any charset, lets use it
                responseCharset = responseHeaders.getContentType().getCharset();
            }
            byte[] responseBody = null;
            if (response.body() != null) {
                responseBody = ByteStreams.toByteArray(response.body().asInputStream());
            }
            FeignResponseDecodedException responseDecoded = new FeignResponseDecodedException(HttpStatus
                                                                                                      .valueOf(response.status()),
                                                                                              response.reason(),
                                                                                              responseHeaders,
                                                                                              responseBody,
                                                                                              responseCharset,
                                                                                              responseEntity);
            return responseDecoded;
        } catch (ClassNotFoundException | NoSuchMethodException | IOException e) {
            // in case of error, we prefer to defer back to the default error decoder
            LOGGER.debug(e.getMessage(), e);
            return super.decode(methodKey, response);
        }
    }
}
