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
package fr.cnes.regards.modules.feature.dto.urn;

import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.framework.urn.validator.RegardsUrn;
import fr.cnes.regards.modules.feature.dto.urn.converter.FeatureUrnConverter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Convert;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * allow us to create URN with the following format:
 * URN:FeatureIdentifier:entityType:tenant:UUID(entityId):Vversion[,order][:REVrevision]
 * <p>
 * <br/>
 * Example:
 * <ul>
 * <li>URN:FEATURE:Collection:CDPP::1</li>
 * <li>URN:FEATURE:Collection:CDPP::1,5:REV2</li>
 * </ul>
 *
 * @author Kevin Marchois
 */
@RegardsUrn
@Convert(converter = FeatureUrnConverter.class)
@Schema(implementation = String.class)
public class FeatureUniformResourceName extends UniformResourceName {

    public static final int MAX_SIZE = 132;

    public static FeatureUniformResourceName build(FeatureIdentifier identifier,
                                                   EntityType entityType,
                                                   String tenant,
                                                   UUID entityId,
                                                   int version) {
        FeatureUniformResourceName urn = new FeatureUniformResourceName();
        urn.setIdentifier(identifier.name());
        urn.setEntityId(entityId);
        urn.setVersion(version);
        urn.setTenant(tenant);
        urn.setEntityType(entityType);
        return urn;
    }

    public FeatureUniformResourceName(FeatureIdentifier oaisIdentifier,
                                      EntityType entityType,
                                      String tenant,
                                      UUID entityId,
                                      int version,
                                      Long order,
                                      String revision) {
        super(oaisIdentifier.name(), entityType, tenant, entityId, version, order, revision);
    }

    public FeatureUniformResourceName() {
        // for testing purpose
    }

    public FeatureUniformResourceName(FeatureIdentifier identifier,
                                      EntityType entityType,
                                      String tenant,
                                      UUID entityId,
                                      Long order,
                                      String revision) {
        super(identifier.name(), entityType, tenant, entityId, order, revision);
    }

    public static boolean isValidUrn(String urn) {
        return UniformResourceName.isValidUrn(urn) && (FeatureIdentifier.FEATURE.toString()
                                                                                .equals(urn.split(DELIMITER)[1]));
    }

    /**
     * take this kind of String
     * URN:OAISIdentifier:entityType:tenant:UUID(entityId):version[,order][:REVrevision]
     * and return a new instance of {@link FeatureUniformResourceName}
     *
     * @param urn String respecting the following regex
     *            URN:.+:.+:.+:.+:\\d{1,3}(,\\d+)?(:REV.+)?
     * @return a new instance of {@link FeatureUniformResourceName}
     * @throws IllegalArgumentException if the given string does not respect the urn
     *                                  pattern
     */
    public static FeatureUniformResourceName fromString(String urn) {
        Pattern pattern = Pattern.compile(URN_PATTERN);
        if (!pattern.matcher(urn).matches()) {
            throw new IllegalArgumentException();
        }
        String[] stringFragment = urn.split(DELIMITER);
        FeatureIdentifier identifier = FeatureIdentifier.valueOf(stringFragment[1]);
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
            return new FeatureUniformResourceName(identifier, entityType, tenant, entityId, order, revision);
        } else {
            return new FeatureUniformResourceName(identifier, entityType, tenant, entityId, version, order, revision);
        }
    }

    /**
     * Build a pseudo random UUID starting with 00000000-0000-0000-0000 for test purpose only.
     * Use {@link #build(FeatureIdentifier, EntityType, String, UUID, int, Long, String)} in production.
     */
    public static FeatureUniformResourceName pseudoRandomUrn(FeatureIdentifier identifier,
                                                             EntityType entityType,
                                                             String tenant,
                                                             int version) {
        return FeatureUniformResourceName.build(identifier,
                                                entityType,
                                                tenant,
                                                UUID.fromString("0-0-0-0-" + (int) (Math.random() * Integer.MAX_VALUE)),
                                                version);
    }

    public static FeatureUniformResourceName clone(FeatureUniformResourceName template, Long order) {
        return FeatureUniformResourceName.build(FeatureIdentifier.valueOf(template.getIdentifier()),
                                                template.getEntityType(),
                                                template.getTenant(),
                                                template.getEntityId(),
                                                template.getVersion()).withOrder(order);
    }
}
