/*
 * LICENSE_PLACEHOLDER
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
 *
 */
public class RegardsAmqpAppenderFilter extends Filter<ILoggingEvent> {

    /**
     * The {@link List} of {@link String} for which the {@link Filter} should not log the event send by a class name
     * containing this {@link String}.
     */
    private final List<String> includes = Arrays.asList("fr.cnes.regards.framework.modules",
                                                        "fr.cnes.regards.framework.security",
                                                        "fr.cnes.regards.framework.logbackappender",
                                                        "fr.cnes.regards.modules");

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

        if (event.getLevel() != level)
            return FilterReply.DENY;

        accept = false;
        String loggerName = event.getLoggerName().toLowerCase();

        includes.forEach(s -> {
            if (!accept && loggerName.contains(s.toLowerCase())) {
                accept = true;
            }
        });

        if (!accept) {
            return FilterReply.DENY;
        }

        return FilterReply.ACCEPT;
    }

    public void setLevel(String level) {
        this.level = Level.toLevel(level);
    }

    public void start() {
        if (this.level == null) {
            level = Level.INFO;
        }
        super.start();
    }

}