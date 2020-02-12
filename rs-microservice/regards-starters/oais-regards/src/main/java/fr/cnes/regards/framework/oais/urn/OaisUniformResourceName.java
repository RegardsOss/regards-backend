/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.util.UUID;
import java.util.regex.Pattern;

import javax.persistence.Convert;

import fr.cnes.regards.framework.oais.urn.converters.OaisUrnConverter;
import fr.cnes.regards.framework.oais.validator.RegardsOaisUrn;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;

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
 */
@RegardsOaisUrn
@Convert(converter = OaisUrnConverter.class)
public class OaisUniformResourceName extends UniformResourceName {

    /**
     * Constructor setting the given parameters as attributes
     */
    public OaisUniformResourceName(OAISIdentifier oaisIdentifier, EntityType entityType, String tenant, UUID entityId,
            int version) {
        super(oaisIdentifier.name(), entityType, tenant, entityId, version, null, null);
    }

    /**
     * Constructor setting the given parameters as attributes
     */
    public OaisUniformResourceName(OAISIdentifier oaisIdentifier, EntityType entityType, String tenant, UUID entityId,
            int version, Long order, String revision) {
        super(oaisIdentifier.name(), entityType, tenant, entityId, version, order, revision);
    }

    /**
     * Constructor setting the given parameters as attributes
     */
    public OaisUniformResourceName(OAISIdentifier oaisIdentifier, EntityType entityType, String tenant, UUID entityId,
            int version, long order) {
        super(oaisIdentifier.name(), entityType, tenant, entityId, version, order, null);
    }

    /**
     * Constructor setting the given parameters as attributes
     */
    public OaisUniformResourceName(OAISIdentifier oaisIdentifier, EntityType entityType, String tenant, UUID entityId,
            int version, String revision) {
        super(oaisIdentifier.name(), entityType, tenant, entityId, version, null, revision);
    }

    public OaisUniformResourceName() {
        // for testing purpose
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
    public static OaisUniformResourceName fromString(String urn) {
        Pattern pattern = Pattern.compile(URN_PATTERN);
        if (!pattern.matcher(urn).matches()) {
            throw new IllegalArgumentException();
        }
        String[] stringFragment = urn.split(DELIMITER);
        OAISIdentifier oaisIdentifier = OAISIdentifier.valueOf(stringFragment[1]);
        EntityType entityType = EntityType.valueOf(stringFragment[2]);
        String tenant = stringFragment[3];
        UUID entityId = UUID.fromString(stringFragment[4]);
        String[] versionWithOrder = stringFragment[5].split(",");
        if (versionWithOrder.length == 2) {
            // Order is precised
            if (stringFragment.length == 7) {
                // Revision is precised
                String revisionString = stringFragment[6];
                // so we have all fields
                return new OaisUniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                        Long.parseLong(versionWithOrder[1]), revisionString.substring(REVISION_PREFIX.length()));
            } else {
                // Revision is missing so we have all except Revision
                return new OaisUniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                        Long.parseLong(versionWithOrder[1]));
            }
        } else {
            // we don't have an order specified
            if (stringFragment.length == 7) {
                // Revision is precised
                String revisionString = stringFragment[6];
                // so we have all fields exception Order
                return new OaisUniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())),
                        revisionString.substring(REVISION_PREFIX.length()));
            } else {
                // Revision is missing so we have all except Revision and Order
                return new OaisUniformResourceName(oaisIdentifier, entityType, tenant, entityId,
                        Integer.parseInt(versionWithOrder[0].substring(VERSION_PREFIX.length())));
            }
        }
    }

    /**
     * Build a pseudo random UUID starting with 00000000-0000-0000-0000
     */
    public static OaisUniformResourceName pseudoRandomUrn(OAISIdentifier oaisIdentifier, EntityType entityType,
            String tenant, int version) {
        return new OaisUniformResourceName(oaisIdentifier, entityType, tenant,
                UUID.fromString("0-0-0-0-" + (int) (Math.random() * Integer.MAX_VALUE)), version);
    }

    public static OaisUniformResourceName clone(OaisUniformResourceName template, Long order) {
        return new OaisUniformResourceName(OAISIdentifier.valueOf(template.getIdentifier()), template.getEntityType(),
                template.getTenant(), template.getEntityId(), template.getVersion(), order);
    }
}
