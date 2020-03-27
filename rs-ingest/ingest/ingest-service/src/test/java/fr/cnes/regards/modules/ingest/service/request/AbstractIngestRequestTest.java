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
package fr.cnes.regards.modules.ingest.service.request;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.oais.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IAbstractRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.storage.domain.database.FileLocation;
import fr.cnes.regards.modules.storage.domain.database.FileReference;
import fr.cnes.regards.modules.storage.domain.database.FileReferenceMetaInfo;

/**
 * Abstract class test to initialize SIP and AIP
 *
 * @author Sébastien Binda
 *
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public abstract class AbstractIngestRequestTest extends AbstractMultitenantServiceTest {

    protected SIPEntity sipEntity;

    protected AIPEntity aipEntity;

    @Autowired
    protected ISIPRepository sipRepo;

    @Autowired
    protected IAIPRepository aipRepo;

    @Autowired
    protected IAbstractRequestRepository abstractRequestRepository;

    @Before
    public void init() throws InterruptedException {
        simulateApplicationReadyEvent();
        // Re-set tenant because above simulation clear it!
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        boolean retry = false;
        do {
            try {
                abstractRequestRepository.deleteAll();
                aipRepo.deleteAll();
                sipRepo.deleteAll();
                retry = false;
            } catch (DataAccessException e) {
                // Retry only once. May be an error of previous transactional context.
                retry = !retry;
                Thread.sleep(2_000);
            }
        } while (retry);
    }

    protected void initSipAndAip(String checksum, String providerId) {
        SIP sip = SIP.build(EntityType.DATA, "providerId");
        sipEntity = SIPEntity.build(getDefaultTenant(), IngestMetadata
                .build("sessionOwner", "session", "ingestChain", Sets.newHashSet(), StorageMetadata.build("storage")),
                                    sip, 1, SIPState.INGESTED);
        sipEntity.setChecksum(checksum);
        sipEntity.setLastUpdate(OffsetDateTime.now());
        sipEntity = sipRepo.save(sipEntity);
        UniformResourceName sipId = sipEntity.getSipIdUrn();
        UniformResourceName aipId = UniformResourceName.fromString(sipEntity.getSipIdUrn().toString());
        aipId.setOaisIdentifier(OAISIdentifier.AIP);
        AIP aip = AIP.build(EntityType.DATA, aipId, Optional.of(sipId), providerId, sipEntity.getVersion());
        aipEntity = AIPEntity.build(sipEntity, AIPState.STORED, aip);
        aipEntity = aipRepo.save(aipEntity);
    }

    protected FileReference simulatefileReference(String checksum, String owner) {
        FileReferenceMetaInfo meta = new FileReferenceMetaInfo(checksum, "MD5", "file.name", 10L, MediaType.TEXT_PLAIN);
        return new FileReference(owner, meta, new FileLocation("somewhere", "file:///somewhere/file.name"));
    }

}
