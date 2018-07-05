package fr.cnes.regards.modules.storage.rest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.geojson.GeoJsonMediaType;
import fr.cnes.regards.framework.oais.EventType;
import fr.cnes.regards.framework.security.utils.HttpConstants;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPCollection;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.AvailabilityRequest;
import fr.cnes.regards.modules.storage.domain.AvailabilityResponse;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.job.AIPQueryFilters;
import fr.cnes.regards.modules.storage.domain.job.AddAIPTagsFilters;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsCollectionContaining;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.internal.matchers.NotNull;
import org.springframework.http.MediaType;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class AIPControllerIT extends AbstractAIPControllerIT {

    private static final int MAX_WAIT = 60000;

    private static final String SESSION = "Session123";

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_080")
    public void testStoreUnvalid() {
        aip.getProperties().getContentInformations().forEach(ci -> ci.getDataObject().setChecksum(null));
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isUnprocessableEntity());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        // perform request
        performDefaultPost(AIPController.AIP_PATH, new AIPCollection(aip), requestBuilderCustomizer,
                "AIP storage should have been schedule properly");
    }

    @Test
    public void testStore() {
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isCreated());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        // perform request
        performDefaultPost(AIPController.AIP_PATH, new AIPCollection(aip), requestBuilderCustomizer,
                "AIP storage should have been schedule properly");
    }

    @Test
    public void testStoreTotalFail() {
        testStore();
        // now just try to store the same aip
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isUnprocessableEntity());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        // perform request
        performDefaultPost(AIPController.AIP_PATH, new AIPCollection(aip), requestBuilderCustomizer,
                "Same AIP cannot be stored twice");
    }

    @Test
    public void testStoreFailPartial() throws MalformedURLException {
        testStore();
        // now just try to store the same aip
        // make requirements
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isPartialContent());
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        AIP aip2 = getAIP();

        // perform request
        performDefaultPost(AIPController.AIP_PATH, new AIPCollection(aip, aip2), requestBuilderCustomizer,
                "Success should be partial, aip cannot be re stored but aip2 can be stored");
    }

    private Map<String, List<String>> getHeaders() {
        Map<String, List<String>> headers = Maps.newHashMap();
        headers.put(HttpConstants.ACCEPT,
                Lists.newArrayList("application/json", MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.put(HttpConstants.CONTENT_TYPE, Lists.newArrayList(GeoJsonMediaType.APPLICATION_GEOJSON_VALUE));
        return headers;
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_140")
    public void testMakeAvailable() throws InterruptedException {
        // ask for an aip to be stored
        testStore();
        // wait for the AIP
        Thread.sleep(20000);
        // get the datafiles checksum
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip);
        Set<String> dataFilesChecksum = dataFiles.stream().map(df -> df.getChecksum()).collect(Collectors.toSet());
        // ask for availability
        AvailabilityRequest availabilityRequest = new AvailabilityRequest(OffsetDateTime.now().plusDays(2),
                dataFilesChecksum.toArray(new String[dataFilesChecksum.size()]));
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.content()
                .json(gson.toJson(new AvailabilityResponse(Sets.newHashSet(), dataFiles, Sets.newHashSet()))));
        performDefaultPost(AIPController.AIP_PATH + AIPController.PREPARE_DATA_FILES, availabilityRequest,
                requestBuilderCustomizer,
                "data should already be available as they are in an online data storage");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_110")
    public void testRetrieveAip() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AIPController.AIP_PATH + AIPController.ID_PATH, requestBuilderCustomizer,
                "we should have the aip", aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STOP_AIP_115")
    @Ignore
    public void testRetrieveDeletedAip() throws InterruptedException {
        testDelete();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        // first we expect that the aip has a DELETION event in its history
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$.properties.pdi.provenanceInformation.history[*].type",
                        IsCollectionContaining.hasItem(EventType.DELETION.name())));
        // now we expect that those events does have a date
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers
                .jsonPath("$.properties.pdi.provenanceInformation.history[?(@.type == \"" + EventType.DELETION.name()
                        + "\")].date", NotNull.NOT_NULL));
        performDefaultGet(AIPController.AIP_PATH + AIPController.ID_PATH, requestBuilderCustomizer,
                "we should have the aip", aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_560")
    @Purpose("System allow to retrieve aips thanks to a list of ip id")
    public void testRetrieveAipBulk() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPost(AIPController.AIP_PATH + AIPController.AIP_BULK, Sets.newHashSet(aip.getId().toString()),
                requestBuilderCustomizer, "we should have the aips");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_560")
    @Purpose("System allow to retrieve aips thanks to a tag")
    public void testRetrieveAipTag() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AIPController.AIP_PATH + AIPController.TAG, requestBuilderCustomizer,
                "we should have the aips", aip.getId().toString(), "tag");
    }

    @Test
    public void testDelete() throws InterruptedException {
        testStore();
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        int wait = 0;
        // lets wait for this AIP to be stored
        while ((aipDao.findOneByIpId(aip.getId().toString()).get().getState() != AIPState.STORED)
                && (wait < MAX_WAIT)) {
            Thread.sleep(1000);
            wait += 1000;
        }
        Assert.assertTrue("AIP was not fully stored in time: " + wait, wait < MAX_WAIT);
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isNoContent());
        requestBuilderCustomizer.addDocumentationSnippet(RequestDocumentation
                .pathParameters(RequestDocumentation.parameterWithName("ip_id").description("IpId of the AIP")));
        performDefaultDelete(AIPController.AIP_PATH + AIPController.ID_PATH, requestBuilderCustomizer,
                "deletion of this aip should be possible", aip.getId().toString());
    }

    @Test
    public void testBulkDelete() {
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isNoContent());

        Set<String> sipIpIds = new HashSet<>();
        sipIpIds.add("SIPIPIDTEST1");
        sipIpIds.add("SIPIPIDTEST2");
        performDefaultPost(AIPController.AIP_PATH + AIPController.AIP_BULK_DELETE, sipIpIds, requestBuilderCustomizer,
                "AIPs should be deleted");
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_120")
    public void testRetrieveAIPFiles() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AIPController.AIP_PATH + AIPController.OBJECT_LINK_PATH, requestBuilderCustomizer,
                "we should have the metadata of the files of the aip", aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_150")
    public void testRetrieveAIPVersionHistory() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AIPController.AIP_PATH + AIPController.VERSION_PATH, requestBuilderCustomizer,
                "we should have the different versions of an aip", aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_AIP_160")
    public void testRetrieveAIPHistory() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AIPController.AIP_PATH + AIPController.HISTORY_PATH, requestBuilderCustomizer,
                "we should have the history of an aip", aip.getId().toString());
    }

    @Test
    @Requirement("REGARDS_DSL_STO_ARC_200")
    @Requirement("REGARDS_DSL_STO_AIP_130")
    @Ignore
    public void testDownload() throws InterruptedException {
        // lets make files available
        // first lets make available the file
        testMakeAvailable();
        // lets ask for download now
        runtimeTenantResolver.forceTenant(DEFAULT_TENANT);
        // lets get the hand of a datafile checksum
        Set<StorageDataFile> dataFiles = dataFileDao.findAllByAip(aip);
        StorageDataFile dataFile = dataFiles.toArray(new StorageDataFile[dataFiles.size()])[0];
        // now lets download it!
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeHeaders().putAll(getHeaders());
        requestBuilderCustomizer.customizeHeaders().put(HttpConstants.ACCEPT,
                Lists.newArrayList(dataFile.getMimeType().toString()));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultGet(AIPController.AIP_PATH + AIPController.DOWNLOAD_AIP_FILE, requestBuilderCustomizer,
                "We should be downloading the data file", aip.getId().toString(), dataFile.getChecksum());
    }

    @Test
    public void testRetrieveAips() {
        testStore();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.customizeRequestParam()
                .param("from", OffsetDateTime.now().minusDays(40).toString())
                .param("to", OffsetDateTime.now().toString())
                .param("state", AIPState.VALID.toString())
                .param("session", SESSION)
                .param("tags", "tag");

        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.content", Matchers.not(Matchers.empty())));
        performDefaultGet(AIPController.AIP_PATH, requestBuilderCustomizer, "There should be some AIP to show");
    }

    @Test
    public void testRetrieveAllAipsTags() throws MalformedURLException {
        createSeveralAips();
        AIPQueryFilters requestParam = new AIPQueryFilters();
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(10)));
        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_SEARCH_PATH, requestParam, requestBuilderCustomizer, "There should be some tags associated to AIPS");
    }


    @Test
    public void testRetrieveAipsTagsWithFilter() throws MalformedURLException {
        List<AIP> aips = createSeveralAips();

        AIPQueryFilters requestParam = new AIPQueryFilters();
        requestParam.setFrom(OffsetDateTime.now().minusDays(40));
        requestParam.setTo(OffsetDateTime.now());
        requestParam.setState(AIPState.STORAGE_ERROR);
        requestParam.setSession(SESSION);
        // Add STORAGE_ERROR AIPs
        requestParam.getAipIds().add(aips.get(0).getId().toString());
        requestParam.getAipIds().add(aips.get(1).getId().toString());
        // Exclude others
        requestParam.getAipIdsExcluded().add(aips.get(2).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(3).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(4).getId().toString());
        // Retrieve only entities having these tags
        requestParam.getTags().add("tag2");


        // There is only 4 different tags on STORAGE_ERROR AIPs
        int nbTags = 4;
        List<String> resultingTags = Arrays.asList("tag4", "tag2", "tag1", "tag3");
        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(nbTags)));
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$", Matchers.containsInAnyOrder(resultingTags.toArray())));
        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_SEARCH_PATH, requestParam, requestBuilderCustomizer, "There should be some tags associated to AIPS");
    }


    @Test
    @Requirement("REGARDS_DSL_STO_AIP_420")
    public void testAddTagToAIPsByQuery() throws MalformedURLException {
        List<AIP> aips = createSeveralAips();

        AddAIPTagsFilters requestParam = new AddAIPTagsFilters();
        requestParam.setFrom(OffsetDateTime.now().minusDays(40));
        requestParam.setTo(OffsetDateTime.now());
        requestParam.setState(AIPState.STORED);
        requestParam.setSession(SESSION);
        // Add STORED AIPs
        requestParam.getAipIds().add(aips.get(2).getId().toString());
        requestParam.getAipIds().add(aips.get(3).getId().toString());
        // Exclude others
        requestParam.getAipIdsExcluded().add(aips.get(0).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(1).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(4).getId().toString());
        // Add these tags to AIPs
        requestParam.getTagsToAdd().add("KGB");
        requestParam.getTagsToAdd().add("Mossad");
        requestParam.getTagsToAdd().add("DGSE");
        requestParam.getTagsToAdd().add("CIA");
        requestParam.getTagsToAdd().add("MI-6");
        requestParam.getTagsToAdd().add("FSB");


        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_MANAGEMENT_PATH, requestParam, requestBuilderCustomizer, "should set tags to AIPS");
    }


    @Test
    @Requirement("REGARDS_DSL_STO_AIP_430")
    public void testDeleteTagFromAIPsByQuery() throws MalformedURLException {
        List<AIP> aips = createSeveralAips();

        AddAIPTagsFilters requestParam = new AddAIPTagsFilters();
        requestParam.setFrom(OffsetDateTime.now().minusDays(40));
        requestParam.setTo(OffsetDateTime.now());
        requestParam.setState(AIPState.STORED);
        requestParam.setSession(SESSION);
        // Add STORED AIPs
        requestParam.getAipIds().add(aips.get(2).getId().toString());
        requestParam.getAipIds().add(aips.get(3).getId().toString());
        // Exclude others
        requestParam.getAipIdsExcluded().add(aips.get(0).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(1).getId().toString());
        requestParam.getAipIdsExcluded().add(aips.get(4).getId().toString());
        // Add these tags to AIPs
        requestParam.getTagsToAdd().add("tag5353");
        requestParam.getTagsToAdd().add("tag8");
        requestParam.getTagsToAdd().add("tag5");


        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        performDefaultPost(AIPController.AIP_PATH + AIPController.TAG_MANAGEMENT_PATH, requestParam, requestBuilderCustomizer, "should set tags to AIPS");
    }

    public List<AIP> createSeveralAips() throws MalformedURLException {
        aipSessionRepo.deleteAll();
        List<AIP> newAIPs = new ArrayList();
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);


        // Create some AIP having errors
        AIP aipOnError1 = getNewAipWithTags(aipSession, "tag1", "tag2", "tag3");
        aipOnError1.setState(AIPState.STORAGE_ERROR);
        aipDao.save(aipOnError1, aipSession);
        newAIPs.add(aipOnError1);


        AIP aipOnError2 = getNewAipWithTags(aipSession, "tag4", "tag2");
        aipOnError2.setState(AIPState.STORAGE_ERROR);
        aipDao.save(aipOnError2, aipSession);
        newAIPs.add(aipOnError2);


        // Create some stored AIP
        AIP aipWaiting1 = getNewAipWithTags(aipSession, "tag5", "tag1", "tag123", "tag5353");
        aipWaiting1.setState(AIPState.STORED);
        aipDao.save(aipWaiting1, aipSession);
        newAIPs.add(aipWaiting1);

        AIP aipWaiting2 = getNewAipWithTags(aipSession, "tag7", "tag8");
        aipWaiting2.setState(AIPState.STORED);
        aipDao.save(aipWaiting2, aipSession);
        newAIPs.add(aipWaiting2);

        // Create some AIP on another session

        AIPSession aipSession2 = new AIPSession();
        aipSession2.setId("Test session 2");
        aipSession2.setLastActivationDate(OffsetDateTime.now());
        aipSession2 = aipSessionRepo.save(aipSession2);

        AIP aipDeleted11 = getNewAipWithTags(aipSession2, "tag6");
        aipDeleted11.setState(AIPState.DELETED);
        aipDao.save(aipDeleted11, aipSession2);
        newAIPs.add(aipDeleted11);
        return newAIPs;
    }
}
