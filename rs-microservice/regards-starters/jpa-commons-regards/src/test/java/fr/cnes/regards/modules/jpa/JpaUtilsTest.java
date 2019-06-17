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
package fr.cnes.regards.modules.jpa;

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
 * Class JpaUtilsTest
 *
 * Test JPA common utils
 * @author CS
 */
public class JpaUtilsTest {

    /**
     * Class logger
     */
    private final static Logger LOG = LoggerFactory.getLogger(JpaUtilsTest.class);

    /**
     * Check for classapth validity when there is instance database entities and multitenant database entities in the
     * same classpath
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
     * Check for embedded HSQLDB database creation
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Check for embedded HSQLDB database creation")
    @Test
    public void embeddedDataSourceTest() throws SQLException {
        final String path = "target/embedded";
        final DataSource datasource = DataSourceHelper.createEmbeddedDataSource("test", path);
        datasource.getConnection().close();
        // Check for database created
        Assert.assertTrue("Error creating embedded database.", Files.exists(Paths.get(path)));
    }

}
