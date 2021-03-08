package fr.cnes.regards.framework.authentication.autoconfigure;

import fr.cnes.regards.framework.authentication.IExternalAuthenticationResolver;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.authentication.client.IExternalAuthenticationClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication
@AutoConfigureBefore({ ExternalAuthenticationAutoConfiguration.class })
public class ExternalAuthenticationAutoConfigure {

    @Autowired
    private IExternalAuthenticationClient externalAuthenticationClient;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @ConditionalOnMissingBean
    @Bean
    public IExternalAuthenticationResolver externalAuthenticationResolver() {
        return new ExternalAuthenticationResolver(externalAuthenticationClient, runtimeTenantResolver);
    }
}
