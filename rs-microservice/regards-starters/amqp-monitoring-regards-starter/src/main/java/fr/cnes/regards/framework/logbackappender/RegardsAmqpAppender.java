/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.exception.JwtException;

/**
 *
 * @author Christophe Mertz
 *
 */
public class RegardsAmqpAppender extends AppenderBase<ILoggingEvent> {

    /**
     * Class logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RegardsAmqpAppender.class);

    /**
     * The {@link Publisher} used to send {@link LogEvent}
     */
    private IPublisher publisher;

    @Autowired
    private JWTService jwtService;

    /**
     * The microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    public RegardsAmqpAppender(IPublisher publisher) {
        super();
        this.publisher = publisher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.qos.logback.core.AppenderBase#append(java.lang.Object)
     */
    @Override
    protected void append(ILoggingEvent eventObject) {
        LOGGER.debug("Send message for app name : " + microserviceName);
        String user = "";
        try {
            user = jwtService.getCurrentToken().getName();
        } catch (JwtException e) {
            LOGGER.error(e.getMessage(), e);
        }
        final LogEvent sended = new LogEvent(eventObject.getFormattedMessage(), microserviceName,
                eventObject.getCallerData()[0].getClassName(), eventObject.getCallerData()[0].getMethodName(),
                Instant.ofEpochMilli(eventObject.getTimeStamp()).toString(), eventObject.getLevel().toString(), user);
        publisher.publish(sended);
        LOGGER.debug("Message sended : " + eventObject.getFormattedMessage());
    }

}
