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
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;

/**
 * JSON message converters manager
 *
 * @author Marc SORDI
 *
 */
public class JsonMessageConverters implements MessageConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonMessageConverters.class);

    private static final String CONVERTER_TYPE_HEADER = "__ctype__";

    public static final String TENANT_HEADER = "__tenant__";

    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Registered JSON message converters
     */
    Map<JsonMessageConverter, MessageConverter> converters = new HashMap<>();

    public JsonMessageConverters(IRuntimeTenantResolver runtimeTenantResolver) {
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        // Retrieve wrapper event
        TenantWrapper<?> wrapper = (TenantWrapper<?>) object;
        JsonMessageConverter jmc = EventUtils.getMessageConverter(wrapper.getContent().getClass());
        // Add converter selector
        messageProperties.getHeaders().put(JsonMessageConverters.CONVERTER_TYPE_HEADER, jmc);
        // Add wrapped type information for Gson deserialization
        messageProperties.getHeaders().put(Gson2JsonMessageConverter.WRAPPED_TYPE_HEADER,
                                           wrapper.getContent().getClass().getName());
        // Add tenant for Gson tenant specific deserialization
        messageProperties.getHeaders().put(JsonMessageConverters.TENANT_HEADER, wrapper.getTenant());
        return selectConverter(jmc).toMessage(object, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {

        MessageProperties messageProperties = message.getMessageProperties();
        if (messageProperties == null) {
            String errorMessage = "Missing message properties";
            LOGGER.error(errorMessage);
            throw new MessageConversionException(errorMessage);
        }

        String tenant = messageProperties.getHeader(TENANT_HEADER);
        try {
            if (tenant != null) {
                runtimeTenantResolver.forceTenant(tenant);
            }
            return selectConverter(message.getMessageProperties()).fromMessage(message);
        } finally {
            if (tenant != null) {
                runtimeTenantResolver.clearTenant();
            }
        }
    }

    public void registerConverter(JsonMessageConverter converterType, MessageConverter converter) {
        Assert.notNull(converterType, "Converter type is required");
        Assert.notNull(converter, "Converter is required");
        converters.put(converterType, converter);
    }

    private MessageConverter selectConverter(JsonMessageConverter converterType) {
        MessageConverter converter = converters.get(converterType);
        if (converter == null) {
            String errorMessage = String.format("Cannot determine JSON converter for type \"%s\"", converterType);
            LOGGER.error(errorMessage);
            throw new MessageConversionException(errorMessage);
        }
        return converter;
    }

    private MessageConverter selectConverter(MessageProperties messageProperties) throws MessageConversionException {

        Object converterType = messageProperties.getHeader(CONVERTER_TYPE_HEADER);
        if (converterType == null) {
            String message = "Cannot determine JSON converter type, falling back to Jackson";
            LOGGER.trace(message);
            converterType = JsonMessageConverter.JACKSON.toString();
            // FIXME remove above behavior only enabled for compatibility and uncomment following lines
            //            String errorMessage = "Cannot determine JSON converter type";
            //            LOGGER.error(errorMessage);
            //            throw new MessageConversionException(errorMessage);
        }

        try {
            JsonMessageConverter converterTypeEnum = JsonMessageConverter.valueOf((String) converterType);
            return selectConverter(converterTypeEnum);

        } catch (Exception e) {
            String errorMessage = String.format("Cannot determine JSON converter for type \"%s\"", converterType);
            LOGGER.error(errorMessage, e);
            throw new MessageConversionException(errorMessage, e);
        }
    }
}
