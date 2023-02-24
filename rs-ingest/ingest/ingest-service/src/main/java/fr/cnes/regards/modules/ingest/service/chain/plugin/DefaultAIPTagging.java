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
package fr.cnes.regards.modules.ingest.service.chain.plugin;

import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginInit;
import fr.cnes.regards.framework.modules.plugins.annotations.PluginParameter;
import fr.cnes.regards.framework.utils.plugins.PluginUtilsRuntimeException;
import fr.cnes.regards.modules.ingest.domain.exception.TagAIPException;
import fr.cnes.regards.modules.ingest.domain.plugin.IAipTagging;
import fr.cnes.regards.modules.ingest.dto.aip.AIP;

import java.util.List;
import java.util.Map;

/**
 * Default AIP tagging plugin that can manage either tags or links or both.<br/>
 * Tags are basic {@link String}.<br/>
 * Links are key value pairs of {@link String}<br/>
 * <br/>
 * Sample tags:
 * <ul>
 * <li>FRANCE</li>
 * <li>SPATIAL</li>
 * </ul>
 * <p>
 * Sample links:
 * <ul>
 * <li>CNES -> http://www.cnes.fr</li>
 * <li>FITS -> https://www.iana.org/assignments/media-types/application/fits</li>
 * </ul>
 *
 * @author Marc Sordi
 */
@Plugin(author = "REGARDS Team",
        description = "Default plugin for AIP tagging",
        id = "DefaultAIPTagging",
        version = "1.0.0",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CNES",
        url = "https://regardsoss.github.io/")
public class DefaultAIPTagging implements IAipTagging {

    public static final String FIELD_NAME_TAGS = "tags"; // Useful for testing

    public static final String FIELD_NAME_LINKS = "links"; // Useful for testing

    @PluginParameter(label = "Tags",
                     description = "List of tags useful to classify AIP for business purpose and harvesting",
                     optional = true)
    private List<String> tags;

    @PluginParameter(keylabel = "Link tag", label = "Link", description = "List of links", optional = true)
    private Map<String, String> links;

    @PluginInit
    public void init() {
        // At least, one tag or link is required
        if ((tags == null || tags.isEmpty()) && (links == null || links.isEmpty())) {
            throw new PluginUtilsRuntimeException(String.format("Tags or links is required in default tag plugin : %s",
                                                                this.getClass().getName()));
        }
    }

    @Override
    public void tag(List<AIP> aips) throws TagAIPException {
        if (aips != null) {
            for (AIP aip : aips) {
                addTags(aip, tags);
                addLinks(aip, links);
            }
        }
    }

    private void addTags(AIP aip, List<String> tags) {
        if (tags != null && !tags.isEmpty()) {
            aip.withContextTags(tags.toArray(new String[tags.size()]));
        }
    }

    private void addLinks(AIP aip, Map<String, String> links) {
        if (links != null) {
            links.forEach((k, v) -> aip.withContextInformation(k, v));
        }
    }
}
