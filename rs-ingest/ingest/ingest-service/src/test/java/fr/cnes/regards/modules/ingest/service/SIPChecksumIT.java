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
import fr.cnes.regards.modules.ingest.dto.sip.SIP;
import fr.cnes.regards.modules.ingest.service.sip.ISIPService;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.security.NoSuchAlgorithmException;

/**
 * Test checksum computation
 *
 * @author Marc SORDI
 */
@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=checksum",
                                   "eureka.client.enabled=false" },
                    locations = { "classpath:application-test.properties" })
public class SIPChecksumIT extends AbstractMultitenantServiceIT {

    @Autowired
    private ISIPService sipService;

    @Autowired
    private Gson gson;

    @Test
    public void test() throws IOException, NoSuchAlgorithmException {
        String checksum1 = getChecksum("SIPV1.json");
        String checksum2 = getChecksum("SIPV2.json");
        Assert.assertEquals(checksum1, checksum2);
    }

    private String getChecksum(String filename) throws NoSuchAlgorithmException, IOException {
        String checksum;
        try (Reader json = new InputStreamReader(this.getClass().getResourceAsStream(filename),
                                                 Charset.forName("UTF-8"))) {
            SIP sip = gson.fromJson(json, SIP.class);
            checksum = sipService.calculateChecksum(sip);
        }
        return checksum;
    }
}
