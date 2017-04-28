/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.representation;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import com.google.common.reflect.TypeToken;

import fr.cnes.regards.framework.amqp.ISubscriber;
import fr.cnes.regards.framework.amqp.domain.IHandler;
import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.event.PluginConfigurationEvent;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.multitenant.ITenantResolver;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.domain.IRepresentation;
import fr.cnes.regards.modules.search.domain.assembler.resource.FacettedPagedResources;

/**
 * HttpMessageConverter implementation allowing us to use the IRepresentation plugins. Declared as an
 * AbstractGenericHttpMessageConverter&#60;Object&#62; so we can handle AbstractEntity and
 * Collection&#60;AbstractEntity&#62; and Page&#60;AbstractEntity&#62; and Resource&#60;AbstractEntity&#62;
 *
 * @author Sylvain Vissiere-Guerinet
 */
@Component
public class RepresentationHttpMessageConverter extends AbstractGenericHttpMessageConverter<Object> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(RepresentationHttpMessageConverter.class);

    /**
     * TypeToken used to recognize AbstractEntity
     */
    private static final TypeToken<? extends AbstractEntity> ABSTRACT_ENTITY_TYPE_TOKEN = new TypeToken<AbstractEntity>() {
    };

    /**
     * TypeToken used to recognize Collection&#60;AbstractEntity&#62;
     */
    private static final TypeToken<Collection<? extends AbstractEntity>> COLLECTION_ABSTRACT_ENTITY_TYPE_TOKEN = new TypeToken<Collection<? extends AbstractEntity>>() {
    };

    /**
     * TypeToken used to recognize Resource&#60;AbstractEntity&#62;
     */
    private static final TypeToken<Resource<? extends AbstractEntity>> RESOURCE_ABSTRACT_ENTITY_TYPE_TOKEN = new TypeToken<Resource<? extends AbstractEntity>>() {
    };

    /**
     * TypeToken used to recognize PagedResources&#60;Resource&#60;AbstractEntity&#62;&#62;
     */
    private static final TypeToken<PagedResources<Resource<? extends AbstractEntity>>> PAGED_RESOURCES_RESOURCE_ABSTRACT_ENTITY_TYPE_TOKEN = new TypeToken<PagedResources<Resource<? extends AbstractEntity>>>() {
    };

    /**
     * TypeToken used to recognize FacettedPagedResources&#60;Resource&#60;AbstractEntity&#62;&#62;
     */
    private static final TypeToken<FacettedPagedResources<Resource<? extends AbstractEntity>>> FACETTED_PAGED_RESOURCES_RESOURCE_ABSTRACT_ENTITY_TYPE_TOKEN = new TypeToken<FacettedPagedResources<Resource<? extends AbstractEntity>>>() {
    };

    /**
     * Plugin service
     */
    private final IPluginService pluginService;

    private final IRuntimeTenantResolver tenantResolver;

    private final ITenantResolver tenantsResolver;

    private final ISubscriber subscriber;

    /**
     * Map of pluginConfiguration to be used for a specific MediaType
     */
    private final Map<String, Map<MediaType, Long>> enabledRepresentationPluginMapByTenant;

    /**
     * Constructor
     *
     * @param pPluginService plugin service
     * @param tenantResolver retrieve current tenant at runtime
     * @param tenantsResolver retrieve all tenants
     * @param subscriber subscribe to AMQP events
     * @throws InstantiationException when error occured on plugin instanciation
     * @throws IllegalAccessException when error occured on plugin instanciation
     * @throws ClassNotFoundException when error occured on plugin instanciation
     */
    public RepresentationHttpMessageConverter(IPluginService pPluginService, IRuntimeTenantResolver tenantResolver,
            ITenantResolver tenantsResolver, ISubscriber subscriber)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        super();
        this.tenantResolver = tenantResolver;
        this.tenantsResolver = tenantsResolver;
        pluginService = pPluginService;
        pluginService.addPluginPackage(IRepresentation.class.getPackage().getName());
        this.subscriber = subscriber;
        enabledRepresentationPluginMapByTenant = Collections.synchronizedMap(new HashMap<>());
        for (String tenant : tenantsResolver.getAllActiveTenants()) {
            HashMap<MediaType, Long> enabledRepresentationPluginMap = new HashMap<>();
            tenantResolver.forceTenant(tenant);
            List<PluginConfiguration> representationConfigurations = pluginService
                    .getPluginConfigurationsByType(IRepresentation.class);
            for (PluginConfiguration representationConf : representationConfigurations) {
                if (representationConf.isActive()) {
                    // for each plugin conf lets extract handled media type and add it to the map
                    MediaType handledType = ((IRepresentation) Class.forName(representationConf.getPluginClassName())
                            .newInstance()).getHandledMediaType();
                    enabledRepresentationPluginMap.put(handledType, representationConf.getId());
                }
            }
            enabledRepresentationPluginMapByTenant.put(tenant, enabledRepresentationPluginMap);
        }
        subscriber.subscribeTo(PluginConfigurationEvent.class, new PluginConfigurationHandler());
        setDefaultCharset(StandardCharsets.UTF_8);
    }

    @Override
    public boolean canRead(Class<?> pClazz, MediaType pMediaType) {
        // IRepresentation plugin are only to be used for outputs.
        return false;
    }

    @Override
    public boolean canWrite(Class<?> pClazz, MediaType pMediaType) {
        if ((pMediaType == null) || MediaType.ALL.equals(pMediaType)) {
            // if there is no media type into the accept header lets set to the default
            return supports(pClazz);
        }
        return (getCurrentTenantPluginConfigurationFor(pMediaType) != null) && supports(pClazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return new ArrayList<>(enabledRepresentationPluginMapByTenant.get(tenantResolver.getTenant()).keySet());
    }

    /**
     * We should support &#60;? extends AbstractEntity&#62;, Collection&#60;? extends AbstractEntity&#62;, Page&#60;?
     * extends AbstractEntity&#62;, Resource&#60;? extends AbstractEntity&#62;, PagedResources&#60;Resource&#60;?
     * extends AbstractEntity&#62;&#62;
     */
    @Override
    protected boolean supports(Class<?> pClazz) {
        // lets check if it's just AbstractEntity/Collection<AbstractEntity> OR
        // Resource<AbstractEntity>/PagedResources<Resource<AbstractEntity>>
        // why it is working for subtype here and we need supertype in the writeInternal method is a mystery, maybe it
        // is because in the writeInternal method we actually have a parameterizedType and not just the class object
        return FACETTED_PAGED_RESOURCES_RESOURCE_ABSTRACT_ENTITY_TYPE_TOKEN.isSubtypeOf(pClazz)
                || PAGED_RESOURCES_RESOURCE_ABSTRACT_ENTITY_TYPE_TOKEN.isSubtypeOf(pClazz)
                || RESOURCE_ABSTRACT_ENTITY_TYPE_TOKEN.isSubtypeOf(pClazz)
                || ABSTRACT_ENTITY_TYPE_TOKEN.isSubtypeOf(pClazz)
                || COLLECTION_ABSTRACT_ENTITY_TYPE_TOKEN.isSubtypeOf(pClazz);
    }

    private Long getCurrentTenantPluginConfigurationFor(MediaType pMediaType) {
        return enabledRepresentationPluginMapByTenant.get(tenantResolver.getTenant()).get(pMediaType);
    }

    @Override
    public AbstractEntity read(Type pType, Class<?> pContextClass, HttpInputMessage pInputMessage)
            throws IOException, HttpMessageNotReadableException {
        // should never be called as this converter cannot read anything
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void writeInternal(Object entity, Type type, HttpOutputMessage pOutputMessage)
            throws IOException, HttpMessageNotWritableException {
        MediaType contentType = MediaType.parseMediaTypes(pOutputMessage.getHeaders().get(HttpHeaders.CONTENT_TYPE))
                .get(0);
        Long confIdToUse = getCurrentTenantPluginConfigurationFor(contentType);
        try {
            IRepresentation pluginToUse = pluginService.getPlugin(confIdToUse);
            // lets determine which type we are eventually writing
            if (ABSTRACT_ENTITY_TYPE_TOKEN.isSupertypeOf(type)) {
                pOutputMessage.getBody()
                        .write(pluginToUse.transform((AbstractEntity) entity, contentType.getCharset()));
            } else {
                if (COLLECTION_ABSTRACT_ENTITY_TYPE_TOKEN.isSupertypeOf(type)) {
                    pOutputMessage.getBody().write(pluginToUse.transform((Collection<AbstractEntity>) entity,
                                                                         contentType.getCharset()));
                } else {
                    if (isFacettedPagedResources(type)) {
                        // canWrite has already assured us that if it is a FacettedPagedResources it is a
                        // FacettedPagedResources<Resource<? extends AbstractEntity>>
                        pOutputMessage.getBody()
                                .write(pluginToUse.transform((FacettedPagedResources<Resource<AbstractEntity>>) entity,
                                                             contentType.getCharset()));
                    } else {
                        if (isPagedResources(type)) {
                            // canWrite has already assured us that if it is a PagedResources it is a
                            // PagedResources<Resource<? extends AbstractEntity>>
                            pOutputMessage.getBody()
                                    .write(pluginToUse.transform((PagedResources<Resource<AbstractEntity>>) entity,
                                                                 contentType.getCharset()));
                        } else {
                            // As we can only handle AbstractEntity or Collection<AbstractEntity> or
                            // PagedResources<Resource<AbstractEntity>> or Resource<AbstractEntity>, now we only have
                            // Resource<AbstractEntity> left
                            pOutputMessage.getBody().write(pluginToUse.transform((Resource<AbstractEntity>) entity,
                                                                                 contentType.getCharset()));
                        }
                    }
                }
            }
            pOutputMessage.getBody().flush();
        } catch (ModuleException e) {
            String message = String.format("Could not instanciate a plugin for the required media type %s",
                                           contentType);
            LOG.error(message, e);
            throw new HttpMessageConversionException(message, e); // thrown if the configuration is deleted
        }
    }

    /**
     * cannot be used with the support method because the class object given is not recognized as a ParameterizedType,
     * but it is into writeInternal
     */
    private boolean isPagedResources(Type pType) {
        if (!(pType instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterized = (ParameterizedType) pType;
        Type rawType = parameterized.getRawType();
        if (!(rawType instanceof Class)) {
            return false;
        }
        Class<?> rawClass = (Class<?>) rawType;
        return PagedResources.class.isAssignableFrom(rawClass);
    }

    /**
     * clone of isPagedResources for FacettedPagedResources
     */
    private boolean isFacettedPagedResources(Type pType) {
        if (!(pType instanceof ParameterizedType)) {
            return false;
        }
        ParameterizedType parameterized = (ParameterizedType) pType;
        Type rawType = parameterized.getRawType();
        if (!(rawType instanceof Class)) {
            return false;
        }
        Class<?> rawClass = (Class<?>) rawType;
        return FacettedPagedResources.class.isAssignableFrom(rawClass);
    }

    @Override
    protected AbstractEntity readInternal(Class<?> pClazz, HttpInputMessage pInputMessage)
            throws IOException, HttpMessageNotReadableException {
        // should never be called as this converter cannot read anything
        return null;
    }

    private void handleCreation(String tenantOfEvent, PluginConfigurationEvent event, Long confId) {
        try {
            PluginConfiguration conf = pluginService.getPluginConfiguration(confId);
            if (conf.isActive()) {
                MediaType newMediaType = ((IRepresentation) Class.forName(conf.getPluginClassName()).newInstance())
                        .getHandledMediaType();
                enabledRepresentationPluginMapByTenant.get(tenantOfEvent).put(newMediaType, confId);
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOG.error("Couldn't add the newly defined Representation plugin for tenant: {}, configuration id: {}",
                      tenantOfEvent, event.getPluginConfId());
            throw new RuntimeException(e);// NOSONAR
        } catch (ModuleException e) {
            // if the event represent a creation and the configuration has already been removed then
            // lets do nothing
            LOG.debug("try to add a representation plugin which configuration has already been removed", e);
        }
    }

    private void handleActivation(String tenantOfEvent, PluginConfigurationEvent event, Long confId) {
        try {
            PluginConfiguration conf = pluginService.getPluginConfiguration(confId);
            MediaType newMediaType = ((IRepresentation) Class.forName(conf.getPluginClassName()).newInstance())
                    .getHandledMediaType();
            enabledRepresentationPluginMapByTenant.get(tenantOfEvent).put(newMediaType, confId);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            LOG.error("Couldn't add the newly activated Representation plugin for tenant: {}, configuration id: {}",
                      tenantOfEvent, event.getPluginConfId());
            throw new RuntimeException(e);// NOSONAR
        } catch (ModuleException e) {
            // if the event represents an activation and the configuration has already been removed then
            // we do not have anything to do because the plugin was previously desactivated
            LOG.debug("try to add a representation plugin which configuration has already been removed", e);
        }
    }

    private void handleDesactivationAndDelete(String tenantOfEvent, Long confId) {
        Map<MediaType, Long> enabledRepresentationForDeterminedTenant = enabledRepresentationPluginMapByTenant
                .get(tenantOfEvent);
        enabledRepresentationForDeterminedTenant.entrySet().removeIf(entry -> entry.getValue().equals(confId));
    }

    private class PluginConfigurationHandler implements IHandler<PluginConfigurationEvent> {

        @Override
        public void handle(TenantWrapper<PluginConfigurationEvent> pWrapper) {
            String tenantOfEvent = pWrapper.getTenant();
            tenantResolver.forceTenant(tenantOfEvent);
            PluginConfigurationEvent event = pWrapper.getContent();
            if (event.getPluginTypes().contains(IRepresentation.class.getName())) {
                Long confId = event.getPluginConfId();
                switch (event.getAction()) {
                    case CREATE:
                        handleCreation(tenantOfEvent, event, confId);
                        break;
                    case ACTIVATE:
                        handleActivation(tenantOfEvent, event, confId);
                        break;
                    // desactivation and deletion of a configuration for a representation plugin means the same thing
                    // for the message handler: we cannot handle the corresponding MediaType so lets just remove it from
                    // those we can handle
                    case DESACTIVATE:
                    case DELETE:
                        handleDesactivationAndDelete(tenantOfEvent, confId);
                        break;
                    default:
                        break;
                }
            }
        }

    }
}
