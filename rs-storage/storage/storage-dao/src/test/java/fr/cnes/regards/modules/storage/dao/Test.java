/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.dao;

import java.net.MalformedURLException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTransactionalTest;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AipType;
import fr.cnes.regards.modules.storage.domain.DataObject;

/**
 * @author Sylvain Vissiere-Guerinet
 *
 */
@TestPropertySource("classpath:dao-storage.properties")
public class Test extends AbstractDaoTransactionalTest {

    @Autowired
    private AIPRepository repository;

    @Autowired
    private DataObjectRepository dataObjectRepo;

    @org.junit.Test
    @Commit
    public void test() throws NoSuchAlgorithmException, MalformedURLException {
        AIP aip = new AIP(AipType.COLLECTION).generateAIP();
        List<DataObject> dataObjects = aip.getDataObjects();
        dataObjectRepo.save(dataObjects);
        repository.save(aip);
    }

}
