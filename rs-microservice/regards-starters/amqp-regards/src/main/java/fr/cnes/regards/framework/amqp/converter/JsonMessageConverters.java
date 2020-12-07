/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.util.Assert;

import fr.cnes.regards.framework.amqp.configuration.AmqpConstants;
import fr.cnes.regards.framework.amqp.event.EventUtils;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
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

    /**
     * @deprecated Use {@link AmqpConstants#REGARDS_CONVERTER_HEADER} instead.
     * Will be remove in V1.3
     */
    @Deprecated
    private static final String CONVERTER_TYPE_HEADER = "__ctype__";

    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Registered JSON message converters
     */
    private final ConcurrentMap<JsonMessageConverter, MessageConverter> converters = new ConcurrentHashMap<>();

    public JsonMessageConverters(IRuntimeTenantResolver runtimeTenantResolver) {
        this.runtimeTenantResolver = runtimeTenantResolver;
    }

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        return selectConverter(object, messageProperties).toMessage(object, messageProperties);
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {

        MessageProperties messageProperties = message.getMessageProperties();
        if (messageProperties == null) {
            String errorMessage = "Missing message properties";
            LOGGER.error(errorMessage);
            throw new MessageConversionException(errorMessage);
        }

        String tenant = messageProperties.getHeader(AmqpConstants.REGARDS_TENANT_HEADER);
        String runtimeTenant = runtimeTenantResolver.getTenant();

        // Check if tenant already set and match!
        if ((tenant != null) && (runtimeTenant != null) && !runtimeTenant.equals(tenant)) {
            String errorMessage = String
                    .format("Inconsistent tenant resolution : runtime tenant \"%s\" does not match with message one : \"%s\"",
                            runtimeTenant, tenant);
            LOGGER.warn(errorMessage);
            // FIXME
            // throw new MessageConversionException(errorMessage);
            // Manage tenant before calling handler to properly force and clean tenant.
        }

        try {
            if ((tenant != null) && (runtimeTenant == null)) {
                runtimeTenantResolver.forceTenant(tenant);
            }
            return selectConverter(message.getMessageProperties()).fromMessage(message);
        } finally {
            if ((tenant != null) && (runtimeTenant == null)) {
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

    private MessageConverter selectConverter(Object object, MessageProperties messageProperties)
            throws MessageConversionException {
        if (ISubscribable.class.isAssignableFrom(object.getClass())
                || IPollable.class.isAssignableFrom(object.getClass())) {
            JsonMessageConverter jmc = EventUtils.getMessageConverter(object.getClass());
            MessageConverter converter = selectConverter(jmc);
            // Inject converter selector
            messageProperties.setHeader(AmqpConstants.REGARDS_CONVERTER_HEADER, jmc);
            return converter;
        } else {
            return selectConverter(messageProperties);
        }

    }

    private MessageConverter selectConverter(MessageProperties messageProperties) throws MessageConversionException {

        Object converterType = messageProperties.getHeader(AmqpConstants.REGARDS_CONVERTER_HEADER);
        if (converterType == null) {
            // Compatibility
            converterType = messageProperties.getHeader(CONVERTER_TYPE_HEADER);
        }
        if (converterType == null) {
            String message = "Cannot determine JSON converter type, falling back to GSON";
            LOGGER.trace(message);
            converterType = JsonMessageConverter.GSON.toString();
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
