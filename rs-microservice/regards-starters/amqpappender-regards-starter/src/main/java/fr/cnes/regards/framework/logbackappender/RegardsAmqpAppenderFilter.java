package fr.cnes.regards.framework.logbackappender;

import java.util.Arrays;
import java.util.List;

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
    private final List<String> excludes = Arrays.asList("amqp", "AutoConfiguration");
    

    /**
     * <code>false</code> if the event should not be log
     */
    private Boolean deny;

    @Override
    public FilterReply decide(ILoggingEvent event) {
        deny = false;
        String loggerName = event.getLoggerName().toLowerCase();
        excludes.forEach(s -> {
            if (!deny && loggerName.contains(s.toLowerCase())) {
                deny = true;
            }
        });

        if (deny) {
            return FilterReply.DENY;
        }

        return FilterReply.ACCEPT;
    }

}