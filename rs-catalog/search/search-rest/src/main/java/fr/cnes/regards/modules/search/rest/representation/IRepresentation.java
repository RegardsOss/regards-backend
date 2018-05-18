/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.search.rest.representation;

import java.nio.charset.Charset;
import java.util.Collection;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;
import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.rest.assembler.resource.FacettedPagedResources;

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
     * This method is used to set the body of the response and so should transform an AbstractEntity in the
     * format the plugin is handling
     *
     * @param pToBeTransformed AbstractIndexedEntity to be sent in the Http Response
     * @param pTargetCharset charset to use for encoding (might be null)
     * @return Byte array to include in the response
     */
    byte[] transform(AbstractEntity pToBeTransformed, Charset pTargetCharset);

    /**
     * This method is used to set the body of the response and so should transform an AbstractEntity in the
     * format the plugin is handling
     *
     * @param entities Collection<AbstractEntity>> to be sent in the Http Response
     * @param pCharset charset to use for encoding (might be null)
     * @return Byte array to include in the response
     */
    byte[] transform(Collection<AbstractEntity> entities, Charset pCharset);

    /**
     * This method is used to set the body of the response and so should transform an AbstractEntity in the
     * format the plugin is handling
     *
     * @param pEntity page of AbstractEntity, wrapped into hateoas pojos, to be sent in the Http Response
     * @param pCharset charset to use for encoding (might be null)
     * @return Byte array to include in the response
     */
    byte[] transform(PagedResources<Resource<AbstractEntity>> pEntity, Charset pCharset);

    /**
     * This method is used to set the body of the response and so should transform an AbstractEntity in the
     * format the plugin is handling
     *
     * @param pEntity facetted page of AbstractEntity, wrapped into hateoas pojo, to be sent in the Http Response
     * @param pCharset charset to use for encoding (might be null)
     * @return Byte array to include in the response
     */
    byte[] transform(FacettedPagedResources<Resource<AbstractEntity>> pEntity, Charset pCharset);

    /**
     * This method is used to set the body of the response and so should transform an AbstractEntity in the
     * format the plugin is handling
     *
     * @param pEntity AbstractEntity, wrapped into hateoas pojo, to be sent in the Http Response
     * @param pCharset charset to use for encoding (might be null)
     * @return Byte array to include in the response
     */
    byte[] transform(Resource<AbstractEntity> pEntity, Charset pCharset);

}
