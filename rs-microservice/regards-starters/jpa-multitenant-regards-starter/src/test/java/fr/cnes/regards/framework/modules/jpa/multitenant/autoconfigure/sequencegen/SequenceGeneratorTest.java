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
package fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.sequencegen;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import fr.cnes.regards.framework.modules.jpa.multitenant.autoconfigure.sequencegen.SequenceGeneratorTest.SequenceGeneratorTestConfiguration;

/**
 * @author Marc Sordi
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { SequenceGeneratorTestConfiguration.class })
@ActiveProfiles("dev")
@TestPropertySource("/seqgen.properties")
public class SequenceGeneratorTest {

    @Test
    public void contextLoad() {
        // Nothing to do
    }

    @Configuration
    @ComponentScan(basePackages = { "fr.cnes.regards.framework.jpa.multitenant.autoconfigure.sequencegen" })
    @EnableAutoConfiguration
    public static class SequenceGeneratorTestConfiguration {

    }
}
