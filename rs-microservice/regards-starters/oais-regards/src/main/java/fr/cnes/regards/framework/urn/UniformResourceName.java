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
package fr.cnes.regards.framework.urn;

import javax.persistence.Convert;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

import fr.cnes.regards.framework.urn.converters.UrnConverter;
import fr.cnes.regards.framework.urn.validator.RegardsOaisUrn;

/**
 * allow us to create URN with the following format:
 * URN:OAISIdentifier:entityType:tenant:UUID(entityId):version[,order][:REVrevision]
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

    public static final String URN_PATTERN = "URN:[^:]+:[^:]+:[^:]+:[^:]+:V\\d{1,3}(,\\d+)?(:REV.+)?";

    private static final String VERSION_PREFIX = "V";

    private static final String DELIMITER = ":";

    private static final String REVISION_PREFIX = "REV";

    private static final int MIN_VERSION_VALUE = 1;

    private static final int MAX_VERSION_VALUE = 999;

    @NotNull
    private OAISIdentifier oaisIdentifier;

    @NotNull
    private EntityType entityType;

    @NotNull
    private String tenant;

    @NotNull
    private UUID entityId;

    /**
     * version number on 3 digits by specs(cf REGARDS_DSL_SYS_ARC_410)
     */
    @Min(MIN_VERSION_VALUE)
    @Max(MAX_VERSION_VALUE)
    private int version;

    /**
     * numeric value ordering the differents AIP from a same SIP
     */
    private Long order;

    private String revision;

    private final static Pattern PATTERN = Pattern.compile(URN_PATTERN);

    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId,
            int pVersion) {
        super();
        oaisIdentifier = pOaisIdentifier;
        entityType = pEntityType;
        tenant = pTenant;
        entityId = pEntityId;
        version = pVersion;
    }

    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId, // NOSONAR
            int pVersion, Long pOrder, String pRevision) {
        this(pOaisIdentifier, pEntityType, pTenant, pEntityId, pVersion);
        order = pOrder;
        revision = pRevision;
    }

    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId, // NOSONAR
            int pVersion, long pOrder) {// NOSONAR
        this(pOaisIdentifier, pEntityType, pTenant, pEntityId, pVersion);
        order = pOrder;
    }

    public UniformResourceName(OAISIdentifier pOaisIdentifier, EntityType pEntityType, String pTenant, UUID pEntityId, // NOSONAR
            int pVersion, String pRevision) {
        this(pOaisIdentifier, pEntityType, pTenant, pEntityId, pVersion);
        revision = pRevision;
    }

    public UniformResourceName() {// NOSONAR
        // for testing purpose
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

    public OAISIdentifier getOaisIdentifier() {
        return oaisIdentifier;
    }

    public void setOaisIdentifier(OAISIdentifier pOaisIdentifier) {
        oaisIdentifier = pOaisIdentifier;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(EntityType pEntityType) {
        entityType = pEntityType;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String pTenant) {
        tenant = pTenant;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID pEntityId) {
        entityId = pEntityId;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int pVersion) {
        version = pVersion;
    }

    public Long getOrder() {
        return order;
    }

    public void setOrder(Long pOrder) {
        order = pOrder;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String pRevision) {
        revision = pRevision;
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
                return new UniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                        Long.parseLong(versionWithOrder[1]), revisionString.substring(REVISION_PREFIX.length()));
            } else {
                // Revision is missing so we have all except Revision
                return new UniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                        Long.parseLong(versionWithOrder[1]));
            }
        } else {
            // we don't have an order specified
            if (stringFragment.length == 7) {
                // Revision is precised
                final String revisionString = stringFragment[6];
                // so we have all fields exception Order
                return new UniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                        revisionString.substring(REVISION_PREFIX.length()));
            } else {
                // Revision is missing so we have all except Revision and Order
                return new UniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())));
            }
        }
    }

    public static boolean isValidUrn(String pUrn) {
        return PATTERN.matcher(pUrn).matches();
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
