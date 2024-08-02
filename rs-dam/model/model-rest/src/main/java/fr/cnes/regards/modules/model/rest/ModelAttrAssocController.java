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
package fr.cnes.regards.modules.model.rest;

import fr.cnes.regards.framework.hateoas.*;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.dto.PluginMetaData;
import fr.cnes.regards.framework.security.annotation.ResourceAccess;
import fr.cnes.regards.framework.security.role.DefaultRole;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.ModelAttrAssoc;
import fr.cnes.regards.modules.model.domain.TypeMetadataConfMapping;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import fr.cnes.regards.modules.model.service.IModelAttrAssocService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * REST controller for managing {@link ModelAttrAssoc}
 *
 * @author Marc Sordi
 */
@RestController
@RequestMapping(ModelAttrAssocController.BASE_MAPPING)
public class ModelAttrAssocController implements IResourceController<ModelAttrAssoc> {

    /**
     * Base mapping
     */
    public static final String BASE_MAPPING = "/models";

    /**
     * Type mapping
     */
    public static final String TYPE_MAPPING = "/{modelName}/attributes";

    public static final String FRAGMENT_BIND_MAPPING = "/fragments";

    public static final String FRAGMENT_UNBIND_MAPPING = "/fragments/{fragmentId}";

    /**
     * Controller path
     */
    public static final String ASSOCS_MAPPING = "/assocs";

    public static final String ENTITY_ASSOCS_MAPPING = "{datasetUrn}" + ASSOCS_MAPPING;

    /**
     * Controller path
     */
    public static final String COMPUTATION_TYPE_MAPPING = ASSOCS_MAPPING + "/computation/types";

    /**
     * Model attribute association service
     */
    @Autowired
    private IModelAttrAssocService modelAttrAssocService;

    /**
     * Resource service
     */
    @Autowired
    private IResourceService resourceService;

    /**
     * Retrieve model attribute associations for a given entity type (optional)
     *
     * @return the model attribute associations
     */
    @ResourceAccess(description = "Endpoint enabling retrieval of all links between models and attribute for a given type of entity",
                    role = DefaultRole.ADMIN)
    @RequestMapping(path = ASSOCS_MAPPING, method = RequestMethod.GET)
    @Operation(summary = "Models and attrs links by type",
               description = "Endpoint enabling retrieval of all links between models and attribute for a given type of entity")
    public ResponseEntity<Collection<ModelAttrAssoc>> getModelAttrAssocsFor(
        @Parameter(description = "Filter using entity type") @RequestParam(name = "type", required = false)
        EntityType type) {
        return ResponseEntity.ok(modelAttrAssocService.getModelAttrAssocsFor(type));
    }

    /**
     * Retrieve which plugin configuration can be used for which attribute type with which possible metadata
     *
     * @return mappings between attribute type, plugin configurations and metadata
     */
    @ResourceAccess(description = "Endpoint enabling retrieval, for every attribute type, compatible plugin "
                                  + "configurations and plugin metadata", role = DefaultRole.ADMIN)
    @RequestMapping(path = COMPUTATION_TYPE_MAPPING, method = RequestMethod.GET)
    @Operation(summary = "Available plugin conf by attr type",
               description = "Endpoint enabling retrieval, for every attribute type, compatible plugin "
                             + "configurations and plugin metadata")
    @ApiResponses(value = { @ApiResponse(responseCode = "200") })
    public ResponseEntity<List<EntityModel<TypeMetadataResourceConfMapping>>> getMappingForComputedAttribute() {
        return ResponseEntity.ok(HateoasUtils.wrapList(transformToTypeMetadataResourceConfMapping(modelAttrAssocService.retrievePossibleMappingsForComputed())));
    }

    private List<TypeMetadataResourceConfMapping> transformToTypeMetadataResourceConfMapping(List<TypeMetadataConfMapping> typeMetadataConfMappings) {
        List<TypeMetadataResourceConfMapping> shit = new ArrayList<>();
        for (TypeMetadataConfMapping typeMetadataConfMapping : typeMetadataConfMappings) {
            shit.add(new TypeMetadataResourceConfMapping(typeMetadataConfMapping));
        }
        return shit;
    }

    /**
     * Get all {@link ModelAttrAssoc}
     *
     * @param modelName {@link Model} identifier
     * @return list of linked {@link ModelAttrAssoc}
     */
    @ResourceAccess(description = "List all model attribute associations", role = DefaultRole.PUBLIC)
    @RequestMapping(path = TYPE_MAPPING, method = RequestMethod.GET)
    @Operation(summary = "Get model attr assocs",
               description = "Return all model attribute associations matching provided criteria.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The list of model attribute associations.") })
    public ResponseEntity<List<EntityModel<ModelAttrAssoc>>> getModelAttrAssocs(
        @Parameter(description = "Filter using model name") @PathVariable String modelName) {
        return ResponseEntity.ok(toResources(modelAttrAssocService.getModelAttrAssocs(modelName), modelName));
    }

    /**
     * Link an {@link AttributeModel} to a {@link Model} with a {@link ModelAttrAssoc}.<br/>
     * This method is only available for {@link AttributeModel} in <b>default</b> {@link Fragment} (i.e. without name
     * space).
     *
     * @param modelName       {@link Model} identifier
     * @param pModelAttribute {@link ModelAttrAssoc} to link
     * @return the {@link ModelAttrAssoc} representing the link between the {@link Model} and the {@link AttributeModel}
     * @throws ModuleException if assignation cannot be done
     */
    @ResourceAccess(description = "Bind an attribute to a model", role = DefaultRole.ADMIN)
    @RequestMapping(path = TYPE_MAPPING, method = RequestMethod.POST)
    @Operation(summary = "Bind attr to model", description = "Bind an attribute to a model.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "The new attribute model association.") })
    public ResponseEntity<EntityModel<ModelAttrAssoc>> bindAttributeToModel(
        @Parameter(description = "Filter using model name") @PathVariable String modelName,
        @Parameter(description = "Model attribute association to save") @Valid @RequestBody
        ModelAttrAssoc pModelAttribute) throws ModuleException {
        return ResponseEntity.ok(toResource(modelAttrAssocService.bindAttributeToModel(modelName, pModelAttribute),
                                            modelName));
    }

    /**
     * Retrieve a {@link ModelAttrAssoc} linked to a {@link Model} name
     *
     * @param modelName   model name
     * @param attributeId attribute id
     * @return linked model attribute
     * @throws ModuleException if attribute cannot be retrieved
     */
    @ResourceAccess(description = "Get a model attribute association", role = DefaultRole.ADMIN)
    @RequestMapping(method = RequestMethod.GET, value = TYPE_MAPPING + "/{attributeId}")
    @Operation(summary = "Get model attr assoc",
               description = "Return a model attribute association matching criteria.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "A model attribute association.") })
    public ResponseEntity<EntityModel<ModelAttrAssoc>> getModelAttrAssoc(
        @Parameter(description = "Filter using model name") @PathVariable String modelName,
        @Parameter(description = "Filter using attribute id") @PathVariable Long attributeId) throws ModuleException {
        return ResponseEntity.ok(toResource(modelAttrAssocService.getModelAttrAssoc(modelName, attributeId),
                                            modelName));
    }

    /**
     * Allow to update calculation properties
     *
     * @param modelName       model name
     * @param attributeId     attribute id
     * @param pModelAttribute attribute
     * @return update model attribute
     * @throws ModuleException if attribute cannot be updated
     */
    @ResourceAccess(description = "Update a model attribute", role = DefaultRole.ADMIN)
    @RequestMapping(method = RequestMethod.PUT, value = TYPE_MAPPING + "/{attributeId}")
    @Operation(summary = "Update model attr assoc",
               description = "Update a model attribute association matching provided model name and attribute id.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The updated model attribute association.") })
    public ResponseEntity<EntityModel<ModelAttrAssoc>> updateModelAttrAssoc(
        @Parameter(description = "Filter using model name") @PathVariable String modelName,
        @Parameter(description = "Filter using attribute id") @PathVariable Long attributeId,
        @Parameter(description = "Model attribute association to save") @Valid @RequestBody
        ModelAttrAssoc pModelAttribute) throws ModuleException {
        return ResponseEntity.ok(toResource(modelAttrAssocService.updateModelAttribute(modelName,
                                                                                       attributeId,
                                                                                       pModelAttribute), modelName));
    }

    /**
     * Unlink a {@link ModelAttrAssoc} from a {@link Model}.<br/>
     * This method is only available for {@link AttributeModel} in <b>default</b> {@link Fragment} (i.e. without
     * namespace).
     *
     * @param modelName   model name
     * @param attributeId attribute id
     * @return nothing
     * @throws ModuleException if attribute cannot be removed
     */
    @ResourceAccess(description = "Unbind an attribute from a model", role = DefaultRole.ADMIN)
    @RequestMapping(method = RequestMethod.DELETE, value = TYPE_MAPPING + "/{attributeId}")
    @Operation(summary = "Unbind attribute", description = "Unbind an attribute from a model.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The model attribute association has been removed.") })
    public ResponseEntity<Void> unbindAttributeFromModel(
        @Parameter(description = "Model that loose one attribute") @PathVariable String modelName,
        @Parameter(description = "Attribute id to unbind") @PathVariable Long attributeId) throws ModuleException {
        modelAttrAssocService.unbindAttributeFromModel(modelName, attributeId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Link all {@link AttributeModel} of a particular {@link Fragment} to a model creating {@link ModelAttrAssoc}.<br/>
     * This method is only available for {@link AttributeModel} in a <b>particular</b> {@link Fragment} (i.e. with name
     * space, not default one).
     *
     * @param modelName model name
     * @param fragment  fragment
     * @return linked model attributes
     * @throws ModuleException if binding cannot be done
     */
    @ResourceAccess(description = "Bind fragment attributes to a model", role = DefaultRole.ADMIN)
    @RequestMapping(method = RequestMethod.POST, value = TYPE_MAPPING + FRAGMENT_BIND_MAPPING)
    @Operation(summary = "Bind fragment", description = "Bind fragment attributes to a model.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200",
                                         description = "The list of model attribute associations created by the bind "
                                                       + "between the model and the fragment.") })
    public ResponseEntity<List<EntityModel<ModelAttrAssoc>>> bindNSAttributeToModel(
        @Parameter(description = "Model receiving the fragment") @PathVariable String modelName,
        @Parameter(description = "Fragment to add to the model") @Valid @RequestBody Fragment fragment)
        throws ModuleException {
        return ResponseEntity.ok(toResources(modelAttrAssocService.bindNSAttributeToModel(modelName, fragment),
                                             modelName));
    }

    /**
     * Unlink all {@link AttributeModel} of a particular {@link Fragment} from a model deleting all associated
     * {@link ModelAttrAssoc}.<br/>
     * This method is only available for {@link AttributeModel} in a <b>particular</b> {@link Fragment} (i.e. with name
     * space, not default one).
     *
     * @param modelName  model name
     * @param fragmentId fragment identifier
     * @return linked model attributes
     */
    @ResourceAccess(description = "Unbind fragment attributes from a model", role = DefaultRole.ADMIN)
    @RequestMapping(method = RequestMethod.DELETE, value = TYPE_MAPPING + FRAGMENT_UNBIND_MAPPING)
    @Operation(summary = "Unbind fragment", description = "Unbind fragment attributes from a model.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fragment is not binded to the model.") })
    public ResponseEntity<Void> unbindNSAttributeFromModel(
        @Parameter(description = "Filter using model name") @PathVariable String modelName,
        @Parameter(description = "Fragment id to unbind") @PathVariable Long fragmentId) {
        modelAttrAssocService.unbindNSAttributeToModel(modelName, fragmentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public EntityModel<ModelAttrAssoc> toResource(ModelAttrAssoc pElement, Object... pExtras) {
        final EntityModel<ModelAttrAssoc> resource = resourceService.toResource(pElement);

        String modelName = (String) pExtras[0];

        resourceService.addLink(resource,
                                this.getClass(),
                                "getModelAttrAssoc",
                                LinkRels.SELF,
                                MethodParamFactory.build(String.class, modelName),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "updateModelAttrAssoc",
                                LinkRels.UPDATE,
                                MethodParamFactory.build(String.class, modelName),
                                MethodParamFactory.build(Long.class, pElement.getId()),
                                MethodParamFactory.build(ModelAttrAssoc.class));
        resourceService.addLink(resource,
                                this.getClass(),
                                "unbindAttributeFromModel",
                                LinkRels.DELETE,
                                MethodParamFactory.build(String.class, modelName),
                                MethodParamFactory.build(Long.class, pElement.getId()));
        resourceService.addLink(resource,
                                this.getClass(),
                                "getModelAttrAssocs",
                                LinkRels.LIST,
                                MethodParamFactory.build(String.class, modelName));
        return resource;
    }

    /**
     * transform {@link TypeMetadataConfMapping} pluginConfigurations and pluginMetaDatas into Collection of resources
     */
    @SuppressWarnings("unused")
    private static class TypeMetadataResourceConfMapping {

        /**
         * Attribute type
         */
        private PropertyType attrType;

        /**
         * Wrapped plugin configurations
         */
        private Collection<EntityModel<PluginConfiguration>> pluginConfigurations;

        /**
         * Wrapped plugin metadata
         */
        private Collection<EntityModel<PluginMetaData>> pluginMetaDatas;

        /**
         * Constructor initializing the attributes from the parameter
         */
        public TypeMetadataResourceConfMapping(TypeMetadataConfMapping mapping) {
            this.attrType = mapping.getAttrType();
            this.pluginConfigurations = HateoasUtils.wrapCollection(mapping.getPluginConfigurations());
            this.pluginMetaDatas = HateoasUtils.wrapCollection(mapping.getPluginMetaDatas());
        }

        /**
         * @return the attribute type
         */
        public PropertyType getAttrType() {
            return attrType;
        }

        /**
         * Set the attribute type
         */
        public void setAttrType(PropertyType attrType) {
            this.attrType = attrType;
        }

        /**
         * @return the plugin configurations wrapped into {@link EntityModel}
         */
        public Collection<EntityModel<PluginConfiguration>> getPluginConfigurations() {
            return pluginConfigurations;
        }

        /**
         * Set the plugin configurations wrapped into {@link EntityModel}
         */
        public void setPluginConfigurations(Collection<EntityModel<PluginConfiguration>> pluginConfigurations) {
            this.pluginConfigurations = pluginConfigurations;
        }

        /**
         * @return the plugin metadata wrapped into {@link EntityModel}
         */
        public Collection<EntityModel<PluginMetaData>> getPluginMetaDatas() {
            return pluginMetaDatas;
        }

        /**
         * Set the plugin metadata wrapped into {@link EntityModel}
         */
        public void setPluginMetaDatas(Collection<EntityModel<PluginMetaData>> pluginMetaDatas) {
            this.pluginMetaDatas = pluginMetaDatas;
        }
    }
}
