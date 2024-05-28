package fr.cnes.regards.modules.configuration.rest;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.ConfigurationDTO;
import fr.cnes.regards.modules.configuration.domain.UIConfiguration;
import fr.cnes.regards.modules.configuration.service.IUIConfigurationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the microservice Access
 *
 * @author Kevin Marchois
 */
@RestController
@RequestMapping("/configuration")
public class UIConfigurationController implements IResourceController<ConfigurationDTO> {

    public static final String CONFIGURATION_PATH = "/configuration";

    public static final String APPLICATION_ID_PATH = "/{applicationId}";

    @Autowired
    private IUIConfigurationService configurationService;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a {@link UIConfiguration}
     *
     * @return {@link UIConfiguration}
     */
    @RequestMapping(value = APPLICATION_ID_PATH,
                    method = RequestMethod.GET,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve Configuration for the given applicationId",
                    role = DefaultRole.PUBLIC)
    public HttpEntity<EntityModel<ConfigurationDTO>> retrieveConfiguration(
        @PathVariable("applicationId") final String applicationId) {
        String conf;
        try {
            conf = configurationService.retrieveConfiguration(applicationId);
            final EntityModel<ConfigurationDTO> resource = toResource(new ConfigurationDTO(conf),
                                                                      new Object[] { applicationId });
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    /**
     * Entry point to add a {@link UIConfiguration}
     *
     * @return {@link UIConfiguration}
     */
    @RequestMapping(value = APPLICATION_ID_PATH,
                    method = RequestMethod.POST,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to add a Configuration", role = DefaultRole.ADMIN)
    public HttpEntity<EntityModel<ConfigurationDTO>> addConfiguration(
        @PathVariable("applicationId") final String applicationId, @Valid @RequestBody ConfigurationDTO toAdd) {
        final String conf = configurationService.addConfiguration(toAdd.getConfiguration(), applicationId);
        final EntityModel<ConfigurationDTO> resource = toResource(new ConfigurationDTO(conf),
                                                                  new Object[] { applicationId });
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to update a {@link UIConfiguration}
     *
     * @return {@link UIConfiguration}
     */
    @RequestMapping(value = APPLICATION_ID_PATH,
                    method = RequestMethod.PUT,
                    produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update a Configuration", role = DefaultRole.ADMIN)
    public HttpEntity<EntityModel<ConfigurationDTO>> updateConfiguration(
        @PathVariable("applicationId") final String applicationId, @Valid @RequestBody ConfigurationDTO toAdd) {
        String conf;
        try {
            conf = configurationService.updateConfiguration(toAdd.getConfiguration(), applicationId);
            final EntityModel<ConfigurationDTO> resource = toResource(new ConfigurationDTO(conf),
                                                                      new Object[] { applicationId });
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } catch (EntityNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
    }

    @Override
    public EntityModel<ConfigurationDTO> toResource(final ConfigurationDTO element, final Object... extras) {
        final EntityModel<ConfigurationDTO> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveConfiguration",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, String.valueOf(extras[0])));
        resourceService.addLink(resource,
                                this.getClass(),
                                "addConfiguration",
                                LinkRels.CREATE,
                                MethodParamFactory.build(String.class, String.valueOf(extras[0])),
                                MethodParamFactory.build(ConfigurationDTO.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateConfiguration",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, String.valueOf(extras[0])),
                                MethodParamFactory.build(ConfigurationDTO.class));
        return resource;
    }

}
