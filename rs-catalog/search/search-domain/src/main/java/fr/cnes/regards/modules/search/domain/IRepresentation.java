/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.domain;

import java.nio.charset.Charset;

import org.springframework.http.MediaType;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 *
 * IRepresentation are used in a HttpMessageConverter so we can transform http outputs to different MIME Type. Each
 * plugin should handle a different MIME Type.
 *
 * @author Sylvain Vissiere-Guerinet
 *
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
     * @param pToBeTransformed
     *            AbstractIndexedEntity to be send in the Http Response
     * @param pTargetCharset
     *            charset to use for encoding (might be null)
     * @return Byte array to include in the response
     */
    byte[] transform(AbstractIndexedEntity pToBeTransformed, Charset pTargetCharset);

}
