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
package fr.cnes.regards.modules.search.rest.download;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.modules.indexer.dao.IEsRepository;

/**
 * @author Kevin Marchois
 */
@TestPropertySource(locations = { "classpath:test.properties" },
        properties = { "regards.tenant=opensearch", "spring.jpa.properties.hibernate.default_schema=opensearch" })
public class CatalogDownloadControllerIT extends AbstractRegardsTransactionalIT {

    private static final String AIP_ID_FAIL = "URN:AIP:DATA:projectFail:b4cf92ae-d2dd-3ec4-9a5e-7bd7ff1f4234:V1";

    public static final String AIP_ID_OK = "URN:AIP:DATA:project1:b4cf92ae-d2dd-3ec4-9a5e-7bd7ff1f4234:V1";

    private static final Logger LOG = LoggerFactory.getLogger(CatalogDownloadControllerIT.class);

    public static final String DOWNLOAD_AIP_FILE = "/downloads/{aip_id}/files/{checksum}";

    @Autowired
    protected IEsRepository esRepository;

    @Before
    public void prepareData() throws ModuleException, InterruptedException {
        initIndex(getDefaultTenant());
    }

    protected void initIndex(String index) {
        if (esRepository.indexExists(index)) {
            esRepository.deleteIndex(index);
        }
        esRepository.createIndex(index);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Test()
    public void restDownloadFileOk() {
        performDefaultGet(DOWNLOAD_AIP_FILE, customizer().expectStatusOk(), "Error message", AIP_ID_OK, "checksumOk");
    }

    @Test()
    public void restDownloadFileNotFound() {
        performDefaultGet(DOWNLOAD_AIP_FILE, customizer().expectStatusNotFound(), "Error message", AIP_ID_OK,
                          "checksumKo");
    }

    @Test()
    public void restDownloadFileForbidden() {
        performDefaultGet(DOWNLOAD_AIP_FILE, customizer().expectStatusForbidden(), "Error message", AIP_ID_FAIL,
                          "checksumKo");
    }
}
