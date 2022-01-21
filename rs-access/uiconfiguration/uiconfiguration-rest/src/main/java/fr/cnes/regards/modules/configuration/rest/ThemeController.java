/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.configuration.rest;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.data.web.SortDefault;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
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
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import fr.cnes.regards.modules.configuration.domain.Theme;
import fr.cnes.regards.modules.configuration.service.IThemeService;

/**
 * REST controller for the microservice Access
 * @author Sébastien Binda
 */
@RestController
@RequestMapping(ThemeController.ROOT_MAPPING)
public class ThemeController implements IResourceController<Theme> {

    public static final String ROOT_MAPPING = "/themes";

    public static final String THEME_ID_MAPPING = "/{themeId}";

    @Autowired
    private IThemeService service;

    @Autowired
    private IResourceService resourceService;

    /**
     * Entry point to retrieve a themes for a given application id {@link Theme}.
     * @param themeId
     * @return {@link UILayout}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = THEME_ID_MAPPING, method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve an IHM theme", role = DefaultRole.PUBLIC)
    public HttpEntity<EntityModel<Theme>> retrieveTheme(@PathVariable("themeId") Long themeId)
            throws EntityNotFoundException {
        return new ResponseEntity<>(toResource(service.retrieveTheme(themeId)), HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all themes
     * @param pageable
     * @param assembler
     * @return {@link Theme}
     */
    @RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to retrieve HMI themes", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedModel<EntityModel<Theme>>> retrieveThemes(
            @SortDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable,
            PagedResourcesAssembler<Theme> assembler) {
        PagedModel<EntityModel<Theme>> resources = toPagedResources(service.retrieveThemes(pageable), assembler);
        return new ResponseEntity<>(resources, HttpStatus.OK);
    }

    /**
     * Entry point to save a new theme
     * @param theme
     * @return {@link Theme}
     */
    @RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to save a new HMI Theme", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Theme>> saveTheme(@Valid @RequestBody Theme theme) {
        return new ResponseEntity<>(toResource(service.saveTheme(theme)), HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm theme.
     * @param themeId
     * @param theme
     * @return {@link Theme}
     * @throws EntityException
     */
    @RequestMapping(value = THEME_ID_MAPPING, method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to update an HMI theme", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Theme>> updateTheme(@PathVariable("themeId") Long themeId,
            @Valid @RequestBody Theme theme) throws EntityException {
        if (!theme.getId().equals(themeId)) {
            throw new EntityInvalidException("Invalide application identifier for theme");
        }
        return new ResponseEntity<>(toResource(service.updateTheme(theme)), HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm theme.
     * @param themeId
     * @return {@link Theme}
     * @throws EntityNotFoundException
     */
    @RequestMapping(value = THEME_ID_MAPPING, method = RequestMethod.DELETE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @ResourceAccess(description = "Endpoint to delete a theme", role = DefaultRole.PROJECT_ADMIN)
    public HttpEntity<EntityModel<Void>> deleteTheme(@PathVariable("themeId") Long themeId)
            throws EntityNotFoundException {
        service.deleteTheme(themeId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<Theme> toResource(final Theme element, final Object... extras) {
        final EntityModel<Theme> resource = resourceService.toResource(element);
        resourceService.addLink(resource, this.getClass(), "retrieveTheme", LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource, this.getClass(), "updateTheme", LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(Theme.class));
        resourceService.addLink(resource, this.getClass(), "deleteTheme", LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }

}
