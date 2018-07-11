package fr.cnes.regards.modules.storage.rest;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.oais.OAISDataObject;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.RequestBuilderCustomizer;
import fr.cnes.regards.modules.storage.dao.IDataFileDao;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPState;
import fr.cnes.regards.modules.storage.domain.database.AIPSession;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.service.IAIPService;
import java.net.MalformedURLException;
import java.time.OffsetDateTime;
import java.util.HashSet;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.util.MimeType;

public class StorageDataFileControllerIT extends AbstractAIPControllerIT {

    private final String SESSION_NAME = "Test session";

    @Autowired
    private IAIPService aipService;

    @Autowired
    private IDataFileDao dao;

    @Test
    public void testRetrieveSessions() throws MalformedURLException, ModuleException {
        aipSessionRepo.deleteAll();
        AIPSession aipSession = new AIPSession();
        aipSession.setId(SESSION_NAME);
        aipSession.setLastActivationDate(OffsetDateTime.now());
        aipSession = aipSessionRepo.save(aipSession);
        // Create some waiting AIP
        AIP aipWaiting1= getNewAip(aipSession);
        aipWaiting1.setState(AIPState.STORING_METADATA);
        aipWaiting1 = aipDao.save(aipWaiting1, aipSession);
        OAISDataObject file = new OAISDataObject();
        file.setChecksum("dfgkdfjgdfg");
        String filename = "hello.jpg";
        file.setFilename(filename);
        file.setFileSize(123456L);
        file.setRegardsDataType(DataType.RAWDATA);
        file.setAlgorithm("toto");
        MimeType mimetype = new MimeType("png");
        StorageDataFile ds = new StorageDataFile(file, mimetype, aipWaiting1, aipSession);
        ds.setUrls(new HashSet<>());
        dao.save(ds);

        RequestBuilderCustomizer requestBuilderCustomizer = getNewRequestBuilderCustomizer();
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.status().isOk());
        requestBuilderCustomizer.addExpectation(MockMvcResultMatchers.jsonPath("$", Matchers.hasSize(1)));

        requestBuilderCustomizer
                .addExpectation(MockMvcResultMatchers.jsonPath("$.[0].content.name", Matchers.is(filename)));

        performDefaultGet(StorageDataFileController.TYPE_MAPPING + StorageDataFileController.AIP_PATH, requestBuilderCustomizer,
                "we should have the list of session", aipWaiting1.getId().toString());
    }

}
