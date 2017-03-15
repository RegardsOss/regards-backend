/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.configuration.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedResources;
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
import fr.cnes.regards.framework.module.annotation.ModuleInfo;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Layout;
import fr.cnes.regards.modules.configuration.domain.Theme;
import fr.cnes.regards.modules.configuration.service.IThemeService;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 *
 */
@RestController
@ModuleInfo(name = "Theme", version = "1.0-SNAPSHOT", author = "REGARDS", legalOwner = "CS",
        documentation = "http://test")
@RequestMapping("/themes")
public class ThemeController implements IResourceController<Theme> {

    @Autowired
    private IThemeService service;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a themes for a given application id {@link Theme}.
     *
     * @return {@link Layout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{themeId}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM theme", role = DefaultRole.PUBLIC)
    public HttpEntity<Resource<Theme>> retrieveTheme(@PathVariable("themeId") final Long pThemeId)
            throws EntityNotFoundException {
        final Theme theme = service.retrieveTheme(pThemeId);
        final Resource<Theme> resource = toResource(theme);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all themes
     *
     * @return {@link Theme}
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve IHM themes", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedResources<Resource<Theme>>> retrieveThemes(final Pageable pPageable,
            final PagedResourcesAssembler<Theme> pAssembler) {
        final Page<Theme> themes = service.retrieveThemes(pPageable);
        final PagedResources<Resource<Theme>> resources = toPagedResources(themes, pAssembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Entry point to save a new theme
     *
     * @return {@link Theme}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new IHM Theme", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Theme>> saveTheme(@Valid @RequestBody final Theme pTheme) throws EntityInvalidException {
        final Theme theme = service.saveTheme(pTheme);
        final Resource<Theme> resource = toResource(theme);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm theme.
     *
     * @return {@link Theme}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{themeId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an IHM theme", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Theme>> updateTheme(@PathVariable("themeId") final Long pThemeId,
            @Valid @RequestBody final Theme pTheme) throws EntityException {

        if (!pTheme.getId().equals(pThemeId)) {
            throw new EntityInvalidException("Invalide application identifier for theme");
        }
        final Theme theme = service.updateTheme(pTheme);
        final Resource<Theme> resource = toResource(theme);
        return new ResponseEntity<>(resource, HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm theme.
     *
     * @return {@link Theme}
     * @throws EntityInvalidException
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = "/{themeId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a theme", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<Resource<Void>> deleteTheme(@PathVariable("themeId") final Long pThemeId)
            throws EntityNotFoundException {
        service.deleteTheme(pThemeId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public Resource<Theme> toResource(final Theme pElement, final Object... pExtras) {
        final Resource<Theme> resource = resourceService.toResource(pElement);
        resourceService.addLink(resource, this.getClass(), "retrieveTheme", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "updateTheme", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource, this.getClass(), "deleteTheme", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, pElement.getId()));
        return resource;
    }

}
