package fr.cnes.regards.modules.accessrights.service.projectuser;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.storage.domain.event.QuotaUpdateEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class QuotaEventHandler implements ApplicationListener<ApplicationReadyEvent> {

    private final ISubscriber subscriber;

    private final IRuntimeTenantResolver runtimeTenantResolver;

    private final IProjectUserService projectUserService;

    public QuotaEventHandler(ISubscriber subscriber,
                             IRuntimeTenantResolver runtimeTenantResolver,
                             IProjectUserService projectUserService) {
        this.subscriber = subscriber;
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.projectUserService = projectUserService;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        subscriber.subscribeTo(QuotaUpdateEvent.class, new QuotaUpdateEventHandler());
    }

    private class QuotaUpdateEventHandler implements IHandler<QuotaUpdateEvent> {

        @Override
        public void handle(TenantWrapper<QuotaUpdateEvent> wrapper) {
            try {
                runtimeTenantResolver.forceTenant(wrapper.getTenant());
                projectUserService.updateQuota(wrapper.getContent().getCurrentQuotaByEmail());
            } finally {
                runtimeTenantResolver.clearTenant();
            }
        }

    }

}
