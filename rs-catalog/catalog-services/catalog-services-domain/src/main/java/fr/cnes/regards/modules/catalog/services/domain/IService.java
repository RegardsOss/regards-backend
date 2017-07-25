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
package fr.cnes.regards.modules.catalog.services.domain;

import org.springframework.http.ResponseEntity;

import fr.cnes.regards.framework.modules.plugins.annotations.PluginInterface;

/**
 *
 * Plugin applying processus according to its parameters.
 *
 * @author Sylvain Vissiere-Guerinet
 */
@PluginInterface(description = "Plugin applying processus on a query")
public interface IService {

    /**
     * Apply the processus described by this instance of IService
     *
     * @return response HTTP including the content-type and the body wanted
     */
    public ResponseEntity<?> apply();

    /**
     * Can this implementation be used with only one datum's id? Should have a PluginParameter representing this datum
     * @return the answer to this question
     */
    boolean isApplyableOnOneData();

    /**
     * Can this implementation be used with a list of data's id? Should have a PluginParameter corresponding to this
     * list
     * @return the answer to this question
     */
    boolean isApplyableOnManyData();

    /**
     * Can this implementation be used with a query similar to the ones used by searches endpoint? Should have a
     * PluginParameter for the query
     * @return the answer to this question
     */
    boolean isApplyableOnQuery();

}
