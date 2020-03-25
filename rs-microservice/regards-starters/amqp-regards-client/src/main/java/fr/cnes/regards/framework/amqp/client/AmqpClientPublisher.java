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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.cnes.regards.framework.amqp.IPublisher;

@Service
@Transactional
@SuppressWarnings("unchecked")
public class AmqpClientPublisher {

    private static Logger LOGGER = LoggerFactory.getLogger(AmqpClientApplication.class);

    @Autowired
    private IPublisher publisher;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Publish a single message loaded from specified JSON file
     */
    public void publish(String exchangeName, Optional<String> queueName, Integer priority, String jsonPathString) {

        Path path = Paths.get(jsonPathString);
        if (!Files.exists(path)) {
            String error = String.format("Unknown json path %s", path);
            LOGGER.error(error);
            throw new IllegalArgumentException(error);
        }

        if (Files.isDirectory(path)) {
            doPublishAll(exchangeName, queueName, priority, path);
        } else {
            doPublish(exchangeName, queueName, priority, path);
        }
    }

    /**
     * Publish all messages load from specified directory.
     */
    private void doPublishAll(String exchangeName, Optional<String> queueName, Integer priority, Path jsonPath) {

        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.json");

        try (Stream<Path> walk = Files.walk(jsonPath)) {
            walk.filter(Files::isRegularFile).filter(p -> matcher.matches(p)).forEach(p -> {
                doPublish(exchangeName, queueName, priority, p);
            });

            //            List<String> result = walk.filter(Files::isRegularFile).filter(p -> matcher.matches(p))
            //                    .map(p -> p.toString()).collect(Collectors.toList());
            //            result.forEach(LOGGER::info);

        } catch (IOException e) {
            String error = String.format("Error inspecting directory : %s", jsonPath);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }

    }

    private void doPublish(String exchangeName, Optional<String> queueName, Integer priority, Path jsonPath) {
        try {
            LOGGER.info("Loading JSON from {}", jsonPath);
            // Load JSON message
            Map<String, Object> message = mapper.readValue(jsonPath.toFile(), Map.class);
            // Broadcast
            publisher.broadcast(exchangeName, queueName, priority, message);
        } catch (IOException e) {
            String error = String.format("Cannot read json from path %s", jsonPath);
            LOGGER.error(error, e);
            throw new IllegalArgumentException(error);
        }
    }
}
