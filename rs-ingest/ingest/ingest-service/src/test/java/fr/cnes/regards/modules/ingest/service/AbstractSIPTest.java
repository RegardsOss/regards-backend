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
package fr.cnes.regards.modules.ingest.service;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import com.google.gson.Gson;

import fr.cnes.regards.framework.oais.builder.InformationPackagePropertiesBuilder;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsServiceTransactionalIT;
import fr.cnes.regards.modules.ingest.dao.ISIPRepository;
import fr.cnes.regards.modules.ingest.dao.ISIPSessionRepository;
import fr.cnes.regards.modules.ingest.domain.SIP;
import fr.cnes.regards.modules.ingest.domain.builder.SIPBuilder;
import fr.cnes.regards.modules.ingest.domain.builder.SIPEntityBuilder;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;

/**
 * Abstract test class to provide SIP Creation tool.
 * @author Sébastien Binda
 */
@TestPropertySource(locations = "classpath:test.properties")
@ContextConfiguration(classes = { TestConfiguration.class })
public class AbstractSIPTest extends AbstractRegardsServiceTransactionalIT {

    @Autowired
    private Gson gson;

    @Autowired
    private ISIPSessionService sipSessionService;

    @Autowired
    private ISIPRepository sipRepository;

    @Autowired
    private ISIPSessionRepository sipSessionRepo;

    /**
     * Create a SIP for test initialization
     * @param sipId
     * @param sessionId
     * @param processing
     * @param owner
     * @param version
     * @return
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    protected SIPEntity createSIP(String sipId, String sessionId, String processing, String owner, Integer version)
            throws NoSuchAlgorithmException, IOException {
        SIPBuilder b = new SIPBuilder(sipId);
        InformationPackagePropertiesBuilder ippb = new InformationPackagePropertiesBuilder();
        ippb.addDescriptiveInformation("version", version.toString());
        SIP sip = b.build(ippb.build());
        SIPEntity sipEntity = SIPEntityBuilder.build(DEFAULT_TENANT, sipSessionService.getSession(sessionId, true), sip,
                                                     processing, owner, version, SIPState.STORED, EntityType.DATA);
        sipEntity.setChecksum(SIPEntityBuilder.calculateChecksum(gson, sip, IngestService.MD5_ALGORITHM));
        return sipRepository.save(sipEntity);
    }

    protected SIPEntity createSIP(String sipId, String sessionId, String processing, String owner, Integer version,
            SIPState state) throws NoSuchAlgorithmException, IOException {
        SIPEntity sipEntity = createSIP(sipId, sessionId, processing, owner, version);
        sipEntity.setState(state);
        return sipRepository.save(sipEntity);
    }

}
