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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.eo.impl;

import com.google.gson.Gson;
import com.rometools.rome.feed.CopyFrom;
import com.rometools.rome.feed.module.ModuleImpl;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.extension.eo.EarthObservationAttribute;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.eo.EOModule;

import java.io.Serializable;
import java.util.Map;

/**
 * Module to handle Earth Observation opensearch parameters into ATOM format responses.
 *
 * @author Léo Mieulet
 * @see <a href="https://docs.opengeospatial.org/is/13-026r9/13-026r9.html"> Annex D (informative): Metadata Mappings</a>
 */
public class EOModuleImpl extends ModuleImpl implements EOModule, Serializable {

    private static final long serialVersionUID = 1L;

    private Map<EarthObservationAttribute, Object> activeProperties;

    private Gson gson;

    public EOModuleImpl() {
        super(EOModuleImpl.class, EOModuleImpl.URI);
    }

    @Override
    public Class<? extends CopyFrom> getInterface() {
        return EOModule.class;
    }

    @Override
    public void copyFrom(CopyFrom obj) {
        EOModuleImpl mod = (EOModuleImpl) obj;
        mod.setActiveProperties(this.getActiveProperties());
        mod.setGsonBuilder(this.getGsonBuilder());
    }

    @Override
    public Gson getGsonBuilder() {
        return this.gson;
    }

    @Override
    public void setGsonBuilder(Gson gson) {
        this.gson = gson;
    }

    @Override
    public Map<EarthObservationAttribute, Object> getActiveProperties() {
        return activeProperties;
    }

    @Override
    public void setActiveProperties(Map<EarthObservationAttribute, Object> activeProperties) {
        this.activeProperties = activeProperties;
    }

}
