/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.microservice.runner;

import com.google.common.base.Strings;
import fr.cnes.regards.framework.utils.eureka.EurekaWaitingUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.Arrays;

/**
 * Listener that follows Spring lifecycle.
 * <p>
 * {@link #contextPrepared(ConfigurableApplicationContext) contextPrepared} is called
 * once the Application Context has been created but before the configuration and beans have been loaded.
 * <p>
 * The RegardsRunListener blocks the microservice execution until the rs-registry and all the microservices
 * it depends on are running.
 *
 * @author Thibaud Michaudel
 **/
public class RegardsRunListener implements SpringApplicationRunListener {

    public static final int DEFAULT_DELAY = 5;

    public static final String DEFAULT_REGISTRY_URL = "http://rs-registry:9032";

    public static final String REGARDS_TEST_PROFILE = "test";

    private SpringApplication app;

    private String[] args;

    public RegardsRunListener(SpringApplication app, String[] args) {
        this.app = app;
        this.args = args;
    }

    @Override
    public void contextPrepared(ConfigurableApplicationContext context) {

        if (Arrays.stream(context.getEnvironment().getActiveProfiles())
                  .noneMatch(p -> p.equals(REGARDS_TEST_PROFILE))) {
            //Not in test context
            String microservicesToWaitList = context.getEnvironment().getProperty("runner.microservices.to.wait");
            String endpointsToWait = context.getEnvironment().getProperty("runner.endpoints.to.wait");
            String delayAsProperty = context.getEnvironment().getProperty("runner.delay");
            int delay = Strings.isNullOrEmpty(delayAsProperty) ? DEFAULT_DELAY : Integer.parseInt(delayAsProperty);
            String registryUrlProperty = context.getEnvironment().getProperty("registry.url");
            String registryUrl = Strings.isNullOrEmpty(registryUrlProperty) ?
                DEFAULT_REGISTRY_URL :
                registryUrlProperty;
            EurekaWaitingUtils.waitBeforeStart(endpointsToWait, registryUrl, microservicesToWaitList, delay);
        }
    }

}
