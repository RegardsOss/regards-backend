/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.jpa.json.test;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.jpa.json.test.domain.ITestEntityRepository;
import fr.cnes.regards.framework.jpa.json.test.domain.JsonbEntity;
import fr.cnes.regards.framework.jpa.json.test.domain.TestEntity;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * @author Sylvain Vissiere-Guerinet
 *
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { JsonbTestConfiguration.class })
@Ignore
public class JsonbTest {

    /**
     * Logger of this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonbTest.class);

    /**
     * bean provided by spring boot starter data jpa
     */
    @Autowired
    private ITestEntityRepository testEntityRepository;

    @Requirement("REGARDS_DSL_DAM_MOD_060")
    @Purpose("Test ability to persist and retrieve structureless data stored as jsonb into postgres")
    @Test
    public void testPersist() {
        final TestEntity te = new TestEntity(new JsonbEntity("name", "content"));
        final TestEntity fromSave = testEntityRepository.save(te);
        final TestEntity fromDB = testEntityRepository.findOne(fromSave.getId());
        Assert.assertEquals(te, fromSave);
        Assert.assertEquals(te, fromDB);
    }

}
