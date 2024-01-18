/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.aip;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.oais.dto.ContentInformationDto;
import fr.cnes.regards.framework.oais.dto.OAISDataObjectLocationDto;
import fr.cnes.regards.framework.oais.dto.aip.AIPDto;
import fr.cnes.regards.framework.oais.dto.sip.SIPDto;
import fr.cnes.regards.framework.oais.dto.urn.OAISIdentifier;
import fr.cnes.regards.framework.oais.dto.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.modules.fileaccess.dto.FileLocationDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceDto;
import fr.cnes.regards.modules.fileaccess.dto.FileReferenceMetaInfoDto;
import fr.cnes.regards.modules.fileaccess.dto.request.RequestResultInfoDto;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.sip.IngestMetadata;
import fr.cnes.regards.modules.ingest.domain.sip.SIPEntity;
import fr.cnes.regards.modules.ingest.dto.AIPState;
import fr.cnes.regards.modules.ingest.dto.SIPState;
import fr.cnes.regards.modules.ingest.dto.aip.StorageMetadata;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.annotation.DirtiesContext.HierarchyMode;
import org.springframework.test.context.TestPropertySource;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * @author Léo Mieulet
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS, hierarchyMode = HierarchyMode.EXHAUSTIVE)
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=aip_storage_service" },
                    locations = { "classpath:application-test.properties" })
public class AIPStorageServiceIT extends AbstractMultitenantServiceIT {

    private static String sessionOwner = "NAASA";

    private static String session = "session d'ingestion";

    private static String ingestChain = "ingest chain";

    private static String providerId = "provider 1";

    private static final String FAKE_CHECKSUM_1 = "0123456789FAKE_CHECKSUM0123456789";

    private static final String FAKE_CHECKSUM_2 = "0456789123FAKE_CHECKSUM0456789123";

    private static final String FAKE_CHECKSUM_3 = "0789456123FAKE_CHECKSUM0789456123";

    private static final String FAKE_CHECKSUM_4 = "FAKE_CHECKSUM0789456123FAKE_CHECKSUM";

    private static final String LOCATION = "AWS";

    private static final String LOCATION_2 = "Alpes";

    private static final String LOCATION_3 = "Pyrenees";

    private static final String LOCATION_4 = "Cordillera de los Andes";

    private static Set<String> categories = Sets.newHashSet("CAT 1", "CAT 2");

    private static SIPEntity sipEntity;

    private static AIPEntity aipEntity1;

    @Autowired
    private IAIPStorageService storageService;

    public void init() {
        sipEntity = SIPEntity.build(getDefaultTenant(),
                                    IngestMetadata.build(sessionOwner,
                                                         session,
                                                         null,
                                                         ingestChain,
                                                         categories,
                                                         StorageMetadata.build(LOCATION,
                                                                               "/dir1/dir2/",
                                                                               new HashSet<>()),
                                                         StorageMetadata.build(LOCATION_2,
                                                                               "/dir1/dir2/",
                                                                               new HashSet<>()),
                                                         StorageMetadata.build(LOCATION_3,
                                                                               "/dir1/dir2/",
                                                                               new HashSet<>())),
                                    SIPDto.build(EntityType.DATA, providerId),
                                    1,
                                    SIPState.INGESTED);
        sipEntity.getSip()
                 .withDataObject(DataType.RAWDATA,
                                 "myfile1.txt",
                                 "MD5",
                                 FAKE_CHECKSUM_1,
                                 0L,
                                 OAISDataObjectLocationDto.build("rs-storage/myfile1.txt", LOCATION),
                                 OAISDataObjectLocationDto.build("rs-storage/myfile3.txt", LOCATION_3))
                 .registerContentInformation()
                 .withDataObject(DataType.DESCRIPTION,
                                 "myfile2.txt",
                                 "MD5",
                                 FAKE_CHECKSUM_2,
                                 0L,
                                 OAISDataObjectLocationDto.build("rs-storage/myfile2.txt", LOCATION_2),
                                 OAISDataObjectLocationDto.build("rs-storage/myfile2.txt", LOCATION_3))
                 .registerContentInformation()
                 .withDataObject(DataType.DOCUMENT,
                                 "myfile3.txt",
                                 "MD5",
                                 FAKE_CHECKSUM_3,
                                 0L,
                                 OAISDataObjectLocationDto.build("rs-storage/myfile3.txt", LOCATION_3))
                 .registerContentInformation();
        aipEntity1 = AIPEntity.build(sipEntity,
                                     AIPState.GENERATED,
                                     AIPDto.build(sipEntity.getSip(),
                                                  OaisUniformResourceName.pseudoRandomUrn(OAISIdentifier.AIP,
                                                                                          EntityType.COLLECTION,
                                                                                          getDefaultTenant(),
                                                                                          1),
                                                  Optional.ofNullable(sipEntity.getSipIdUrn()),
                                                  providerId,
                                                  sipEntity.getVersion()));
        aipEntity1.setStorages(Sets.newHashSet(LOCATION, LOCATION_2, LOCATION_3));
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_040")
    @Purpose("System should update AIPs with their location when a file is successfully stored")
    public void testAddAIPLocation() {
        // Test usual behavior
        init();
        Assert.assertEquals("3 storage in ingest metadata at the beginning", 3, aipEntity1.getStorages().size());
        Assert.assertEquals("No event before", 0, aipEntity1.getAip().getHistory().size());
        Collection<RequestResultInfoDto> storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_3, LOCATION_4);

        AIPUpdateResult isUpdated = storageService.addAIPLocations(aipEntity1, storeRequestsInfos);
        Assert.assertTrue("Should detect some change", isUpdated.isAipEntityUpdated());
        Assert.assertTrue("Should detect some change", isUpdated.isAipUpdated());
        Assert.assertEquals("Now 4 storages should be defined in ingest metadata", 4, aipEntity1.getStorages().size());
        Optional<ContentInformationDto> ciOp = aipEntity1.getAip()
                                                         .getProperties()
                                                         .getContentInformations()
                                                         .stream()
                                                         .filter(ci -> ci.getDataObject()
                                                                         .getChecksum()
                                                                         .equals(FAKE_CHECKSUM_3))
                                                         .findFirst();
        Assert.assertTrue(ciOp.isPresent());
        Assert.assertEquals("Now two locations should be defined in that dataobject",
                            2,
                            ciOp.get().getDataObject().getLocations().size());
        Assert.assertEquals("Some event have been added", 1, aipEntity1.getAip().getHistory().size());

        // Test add only to locations of the file
        init();
        storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_3, LOCATION_2);
        isUpdated = storageService.addAIPLocations(aipEntity1, storeRequestsInfos);
        Assert.assertTrue("Should detect some change", isUpdated.isAipEntityUpdated());
        Assert.assertTrue("Should detect some change", isUpdated.isAipUpdated());
        Assert.assertEquals("Sill 3 storages should be defined in ingest metadata", 3, aipEntity1.getStorages().size());
        ciOp = aipEntity1.getAip()
                         .getProperties()
                         .getContentInformations()
                         .stream()
                         .filter(ci -> ci.getDataObject().getChecksum().equals(FAKE_CHECKSUM_3))
                         .findFirst();
        Assert.assertTrue(ciOp.isPresent());
        Assert.assertEquals("Now two locations should be defined in that dataobject",
                            2,
                            ciOp.get().getDataObject().getLocations().size());
    }

    @Test
    public void testAddAIPLocationNotUpdating() {
        init();
        // Test with unrelated FAKE_CHECKSUM_4
        Collection<RequestResultInfoDto> storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_4, LOCATION_2);
        AIPUpdateResult isUpdated = storageService.addAIPLocations(aipEntity1, storeRequestsInfos);
        Assert.assertFalse("Should not detect any change", isUpdated.isAipEntityUpdated());
        Assert.assertFalse("Should not detect any change", isUpdated.isAipUpdated());

        // Test with something that is already saved in the AIP on that location
        storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_1, LOCATION_3);
        isUpdated = storageService.addAIPLocations(aipEntity1, storeRequestsInfos);
        Assert.assertFalse("Should not detect some change", isUpdated.isAipEntityUpdated());
        Assert.assertFalse("Should not detect some change", isUpdated.isAipUpdated());
    }

    @Test
    public void testRemoveAIPLocation() {
        // Test usual behavior
        init();
        Assert.assertEquals("3 storage in ingest metadata at the beginning", 3, aipEntity1.getStorages().size());
        Assert.assertEquals("No event before", 0, aipEntity1.getAip().getHistory().size());
        Collection<RequestResultInfoDto> storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_2, LOCATION_2);

        AIPUpdateResult isUpdated = storageService.removeAIPLocations(aipEntity1, storeRequestsInfos);
        Assert.assertTrue("Should detect some change", isUpdated.isAipEntityUpdated());
        Assert.assertTrue("Should detect some change", isUpdated.isAipUpdated());
        Assert.assertEquals("Now 2 storages in ingest metadata remaining", 2, aipEntity1.getStorages().size());
        Optional<ContentInformationDto> ciOp = aipEntity1.getAip()
                                                         .getProperties()
                                                         .getContentInformations()
                                                         .stream()
                                                         .filter(ci -> ci.getDataObject()
                                                                         .getChecksum()
                                                                         .equals(FAKE_CHECKSUM_2))
                                                         .findFirst();
        Assert.assertTrue(ciOp.isPresent());
        Assert.assertEquals("Still 1 location should be defined in that dataobject",
                            1,
                            ciOp.get().getDataObject().getLocations().size());
        Assert.assertNotEquals("The remaining location should not be the one we've just removed",
                               LOCATION_2,
                               ciOp.get().getDataObject().getLocations().iterator().next().getStorage());
        Assert.assertEquals("Some event have been added", 1, aipEntity1.getAip().getHistory().size());

        // Now test only removing from locations of the dataobject (and not ingest metadata)
        init();
        storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_3, LOCATION_3);
        isUpdated = storageService.removeAIPLocations(aipEntity1, storeRequestsInfos);

        Assert.assertTrue("Should detect some change", isUpdated.isAipEntityUpdated());
        Assert.assertTrue("Should detect some change", isUpdated.isAipUpdated());
        Assert.assertEquals("Still 3 storages in ingest metadata", 3, aipEntity1.getStorages().size());
        ciOp = aipEntity1.getAip()
                         .getProperties()
                         .getContentInformations()
                         .stream()
                         .filter(ci -> ci.getDataObject().getChecksum().equals(FAKE_CHECKSUM_3))
                         .findFirst();
        Assert.assertTrue(ciOp.isPresent());
        Assert.assertEquals("No more location should be defined in that dataobject",
                            0,
                            ciOp.get().getDataObject().getLocations().size());
    }

    @Test
    public void testRemoveAIPLocationNotUpdating() {
        init();
        // Test to remove a location not referenced by the dataobject
        Collection<RequestResultInfoDto> storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_3, LOCATION_2);
        AIPUpdateResult isUpdated = storageService.removeAIPLocations(aipEntity1, storeRequestsInfos);
        Assert.assertFalse("Should not detect some change", isUpdated.isAipEntityUpdated());
        Assert.assertFalse("Should not detect some change", isUpdated.isAipUpdated());

        // Test to remove from a missing dataobject
        storeRequestsInfos = getStorageQueryResult(FAKE_CHECKSUM_4, LOCATION_2);
        isUpdated = storageService.removeAIPLocations(aipEntity1, storeRequestsInfos);
        Assert.assertFalse("Should not detect some change", isUpdated.isAipEntityUpdated());
        Assert.assertFalse("Should not detect some change", isUpdated.isAipUpdated());
    }

    private ArrayList<RequestResultInfoDto> getStorageQueryResult(String fakeChecksum3, String location) {
        return Lists.newArrayList(RequestResultInfoDto.build("groupId",
                                                             fakeChecksum3,
                                                             location,
                                                             null,
                                                             Sets.newHashSet("someone"),
                                                             new FileReferenceDto(OffsetDateTime.now(),
                                                                                  new FileReferenceMetaInfoDto(
                                                                                      fakeChecksum3,
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      null,
                                                                                      null),
                                                                                  new FileLocationDto(location,
                                                                                                      "http://someurl.com"),
                                                                                  Sets.newHashSet()),
                                                             null));
    }
}