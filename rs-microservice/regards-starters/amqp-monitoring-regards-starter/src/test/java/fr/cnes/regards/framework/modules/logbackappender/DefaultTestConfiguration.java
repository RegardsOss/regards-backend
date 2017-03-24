/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.modules.logbackappender;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import com.google.common.collect.ImmutableSet;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.logbackappender.domain.ILogEventHandler;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;

/**
 * Default Amqp Regards Appender test configuration
 *
 * @author Christophe Mertz
 *
 */
@Configuration
@EnableAutoConfiguration
@PropertySource("classpath:amqp-rabbit.properties")
public class DefaultTestConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultTestConfiguration.class);

    @Bean
    public ILogEventHandler logEventHandler() {
        return new LogEventHandlerTest();
    }

    /**
     * This class is used to test the publish and subscribe of the {@link LogEvent}.
     * 
     * @author Christophe Mertz
     *
     */
    private class LogEventHandlerTest implements ILogEventHandler {

        private List<TenantWrapper<LogEvent>> wrappers = new ArrayList<>();

        private Boolean lock = Boolean.TRUE;

        public void handle(TenantWrapper<LogEvent> pWrapper) {
            LOGGER.debug("a new event received : " + pWrapper.getTenant() + "-" + pWrapper.getContent());
            synchronized (lock) {
                getMessage().add(pWrapper);
            }

        }

        public List<TenantWrapper<LogEvent>> getMessage() {
            return this.wrappers;
        }

        @Override
        synchronized public List<TenantWrapper<LogEvent>> getMessages() {
            List<TenantWrapper<LogEvent>> result;
            synchronized (lock) {
                result = ImmutableSet.copyOf(getMessage()).asList();
                this.wrappers.clear();
            }
            return result;
        }

    }
}
