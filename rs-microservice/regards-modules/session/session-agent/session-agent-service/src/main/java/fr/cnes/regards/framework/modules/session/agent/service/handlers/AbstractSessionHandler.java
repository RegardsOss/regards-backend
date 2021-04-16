package fr.cnes.regards.framework.modules.session.agent.service.handlers;

import fr.cnes.regards.framework.amqp.batch.IBatchHandler;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * @author Iliana Ghazali
 **/
public abstract class AbstractSessionHandler<T extends ISubscribable> implements IBatchHandler<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSessionHandler.class);

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Override
    public boolean validate(String tenant, T message) {
        return true;
    }

    @Override
    public void handleBatch(String tenant, List<T> messages) {
        runtimeTenantResolver.forceTenant(tenant);
        try {
            LOGGER.trace("Processing bulk of {} items", messages.size());
            long start = System.currentTimeMillis();
            processBulk(messages);
            if (!messages.isEmpty()) {
                LOGGER.debug("{} items registered in {} ms", messages.size(), System.currentTimeMillis() - start);
            }
        } finally {
            runtimeTenantResolver.clearTenant();
        }
    }

    protected abstract void processBulk(List<T> items);
}
