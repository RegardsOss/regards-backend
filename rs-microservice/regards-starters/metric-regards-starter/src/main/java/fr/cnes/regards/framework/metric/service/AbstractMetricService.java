/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */

package fr.cnes.regards.framework.metric.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;

import java.util.List;
import java.util.Map;

/**
 * Abstract class providing methods for managing metrics
 *
 * @author mnguyen0
 */
public class AbstractMetricService {

    protected MeterRegistry registry;

    protected AbstractMetricService(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Generate the list of tags needed by the counter with the given values.
     *
     * @param tags Key value pairs representing the tags
     */
    protected List<Tag> generateTags(Map<String, String> tags) {
        return tags.entrySet().stream().map(entry -> Tag.of(entry.getKey(), entry.getValue())).toList();
    }

    /**
     * Increments Prometheus counter with dynamic tags
     */
    protected void incrementCounter(String metricName, Map<String, String> tags, int increment) {
        Counter counter = registry.counter(metricName, generateTags(tags));
        counter.increment(increment);
    }

}
