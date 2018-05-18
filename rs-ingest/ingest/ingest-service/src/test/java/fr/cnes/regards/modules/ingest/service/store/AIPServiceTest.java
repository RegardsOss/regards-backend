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
package fr.cnes.regards.modules.ingest.service.store;

import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Sets;
import fr.cnes.regards.framework.module.rest.exception.EntityNotFoundException;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dao.IAIPRepository;
import fr.cnes.regards.modules.ingest.domain.entity.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPEntity;
import fr.cnes.regards.modules.ingest.domain.entity.SIPState;
import fr.cnes.regards.modules.ingest.domain.entity.SipAIPState;
import fr.cnes.regards.modules.ingest.service.AbstractSIPTest;
import fr.cnes.regards.modules.ingest.service.ISIPService;
import fr.cnes.regards.modules.storage.domain.AIPState;

/**
 * AIP Service tests.
 * @author Sébastien Binda
 */
public class AIPServiceTest extends AbstractSIPTest {

    private final Set<AIPEntity> aips = Sets.newHashSet();

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IAIPRepository aipRepository;

    @Autowired
    private ISIPService sipService;

    private SIPEntity sip;

    @Override
    public void doInit() throws Exception {
        // generate one SIP
        sip = createSIP("AIP_SERVICE_TEST_001", null, "PROCESSING_001", "OWNER_001", 1, SIPState.CREATED);

        // Create two associated AIPs
        aips.add(createAIP(UniformResourceName
                                   .fromString("URN:AIP:DATA:project1:ebd5100a-b8fc-3e15-8ce1-4fdd1c98794a:V1"),
                           sip,
                           SipAIPState.CREATED));
        aips.add(createAIP(UniformResourceName
                                   .fromString("URN:AIP:DATA:project1:ebd5100a-b8fc-3e15-8ce1-4fdd1c98794b:V1"),
                           sip,
                           SipAIPState.CREATED));
    }

    @Purpose("Check that a SIP is updated to INDEXED state only when all the AIPs associated are in the INDEXED state")
    @Test
    public void testEntityIndexed() throws EntityNotFoundException {

        int count = 0;
        for (AIPEntity aip : aips) {
            count++;
            aipService.setAipToIndexed(aip);
            if (count == aips.size()) {
                // Check for SIP state updated
                SIPEntity currentSip = sipService.getSIPEntity(sip.getIpId());
                Assert.assertTrue(SIPState.INDEXED.equals(currentSip.getState()));
                // Check that all AIPs has been deleted
                Assert.assertTrue("No AIP should be remaining in ingest after indexation done.",
                                  aipRepository.findBySip(sip).isEmpty());
            } else {
                Optional<AIPEntity> updatedAip = aipService.searchAip(UniformResourceName.fromString(aip.getIpId()));
                Assert.assertTrue(String.format("AIP should be in INDEXED state not %s", updatedAip.get().getState()),
                                  updatedAip.isPresent() && SipAIPState.INDEXED.equals(updatedAip.get().getState()));
                // Check for SIP state not updated
                SIPEntity currentSip = sipService.getSIPEntity(sip.getIpId());
                Assert.assertTrue(SIPState.CREATED.equals(currentSip.getState()));
            }
        }
        Assert.assertTrue("Error no AIP updated during the test.", count > 0);
    }

    @Purpose("Check that a SIP is updated to STORED state only when all the AIPs associated are in the STORED state")
    @Test
    public void testEntityStored() throws EntityNotFoundException {
        int count = 0;
        for (AIPEntity aip : aips) {
            aipService.setAipToStored(aip.getIpId(), AIPState.STORED);
            Optional<AIPEntity> updatedAip = aipService.searchAip(UniformResourceName.fromString(aip.getIpId()));
            Assert.assertTrue("AIP should be in STORED state",
                              updatedAip.isPresent() && AIPState.STORED.equals(updatedAip.get().getState()));
            count++;
            if (count == aips.size()) {
                // Check for SIP state updated
                SIPEntity currentSip = sipService.getSIPEntity(sip.getIpId());
                Assert.assertTrue(SIPState.STORED.equals(currentSip.getState()));
            } else {
                // Check for SIP state not updated
                SIPEntity currentSip = sipService.getSIPEntity(sip.getIpId());
                Assert.assertTrue(SIPState.CREATED.equals(currentSip.getState()));
            }
        }
        Assert.assertTrue("Error no AIP updated during the test.", count > 0);

        // Check that all AIPs has been deleted
        Assert.assertTrue("No AIP should be deleted in ingest after storage done.",
                          aipRepository.findBySip(sip).size() == aips.size());
    }

}
