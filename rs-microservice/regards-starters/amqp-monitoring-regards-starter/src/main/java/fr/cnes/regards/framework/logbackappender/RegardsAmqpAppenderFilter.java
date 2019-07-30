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
package fr.cnes.regards.framework.logbackappender;

import java.util.Arrays;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

/**
 * @author Christophe Mertz
 */
public class RegardsAmqpAppenderFilter extends Filter<ILoggingEvent> {

    /**
     * The {@link List} of {@link String} for which the {@link Filter} should not log the event send by a class name
     * containing this {@link String}.
     */
    private final List<String> includes = Arrays
            .asList("fr.cnes.regards.framework.modules", "fr.cnes.regards.framework.security",
                    "fr.cnes.regards.framework.logbackappender", "fr.cnes.regards.modules");

    /**
     * <code>false</code> if the event should not be log
     */
    private Boolean accept;

    /**
     * The log {@link Level}
     */
    private Level level;

    @Override
    public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel() != level) {
            return FilterReply.DENY;
        }
        accept = false;
        String loggerName = event.getLoggerName().toLowerCase();
        includes.forEach(s -> {
            if (!accept && loggerName.contains(s.toLowerCase())) {
                accept = true;
            }
        });

        if (!accept) { // NOSONAR
            return FilterReply.DENY;
        }
        return FilterReply.ACCEPT;
    }

    public void setLevel(String level) {
        this.level = Level.toLevel(level);
    }

    @Override
    public void start() {
        if (this.level == null) {
            level = Level.INFO;
        }
        super.start();
    }

}