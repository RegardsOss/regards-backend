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
package fr.cnes.regards.modules.model.domain.attributes;

import fr.cnes.regards.modules.model.domain.attributes.restriction.AbstractRestriction;
import fr.cnes.regards.modules.model.domain.attributes.restriction.RestrictionFactory;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import org.springframework.util.Assert;

/**
 * Attribute model builder
 *
 * @author Marc Sordi
 */
public final class AttributeModelBuilder {

    private final String name;

    private final PropertyType type;

    private final String label;

    private boolean isAlterable = false;

    private boolean isOptional = false;

    private boolean isIndexed = false;

    private Long id;

    private String description;

    private Fragment fragment;

    private boolean isStatic = false;

    private boolean isInternal = false;

    private AbstractRestriction restriction;

    /**
     * Initialize the builder by instanciating a minimal attribute model
     *
     * @param name  attribute name
     * @param type  attribute type
     * @param label attribute label
     */
    public AttributeModelBuilder(String name, PropertyType type, String label) {
        this.name = name;
        this.type = type;
        this.label = label;
    }

    /**
     * Build an AttributeModel using the builder configuration
     *
     * @return the AttributeModel
     */
    public AttributeModel build() {
        AttributeModel am = new AttributeModel();
        am.setName(name);
        am.setType(type);
        am.setLabel(label);
        am.setAlterable(isAlterable);
        am.setOptional(isOptional);
        am.setIndexed(isIndexed);
        am.setId(id);
        am.setDescription(description);
        am.setFragment(fragment);
        am.setDynamic(!isStatic);
        am.setInternal(isInternal);
        am.setRestriction(restriction);
        return am;
    }

    public AttributeModelBuilder setId(Long id) {
        this.id = id;
        return this;
    }

    public AttributeModelBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    public AttributeModelBuilder setAlterable(boolean isAlterable) {
        this.isAlterable = isAlterable;
        return this;
    }

    public AttributeModelBuilder setOptional(boolean isOptional) {
        this.isOptional = isOptional;
        return this;
    }

    public AttributeModelBuilder setFragment(Fragment fragment) {
        this.fragment = fragment;
        return this;
    }

    public AttributeModelBuilder setFragmentUsingDefault() {
        this.fragment = Fragment.buildDefault();
        return this;
    }

    public AttributeModelBuilder setStatic(boolean isStatic) {
        this.isStatic = isStatic;
        return this;
    }

    public AttributeModelBuilder setInternal(boolean isInternal) {
        this.isInternal = isInternal;
        if (isInternal) {
            this.isStatic = true;
        }
        return this;
    }

    public AttributeModelBuilder setIndexed(boolean indexed) {
        isIndexed = indexed;
        return this;
    }

    // Restriction

    public AttributeModelBuilder setRestriction(AbstractRestriction restriction) {
        Assert.notNull(restriction, "Restriction is required");
        if (!restriction.supports(this.type)) {
            throw new IllegalArgumentException(
                "Unsupported restriction " + restriction.getType() + " for attribute type " + restriction.getType());
        }
        this.restriction = restriction;
        return this;
    }

    public AttributeModelBuilder setEnumerationRestriction(String... pAcceptableValues) {
        return setRestriction(RestrictionFactory.buildEnumerationRestriction(pAcceptableValues));
    }

    public AttributeModelBuilder setFloatRangeRestriction(Double pMin,
                                                          Double pMax,
                                                          boolean pMinExcluded,
                                                          boolean pMaxExcluded) {
        return setRestriction(RestrictionFactory.buildFloatRangeRestriction(pMin, pMax, pMinExcluded, pMaxExcluded));
    }

    public AttributeModelBuilder setIntegerRangeRestriction(Integer pMin,
                                                            Integer pMax,
                                                            boolean pMinExcluded,
                                                            boolean pMaxExcluded) {
        return setRestriction(RestrictionFactory.buildIntegerRangeRestriction(pMin, pMax, pMinExcluded, pMaxExcluded));
    }

    public AttributeModelBuilder setLongRangeRestriction(Long pMin,
                                                         Long pMax,
                                                         boolean pMinExcluded,
                                                         boolean pMaxExcluded) {
        return setRestriction(RestrictionFactory.buildLongRangeRestriction(pMin, pMax, pMinExcluded, pMaxExcluded));
    }

    public AttributeModelBuilder setNoRestriction() {
        this.restriction = null;
        return this;
    }

    public AttributeModelBuilder setPatternRestriction(String pPattern) {
        return setRestriction(RestrictionFactory.buildPatternRestriction(pPattern));
    }
}
