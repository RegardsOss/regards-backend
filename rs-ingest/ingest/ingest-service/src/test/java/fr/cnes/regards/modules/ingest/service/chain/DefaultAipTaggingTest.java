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
package fr.cnes.regards.modules.ingest.service.chain;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.gson.Gson;

import fr.cnes.regards.framework.modules.plugins.domain.PluginParameter;
import fr.cnes.regards.framework.oais.ContentInformation;
import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.framework.oais.urn.EntityType;
import fr.cnes.regards.framework.oais.urn.UniformResourceName;
import fr.cnes.regards.framework.utils.plugins.PluginParametersFactory;
import fr.cnes.regards.framework.utils.plugins.PluginUtils;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.ingest.domain.exception.TagAIPException;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipTagging;
import fr.cnes.regards.modules.ingest.service.plugin.DefaultAIPTagging;
import fr.cnes.regards.modules.storage.domain.AIP;
import fr.cnes.regards.modules.storage.domain.AIPBuilder;

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

    private final Gson gson = new Gson();

    @Test
    public void addOnlyTags() throws TagAIPException {

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(DefaultAIPTagging.FIELD_NAME_TAGS, gson.toJson(TAGS)).getParameters();

        DefaultAIPTagging plugin = PluginUtils.getPlugin(parameters, DefaultAIPTagging.class,
                                                         Arrays.asList(MODULE_PACKAGE), null);
        Assert.assertNotNull(plugin);
        tag(plugin, TAGS, null);
    }

    @Test
    public void addOnlyLinks() throws TagAIPException {

        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(DefaultAIPTagging.FIELD_NAME_LINKS, gson.toJson(LINKS)).getParameters();

        DefaultAIPTagging plugin = PluginUtils.getPlugin(parameters, DefaultAIPTagging.class,
                                                         Arrays.asList(MODULE_PACKAGE), null);
        Assert.assertNotNull(plugin);
        tag(plugin, null, LINKS);
    }

    @Test
    public void addTagsAndLinks() throws TagAIPException {
        List<PluginParameter> parameters = PluginParametersFactory.build()
                .addParameter(DefaultAIPTagging.FIELD_NAME_TAGS, gson.toJson(TAGS))
                .addParameter(DefaultAIPTagging.FIELD_NAME_LINKS, gson.toJson(LINKS)).getParameters();

        DefaultAIPTagging plugin = PluginUtils.getPlugin(parameters, DefaultAIPTagging.class,
                                                         Arrays.asList(MODULE_PACKAGE), null);
        Assert.assertNotNull(plugin);
        tag(plugin, TAGS, LINKS);
    }

    @Test(expected = PluginUtilsRuntimeException.class)
    public void addNothing() throws TagAIPException {
        List<PluginParameter> parameters = PluginParametersFactory.build().getParameters();

        DefaultAIPTagging plugin = PluginUtils.getPlugin(parameters, DefaultAIPTagging.class,
                                                         Arrays.asList(MODULE_PACKAGE), null);
        Assert.assertNotNull(plugin);
        tag(plugin, null, null);
    }

    private void tag(IAipTagging plugin, List<String> tags, Map<String, String> links) throws TagAIPException {
        String urn = "URN:AIP:DATA:PROJECT:00000011-0022-0033-0044-000000000055:V1";
        String sipId = "sipId1";
        String filename = "test.netcdf";
        String md5 = "plifplafplouf";
        AIPBuilder builder = new AIPBuilder(UniformResourceName.fromString(urn), sipId, EntityType.DATA);
        builder.getContentInformationBuilder().setDataObject(DataType.RAWDATA, Paths.get("target", filename), md5);
        builder.addContentInformation();
        AIP single = builder.build();

        plugin.tag(Arrays.asList(single));

        Assert.assertEquals(urn, single.getId().toString());
        Assert.assertEquals(sipId, single.getSipId());
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
