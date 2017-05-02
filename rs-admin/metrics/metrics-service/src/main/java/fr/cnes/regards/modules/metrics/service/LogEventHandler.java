/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.metrics.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.logbackappender.RegardsAmqpAppender;
import fr.cnes.regards.framework.logbackappender.domain.ILogEventHandler;
import fr.cnes.regards.framework.logbackappender.domain.LogEvent;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.metrics.dao.ILogEventRepository;
import fr.cnes.regards.modules.metrics.domain.LogEventJpa;

/**
 * This class defines the handler executed for each {@link LogEventJpa} send by each microservice through the
 * {@link RegardsAmqpAppender} used in logger.
 * 
 * @author Christophe Mertz
 *
 */
@Service
public class LogEventHandler implements ILogEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogEvent.class);

    private IRuntimeTenantResolver runtimeTenantResolver;

    private ILogEventRepository logEventRepository;

    public LogEventHandler(IRuntimeTenantResolver runtimeTenantResolver, ILogEventRepository logEventRepository) {
        LOGGER.debug("Creation bean of type <LogEventHandler>");
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.logEventRepository = logEventRepository;
    }

    public void handle(TenantWrapper<LogEvent> pWrapper) {
        try {
            LogEvent logEvent = pWrapper.getContent();
            LOGGER.debug("[" + pWrapper.getTenant() + "] a new event received : " + logEvent.toString());

            runtimeTenantResolver.forceTenant(pWrapper.getTenant());
            LogEventJpa logEventToSave = new LogEventJpa(pWrapper.getContent());
            logEventRepository.save(logEventToSave);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage(), ex);
        }
    }

}