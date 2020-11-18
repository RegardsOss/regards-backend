package fr.cnes.regards.modules.order.service.processing;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.jpa.multitenant.transactional.MultitenantTransactional;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.order.dao.IBasketDatasetSelectionRepository;
import fr.cnes.regards.modules.order.domain.basket.BasketDatasetSelection;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.event.RightsPluginConfigurationEvent;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;

import java.util.List;

public class RightsPluginConfigurationEventHandler implements IRightsPluginConfigurationEventHandler{

    private static final Logger LOGGER = LoggerFactory.getLogger(RightsPluginConfigurationEventHandler.class);

    private final IRuntimeTenantResolver runtimeTenantResolver;
    private final ISubscriber subscriber;
    private final IBasketDatasetSelectionRepository dsSelRepo;

    @Autowired
    public RightsPluginConfigurationEventHandler(
            IRuntimeTenantResolver runtimeTenantResolver,
            ISubscriber subscriber,
            IBasketDatasetSelectionRepository dsSelRepo
    ) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.subscriber = subscriber;
        this.dsSelRepo = dsSelRepo;
    }

    @Override public void onApplicationEvent(ApplicationEvent event) {
        subscriber.subscribeTo(RightsPluginConfigurationEvent.class, this);
    }

    @Override public void handle(String tenant, RightsPluginConfigurationEvent evt) {
        runtimeTenantResolver.forceTenant(tenant);
        LOGGER.info("Received RightsPluginConfigurationEvent: {}", evt);
        switch(evt.getType()) {
            case DELETE: { handleDelete(evt.getBefore()); break; }
            case UPDATE: { handleUpdate(evt.getAfter()); break; }
        }
    }

    @MultitenantTransactional
    private void handleUpdate(ProcessPluginConfigurationRightsDTO after) {
        String processBusinessId = after.getPluginConfiguration().getBusinessId();
        io.vavr.collection.List<String> allowedDatasets = after.getRights().getDatasets();
        List<BasketDatasetSelection> dsSels = dsSelRepo.findByProcessBusinessId(processBusinessId);
        List<BasketDatasetSelection> modified = Stream.ofAll(dsSels)
                .flatMap(dsSel -> {
                    if (allowedDatasets.contains(dsSel.getDatasetIpid())) {
                        return Option.none();
                    }
                    else {
                        LOGGER.info("BasketDatasetSelection {} not applicable to process {} anymore", dsSel.getId(), processBusinessId);
                        dsSel.setProcessDatasetDescription(null);
                        return Option.of(dsSel);
                    }
                })
                .asJava();
        if (!modified.isEmpty()) {
            dsSelRepo.saveAll(modified);
        }
    }

    @MultitenantTransactional
    private void handleDelete(ProcessPluginConfigurationRightsDTO before) {
        // If a basket selection references the corresponding process, remove it from the selection
        String processBusinessId = before.getPluginConfiguration().getBusinessId();
        List<BasketDatasetSelection> dsSels = dsSelRepo.findByProcessBusinessId(processBusinessId);
        if (!dsSels.isEmpty()) {
            dsSels.forEach(dsSel -> {
                LOGGER.info("BasketDatasetSelection {} not applicable to process {} anymore", dsSel.getId(), processBusinessId);
                dsSel.setProcessDatasetDescription(null);
            });
            dsSelRepo.saveAll(dsSels);
        }
    }

}
