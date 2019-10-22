/**
 *
 */
package fr.cnes.regards.framework.metric.autoconfigure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.logging.LoggingMeterRegistry;

/**
 * @author kevin
 *
 */
@Configuration
public class MetricAutoConfiguration {

    @Bean
    TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    @Bean
    LoggingMeterRegistry loggingMeterRegistry() {
        return new LoggingMeterRegistry();

    }
}
