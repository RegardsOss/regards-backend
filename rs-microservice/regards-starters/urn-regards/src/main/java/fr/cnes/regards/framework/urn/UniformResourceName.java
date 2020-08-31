/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.urn;

import javax.persistence.Convert;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.lang.Nullable;

import fr.cnes.regards.framework.urn.converters.UrnConverter;
import fr.cnes.regards.framework.urn.validator.RegardsUrn;

/**
 * allow us to create URN with the following format:
 * URN:StringId:entityType:tenant:UUID(entityId):Vversion[,order][:REVrevision]
 *
 * <br/>
 * Example:
 * <ul>
 * <li>URN:SIP:Collection:CDPP::1</li>
 * <li>URN:AIP:Collection:CDPP::1,5:REV2</li>
 * </ul>
 * @author Sylvain Vissiere-Guerinet
 */
@Convert(converter = UrnConverter.class)
@RegardsUrn
public class UniformResourceName {

    public static final int MAX_SIZE = 255;

    public static final String LAST_VALUE = "LAST";

    /**
     * URN pattern
     */
    public static final String URN_PATTERN =
            "URN:[^:]+:[^:]+:[^:]+:[^:]+:(V\\d{1,3}|" + LAST_VALUE + ")(,\\d+)?(:REV.+)?";

    /**
     * Version prefix
     */
    protected static final String VERSION_PREFIX = "V";

    /**
     * Section delimiter
     */
    protected static final String DELIMITER = ":";

    /**
     * Revision prefix
     */
    protected static final String REVISION_PREFIX = "REV";

    protected static final String BASE_URN_ZERO = "00000000-0000-0000-0000";

    /**
     * Compiled pattern
     */
    protected static final Pattern PATTERN = Pattern.compile(URN_PATTERN);

    /**
     * Version minimum value
     */
    private static final int MIN_VERSION_VALUE = 1;

    /**
     * Version maximum value
     */
    private static final int MAX_VERSION_VALUE = 999;

    /**
     * the identifier
     */
    @NotNull
    private String identifier;

    /**
     * Entity type
     */
    @NotNull
    private EntityType entityType;

    /**
     * Tenant which the entity belongs toù
     */
    @NotNull
    private String tenant;

    /**
     * Entity id
     */
    @NotNull
    private UUID entityId;

    /**
     * Entity version number on 3 digits by specs(cf REGARDS_DSL_SYS_ARC_410)
     */
    @Min(MIN_VERSION_VALUE)
    @Max(MAX_VERSION_VALUE)
    private Integer version;

    /**
     * numeric value ordering the differents element from a same identifier
     */
    private Long order;

    /**
     * Revision of the entity
     */
    private String revision;

    private boolean last = false;

    /**
     * Constructor setting the given parameters as attributes
     */
    protected UniformResourceName(String identifier, EntityType entityType, String tenant, UUID entityId,
            Integer version, @Nullable Long order, @Nullable String revision) {
        this.identifier = identifier;
        this.entityType = entityType;
        this.tenant = tenant;
        this.entityId = entityId;
        this.version = version;
        this.order = order;
        this.revision = revision;
    }

    /**
     * Constructor setting the given parameters as attributes
     */
    protected UniformResourceName(String identifier, EntityType entityType, String tenant, UUID entityId,
            @Nullable Long order, @Nullable String revision) {
        this.identifier = identifier;
        this.entityType = entityType;
        this.tenant = tenant;
        this.entityId = entityId;
        this.last = true;
        this.order = order;
        this.revision = revision;
    }

    public UniformResourceName() {
        // Deserialization
    }

    public static UniformResourceName build(String identifier, EntityType entityType, String tenant, UUID entityId,
            @Nullable Integer version, @Nullable Long order, @Nullable String revision) {
        UniformResourceName urn = new UniformResourceName();
        urn.setIdentifier(identifier);
        urn.setEntityType(entityType);
        urn.setTenant(tenant);
        urn.setEntityId(entityId);
        urn.setVersion(version);
        urn.setOrder(order);
        urn.setRevision(revision);
        return urn;
    }

    public static UniformResourceName build(String identifier, EntityType entityType, String tenant, UUID entityId,
            int version) {
        return build(identifier, entityType, tenant, entityId, version, null, null);
    }

    public static UniformResourceName build(Enum<?> identifier, EntityType entityType, String tenant, UUID entityId,
            int version) {
        return build(identifier.toString(), entityType, tenant, entityId, version, null, null);
    }

    /**
     * @return whether the given string is a urn or not
     */
    public static boolean isValidUrn(String urn) {
        return PATTERN.matcher(urn).matches();
    }

    /**
     * take this kind of String
     * URN:OAISIdentifier:entityType:tenant:UUID(entityId):version[,order][:REVrevision]
     * and return a new instance of {@link UniformResourceName}
     *
     * @param urn String respecting the following regex
     *            URN:.+:.+:.+:.+:\\d{1,3}(,\\d+)?(:REV.+)?
     * @return a new instance of {@link UniformResourceName}
     * @throws IllegalArgumentException if the given string does not respect the urn
     *                                  pattern
     */
    public static UniformResourceName fromString(String urn) {
        Pattern pattern = Pattern.compile(URN_PATTERN);
        if (!pattern.matcher(urn).matches()) {
            throw new IllegalArgumentException();
        }
        String[] stringFragment = urn.split(DELIMITER);
        String identifier = stringFragment[1];
        EntityType entityType = EntityType.valueOf(stringFragment[2]);
        String tenant = stringFragment[3];
        UUID entityId = UUID.fromString(stringFragment[4]);
        String[] versionWithOrder = stringFragment[5].split(",");
        boolean last = versionWithOrder[0].contains(LAST_VALUE);
        Integer version = null;
        if (!last) {
            // if this is not a last URN then lets compute version
            version = Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length()));
        }
        Long order = null;
        String revision = null;
        if (versionWithOrder.length == 2) {
            order = Long.parseLong(versionWithOrder[1]);
        }
        if (stringFragment.length == 7) {
            // Revision is precised
            revision = stringFragment[6].substring(REVISION_PREFIX.length());
        }
        if (last) {
            return new UniformResourceName(identifier, entityType, tenant, entityId, order, revision);
        } else {
            return new UniformResourceName(identifier, entityType, tenant, entityId, version, order, revision);
        }
    }

    /**
     * By default UUID.randomUUID() must not be used. It is generating a true random
     * UUID which makes it undetectable. To avoid this, pseudo random UUID is used
     * with following format : 00000000-0000-0000-0000-&lt;random-int>
     */
    public boolean isRandomEntityId() {
        return entityId.toString().startsWith(BASE_URN_ZERO);
    }

    @SuppressWarnings("unchecked")
    public <U extends UniformResourceName> U withOrder(Long order) {
        this.setOrder(order);
        return (U) this;
    }

    @SuppressWarnings("unchecked")
    public <U extends UniformResourceName> U withRevision(String revision) {
        this.setRevision(revision);
        return (U) this;
    }

    @Override
    public String toString() {
        final StringJoiner urnBuilder = new StringJoiner(":", "URN:", "");
        urnBuilder.add(identifier);
        urnBuilder.add(entityType.toString());
        urnBuilder.add(tenant);
        urnBuilder.add(entityId.toString());
        String orderString = "";
        if (order != null) {
            orderString = "," + order;
        }
        // order is not added with the joiner because it is "version,order" and not
        // "version:order"
        if (last) {
            urnBuilder.add(LAST_VALUE + orderString);
        } else {
            urnBuilder.add(VERSION_PREFIX + version + orderString);
        }
        if (revision != null) {
            urnBuilder.add(REVISION_PREFIX + revision);
        }
        return urnBuilder.toString();
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Set the identifier
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the entity type
     */
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * Set the entity type
     */
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * Set the tenant
     */
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    /**
     * @return the entity id
     */
    public UUID getEntityId() {
        return entityId;
    }

    /**
     * Set the entity id
     */
    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    /**
     * @return the version
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Set the version
     */
    public void setVersion(Integer version) {
        this.version = version;
    }

    /**
     * @return the order
     */
    public Long getOrder() {
        return order;
    }

    /**
     * Set the order
     */
    public void setOrder(Long order) {
        this.order = order;
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**    public
     * Set the revision
     */
    public void setRevision(String revision) {
        this.revision = revision;
    }

    public boolean isLast() {
        return last;
    }

    public UniformResourceName toLast() {
        return new UniformResourceName(identifier, entityType, tenant, entityId, order, revision);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || (!getClass().isAssignableFrom(o.getClass()) && !o.getClass().isAssignableFrom(getClass()))) {
            return false;
        }
        UniformResourceName that = (UniformResourceName) o;
        return last == that.last && Objects.equals(identifier, that.identifier) && entityType == that.entityType
                && Objects.equals(tenant, that.tenant) && Objects.equals(entityId, that.entityId) && Objects
                .equals(version, that.version) && Objects.equals(order, that.order) && Objects
                .equals(revision, that.revision);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, entityType, tenant, entityId, version, order, revision, last);
    }
}
