/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.jpa;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.jpa.exception.MultiDataBasesException;
import fr.cnes.regards.framework.jpa.utils.DaoUtils;
import fr.cnes.regards.framework.jpa.utils.DataSourceHelper;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 *
 * Class JpaUtilsTest
 *
 * Test JPA common utils
 *
 * @author CS
 * @since 1.0-SNAPSHOT
 */
public class JpaUtilsTest {

    /**
     * Class logger
     */
    private final static Logger LOG = LoggerFactory.getLogger(JpaUtilsTest.class);

    /**
     *
     * Check for classapth validity when there is instance database entities and multitenant database entities in the
     * same classpath
     *
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for classpath validity when there is instance database entities and multitenant database entities in the same classpath with a Repository class")
    @Test
    public void test() {

        final String errorMessage = "There should be an exception the classpath is invalid";
        try {
            DaoUtils.checkClassPath("fr.cnes.regards.modules.jpa.test.invalid");
            Assert.fail(errorMessage);
        } catch (final MultiDataBasesException e) {
            LOG.info(e.getMessage());
        }

        try {
            DaoUtils.checkClassPath("fr.cnes.regards.modules.jpa.test.invalid2");
            Assert.fail(errorMessage);
        } catch (final MultiDataBasesException e) {
            LOG.info(e.getMessage());
        }

        try {
            DaoUtils.checkClassPath("fr.cnes.regards.modules.jpa.test.invalid3");
            Assert.fail(errorMessage);
        } catch (final MultiDataBasesException e) {
            LOG.info(e.getMessage());
        }

        try {
            DaoUtils.checkClassPath("fr.cnes.regards.modules.jpa.test.valid");
        } catch (final MultiDataBasesException e) {
            LOG.info(e.getMessage());
            Assert.fail(e.getMessage());
        }
    }

    /**
     *
     * Check for embedded HSQLDB database creation
     *
     * @throws IOException
     *             Connection error.
     * @throws SQLException
     *             Creation error.
     * @since 1.0-SNAPSHOT
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for embedded HSQLDB database creation")
    @Test
    public void embeddedDataSourceTest() throws IOException, SQLException {
        final String path = "target/embedded";
        final DataSource datasource = DataSourceHelper.createEmbeddedDataSource("test", path);
        datasource.getConnection().close();
        // Check for database created
        Assert.assertTrue("Error creating embedded database.", Files.exists(Paths.get(path)));
    }

}
