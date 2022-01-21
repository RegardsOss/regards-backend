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
package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Response for working subsets preparation before request handled.
 *
 * @author Sébastien
 *
 */
public class PreparationResponse<W, R> {

    /**
     * List of subsets successfully generated
     */
    private final Collection<W> workingSubsets = Sets.newHashSet();

    /**
     * Request that are not handled by preparation process
     */
    private final Map<R, String> preparationErrors = Maps.newHashMap();

    public static <W, R> PreparationResponse<W, R> build(Collection<W> workingSubsets, Map<R, String> errors) {
        PreparationResponse<W, R> response = new PreparationResponse<>();
        response.workingSubsets.addAll(workingSubsets);
        response.preparationErrors.putAll(errors);
        return response;
    }

    /**
     * Add a generated subset
     * @param subset
     */
    public void addPreparedSubset(W subset) {
        workingSubsets.add(subset);
    }

    /**
     * Add a request to the list of not handled requests.
     * @param request
     * @param errorCause
     */
    public void addPreparationError(R request, String errorCause) {
        preparationErrors.put(request, errorCause);
    }

    public Collection<W> getWorkingSubsets() {
        return workingSubsets;
    }

    public Map<R, String> getPreparationErrors() {
        return preparationErrors;
    }

}
