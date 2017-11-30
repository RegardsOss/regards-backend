/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.oais.urn;

import javax.persistence.Convert;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

import fr.cnes.regards.framework.oais.urn.converters.UrnConverter;
import fr.cnes.regards.framework.oais.urn.validator.RegardsOaisUrn;

/**
 * allow us to create URN with the following format:
 * URN:OAISIdentifier:entityType:tenant:UUID(entityId):Vversion[,order][:REVrevision]
 *
 * <br/>
 * Example:
 * <ul>
 * <li>URN:SIP:Collection:CDPP::1</li>
 * <li>URN:AIP:Collection:CDPP::1,5:REV2</li>
 * </ul>
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RegardsOaisUrn
@Convert(converter = UrnConverter.class)
public class UniformResourceName {

    public static final int MAX_SIZE = 128;

    /**
     * URN pattern
     */
    public static final String URN_PATTERN = "URN:[^:]+:[^:]+:[^:]+:[^:]+:V\\d{1,3}(,\\d+)?(:REV.+)?";

    /**
     * Version prefix
     */
    private static final String VERSION_PREFIX = "V";

    /**
     * Section delimiter
     */
    private static final String DELIMITER = ":";

    /**
     * Revision prefix
     */
    private static final String REVISION_PREFIX = "REV";

    /**
     * Version minimum value
     */
    private static final int MIN_VERSION_VALUE = 1;

    /**
     * Version maximum value
     */
    private static final int MAX_VERSION_VALUE = 999;

    /**
     * Compiled pattern
     */
    private static final Pattern PATTERN = Pattern.compile(URN_PATTERN);

    /**
     * the oais identifier
     */
    @NotNull
    private OAISIdentifier oaisIdentifier;

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
    private int version;

    /**
     * numeric value ordering the differents AIP from a same SIP
     */
    private Long order;

    /**
     * Revision of the entity
     */
    private String revision;

    /**
     * Constructor setting the given parameters as attributes
     * @param pOaisIdentifier
     * @param pEntityType
     * @param pTenant
     * @param pEntityId
     * @param pVersion
     */
    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId,
            int pVersion) {
        super();
        oaisIdentifier = pOaisIdentifier;
        entityType = pEntityType;
        tenant = pTenant;
        entityId = pEntityId;
        version = pVersion;
    }

    /**
     * Constructor setting the given parameters as attributes
     * @param pOaisIdentifier
     * @param pEntityType
     * @param pTenant
     * @param pEntityId
     * @param pVersion
     * @param pOrder
     * @param pRevision
     */
    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId,
            // NOSONAR
            int pVersion, Long pOrder, String pRevision) {
        this(pOaisIdentifier, pEntityType, pTenant, pEntityId, pVersion);
        order = pOrder;
        revision = pRevision;
    }

    /**
     * Constructor setting the given parameters as attributes
     * @param pOaisIdentifier
     * @param pEntityType
     * @param pTenant
     * @param pEntityId
     * @param pVersion
     * @param pOrder
     */
    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId,
            // NOSONAR
            int pVersion, long pOrder) {// NOSONAR
        this(pOaisIdentifier, pEntityType, pTenant, pEntityId, pVersion);
        order = pOrder;
    }

    /**
     * Constructor setting the given parameters as attributes
     * @param pOaisIdentifier
     * @param pEntityType
     * @param pTenant
     * @param pEntityId
     * @param pVersion
     * @param pRevision
     */
    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId,
            // NOSONAR
            int pVersion, String pRevision) {
        this(pOaisIdentifier, pEntityType, pTenant, pEntityId, pVersion);
        revision = pRevision;
    }

    public UniformResourceName() {// NOSONAR
        // for testing purpose
    }

    /**
     *
     * take this kind of String URN:OAISIdentifier:entityType:tenant:UUID(entityId):version[,order][:REVrevision] and
     * return a new instance of {@link UniformResourceName}
     *
     * @param pUrn
     *            String respecting the following regex URN:.+:.+:.+:.+:\\d{1,3}(,\\d+)?(:REV.+)?
     * @return a new instance of {@link UniformResourceName}
     * @throws IllegalArgumentException if the given string does not respect the urn pattern
     */
    public static UniformResourceName fromString(String pUrn) {
        final Pattern pattern = Pattern.compile(URN_PATTERN);
        if (!pattern.matcher(pUrn).matches()) {
            throw new IllegalArgumentException();
        }
        final String[] stringFragment = pUrn.split(DELIMITER);
        final OAISIdentifier oaisIdentifier = OAISIdentifier.valueOf(stringFragment[1]);
        final EntityType entityType = EntityType.valueOf(stringFragment[2]);
        final String tenant = stringFragment[3];
        final UUID entityId = UUID.fromString(stringFragment[4]);
        final String[] versionWithOrder = stringFragment[5].split(",");
        if (versionWithOrder.length == 2) {
            // Order is precised
            if (stringFragment.length == 7) {
                // Revision is precised
                final String revisionString = stringFragment[6];
                // so we have all fields
                return new UniformResourceName(oaisIdentifier,
                                               entityType,
                                               tenant,
                                               entityId,
                                               Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                                               Long.parseLong(versionWithOrder[1]),
                                               revisionString.substring(REVISION_PREFIX.length()));
            } else {
                // Revision is missing so we have all except Revision
                return new UniformResourceName(oaisIdentifier,
                                               entityType,
                                               tenant,
                                               entityId,
                                               Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                                               Long.parseLong(versionWithOrder[1]));
            }
        } else {
            // we don't have an order specified
            if (stringFragment.length == 7) {
                // Revision is precised
                final String revisionString = stringFragment[6];
                // so we have all fields exception Order
                return new UniformResourceName(oaisIdentifier,
                                               entityType,
                                               tenant,
                                               entityId,
                                               Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                                               revisionString.substring(REVISION_PREFIX.length()));
            } else {
                // Revision is missing so we have all except Revision and Order
                return new UniformResourceName(oaisIdentifier,
                                               entityType,
                                               tenant,
                                               entityId,
                                               Integer.parseInt(versionWithOrder[0]
                                                                        .substring(VERSION_PREFIX.length())));
            }
        }
    }

    /**
     * @param pUrn
     * @return whether the given string is a urn or not
     */
    public static boolean isValidUrn(String pUrn) {
        return PATTERN.matcher(pUrn).matches();
    }

    @Override
    public String toString() {
        final StringJoiner urnBuilder = new StringJoiner(":", "URN:", "");
        urnBuilder.add(oaisIdentifier.toString());
        urnBuilder.add(entityType.toString());
        urnBuilder.add(tenant);
        urnBuilder.add(entityId.toString());
        String orderString = "";
        if (order != null) {
            orderString = "," + order;
        }
        // order is not added with the joiner because it is "version,order" and not "version:order"
        urnBuilder.add(VERSION_PREFIX + version + orderString);
        if (revision != null) {
            urnBuilder.add(REVISION_PREFIX + revision);
        }
        return urnBuilder.toString();
    }

    /**
     * @return the oais identifier
     */
    public OAISIdentifier getOaisIdentifier() {
        return oaisIdentifier;
    }

    /**
     * Set the oais identifier
     * @param pOaisIdentifier
     */
    public void setOaisIdentifier(OAISIdentifier pOaisIdentifier) {
        oaisIdentifier = pOaisIdentifier;
    }

    /**
     * @return the entity type
     */
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * Set the entity type
     * @param pEntityType
     */
    public void setEntityType(EntityType pEntityType) {
        entityType = pEntityType;
    }

    /**
     * @return the tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * Set the tenant
     * @param pTenant
     */
    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    /**
     * @return the entity id
     */
    public UUID getEntityId() {
        return entityId;
    }

    /**
     * Set the entity id
     * @param pEntityId
     */
    public void setEntityId(UUID pEntityId) {
        entityId = pEntityId;
    }

    /**
     * @return the version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Set the version
     * @param pVersion
     */
    public void setVersion(int pVersion) {
        version = pVersion;
    }

    /**
     * @return the order
     */
    public Long getOrder() {
        return order;
    }

    /**
     * Set the order
     * @param pOrder
     */
    public void setOrder(Long pOrder) {
        order = pOrder;
    }

    /**
     * @return the revision
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Set the revision
     * @param pRevision
     */
    public void setRevision(String pRevision) {
        revision = pRevision;
    }

    @Override
    public boolean equals(Object pOther) {
        return (pOther instanceof UniformResourceName) && pOther.toString().equals(toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
