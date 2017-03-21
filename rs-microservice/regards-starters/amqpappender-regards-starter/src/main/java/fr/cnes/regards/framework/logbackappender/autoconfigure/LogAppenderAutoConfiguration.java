/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender.autoconfigure;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.ext.spring.ApplicationContextHolder;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.logbackappender.RegardsAmqpAppender;

/**
 * @author Christophe Mertz
 *
 */
@Configuration
public class LogAppenderAutoConfiguration {

    /**
     * This method regardsAmqpAppender has the same name as the appender defined in logback.xml.
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

}
