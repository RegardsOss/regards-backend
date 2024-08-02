/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import fr.cnes.regards.framework.modules.plugins.annotations.Plugin;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;
import fr.cnes.regards.modules.ingest.domain.plugin.ISipPostprocessing;
import fr.cnes.regards.modules.ingest.domain.request.postprocessing.PostProcessResult;

import java.util.Collection;

/**
 * @author Iliana Ghazali
 */

@Plugin(id = "PostProcessFailTestPlugin",
        version = "1.0.0-SNAPSHOT",
        description = "Test plugin",
        author = "REGARDS Team",
        contact = "regards@c-s.fr",
        license = "GPLv3",
        owner = "CSSI",
        url = "https://github.com/RegardsOss")
public class AIPPostProcessFailTestPlugin implements ISipPostprocessing {

    @Override
    public PostProcessResult postprocess(Collection<AIPEntity> aipEntities) {
        PostProcessResult ppr = new PostProcessResult();
        ppr.buildErrors(Maps.newHashMap());
        boolean setError = false;
        for (AIPEntity aip : aipEntities) {
            if (setError) {
                ppr.addSuccess(aip.getAipId());
                setError = false;
            } else {
                ppr.addError(aip.getAipId(), Sets.newHashSet("Simulated test error !!!!"));
                setError = true;
            }
        }
        return ppr;
    }
}
