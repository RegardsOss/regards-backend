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

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

import fr.cnes.regards.framework.amqp.event.JsonMessageConverter;

/**
 * JSON message converters manager
 *
 * @author Marc SORDI
 *
 */
public class JsonMessageConverters implements MessageConverter {

    /**
     * Registered JSON message converters
     */
    Map<JsonMessageConverter, MessageConverter> converters = new HashMap<>();

    @Override
    public Message toMessage(Object object, MessageProperties messageProperties) throws MessageConversionException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object fromMessage(Message message) throws MessageConversionException {
        // TODO Auto-generated method stub
        return null;
    }

}
