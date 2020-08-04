/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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


package fr.cnes.regards.modules.ingest.domain.request.postprocessing;

import java.util.Map;

import com.google.common.collect.Maps;
import fr.cnes.regards.modules.ingest.domain.aip.AIPEntity;

/**
 *
 * @author Iliana Ghazali
 */

public class PostProcessResult {

    private Map<AIPEntity, String> errors = Maps.newHashMap();

    public void build(Map<AIPEntity, String> errors) {
        PostProcessResult p = new PostProcessResult();
        p.errors.putAll(errors);
    }

    public void addError(AIPEntity aip, String errorMessage) {
        this.errors.put(aip, errorMessage);
    }

    public Map<AIPEntity, String> getErrors() {
        return errors;
    }
}
