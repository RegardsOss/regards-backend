/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.domain.IRepresentation;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
public class IRepresentationHttpMessageConverter implements HttpMessageConverter<AbstractEntity> {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(IRepresentationHttpMessageConverter.class);

    /**
     * Plugin service
     */
    private final IPluginService pluginService;

    /**
     * Map of pluginConfiguration to be used for a specific MediaType
     *
     * TODO: handle deletion of pluginConf
     */
    private final Map<MediaType, PluginConfiguration> enabledRepresentationPluginMap;

    /**
     * Constructor
     * @param pPluginService Plugin service
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public IRepresentationHttpMessageConverter(IPluginService pPluginService)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        super();
        pluginService = pPluginService;
        List<PluginConfiguration> representationConfigurations = pluginService
                .getPluginConfigurationsByType(IRepresentation.class);
        enabledRepresentationPluginMap = new HashMap<>();
        for (PluginConfiguration representationConf : representationConfigurations) {
            MediaType handledType = ((IRepresentation) Class.forName(representationConf.getPluginClassName())
                    .newInstance()).getHandledMediaType();
            enabledRepresentationPluginMap.put(handledType, representationConf);
        }

    }

    @Override
    public boolean canRead(Class<?> pClazz, MediaType pMediaType) {
        // IRepresentation plugin are only to be used for outputs.
        return false;
    }

    @Override
    public boolean canWrite(Class<?> pClazz, MediaType pMediaType) {
        return AbstractEntity.class.isInstance(pClazz);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return new ArrayList<>(enabledRepresentationPluginMap.keySet());
    }

    @Override
    public AbstractEntity read(Class<? extends AbstractEntity> pClazz, HttpInputMessage pInputMessage)
            throws IOException {
        // should never be called as this converter cannot read anything
        return null;
    }

    @Override
    public void write(AbstractEntity entity, MediaType pContentType, HttpOutputMessage pOutputMessage)
            throws IOException {
        PluginConfiguration confToUse = enabledRepresentationPluginMap.get(pContentType);
        try {
            IRepresentation pluginToUse = pluginService.getPlugin(confToUse.getId());
            pOutputMessage.getBody().write(pluginToUse.transform(entity, pContentType.getCharset()));
            pOutputMessage.getBody().flush();
        } catch (ModuleException e) {
            LOG.error(String.format("Could not instance a plugin for the required media type %s", pContentType), e);
        }
    }

}
