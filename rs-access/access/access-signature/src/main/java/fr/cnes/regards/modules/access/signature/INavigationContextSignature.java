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

import fr.cnes.regards.modules.access.domain.NavigationContext;
import fr.cnes.regards.modules.core.exception.AlreadyExistingException;
import fr.cnes.regards.modules.core.exception.EntityNotFoundException;

/**
 * 
 * @author Christophe Mertz
 *
 */
@RequestMapping("/tiny")
public interface INavigationContextSignature {

    /**
     * 
     * @param pTinyUrl
     * @return
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> load(@PathVariable("navCtxId") Long pNavCtxId)
            throws EntityNotFoundException;

    /**
     * 
     * @param pTinyUrl
     * @param pNavigationContext
     * @return
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> update(@PathVariable("navCtxId") Long pNavCtxId, @Valid @RequestBody NavigationContext pNavigationContext)
            throws EntityNotFoundException;

    /**
     * 
     * @param pTinyUrl
     */
    @RequestMapping(value = "/url/{navCtxId}", method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Void> delete(@PathVariable("navCtxId") Long pNavCtxId) throws EntityNotFoundException;

    /**
     * 
     * @return
     */
    @RequestMapping(value = "/urls", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<List<Resource<NavigationContext>>> list();

    @RequestMapping(value = "/urls", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    ResponseEntity<Resource<NavigationContext>> create(@Valid @RequestBody NavigationContext pNavigationContext)
            throws AlreadyExistingException;

}
