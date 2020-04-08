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

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class AmqpClientApplication implements ApplicationRunner {

    private static Logger LOGGER = LoggerFactory.getLogger(AmqpClientApplication.class);

    private static final String ARG_NS = "regards.amqp.";

    private static final String ARG_EXCHANGE_NAME = ARG_NS + "exchange";

    private static final String ARG_QUEUE_NAME = ARG_NS + "queue";

    private static final String ARG_PRIORITY = ARG_NS + "priority";

    private static final String ARG_HEADERS = ARG_NS + "headers";

    private static final String ARG_JSON = ARG_NS + "json";

    private static final String ARG_REPEAT = ARG_NS + "repeat";

    @Value("${" + ARG_EXCHANGE_NAME + "}")
    private String exchangeName;

    @Value("${" + ARG_QUEUE_NAME + ":}")
    private String queueName;

    @Value("${" + ARG_PRIORITY + ":0}")
    private Integer priority;

    @Value("${" + ARG_JSON + "}")
    private String jsonPathString;

    @Value("#{${" + ARG_HEADERS + "}}")
    Map<String, Object> headers;

    /**
     * In case of template, generation number
     */
    @Value("#{${" + ARG_REPEAT + ":10}}")
    private Integer repeat;

    @Autowired
    private AmqpClientPublisher publisher;

    public static void main(String[] args) {
        LOGGER.info("Starting AMQP client");
        ConfigurableApplicationContext ctx = new SpringApplicationBuilder().bannerMode(Mode.OFF)
                .web(WebApplicationType.NONE).sources(AmqpClientApplication.class).run(args);
        int exitCode = SpringApplication.exit(ctx, () -> 0);
        LOGGER.info("AMQP client exits with code {}", exitCode);
        System.exit(exitCode);
    }

    /**
     * This method will be executed after the application context is loaded and
     * right before the Spring Application main method is completed.
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        LOGGER.info("EXECUTING : command line runner");
        Optional<String> queue = Optional.empty();
        if ((queueName != null) && !queueName.isEmpty()) {
            queue = Optional.of(queueName);
        }
        publisher.publish(exchangeName, queue, priority, headers, jsonPathString, repeat);
    }
}
