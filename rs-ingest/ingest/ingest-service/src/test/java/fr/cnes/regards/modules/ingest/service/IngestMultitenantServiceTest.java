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
package fr.cnes.regards.modules.ingest.service;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.IIngestRequestRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.chain.IngestProcessingChain;
import fr.cnes.regards.modules.ingest.domain.sip.VersioningMode;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import fr.cnes.regards.modules.ingest.dto.sip.IngestMetadataDto;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.chain.IIngestProcessingChainService;
import fr.cnes.regards.modules.ingest.service.plugin.AIPGenerationTestPlugin;
import fr.cnes.regards.modules.ingest.service.plugin.ValidationTestPlugin;
import fr.cnes.regards.modules.test.IngestServiceTest;

/**
 * Overlay of the default class to manage context cleaning in non transactional testing
 *
 * @author Marc SORDI
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "eureka.client.enabled=false" },
        locations = { "classpath:application-test.properties" })
public abstract class IngestMultitenantServiceTest extends AbstractMultitenantServiceTest {

    protected static final long TWO_SECONDS = 2000;

    protected static final long FIVE_SECONDS = 5000;

    protected static final long TEN_SECONDS = 10000;

    protected final static String CHAIN_PP_LABEL = "ChainWithPostProcess";

    protected final static String CHAIN_PP_WITH_ERRORS_LABEL = "ChainWithPostProcessErrors";

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    protected IIngestRequestRepository ingestRequestRepository;

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected IAIPRepository aipRepository;

    @Autowired
    protected IngestServiceTest ingestServiceTest;

    @Autowired
    protected IIngestProcessingChainService ingestProcessingService;

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
        String fileName = String.format("file-%s.dat", providerId);
        SIP sip = SIP.build(EntityType.DATA, providerId);
        sip.withDataObject(DataType.RAWDATA, Paths.get("src", "test", "resources", "data", fileName), "MD5",
                           UUID.randomUUID().toString());
        sip.withSyntax(MediaType.APPLICATION_JSON);
        sip.registerContentInformation();
        if ((tags != null) && !tags.isEmpty()) {
            sip.withContextTags(tags.toArray(new String[0]));
        }

        // Add creation event
        sip.withEvent(String.format("SIP %s generated", providerId));

        return sip;
    }

    protected IngestProcessingChain createChainWithPostProcess(String label, Class<?> postProcessPluginClass)
            throws ModuleException {
        IngestProcessingChain newChain = new IngestProcessingChain();
        newChain.setDescription(label);
        newChain.setName(label);

        PluginConfiguration validation = PluginConfiguration.build(ValidationTestPlugin.class, null, Sets.newHashSet());
        validation.setIsActive(true);
        validation.setLabel("validationPlugin_ipst");
        newChain.setValidationPlugin(validation);

        PluginConfiguration generation = PluginConfiguration.build(AIPGenerationTestPlugin.class, null,
                                                                   Sets.newHashSet());
        generation.setIsActive(true);
        generation.setLabel("generationPlugin_ipst");
        newChain.setGenerationPlugin(generation);

        PluginConfiguration postprocess = PluginConfiguration.build(postProcessPluginClass, null, Sets.newHashSet());
        postprocess.setIsActive(true);
        postprocess.setLabel("postprocess test plugin");

        newChain.setPostProcessingPlugin(postprocess);

        return ingestProcessingService.createNewChain(newChain);
    }

    protected void publishSIPEvent(SIP sip, String storage, String session, String sessionOwner,
            List<String> categories) {
        publishSIPEvent(sip, Lists.newArrayList(storage), session, sessionOwner, categories, Optional.empty());
    }

    protected void publishSIPEvent(SIP sip, List<String> storages, String session, String sessionOwner,
            List<String> categories, Optional<String> chainLabel) {
        publishSIPEvent(sip, storages, session, sessionOwner, categories, chainLabel, null);
    }

    protected void publishSIPEvent(SIP sip, List<String> storages, String session, String sessionOwner,
            List<String> categories, Optional<String> chainLabel, VersioningMode versioningMode) {
        publishSIPEvent(Sets.newHashSet(sip), storages, session, sessionOwner, categories, chainLabel, versioningMode);
    }

    protected void publishSIPEvent(Collection<SIP> sips, List<String> storages, String session, String sessionOwner,
            List<String> categories, Optional<String> chainLabel, VersioningMode versioningMode) {
        // Create event
        List<StorageMetadata> storagesMeta = storages.stream().map(StorageMetadata::build).collect(Collectors.toList());
        IngestMetadataDto mtd = IngestMetadataDto
                .build(sessionOwner, session, chainLabel.orElse(IngestProcessingChain.DEFAULT_INGEST_CHAIN_LABEL),
                       Sets.newHashSet(categories), versioningMode, storagesMeta);
        ingestServiceTest.sendIngestRequestEvent(sips, mtd);
    }

}
