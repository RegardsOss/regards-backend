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
package fr.cnes.regards.framework.amqp.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.framework.amqp.IPublisher;

@Service
@Transactional
public class AmqpClientPublisher {

    private static Logger LOGGER = LoggerFactory.getLogger(AmqpClientApplication.class);

    @Autowired
    private IPublisher publisher;

    private final ObjectMapper mapper = new ObjectMapper();

    public void publish(String exchangeName, Optional<String> queueName, Integer priority, String jsonPath) {
        // Load JSON message
        Map<String, Object> message = loadJson(jsonPath);
        // Broadcast
        publisher.broadcast(exchangeName, queueName, priority, message);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadJson(String jsonPath) {
        Path path = Paths.get(jsonPath);
        if (!Files.exists(path)) {
            String error = String.format("Unknow json path %s", path);
            LOGGER.error(error);
            throw new IllegalArgumentException(error);
        }
        try {
            return mapper.readValue(path.toFile(), Map.class);
        } catch (IOException e) {
            String error = String.format("Cannot read json from path %s", path);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }
    }
}
