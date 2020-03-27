/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.modules.jpa.instance.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.pojo.TestProject;
import fr.cnes.regards.framework.modules.jpa.instance.autoconfigure.repository.IProjectTestRepository;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.framework.test.report.annotation.Requirement;

/**
 * Class MultiTenancyDaoTest
 *
 * Unit tests for multitenancy DAO
 * @author CS
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { InstanceDaoTestConfiguration.class })
@DirtiesContext
public class InstanceDaoTest {

    /**
     * JPA Project repository
     */
    @Autowired
    private IProjectTestRepository projectRepository;

    /**
     * Unit test to check that the spring JPA multitenancy context is loaded successfully
     *
     * S
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check that the spring JPA multitenancy context is loaded successfully")
    @Test
    public void contextLoads() {
        // Nothing to do. Only tests if the spring context is ok.
    }

    /**
     * Unit test to check JPA uses the good tenant through the tenant resolver
     */
    @Requirement("REGARDS_DSL_SYS_ARC_050")
    @Purpose("Unit test to check JPA uses the good tenant through the tenant resolver")
    @Test
    public void multitenancyAccessTest() {

        final List<TestProject> resultsP = new ArrayList<>();

        // Delete all previous data if any
        projectRepository.deleteAll();

        // Add a new Project
        final TestProject newProject = new TestProject();
        newProject.setName("Project 1");
        projectRepository.save(newProject);

        // Check results
        final Iterable<TestProject> listP = projectRepository.findAll();
        listP.forEach(resultsP::add);
        Assert.assertEquals(String.format("Error, there must be 1 elements in database associated to instance (%d)",
                                          resultsP.size()), 1, resultsP.size());

    }

}
