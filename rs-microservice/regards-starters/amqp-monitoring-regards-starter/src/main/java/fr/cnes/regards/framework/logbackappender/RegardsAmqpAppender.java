/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.logbackappender;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import fr.cnes.regards.framework.amqp.IPublisher;
import fr.cnes.regards.framework.amqp.Publisher;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.security.utils.jwt.JWTService;
import fr.cnes.regards.framework.security.utils.jwt.SecurityUtils;

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
    IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * The microservice name
     */
    @Value("${spring.application.name}")
    private String microserviceName;

    public RegardsAmqpAppender(IPublisher publisher) {
        super();
        this.publisher = publisher;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {

        String user = SecurityUtils.getActualUser();
        String tenant = runtimeTenantResolver.getTenant();

        if (tenant != null) {
            LOGGER.debug("[" + tenant + "] <" + microserviceName + "> send message  <"
                    + eventObject.getFormattedMessage() + ">");

            Instant instant = Instant.ofEpochMilli(eventObject.getTimeStamp());
            LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

            final LogEvent sended = new LogEvent(eventObject.getFormattedMessage(), microserviceName,
                    eventObject.getCallerData()[0].getClassName(), eventObject.getCallerData()[0].getMethodName(),
                    ldt.toString(), eventObject.getLevel().toString(), user);
            publisher.publish(sended);

            LOGGER.debug("[" + tenant + "] message sended : " + sended.toString());
        }
    }

}
