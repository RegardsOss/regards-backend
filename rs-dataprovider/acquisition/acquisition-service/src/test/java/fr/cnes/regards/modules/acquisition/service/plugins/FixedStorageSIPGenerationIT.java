/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.service.plugins;

import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Maps;
import com.google.gson.Gson;

import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.test.integration.AbstractRegardsTransactionalIT;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.ingest.domain.SIP;

@TestPropertySource(locations = { "classpath:test.properties" })
@RegardsTransactional
public class FixedStorageSIPGenerationIT extends AbstractRegardsTransactionalIT {

    @Autowired
    private IPluginService pluginService;

    @Autowired
    private Gson gson;

    @Before
    public void init() throws ModuleException, MalformedURLException {
        pluginService.addPluginPackage(FixedStorageSIPGeneration.class.getPackage().getName());
    }

    @Test
    public void testPlugin() throws ModuleException {

        // Init plugin conf
        PluginMetaData plugin = PluginUtils
                .createPluginMetaData(FixedStorageSIPGeneration.class,
                                      FixedStorageSIPGeneration.class.getPackage().getName(),
                                      FixedStorageSIPGeneration.class.getPackage().getName());

        Map<String, String> mapping = Maps.newHashMap();
        mapping.put("plugin1", "dir1");
        mapping.put("plugin2", "dir2");
        mapping.put("plugin3", "dir3");
        mapping.put("plugin4", "");
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(FixedStorageSIPGeneration.PLUGINID_STORAGEDIR_MAP, mapping).getParameters();
        PluginConfiguration pluginConf = pluginService
                .savePluginConfiguration(new PluginConfiguration(plugin, "sipGenerationPlugin", parameters));

        FixedStorageSIPGeneration pluginImp = pluginService.getPlugin(pluginConf.getId());

        Product product = new Product();
        product.setProductName("testProduct");
        AcquisitionFile acqFile = new AcquisitionFile();
        AcquisitionFileInfo info = new AcquisitionFileInfo();
        info.setDataType(DataType.RAWDATA);
        info.setMimeType("octet-stream");
        acqFile.setFileInfo(info);
        acqFile.setChecksum("checksum");
        acqFile.setChecksumAlgorithm("MD5");
        acqFile.setFilePath(Paths.get("/tmp/file.txt"));
        product.addAcquisitionFile(acqFile);
        SIP sip = pluginImp.generate(product);
        Assert.assertNotNull(sip);
        @SuppressWarnings("unchecked")
        List<MiscStorageInformation> miscStorage = (List<MiscStorageInformation>) sip.getProperties()
                .getMiscInformation().get(FixedStorageSIPGeneration.SIP_MISC_STORAGE_KEY);
        Assert.assertNotNull(miscStorage);
        Assert.assertEquals(miscStorage.size(), mapping.size());
    }

}
