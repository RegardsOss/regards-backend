/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.domain.builder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.google.gson.Gson;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.file.ChecksumUtils;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPSession;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Tool class to generate new {@link SIPEntity} entities.
 * @author Sébastien Binda
 */
public final class SIPEntityBuilder {

    private SIPEntityBuilder() {

    }

    public static SIPEntity build(String tenant, SIPSession session, SIP sip, String processing, String owner,
            Integer version, SIPState state, EntityType entityType) {
        SIPEntity sipEntity = new SIPEntity();

        UUID uuid = UUID.nameUUIDFromBytes(sip.getId().getBytes());

        UniformResourceName urn = new UniformResourceName(OAISIdentifier.SIP, entityType, tenant, uuid, version);

        sipEntity.setIpId(urn.toString());
        sipEntity.setOwner(owner);
        sipEntity.setIngestDate(OffsetDateTime.now());
        sipEntity.setSipId(sip.getId());
        sipEntity.setState(state);
        sipEntity.setSip(sip);
        sipEntity.setProcessing(processing);
        sipEntity.setSession(session);
        sipEntity.setVersion(version);

        return sipEntity;
    }

    public static String calculateChecksum(Gson gson, SIP sip, String algorithm)
            throws NoSuchAlgorithmException, IOException {
        String jsonSip = gson.toJson(sip);
        InputStream inputStream = new ByteArrayInputStream(jsonSip.getBytes());
        return ChecksumUtils.computeHexChecksum(inputStream, algorithm);
    }

}
