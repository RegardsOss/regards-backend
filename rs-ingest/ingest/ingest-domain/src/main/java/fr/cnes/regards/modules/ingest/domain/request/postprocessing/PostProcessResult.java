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
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 *
 * @author Iliana Ghazali
 */

public class PostProcessResult {

    private final Map<String, Set<String>> errors = Maps.newHashMap();

    private final Set<String> successes = Sets.newHashSet();

    private boolean interupted = false;

    /**
     * Build postprocess result of sipad post processing plugin
     * @param errors map of aipId and related errors encountered during process
     */
    public void buildErrors(Map<String, Set<String>> errors) {
        this.errors.putAll(errors);
    }

    public void addError(String aip, Set<String> errorMessages) {
        this.errors.put(aip, errorMessages);
    }

    public Map<String, Set<String>> getErrors() {
        return errors;
    }

    public void addSuccess(String aipId) {
        this.successes.add(aipId);
    }

    public Set<String> getSuccesses() {
        return successes;
    }

    public boolean isInterrupted() {
        return this.interupted;
    }

    public void setInterrupted() {
        this.interupted = true;
    }
}
