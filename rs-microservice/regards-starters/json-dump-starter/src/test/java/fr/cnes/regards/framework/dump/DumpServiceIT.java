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


package fr.cnes.regards.framework.dump;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 *
 * @author Iliana Ghazali
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ComponentScan(basePackages = { "fr.cnes.regards.framework" })
@EnableAutoConfiguration
@TestPropertySource(properties = {"spring.application.name=test", "ppté=15"})
public class DumpServiceIT {

    @Autowired
    private DumpService dumpService;

    @Test
    public void testGenerateJsonZips() throws IOException {
        //create test data
        int numOfJson = 1000000;
        ArrayList<ObjectDump> jsonEntities = TestData.buildJsonCollection(numOfJson);

        //create zip files
        dumpService.generateJsonZips(jsonEntities, "target/dump");

        // todo assert
        Assert.assertTrue("target dir should exists", Files.exists(Paths.get("target", "dump")));


        //TODO nettoyage

    }

}
