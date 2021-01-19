/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ingest.service.plugin;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import fr.cnes.regards.framework.modules.plugins.domain.PluginConfiguration;
import fr.cnes.regards.framework.modules.plugins.domain.parameter.IPluginParam;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.urn.OaisUniformResourceName;
import fr.cnes.regards.framework.urn.DataType;
import fr.cnes.regards.framework.urn.EntityType;
import fr.cnes.regards.framework.utils.plugins.PluginParameterTransformer;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.framework.utils.plugins.exception.NotAvailablePluginConfigurationException;
import fr.cnes.regards.modules.ingest.domain.exception.TagAIPException;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipTagging;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;
import fr.cnes.regards.modules.ingest.service.chain.plugin.DefaultAIPTagging;

/**
 * Test {@link DefaultAIPTagging} plugin
 *
 * @author Marc Sordi
 *
 */
public class DefaultAipTaggingTest {

    private static final String MODULE_PACKAGE = "fr.cnes.regards.modules.ingest";

    private static final List<String> TAGS = Arrays.asList("FRANCE", "JAPON", "MALAYSIE");

    private static final Map<String, String> LINKS = createLinks();

    private static Map<String, String> createLinks() {
        Map<String, String> links = new HashMap<>();
        links.put("CNES", "http://www.cnes.fr");
        links.put("FITS", "https://www.iana.org/assignments/media-types/application/fits");
        return links;
    }

    @Test
    public void addOnlyTags() throws TagAIPException, NotAvailablePluginConfigurationException {

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(DefaultAIPTagging.FIELD_NAME_TAGS, PluginParameterTransformer.toJson(TAGS)));

        DefaultAIPTagging plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(DefaultAIPTagging.class, null, parameters), null);
        Assert.assertNotNull(plugin);
        tag(plugin, TAGS, null);
    }

    @Test
    public void addOnlyLinks() throws TagAIPException, NotAvailablePluginConfigurationException {

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(DefaultAIPTagging.FIELD_NAME_LINKS, PluginParameterTransformer.toJson(LINKS)));

        DefaultAIPTagging plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(DefaultAIPTagging.class, null, parameters), null);
        Assert.assertNotNull(plugin);
        tag(plugin, null, LINKS);
    }

    @Test
    public void addTagsAndLinks() throws TagAIPException, NotAvailablePluginConfigurationException {

        Set<IPluginParam> parameters = IPluginParam
                .set(IPluginParam.build(DefaultAIPTagging.FIELD_NAME_TAGS, PluginParameterTransformer.toJson(TAGS)),
                     IPluginParam.build(DefaultAIPTagging.FIELD_NAME_LINKS, PluginParameterTransformer.toJson(LINKS)));
        DefaultAIPTagging plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(DefaultAIPTagging.class, null, parameters), null);
        Assert.assertNotNull(plugin);
        tag(plugin, TAGS, LINKS);
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void addNothing() throws TagAIPException, NotAvailablePluginConfigurationException {

        Set<IPluginParam> parameters = IPluginParam.set();
        PluginUtils.setup(MODULE_PACKAGE);
        DefaultAIPTagging plugin = PluginUtils
                .getPlugin(PluginConfiguration.build(DefaultAIPTagging.class, null, parameters), null);
        Assert.assertNotNull(plugin);
        tag(plugin, null, null);
    }

    private void tag(IAipTagging plugin, List<String> tags, Map<String, String> links) throws TagAIPException {
        String aipUrn = "URN:AIP:DATA:PROJECT:00000011-0022-0033-0044-000000000055:V1";
        String sipUrn = "URN:SIP:DATA:PROJECT:00000011-0022-0033-0044-000000000055:V1";
        String providerId = "providerId1";
        String filename = "test.netcdf";
        String md5 = "plifplafplouf";

        AIP single = AIP.build(EntityType.DATA, OaisUniformResourceName.fromString(aipUrn),
                               Optional.of(OaisUniformResourceName.fromString(sipUrn)), providerId, 1);
        single.withDataObject(DataType.RAWDATA, Paths.get("target", filename), md5);
        single.registerContentInformation();

        plugin.tag(Arrays.asList(single));

        Assert.assertEquals(aipUrn, single.getId().toString());
        Assert.assertEquals(providerId, single.getProviderId());
        ContentInformation ci = single.getProperties().getContentInformations().iterator().next();
        Assert.assertEquals(filename, ci.getDataObject().getFilename());
        Assert.assertEquals(md5, ci.getDataObject().getChecksum());

        // Check tags
        if (tags != null) {
            Collection<String> aipTags = single.getProperties().getPdi().getTags();
            for (String tag : tags) {
                Assert.assertTrue(aipTags.contains(tag));
            }
        } else {
            Assert.assertTrue(single.getProperties().getPdi().getTags().isEmpty());
        }

        // Check links
        if (links != null) {
            Map<String, Object> contextInfo = single.getProperties().getPdi().getContextInformation();
            for (Map.Entry<String, String> link : links.entrySet()) {
                Assert.assertTrue(contextInfo.containsKey(link.getKey()));
                Assert.assertEquals(link.getValue(), contextInfo.get(link.getKey()));
            }
        }
    }

}
