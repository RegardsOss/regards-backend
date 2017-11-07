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
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

public final class SIPEntityBuilder {

    private SIPEntityBuilder() {

    }

    public static SIPEntity build(String tenant, String sessionId, SIP sip, String processing, String owner,
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
        sipEntity.setSessionId(sessionId);
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
