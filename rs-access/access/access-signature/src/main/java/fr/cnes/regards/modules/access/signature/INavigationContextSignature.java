/*
 * LICENSE_PLACEHOLDER
 */

package fr.cnes.regards.modules.access.signature;

import java.util.List;

import javax.validation.Valid;

import org.springframework.hateoas.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import fr.cnes.regards.framework.module.rest.exception.AlreadyExistingException;
import fr.cnes.regards.framework.module.rest.exception.ModuleAlreadyExistsException;
import fr.cnes.regards.framework.module.rest.exception.ModuleEntityNotFoundException;
import fr.cnes.regards.modules.access.domain.NavigationContext;

/**
 * 
 * @author Christophe Mertz
 *
 */
@RequestMapping("/tiny")
public interface INavigationContextSignature {

    /**
     * Load a {@link NavigationContext}
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @return the loaded {@link NavigationContext}
     * @throws ModuleEntityNotFoundException
     *             throw if an error occurs
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> load(@PathVariable("navCtxId") Long pNavCtxId)
            throws ModuleEntityNotFoundException;

    /**
     * Update a {@link NavigationContext}.
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @param pNavigationContext
     *            the {@link NavigationContext} to update
     * @return the updated {@link NavigationContext}
     * @throws ModuleEntityNotFoundException
     *             throw if an error occurs
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> update(@PathVariable("navCtxId") Long pNavCtxId,
            @Valid @RequestBody NavigationContext pNavigationContext) throws ModuleEntityNotFoundException;

    /**
     * Delete a {@link NavigationContext}.
     * 
     * @param pNavCtxId
     *            the id of the {@link NavigationContext} to load
     * @return void
     * @throws ModuleEntityNotFoundException
     *             throw if an error occurs
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> delete(@PathVariable("navCtxId") Long pNavCtxId) throws ModuleEntityNotFoundException;

    /**
     * Lists all the {@link} {@link NavigationContext}.
     * 
     * @return a list of {@link NavigationContext}
     */
    @RequestMapping(value = "/urls", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<NavigationContext>>> list();

    /**
     * Create a {@link NavigationContext}
     * 
     * @param pNavigationContext
     *            the {@link NavigationContext} to create
     * @return the created {@link NavigationContext}
     * @throws ModuleAlreadyExistsException
     *             the {@link NavigationContext} already exists
     */
    @RequestMapping(value = "/urls", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> create(@Valid @RequestBody NavigationContext pNavigationContext)
            throws ModuleAlreadyExistsException;

}
