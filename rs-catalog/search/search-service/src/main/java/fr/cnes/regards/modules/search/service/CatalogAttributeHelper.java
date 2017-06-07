/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.service;

import java.util.List;

import org.springframework.stereotype.Service;

import fr.cnes.regards.framework.feign.security.FeignSecurityManager;
import fr.cnes.regards.framework.hateoas.HateoasUtils;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.entities.gson.IAttributeHelper;
import fr.cnes.regards.modules.models.client.IAttributeModelClient;
import fr.cnes.regards.modules.models.domain.attributes.AttributeModel;

/**
*
* Helper class to retrieve model attributes
* @author Marc Sordi
*
*/
@Service
public class CatalogAttributeHelper implements IAttributeHelper {

    /**
     * Runtime tenant resolver
     */
    private final IRuntimeTenantResolver runtimeTenantResolver;

    /**
     * Feign client for rs-dam {@link AttributeModel} controller. Autowired by Spring.
     */
    private final IAttributeModelClient attributeModelClient;

    public CatalogAttributeHelper(IRuntimeTenantResolver runtimeTenantResolver,
            IAttributeModelClient attributeModelClient) {
        this.runtimeTenantResolver = runtimeTenantResolver;
        this.attributeModelClient = attributeModelClient;
    }

    @Override
    public List<AttributeModel> getAllAttributes(String pTenant) {
        try {
            runtimeTenantResolver.forceTenant(pTenant);
            FeignSecurityManager.asSystem();
            return HateoasUtils.unwrapList(attributeModelClient.getAttributes(null, null).getBody());
        } finally {
            runtimeTenantResolver.clearTenant();
            FeignSecurityManager.reset();
        }
    }
}
