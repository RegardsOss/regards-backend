/*
 * Copyright 2017-2023 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.amqp.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.validation.constraints.NotEmpty;
import java.time.Duration;
import java.util.List;

/**
 * Properties to configure the re-routing of AMQP messages that could not be delivered.
 *
 * @author Iliana Ghazali
 **/
@ConfigurationProperties("regards.amqp.retry")
public class RetryProperties {

    /**
     * Delay between retry attempts. By default, messages will be retried four times maximum after failures with the
     * following delays: 5 seconds, 30 seconds, 2 minutes and 10 minutes. If you want to override the default values,
     * add the 'regards.amqp.retry.delayAttempts' property into the spring property file. For example, to retry
     * messages three times maximum with different delays define 'regards.amqp.retry.delayAttempts=30s,1m,2m'.
     */
    @NotEmpty
    private List<Duration> delayAttempts;

    public RetryProperties() {
        this.delayAttempts = List.of(Duration.ofSeconds(5),
                                     Duration.ofSeconds(30),
                                     Duration.ofMinutes(2),
                                     Duration.ofMinutes(10));
    }

    public List<Duration> getDelayAttempts() {
        return delayAttempts;
    }

    public void setDelayAttempts(List<Duration> delayAttempts) {
        this.delayAttempts = delayAttempts;
    }

    public int getMaxRetries() {
        return delayAttempts.size();
    }

}
