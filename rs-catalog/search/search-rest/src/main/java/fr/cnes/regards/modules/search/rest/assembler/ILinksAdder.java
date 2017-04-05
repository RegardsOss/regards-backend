/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.search.rest.assembler;

import org.springframework.hateoas.Resource;

import fr.cnes.regards.modules.entities.domain.Dataset;

/**
 * Functionnal interface for any class/service able to add HATEOAS links to a HATEOAS resource.
 * For example, it can be implemented by a controller or a resources assembler.
 *
 * @author Xavier-Alexandre Brochard
 */
@FunctionalInterface
public interface ILinksAdder {

    /**
     * Add links to the passed resource
     * @param pResource
     * @return the resource augmented with links
     */
    Resource<Dataset> addLinks(Resource<Dataset> pResource);

}