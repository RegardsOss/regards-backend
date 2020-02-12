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
package fr.cnes.regards.modules.ingest.service;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.test.IngestServiceTest;

/**
 * Overlay of the default class to manage context cleaning in non transactional testing
 *
 * @author Marc SORDI
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
public abstract class IngestMultitenantServiceTest extends AbstractMultitenantServiceTest {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected static final long TWO_SECONDS = 2000;

    protected static final long FIVE_SECONDS = 5000;

    protected static final long TEN_SECONDS = 10000;

    @Autowired
    protected IIngestRequestRepository ingestRequestRepository;

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected IAIPRepository aipRepository;

    @Autowired
    protected IngestServiceTest ingestServiceTest;

    @Before
    public void init() throws Exception {
        runtimeTenantResolver.forceTenant(getDefaultTenant());
        ingestServiceTest.init();
        doInit();
    }

    /**
     * Custom test initialization to override
     * @throws Exception
     */
    protected void doInit() throws Exception {
        // Override to init something
    }

    @After
    public void clear() throws Exception {
        ingestServiceTest.clear();
        doAfter();
    }

    /**
     * Custom test cleaning to override
     * @throws Exception
     */
    protected void doAfter() throws Exception {
        // Override to init something
    }

    protected SIP create(String providerId, List<String> tags) {
        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA,
                           Paths.get("src", "main", "test", "resources", "data", "cdpp_collection.json"), "MD5",
                           "azertyuiopqsdfmlmld");
        sip.withSyntax(MediaType.APPLICATION_JSON);
        sip.registerContentInformation();
        if ((tags != null) && !tags.isEmpty()) {
            sip.withContextTags(tags.toArray(new String[0]));
        }

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        return sip;
    }

    protected void publishSIPEvent(SIP sip, String storage, String session, String sessionOwner,
            List<String> categories) {
        this.publishSIPEvent(sip, Lists.newArrayList(storage), session, sessionOwner, categories);
    }

    protected void publishSIPEvent(SIP sip, List<String> storages, String session, String sessionOwner,
            List<String> categories) {
        // Create event
        List<StorageMetadata> storagesMeta = storages.stream().map(StorageMetadata::build).collect(Collectors.toList());
        IngestMetadataDto mtd = IngestMetadataDto.build(sessionOwner, session,
                                                        IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL,
                                                        Sets.newHashSet(categories), storagesMeta);
        ingestServiceTest.sendIngestRequestEvent(sip, mtd);
    }

}
