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
package fr.cnes.regards.framework.metric.autoconfigure;

import java.time.Duration;

import org.springframework.boot.actuate.autoconfigure.metrics.export.properties.StepRegistryProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.micrometer.core.instrument.logging.LoggingRegistryConfig;

@ConfigurationProperties(prefix = "management.metrics.export.logging")
public class LoggingRegistryConfiguration extends StepRegistryProperties implements LoggingRegistryConfig {

    private boolean logInactive = false;

    public boolean isLogInactive() {
        return logInactive;
    }

    public void setLogInactive(boolean logInactive) {
        this.logInactive = logInactive;
    }

    @Override
    public boolean enabled() {
        return isEnabled();
    }

    @Override
    public boolean logInactive() {
        return logInactive;
    }

    @Override
    public Duration step() {
        return getStep();
    }

    @Override
    public String get(String key) {
        return null;
    }
}
