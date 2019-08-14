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
package fr.cnes.regards.modules.ingest.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.jobs.dao.IJobInfoRepository;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.aip.AIPState;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.AIPBuilder;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.dto.sip.SIPBuilder;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;

/**
 * Abstract test class to provide SIP Creation tool.
 * @author Sébastien Binda
 */
@ActiveProfiles({ "testAmqp", "disable-scheduled-ingest", "noschdule" })
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { TestConfiguration.class })
public abstract class AbstractSipIT extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private Gson gson;

    @Autowired
    protected ISIPRepository sipRepository;

    @Autowired
    protected IAIPRepository aipRepository;

    @Autowired
    private IJobInfoRepository jobInfoRepo;

    @Autowired
    private IRuntimeTenantResolver tenantResolver;

    @Autowired
    private ISIPService sipService;

    @Before
    public void init() throws Exception {
        tenantResolver.forceTenant(getDefaultTenant());
        aipRepository.deleteAll();
        sipRepository.deleteAll();
        jobInfoRepo.deleteAll();
        doInit();
    }

    @After
    public void clear() {
        aipRepository.deleteAll();
        sipRepository.deleteAll();
    }

    /**
     * Create a SIP for test initialization
     */
    protected SIPEntity createSIP(String providerId, String sessionOwner, String session, String ingestChain,
            Integer version) throws NoSuchAlgorithmException, IOException, ModuleException {
        SIPBuilder b = new SIPBuilder(providerId);
        InformationPackagePropertiesBuilder ippb = new InformationPackagePropertiesBuilder();
        ippb.addDescriptiveInformation("version", version.toString());
        SIP sip = b.build(ippb.build());
        SIPEntity sipEntity = SIPEntity.build(getDefaultTenant(),
                                              IngestMetadata.build(sessionOwner, session, ingestChain), sip, version,
                                              SIPState.INGESTED, EntityType.DATA);
        sipEntity.setChecksum(sipService.calculateChecksum(sip));
        return sipRepository.save(sipEntity);
    }

    protected SIPEntity createSIP(String providerId, String sessionOwner, String session, String processing,
            String owner, Integer version, SIPState state)
            throws NoSuchAlgorithmException, IOException, ModuleException {
        SIPEntity sipEntity = createSIP(providerId, sessionOwner, session, processing, version);
        sipEntity.setState(state);
        return sipRepository.save(sipEntity);
    }

    protected AIPEntity createAIP(UniformResourceName aipId, SIPEntity sip, AIPState state) {
        AIPEntity aip = new AIPEntity();
        aip.setAip(new AIPBuilder(aipId, Optional.of(sip.getSipIdUrn()), sip.getProviderId(), EntityType.DATA).build());
        aip.setCreationDate(OffsetDateTime.now());
        aip.setAipId(aipId);
        aip.setSip(sip);
        aip.setState(state);
        aipRepository.save(aip);
        return aip;
    }

    public abstract void doInit() throws Exception;

}
