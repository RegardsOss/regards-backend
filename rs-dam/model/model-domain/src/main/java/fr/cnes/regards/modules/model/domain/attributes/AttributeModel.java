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
package fr.cnes.regards.modules.model.domain.attributes;

import fr.cnes.regards.framework.gson.utils.GSONConstants;
import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.module.manager.ConfigIgnore;
import fr.cnes.regards.modules.model.domain.IXmlisable;
import fr.cnes.regards.modules.model.domain.Model;
import fr.cnes.regards.modules.model.domain.attributes.restriction.*;
import fr.cnes.regards.modules.model.domain.schema.Attribute;
import fr.cnes.regards.modules.model.domain.schema.Property;
import fr.cnes.regards.modules.model.domain.schema.Restriction;
import fr.cnes.regards.modules.model.domain.schema.Type;
import fr.cnes.regards.modules.model.domain.validator.JsonString;
import fr.cnes.regards.modules.model.dto.properties.PropertyType;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author msordi
 */
@Entity
@Table(name = "t_attribute_model",
       uniqueConstraints = @UniqueConstraint(name = "uk_attribute_model_name_fragment_id",
                                             columnNames = { "name", "fragment_id" }))
@SequenceGenerator(name = "attModelSequence", initialValue = 1, sequenceName = "seq_att_model")
public class AttributeModel implements IIdentifiable<Long>, IXmlisable<Attribute> {

    /**
     * Internal identifier
     */
    @Id
    @ConfigIgnore
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "attModelSequence")
    private Long id;

    /**
     * Attribute name
     */
    @NotNull(message = "Name cannot be null")
    @Pattern(regexp = Model.NAME_REGEXP,
             message = "Attribute name must conform to regular expression \"" + Model.NAME_REGEXP + "\".")
    @Size(min = Model.NAME_MIN_SIZE,
          max = Model.NAME_MAX_SIZE,
          message = "Attribute name must be between "
                    + Model.NAME_MIN_SIZE
                    + " and "
                    + Model.NAME_MAX_SIZE
                    + " length.")
    @Column(nullable = false, updatable = false, length = Model.NAME_MAX_SIZE)
    private String name;

    /**
     * Optional attribute description
     */
    @Column(columnDefinition = "text")
    private String description;

    /**
     * Attribute type
     */
    @NotNull(message = "Type cannot be null")
    @Column(length = 32, nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private PropertyType type;

    /**
     * Unit useful for number based attributes
     */
    @Column(length = 16)
    private String unit;

    /**
     * Precision useful for double based attributes
     */
    @Column
    private Integer precision;

    /**
     * Array size useful for array based attributes
     */
    @Column
    private Integer arraysize;

    /**
     * Optional fragment
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "fragment_id",
                foreignKey = @ForeignKey(name = "fk_fragment_id"),
                nullable = false,
                updatable = false)
    private Fragment fragment;

    /**
     * Whether this attribute can be altered by users
     */
    @Column
    private boolean alterable = true;

    /**
     * Whether this attribute is optional
     */
    @Column
    private boolean optional;

    /**
     * Whether this attribute can be searched with elasticsearch
     */
    @Column
    private boolean indexed = false;

    /**
     * Optional elasticsearch mapping configuration for this attribute
     */
    @JsonString(message = "Elasticsearch mapping must be a valid json format")
    @Column(name = "es_mapping", columnDefinition = "text")
    private String esMapping;

    /**
     * Attribute label
     */
    @Column(length = 255)
    @NotBlank(message = "Label cannot be empty")
    @Size(max = 255, message = "Label must be between 1 and 255 characters.")
    private String label;

    /**
     * Applicable restriction
     */
    @OneToOne(orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "restriction_id", foreignKey = @ForeignKey(name = "fk_restriction_id"))
    @Valid
    private AbstractRestriction restriction;

    /**
     * Optional group for displaying purpose
     */
    @Pattern(regexp = Model.NAME_REGEXP,
             message = "Group name must conform to regular expression \"" + Model.NAME_REGEXP + "\".")
    @Size(min = Model.NAME_MIN_SIZE,
          max = Model.NAME_MAX_SIZE,
          message = "Group name must be between " + Model.NAME_MIN_SIZE + " and " + Model.NAME_MAX_SIZE + " length.")
    @Column(name = "group_name", length = Model.NAME_MAX_SIZE)
    private String group;

    /**
     * Custom attribute properties
     */
    @Valid
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_id", foreignKey = @ForeignKey(name = "fk_att_ppty_att"))
    private List<AttributeProperty> properties;

    /**
     * Used in search request parsing only
     */
    @Transient
    private boolean dynamic = true;

    /**
     * Used in search request parsing only to identify an internal (i.e. private) properties. It's a property available
     * in the feature decorator in other words : the entity.
     */
    @Transient
    private boolean internal = false;

    /**
     * Used in search request. Define the JSON path to the related values in entities
     */
    @Transient
    private String jsonPath;

    /**
     * Indicates if this attribute is a real atribute from the model or if it is a generated one from a JsonObject attributes.
     * <p>
     * Look at fr.cnes.regards.modules.model.gson.AbstractAttributeHelper class. Generates attributes from a JsonObject attribute type thanks to
     * JsonSchema associated in restriction.
     */
    @Transient
    private boolean virtual = false;

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long pId) {
        id = pId;
    }

    public String getName() {
        return name;
    }

    public void setName(String pName) {
        name = pName;
        buildPublicJsonPath();
    }

    public PropertyType getType() {
        return type;
    }

    public void setType(PropertyType pType) {
        type = pType;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public boolean hasFragment() {
        return (fragment != null) && !fragment.isDefaultFragment();
    }

    public Fragment getFragment() {
        return fragment;
    }

    public void setFragment(Fragment pFragment) {
        fragment = pFragment;
        buildPublicJsonPath();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String pDescription) {
        description = pDescription;
    }

    public AbstractRestriction getRestriction() {
        return restriction;
    }

    public void setRestriction(AbstractRestriction pRestriction) {
        restriction = pRestriction;
    }

    public boolean hasRestriction() {
        return (restriction != null) && !restriction.getType().equals(RestrictionType.NO_RESTRICTION);
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public boolean equals(Object pObj) {
        if (pObj instanceof AttributeModel) {
            final AttributeModel attmod = (AttributeModel) pObj;
            boolean hasSameFragment = true;
            if (fragment != null) {
                hasSameFragment = fragment.equals(attmod.getFragment());
            }
            return attmod.getName().equals(name) && hasSameFragment;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (fragment != null) {
            return name.hashCode() + fragment.hashCode();
        }
        return name.hashCode();
    }

    public boolean isAlterable() {
        return alterable;
    }

    public void setAlterable(boolean pAlterable) {
        alterable = pAlterable;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean pOptional) {
        optional = pOptional;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean pIndexed) {
        this.indexed = pIndexed;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String pGroup) {
        group = pGroup;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String pUnit) {
        unit = pUnit;
    }

    public Integer getPrecision() {
        return precision;
    }

    public void setPrecision(Integer pPrecision) {
        precision = pPrecision;
    }

    public Integer getArraysize() {
        return arraysize;
    }

    public void setArraysize(Integer pArraysize) {
        arraysize = pArraysize;
    }

    public String getEsMapping() {
        return esMapping;
    }

    public void setEsMapping(String esMapping) {
        this.esMapping = esMapping;
    }

    @Override
    public Attribute toXml() {
        final Attribute xmlAtt = new Attribute();
        xmlAtt.setName(name);
        xmlAtt.setLabel(label);
        xmlAtt.setDescription(description);
        xmlAtt.setAlterable(alterable);
        xmlAtt.setOptional(optional);
        xmlAtt.setIndexed(indexed);
        if (restriction != null) {
            xmlAtt.setRestriction(restriction.toXml());
        }
        Type xmlType = new Type();
        if (arraysize != null) {
            xmlType.setArraysize(BigInteger.valueOf(arraysize));
        }
        if (precision != null) {
            xmlType.setPrecision(BigInteger.valueOf(precision));
        }

        if (esMapping != null) {
            xmlAtt.setEsMapping(esMapping);
        }
        xmlType.setUnit(unit);
        xmlType.setValue(fr.cnes.regards.modules.model.domain.schema.RestrictionType.fromValue(type.toString()));
        xmlAtt.setType(xmlType);
        xmlAtt.setGroup(group);

        if (properties != null) {
            for (AttributeProperty ppty : properties) {
                Property xmlProperty = new Property();
                xmlProperty.setKey(ppty.getKey());
                xmlProperty.setValue(ppty.getValue());
                xmlAtt.getProperty().add(xmlProperty);
            }
        }
        return xmlAtt;
    }

    @Override
    public void fromXml(Attribute XmlElement) {
        setName(XmlElement.getName());
        setLabel(XmlElement.getLabel());
        setDescription(XmlElement.getDescription());
        setAlterable(XmlElement.isAlterable());
        setOptional(XmlElement.isOptional());
        setIndexed(XmlElement.isIndexed());
        setEsMapping(XmlElement.getEsMapping());
        if (XmlElement.getRestriction() != null) {
            final Restriction xmlRestriction = XmlElement.getRestriction();
            if (xmlRestriction.getEnumeration() != null) {
                restriction = new EnumerationRestriction();
            } else if (xmlRestriction.getDoubleRange() != null) {
                restriction = new DoubleRangeRestriction();
            } else if (xmlRestriction.getIntegerRange() != null) {
                restriction = new IntegerRangeRestriction();
            } else if (xmlRestriction.getLongRange() != null) {
                restriction = new LongRangeRestriction();
            } else if (xmlRestriction.getPattern() != null) {
                restriction = new PatternRestriction();
            } else if (xmlRestriction.getJsonSchema() != null) {
                restriction = new JsonSchemaRestriction();
            }

            // Cause null pointer exception if implementation not consistent with XSD
            restriction.fromXml(XmlElement.getRestriction());
        }
        Type xmlType = XmlElement.getType();
        if (xmlType.getArraysize() != null) {
            setArraysize(xmlType.getArraysize().intValueExact());
        }
        if (xmlType.getPrecision() != null) {
            setPrecision(xmlType.getPrecision().intValueExact());
        }
        setUnit(xmlType.getUnit());
        setType(PropertyType.valueOf(xmlType.getValue().value()));
        setGroup(XmlElement.getGroup());

        if (!XmlElement.getProperty().isEmpty()) {
            List<AttributeProperty> ppts = new ArrayList<>();
            for (Property xmlProperty : XmlElement.getProperty()) {
                AttributeProperty attPpty = new AttributeProperty();
                attPpty.setKey(xmlProperty.getKey());
                attPpty.setValue(xmlProperty.getValue());
                ppts.add(attPpty);
            }
            setProperties(ppts);
        }
        buildPublicJsonPath();
    }

    public List<AttributeProperty> getProperties() {
        return properties == null ? Collections.emptyList() : properties;
    }

    public void setProperties(List<AttributeProperty> pProperties) {
        properties = pProperties;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    public void setDynamic(boolean pDynamic) {
        dynamic = pDynamic;
        buildPublicJsonPath();
    }

    public boolean isInternal() {
        return internal;
    }

    public void setInternal(boolean internal) {
        this.internal = internal;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String pJsonPath) {
        jsonPath = pJsonPath;
    }

    public String getFullName() {
        return fragment == null ? name : fragment.getName() + "." + name;
    }

    /**
     * Retrieve full feature attribute json path
     *
     * @return json path
     */
    public String getFullJsonPath() {
        if (isInternal()) {
            return getJsonPathForNamespace("");
        } else if (isDynamic()) {
            return getJsonPathForNamespace(PropertyNamespaces.FEATURE_PROPERTIES_PATH);
        } else {
            return getJsonPathForNamespace(PropertyNamespaces.FEATURE);
        }
    }

    /**
     * Construct a json path for the current attribute depending on given prefix namespace
     *
     * @return json path
     */
    public String getJsonPathForNamespace(String namespace) {
        StringBuilder builder = new StringBuilder(namespace);
        if (!namespace.isEmpty()) {
            builder.append(GSONConstants.JSON_PATH_SEPARATOR);
        }
        if (hasFragment()) {
            builder.append(fragment.getName());
            builder.append(GSONConstants.JSON_PATH_SEPARATOR);
        }
        builder.append(name);
        return builder.toString();
    }

    /**
     * Build Json path for related property
     */
    public String getJsonPropertyPath() {
        StringBuilder builder = new StringBuilder();
        if (hasFragment()) {
            builder.append(fragment.getName());
            builder.append(GSONConstants.JSON_PATH_SEPARATOR);
        }
        builder.append(name);
        return builder.toString();
    }

    /**
     * Private method to construct the public json path of the attribute.
     * The public json path is the path requested by clients to search for features.
     * if full private path is feature.properties.attribute, then the public path is properties.attribute.
     * The public entities returned does not contains the feature element.
     */
    @PostLoad
    public void buildPublicJsonPath() {
        if (!isInternal()) {
            String namespace = "";
            if (isDynamic()) {
                namespace = PropertyNamespaces.FEATURE_PROPERTIES;
            }
            jsonPath = getJsonPathForNamespace(namespace);
        } else {
            jsonPath = null;
        }
    }

    /**
     * Does the current attribute type is {@link PropertyType#STRING} or {@link PropertyType#STRING_ARRAY} ?
     *
     * @return {@link Boolean}
     */
    public boolean isTextAttribute() {
        switch (this.type) {
            case STRING:
            case STRING_ARRAY:
            case URL:
                return true;
            case BOOLEAN:
            case DATE_ARRAY:
            case DATE_INTERVAL:
            case DATE_ISO8601:
            case DOUBLE:
            case DOUBLE_ARRAY:
            case DOUBLE_INTERVAL:
            case INTEGER:
            case INTEGER_ARRAY:
            case INTEGER_INTERVAL:
            case LONG:
            case LONG_ARRAY:
            case LONG_INTERVAL:
            default:
                return false;
        }
    }

    public boolean isBooleanAttribute() {
        return this.type == PropertyType.BOOLEAN;
    }

    @Override
    public String toString() {
        return "AttributeModel{"
               + "id="
               + id
               + ", name='"
               + name
               + '\''
               + ", type="
               + type
               + ", fragment="
               + fragment
               + ", jsonPath='"
               + jsonPath
               + '\''
               + '}';
    }

    /**
     *
     */
    public RestrictionType getRestrictionType() {
        return Optional.ofNullable(restriction)
                       .map(AbstractRestriction::getType)
                       .orElse(RestrictionType.NO_RESTRICTION);
    }

}
