/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.model.domain.event;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.modules.model.domain.attributes.AttributeModel;
import fr.cnes.regards.modules.model.domain.attributes.AttributeProperty;
import fr.cnes.regards.modules.model.domain.attributes.Fragment;
import fr.cnes.regards.modules.model.domain.attributes.restriction.AbstractRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionType;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;

/**
 * {@link AttributeModel} event common information
 *
 * @author Marc Sordi
 *
 */
public abstract class AbstractAttributeModelEvent implements ISubscribable {

    /**
     * {@link Fragment} name
     */
    private String fragmentName;

    /**
     * {@link AttributeModel} name
     */
    private String attributeName;

    /**
     * {@link PropertyType}
     */
    private PropertyType propertyType;

    /**
     * {@link RestrictionType}
     */
    private RestrictionType restrictionType;

    /**
     * Full JSON path
     */
    private String fullJsonPath;

    /**
     * Model attribute properties
     */
    private Map<String, String> attributeProperties;

    public AbstractAttributeModelEvent() {
        attributeProperties = new HashMap<>();
    }

    public AbstractAttributeModelEvent(AttributeModel pAttributeModel) {
        this.fragmentName = pAttributeModel.getFragment().getName();
        this.attributeName = pAttributeModel.getName();
        this.propertyType = pAttributeModel.getType();
        this.fullJsonPath = pAttributeModel.getFullJsonPath();
        this.restrictionType = Optional.ofNullable(pAttributeModel.getRestriction()).map(AbstractRestriction::getType)
                .orElse(RestrictionType.NO_RESTRICTION);
        this.attributeProperties = pAttributeModel.getProperties() == null ?
                new HashMap<>() :
                pAttributeModel.getProperties().stream().collect(Collectors.toMap(AttributeProperty::getKey,
                                                                                  AttributeProperty::getValue));
    }

    public PropertyType getPropertyType() {
        return propertyType;
    }

    public void setPropertyType(PropertyType pPropertyType) {
        propertyType = pPropertyType;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String pAttributeName) {
        attributeName = pAttributeName;
    }

    public String getFragmentName() {
        return fragmentName;
    }

    public void setFragmentName(String pFragmentName) {
        fragmentName = pFragmentName;
    }

    public RestrictionType getRestrictionType() {
        return restrictionType;
    }

    public void setRestrictionType(RestrictionType restrictionType) {
        this.restrictionType = restrictionType;
    }

    public String getFullJsonPath() {
        return fullJsonPath;
    }

    public void setFullJsonPath(String fullJsonPath) {
        this.fullJsonPath = fullJsonPath;
    }

    public Map<String, String> getAttributeProperties() {
        return attributeProperties;
    }

    public void setAttributeProperties(Map<String, String> attributeProperties) {
        this.attributeProperties = attributeProperties;
    }
}
