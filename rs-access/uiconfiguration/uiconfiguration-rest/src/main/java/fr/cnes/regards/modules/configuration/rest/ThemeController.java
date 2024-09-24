/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityException;
import fr.cnes.regards.framework.module.rest.exception.EntityInvalidException;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.configuration.domain.Theme;
import fr.cnes.regards.modules.configuration.domain.UILayout;
import fr.cnes.regards.modules.configuration.service.IThemeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the microservice Access
 *
 * @author Sébastien Binda
 */
@Tag(name = "Theme controller")
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
     *
     * @return {@link UILayout}
     */
    @GetMapping(value = THEME_ID_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to retrieve an IHM theme by its identifier", role = DefaultRole.PUBLIC)
    @Operation(summary = "Get an UI theme", description = "Retrieve an UI theme by its identifier")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Returns the UI theme") })
    public HttpEntity<EntityModel<Theme>> retrieveTheme(@PathVariable("themeId") Long themeId)
        throws EntityNotFoundException {
        return new ResponseEntity<>(toResource(service.retrieveTheme(themeId)), HttpStatus.OK);
    }

    /**
     * Entry point to retrieve all themes
     *
     * @return {@link Theme}
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get HMI themes", description = "Return a page of HMI themes")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "All HMI themes were retrieved.") })
    @ResourceAccess(description = "Endpoint to retrieve HMI themes", role = DefaultRole.PUBLIC)
    public HttpEntity<PagedModel<EntityModel<Theme>>> retrieveThemes(
        @SortDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable,
        @Parameter(hidden = true) PagedResourcesAssembler<Theme> assembler) {
        return new ResponseEntity<>(toPagedResources(service.retrieveThemes(pageable), assembler), HttpStatus.OK);
    }

    /**
     * Entry point to save a new theme
     *
     * @return {@link Theme}
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to save a new UI theme", role = DefaultRole.PROJECT_ADMIN)
    @Operation(summary = "Register UI theme", description = "Save a new UI theme")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Return the new UI theme") })
    public HttpEntity<EntityModel<Theme>> saveTheme(@Valid @RequestBody Theme theme) {
        return new ResponseEntity<>(toResource(service.saveTheme(theme)), HttpStatus.OK);
    }

    /**
     * Entry point to save a new ihm theme.
     *
     * @return {@link Theme}
     */
    @PutMapping(value = THEME_ID_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to update an UI theme", role = DefaultRole.PROJECT_ADMIN)
    @Operation(summary = "Update UI theme", description = "Update an UI theme")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Return the updated UI theme") })
    public HttpEntity<EntityModel<Theme>> updateTheme(@PathVariable("themeId") Long themeId,
                                                      @Valid @RequestBody Theme theme) throws EntityException {
        if (!theme.getId().equals(themeId)) {
            throw new EntityInvalidException("Invalide application identifier for theme");
        }
        return new ResponseEntity<>(toResource(service.updateTheme(theme)), HttpStatus.OK);
    }

    /**
     * Entry point to delete an ihm theme.
     *
     * @return {@link Theme}
     */
    @DeleteMapping(value = THEME_ID_MAPPING, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResourceAccess(description = "Endpoint to delete an UI theme by its identifier", role = DefaultRole.PROJECT_ADMIN)
    @Operation(summary = "Delete UI theme", description = "Delete an UI theme by its identifier")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Return the status") })
    public HttpEntity<EntityModel<Void>> deleteTheme(@PathVariable("themeId") Long themeId)
        throws EntityNotFoundException {
        service.deleteTheme(themeId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<Theme> toResource(final Theme element, final Object... extras) {
        final EntityModel<Theme> resource = resourceService.toResource(element);
        resourceService.addLink(resource,
                                this.getClass(),
                                "retrieveTheme",
                                LinkRels.SELF,
                                MethodParamFactory.build(Long.class, element.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateTheme",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(Long.class, element.getId()),
                                MethodParamFactory.build(Theme.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "deleteTheme",
                                LinkRels.DELETE,
                                MethodParamFactory.build(Long.class, element.getId()));
        return resource;
    }

}
