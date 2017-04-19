/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.domain;

import java.nio.charset.Charset;
import java.util.Collection;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;

/**
 * IRepresentation are used in a HttpMessageConverter so we can transform http outputs to different MIME Type. Each
 * plugin should handle a different MIME Type. For now dynamic parameters are not handled
 *
 * @author Sylvain Vissiere-Guerinet
 */
@PluginInterface(description = "plugin interface for representation plugins")
public interface IRepresentation {

    /**
     * Should return the constant value corresponding to the MediaType the implemention handle
     *
     * @return handled media type of this implementation
     */
    MediaType getHandledMediaType();

    /**
     * This method is used to set the body of the response and so should transform an AbstractIndexedEntity in the
     * format the plugin is handling
     *
     * @param pToBeTransformed AbstractIndexedEntity to be sent in the Http Response
     * @param pTargetCharset charset to use for encoding (might be null)
     * @return Byte array to include in the response
     */
    byte[] transform(AbstractEntity pToBeTransformed, Charset pTargetCharset);

    byte[] transform(Collection<AbstractEntity> pEntity, Charset pCharset);

    byte[] transform(PagedResources<Resource<AbstractEntity>> pEntity, Charset pCharset);

    byte[] transform(Resource<AbstractEntity> pEntity, Charset pCharset);

}
