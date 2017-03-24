/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender.autoconfigure;

import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.ext.spring.ApplicationContextHolder;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.amqp.Subscriber;
import fr.cnes.regards.framework.logbackappender.RegardsAmqpAppender;
import fr.cnes.regards.framework.logbackappender.domain.ILogEventHandler;
import fr.cnes.regards.framework.logbackappender.domain.IMonitoringLogEvent;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;
import fr.cnes.regards.framework.logbackappender.domain.MonitoringLogEvent;

/**
 * This auto configuration defines :</br>
 * <li>a bean with name <code>regardsAmqpAppender</code> used in logback.xml to defined a {@link RegardsAmqpAppender}
 * <li>a bean of type {@link IMonitoringLogEvent} to subscribe to the {@link LogEvent} published by
 * {@link RegardsAmqpAppender}
 * 
 * @author Christophe Mertz
 *
 */
@Configuration
public class LogAppenderAutoConfiguration {

    /**
     * This method regardsAmqpAppender has the same name as the appender defined in logback.xml
     * 
     * @param ctx
     *            the {@link LoggerContext}
     * @param publisher
     *            the {@link Publisher} used to send log message
     * @return a {@link RegardsAmqpAppender}
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public RegardsAmqpAppender regardsAmqpAppender(LoggerContext ctx, IPublisher publisher) {
        RegardsAmqpAppender appender = new RegardsAmqpAppender(publisher);
        appender.setContext(ctx);
        return appender;
    }

    @Bean
    public static ApplicationContextHolder applicationContextHolder() {
        return new ApplicationContextHolder();
    }

    @Bean
    public static LoggerContext loggerContext() {
        LoggerFactory.getILoggerFactory();
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    /**
     * This bean is used to subscribe to {@link LogEvent} from all known tenants.
     * 
     * @param subscriber
     *            The {@link Subscriber}
     * @param logEvenHandler
     *            The bean of type {@link ILogEventHandler} used to handle the {@link LogEvent}
     * @return a bean of type {@link IMonitoringLogEvent}
     */
    @Bean
    @ConditionalOnBean(ILogEventHandler.class)
    public IMonitoringLogEvent monitoringLogEvent(ISubscriber subscriber, ILogEventHandler logEvenHandler) {
        return new MonitoringLogEvent(subscriber, logEvenHandler);
    }

}
