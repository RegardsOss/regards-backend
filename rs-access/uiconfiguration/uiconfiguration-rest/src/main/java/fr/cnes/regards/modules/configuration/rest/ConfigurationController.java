package fr.cnes.regards.modules.configuration.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Configuration;
import fr.cnes.regards.modules.configuration.service.IConfigurationService;

/**
 * REST controller for the microservice Access
 *
 * @author Kevin Marchois
 *
 */
@RestController
@RequestMapping("/configuration")
public class ConfigurationController implements IResourceController<Configuration> {

	@Autowired
	private IConfigurationService configurationService;
	
	@Autowired
    private IResourceService resourceService;
	
	/**
     * Entry point to retrieve a {@link Configuration}
     * @param applicationId
     *
     * @return {@link Configuration}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve Configuration for the given applicationId",
            role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<Configuration>> retrieveConfiguration(@PathVariable("applicationId") final String applicationId)
            throws EntityNotFoundException {
        final Configuration layout = configurationService.retrieveConfiguration(applicationId);
        final Resource<Configuration> resource = toResource(layout);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }
    
    /**
     * Entry point to add a {@link Configuration}
     * @param applicationId
     *
     * @return {@link Configuration}
     * @throws ModuleException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to add a Configuration",
            role = DefaultRole.ADMIN)
    public HttpEntity<Resource<Configuration>> addConfiguration(@PathVariable("applicationId") final String applicationId, 
    		@Valid @RequestBody Configuration toAdd)
            throws ModuleException {
        final Configuration conf = configurationService.addConfiguration(toAdd);
        final Resource<Configuration> resource = toResource(conf);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }
    
    /**
     * Entry point to update a {@link Configuration}
     * @param applicationId
     *
     * @return {@link Configuration}
     * @throws ModuleException
     */
    @RequestMapping(value = "/{applicationId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update a Configuration",
            role = DefaultRole.ADMIN)
    public HttpEntity<Resource<Configuration>> updateConfiguration(@PathVariable("applicationId") final String applicationId, 
    		@Valid @RequestBody Configuration toAdd)
            throws ModuleException {
        final Configuration conf = configurationService.updateConfiguration(toAdd);
        final Resource<Configuration> resource = toResource(conf);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }
    
	@Override
    public Resource<Configuration> toResource(final Configuration element, final Object... extras) {
        final Resource<Configuration> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveConfiguration", LinkRels.SELF,
                                MethodParamFactory.build(String.class, element.getApplicationId()));
        resourceService.addLink(resource, this.getClass(), "addConfiguration", LinkRels.CREATE,
                MethodParamFactory.build(String.class, element.getApplicationId()),
                MethodParamFactory.build(Configuration.class));
        resourceService.addLink(resource, this.getClass(), "updateConfiguration", LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, element.getApplicationId()),
                                		MethodParamFactory.build(Configuration.class));
        return resource;
    }

}
