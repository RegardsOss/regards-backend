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


package fr.cnes.regards.modules.ingest.service;

import com.google.gson.Gson;
import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceIT;
import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Test deserialization of a json file for a sip
 *
 * @author Iliana Ghazali
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=sip_deserialization_it",
                                   "eureka.client.enabled=false" },
                    locations = { "classpath:application-test.properties" })
public class SIPDeserializationIT extends AbstractMultitenantServiceIT {

    @Autowired
    private Gson gson;

    @Test
    @Purpose(
        "Test generation of a SIP from a file with the deserialization 'hack' used in InformationPackageMapTypeAdapter")
    public void testDeserializationHack() throws IOException {
        String filename = "sip_testDeserialization.json";
        try (Reader json = new InputStreamReader(this.getClass().getResourceAsStream(filename),
                                                 StandardCharsets.UTF_8)) {
            SIP sip = gson.fromJson(json, SIP.class);

            // number from descriptiveInformation
            // should be returned as Long if it does not contains "."
            Map<String, Object> cycle = (Map<String, Object>) sip.getProperties()
                                                                 .getDescriptiveInformation()
                                                                 .get("cycle_range");
            Assert.assertEquals(Long.class, cycle.get("min_cycle").getClass());
            Assert.assertEquals(Long.class, cycle.get("max_cycle").getClass());
            // should be returned as Double if it contains "."
            Map<String, Object> coordinates = (Map<String, Object>) sip.getProperties()
                                                                       .getDescriptiveInformation()
                                                                       .get("coordinates");
            Assert.assertEquals(Double.class, coordinates.get("min_coordinates").getClass());
            Assert.assertEquals(Double.class, coordinates.get("max_coordinates").getClass());

        }
    }
}
