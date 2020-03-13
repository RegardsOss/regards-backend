/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.AbstractMessageConverter;
import org.springframework.amqp.support.converter.MessageConversionException;

import com.google.common.reflect.TypeParameter;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import fr.cnes.regards.framework.amqp.configuration.RabbitVersion;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;

/**
 * GSON message converter
 *
 * @author Marc SORDI
 *
 */
public class Gson2JsonMessageConverter extends AbstractMessageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(Gson2JsonMessageConverter.class);

    private static final String WRAPPED_TYPE_HEADER = "__gson_wrapped_type__";

    public static final String DEFAULT_CHARSET = "UTF-8";

    private final Gson gson;

    public Gson2JsonMessageConverter(Gson gson) {
        this.gson = gson;
    }

    @Override
    protected Message createMessage(Object object, MessageProperties messageProperties) {
        byte[] bytes = gson.toJson(object).getBytes();
        messageProperties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
        messageProperties.setContentEncoding(DEFAULT_CHARSET);
        messageProperties.setContentLength(bytes.length);
        // Add wrapped type information for Gson deserialization
        messageProperties.setHeader(Gson2JsonMessageConverter.WRAPPED_TYPE_HEADER, object.getClass().getName());
        return new Message(bytes, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        Object content = null;
        MessageProperties messageProperties = message.getMessageProperties();
        if (messageProperties != null) {
            try (Reader json = new InputStreamReader(new ByteArrayInputStream(message.getBody()),
                    Charset.forName("UTF-8"))) {
                content = gson.fromJson(json, createTypeToken(message));
            } catch (IOException e) {
                LOGGER.warn("Could not convert incoming message", e);
            }
        } else {
            LOGGER.warn("Could not convert incoming message");
        }
        if (content == null) {
            content = message.getBody();
        }
        return content;
    }

    private static Type createTypeToken(Message message) throws MessageConversionException {
        try {
            Class<?> eventType = Class.forName((String) message.getMessageProperties().getHeader(WRAPPED_TYPE_HEADER));
            if (RabbitVersion.isVersion1_1(message)) {
                return TypeToken.of(eventType).getType();
            } else if (RabbitVersion.isVersion1(message)) {
                return createTypeToken(eventType).getType();
            } else {
                throw new MessageConversionException("Unknown message api version");
            }
        } catch (ClassNotFoundException e) {
            throw new MessageConversionException("Cannot convert incoming message", e);
        }
    }

    @SuppressWarnings("serial")
    private static <T> TypeToken<TenantWrapper<T>> createTypeToken(Class<T> clazz) {
        return new TypeToken<TenantWrapper<T>>() {
        }.where(new TypeParameter<T>() {
        }, TypeToken.of(clazz));
    }
}
