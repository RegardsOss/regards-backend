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
package fr.cnes.regards.modules.dam.domain.entities.feature;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import fr.cnes.regards.framework.geojson.AbstractFeature;
import fr.cnes.regards.framework.gson.annotation.GsonIgnore;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.entities.StaticProperties;
import fr.cnes.regards.modules.indexer.domain.DataFile;
import fr.cnes.regards.modules.model.dto.properties.IProperty;
import fr.cnes.regards.modules.model.dto.properties.ObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import javax.persistence.Transient;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.*;

/**
 * Public and common entity feature properties
 *
 * @author Marc Sordi
 */
public abstract class EntityFeature extends AbstractFeature<Set<IProperty<?>>, UniformResourceName> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityFeature.class);


    @ConfigIgnore
    protected UniformResourceName virtualId;

    /**
     * Submission information package provider identifier
     */
    @NotBlank(message = "Feature provider id is required")
    protected String providerId;

    @NotNull(message = "Feature type is required")
    protected EntityType entityType;

    @NotBlank(message = "Label is required")
    protected String label;

    @NotBlank(message = "Model name is required")
    protected String model;

    /**
     * Related entity files
     */
    @Valid
    protected Multimap<DataType, DataFile> files = HashMultimap.create();

    /**
     * Related entity tags
     */
    protected Set<String> tags = new HashSet<>();

    // To perform quick access to attribute from its name
    @Transient
    @GsonIgnore
    private Map<String, IProperty<?>> propertyMap = null;

    @Transient
    @GsonIgnore
    private Map<String, IProperty<?>> staticPropertyMap = null;

    @ConfigIgnore
    private boolean last = false;

    @ConfigIgnore
    private Integer version;

    public EntityFeature(UniformResourceName id, String providerId, EntityType entityType, String label) {
        Assert.notNull(entityType, "Entity type is required");
        this.id = id;
        this.version = id == null ? 0 : id.getVersion();
        this.providerId = providerId;
        this.entityType = entityType;
        this.label = label;
        this.properties = new HashSet<>();
    }

    public void addProperty(IProperty<?> property) {
        this.properties.add(property);
        // If property key is null, it is not a valid property and so it may not pass validation process
        if (property.getName() != null) {
            propertyMap = Maps.uniqueIndex(this.properties, IProperty::getName);
        }
    }

    public IProperty<?> getProperty(String name) {
        if (propertyMap == null) {
            propertyMap = Maps.uniqueIndex(this.properties, IProperty::getName);
        }
        if (!name.contains(".")) {
            return this.propertyMap.get(name);
        } else {
            if (this.propertyMap.get(name.substring(0, name.indexOf('.'))) instanceof ObjectProperty fragment) {
                String propName = name.substring(name.indexOf('.') + 1);
                Optional<IProperty<?>> attOpt = fragment.getValue()
                                                        .stream()
                                                        .filter(p -> p.getName().equals(propName))
                                                        .findFirst();
                return attOpt.orElse(null);
            } else {
                LOGGER.debug("Failed to get property \"{}\" from entity \"{}\".", name, providerId);
            }
            return null;
        }
    }

    public IProperty<?> getStaticProperty(String name) {
        if (staticPropertyMap == null) {
            Set<IProperty<?>> staticProperties = new HashSet<>();
            // Unique identifier
            staticProperties.add(IProperty.buildString(StaticProperties.FEATURE_ID, id.toString()));
            // Virtual identifier
            staticProperties.add(IProperty.buildString(StaticProperties.FEATURE_VIRTUAL_ID, virtualId.toString()));
            // Version
            staticProperties.add(IProperty.buildInteger(StaticProperties.FEATURE_VERSION, version));
            // Is last version
            staticProperties.add(IProperty.buildBoolean(StaticProperties.FEATURE_IS_LAST_VERSION, last));
            // SIP identifier alias provider identifier
            staticProperties.add(IProperty.buildString(StaticProperties.FEATURE_PROVIDER_ID, providerId));
            // Required label for minimal display purpose
            staticProperties.add(IProperty.buildString(StaticProperties.FEATURE_LABEL, label));
            // Related model name
            staticProperties.add(IProperty.buildString(StaticProperties.FEATURE_MODEL, model));
            // Tags
            staticProperties.add(IProperty.buildStringArray(StaticProperties.FEATURE_TAGS,
                                                            tags.toArray(new String[0])));

            staticPropertyMap = Maps.uniqueIndex(staticProperties, IProperty::getName);
        }
        return this.staticPropertyMap.get(name);
    }

    public void removeProperty(IProperty<?> property) {
        this.properties.remove(property);
        propertyMap = Maps.uniqueIndex(this.properties, IProperty::getName);
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Multimap<DataType, DataFile> getFiles() {
        return files;
    }

    public void setFiles(Multimap<DataType, DataFile> files) {
        this.files = files;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        this.tags.add(tag);
    }

    public void addTags(String... tags) {
        this.tags.addAll(Arrays.asList(tags));
    }

    public void addTags(Collection<String> tags) {
        this.tags.addAll(tags);
    }

    public void removeTags(Collection<String> tags) {
        this.tags.removeAll(tags);
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    @Override
    public void setProperties(Set<IProperty<?>> properties) {
        super.setProperties(properties);
        propertyMap = Maps.uniqueIndex(this.properties, IProperty::getName);
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public UniformResourceName getVirtualId() {
        return virtualId;
    }

    public void setVirtualId() {
        this.virtualId = id.toLast();
    }

    public void removeVirtualId() {
        this.virtualId = null;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
