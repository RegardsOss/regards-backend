/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;

/**
 * JSON message converters manager
 *
 * @author Marc SORDI
 *
 */
public class JsonMessageConverters implements MessageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageConverters.class);

    public static final String CONVERTER_TYPE_HEADER = "__ctype__";

    /**
     * Registered JSON message converters
     */
    Map<JsonMessageConverter, MessageConverter> converters = new HashMap<>();

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        return selectConverter(object).toMessage(object, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        return selectConverter(message.getMessageProperties()).fromMessage(message);
    }

    public void registerConverter(JsonMessageConverter converterType, MessageConverter converter) {
        Assert.notNull(converterType, "Converter type is required");
        Assert.notNull(converter, "Converter is required");
        converters.put(converterType, converter);
    }

    private MessageConverter selectConverter(Object object) {
        TenantWrapper<?> wrapper = (TenantWrapper<?>) object;
        return converters.get(EventUtils.getMessageConverter(wrapper.getContent().getClass()));
    }

    private MessageConverter selectConverter(MessageProperties messageProperties) throws MessageConversionException {
        if (messageProperties == null) {
            String errorMessage = "Missing message properties";
            LOGGER.error(errorMessage);
            throw new MessageConversionException(errorMessage);
        }

        Object converterType = messageProperties.getHeaders().get(CONVERTER_TYPE_HEADER);
        if (converterType == null) {
            String errorMessage = "Cannot determine JSON converter type";
            LOGGER.error(errorMessage);
            throw new MessageConversionException(errorMessage);
        }

        try {
            JsonMessageConverter converterTypeEnum = JsonMessageConverter.valueOf((String) converterType);
            MessageConverter converter = converters.get(converterTypeEnum);
            if (converter == null) {
                String errorMessage = String.format("Cannot determine JSON converter for type \"%s\"",
                                                    converterTypeEnum);
                LOGGER.error(errorMessage);
                throw new MessageConversionException(errorMessage);
            }
            return converter;

        } catch (Exception e) {
            String errorMessage = String.format("Cannot determine JSON converter for type \"%s\"", converterType);
            LOGGER.error(errorMessage, e);
            throw new MessageConversionException(errorMessage, e);
        }
    }
}
