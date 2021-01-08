/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.controller;

import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BID_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.BID_USERROLE_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.CONFIG_SUFFIX;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.PROCESSPLUGIN_PATH;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.PROCESS_BUSINESS_ID_PARAM;
import static fr.cnes.regards.modules.processing.ProcessingConstants.Path.Param.USER_ROLE_PARAM;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.cnes.regards.framework.hateoas.IResourceController;
import fr.cnes.regards.framework.hateoas.IResourceService;
import fr.cnes.regards.framework.hateoas.LinkRels;
import fr.cnes.regards.framework.hateoas.MethodParamFactory;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.endpoint.IAuthoritiesProvider;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.modules.processing.dto.ProcessPluginConfigurationRightsDTO;
import fr.cnes.regards.modules.processing.service.IProcessPluginConfigService;
import fr.cnes.regards.modules.processing.service.ProcessPluginConfigService.DeleteAttemptOnUsedProcessException;

/**
 * This class is he controller for manipulating {@link fr.cnes.regards.modules.processing.entity.RightsPluginConfiguration}s.
 *
 * @author gandrieu
 */
@RestController
@RequestMapping(path = PROCESSPLUGIN_PATH + CONFIG_SUFFIX)
public class ProcessPluginConfigController implements IResourceController<ProcessPluginConfigurationRightsDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessPluginConfigController.class);

    private final IProcessPluginConfigService rightsConfigService;

    private final IResourceService resourceService;

    private final IAuthoritiesProvider authoritiesProvider;

    @Autowired
    public ProcessPluginConfigController(IAuthoritiesProvider authoritiesProvider,
            IProcessPluginConfigService rightsConfigService, IResourceService resourceService) {
        this.rightsConfigService = rightsConfigService;
        this.resourceService = resourceService;
        this.authoritiesProvider = authoritiesProvider;
    }

    @GetMapping
    @ResourceAccess(description = "Find all registered configured processes", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<Collection<EntityModel<ProcessPluginConfigurationRightsDTO>>>

            findAll(@RequestParam(required = false) String processNameLike) {
        Collection<ProcessPluginConfigurationRightsDTO> allRightsPluginConfigs = rightsConfigService
                .findAllRightsPluginConfigs();
        final Collection<ProcessPluginConfigurationRightsDTO> filteredRightsPluginConfigs;
        if (processNameLike != null) {
            filteredRightsPluginConfigs = allRightsPluginConfigs.stream()
                    .filter(dto -> dto.getPluginConfiguration().getLabel().matches("^.*" + processNameLike + ".*$"))
                    .filter(dto -> authoritiesProvider.shouldAccessToResourceRequiring(dto.getRights().getRole()))
                    .collect(Collectors.toSet());
        } else {
            filteredRightsPluginConfigs = allRightsPluginConfigs.stream()
                    .filter(dto -> authoritiesProvider.shouldAccessToResourceRequiring(dto.getRights().getRole()))
                    .collect(Collectors.toSet());
        }
        return ResponseEntity
                .ok(filteredRightsPluginConfigs.stream().map(this::toResource).collect(Collectors.toList()));
    }

    @PostMapping(consumes = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_JSON_UTF8_VALUE })
    @ResourceAccess(description = "Create a process configuration from a plugin", role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<ProcessPluginConfigurationRightsDTO>> create(
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto) throws EntityNotFoundException {
        LOGGER.info("Creating plugin from {}", rightsDto);
        return new ResponseEntity<>(toResource(rightsConfigService.create(rightsDto)), HttpStatus.CREATED);
    }

    @GetMapping(path = BID_SUFFIX)
    @ResourceAccess(description = "Find a configured process by its business uuid", role = DefaultRole.REGISTERED_USER)
    public ResponseEntity<EntityModel<ProcessPluginConfigurationRightsDTO>> findByBusinessId(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId) {
        return ResponseEntity.ok(toResource(rightsConfigService.findByBusinessId(processBusinessId)));
    }

    @PutMapping(path = BID_SUFFIX)
    @ResourceAccess(description = "Update the given process with the given rights configuration",
            role = DefaultRole.ADMIN)
    public ResponseEntity<EntityModel<ProcessPluginConfigurationRightsDTO>> update(
            @PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestBody ProcessPluginConfigurationRightsDTO rightsDto) throws ModuleException {
        return ResponseEntity.ok(toResource(rightsConfigService.update(processBusinessId, rightsDto)));
    }

    @DeleteMapping(path = BID_SUFFIX)
    @ResourceAccess(description = "Delete the given process", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> delete(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId) {
        try {
            rightsConfigService.delete(processBusinessId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (DeleteAttemptOnUsedProcessException | ModuleException e) {
            LOGGER.error(e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    @PutMapping(path = BID_USERROLE_SUFFIX)
    @ResourceAccess(description = "Attache the given role to the given process", role = DefaultRole.ADMIN)
    public ResponseEntity<Void> attachRoleToProcess(@PathVariable(PROCESS_BUSINESS_ID_PARAM) UUID processBusinessId,
            @RequestParam(USER_ROLE_PARAM) String userRole) {
        rightsConfigService.attachRoleToProcess(processBusinessId, userRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    public EntityModel<ProcessPluginConfigurationRightsDTO> toResource(
            final ProcessPluginConfigurationRightsDTO element, final Object... extras) {
        final EntityModel<ProcessPluginConfigurationRightsDTO> resource = resourceService.toResource(element);
        PluginConfiguration pluginConfiguration = element.getPluginConfiguration();
        if (pluginConfiguration != null) {
            String businessIdStr = pluginConfiguration.getBusinessId();
            UUID businessId = UUID.fromString(businessIdStr);
            resourceService.addLink(resource, this.getClass(), "findByBusinessId", LinkRels.SELF,
                                    MethodParamFactory.build(UUID.class, businessId));
            resourceService.addLink(resource, this.getClass(), "update", LinkRels.UPDATE,
                                    MethodParamFactory.build(UUID.class, businessId),
                                    MethodParamFactory.build(ProcessPluginConfigurationRightsDTO.class));
            if (rightsConfigService.canDelete(businessId)) {
                resourceService.addLink(resource, this.getClass(), "delete", LinkRels.DELETE,
                                        MethodParamFactory.build(UUID.class, businessId));
            }
        }
        return resource;
    }
}
