/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractMultitenantServiceTest;
import fr.cnes.regards.framework.jpa.utils.RegardsTransactional;
import fr.cnes.regards.framework.module.rest.exception.ModuleException;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.PluginMetaData;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.modules.plugins.service.IPluginService;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.acquisition.domain.AcquisitionFile;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionFileInfo;
import fr.cnes.regards.modules.ingest.dto.sip.SIP;

@TestPropertySource(properties = { "spring.jpa.properties.hibernate.default_schema=acq_service" })
@RegardsTransactional
public class FixedStorageSIPGenerationIT extends AbstractMultitenantServiceTest {

    @Autowired
    private IPluginService pluginService;

    @Test
    public void testPlugin() throws ModuleException, NotAvailablePluginConfigurationException {

        // Init plugin conf

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(FixedStorageSIPGeneration.STAF_STORAGE_NODE, "node1"),
                     IPluginParam.build(FixedStorageSIPGeneration.DATASET, "dataset1"));
        PluginConfiguration pluginConf = pluginService
                .savePluginConfiguration(new PluginConfiguration("sipGenerationPlugin", parameters, FixedStorageSIPGeneration.class.getAnnotation(Plugin.class).id()));

        FixedStorageSIPGeneration pluginImp = pluginService.getPlugin(pluginConf.getBusinessId());

        Product product = new Product();
        product.setProductName("testProduct");
        AcquisitionFile acqFile = new AcquisitionFile();
        AcquisitionFileInfo info = new AcquisitionFileInfo();
        info.setDataType(DataType.RAWDATA);
        info.setMimeType(MimeType.valueOf("application/octet-stream"));
        acqFile.setFileInfo(info);
        acqFile.setFilePath(Paths.get("/tmp/file.txt"));
        product.addAcquisitionFile(acqFile);
        SIP sip = pluginImp.generate(product);
        Assert.assertNotNull(sip);
        Map<String, Object> infos = sip.getProperties().getDescriptiveInformation();
        Assert.assertNotNull(infos);
        Assert.assertEquals("node1", infos.get(FixedStorageSIPGeneration.SIP_STAF_STORAGE_NODE_KEY));
        Assert.assertEquals("dataset1", infos.get(FixedStorageSIPGeneration.SIP_DATASET_KEY));
    }

}
