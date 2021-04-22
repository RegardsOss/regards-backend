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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.gml.impl;

import java.io.Serializable;
import java.time.OffsetDateTime;

import com.google.gson.Gson;
import com.rometools.modules.georss.GeoRSSModule;
import com.rometools.rome.feed.CopyFrom;

import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.gml.GmlTimeModule;

/**
 * Module to handle TIME & GEO opensearch parameters into ATOM format responses.
 * com.rometools.rome module implementation to handle specifics GML Time&Geo attributes.
 * @see <a href="https://rometools.github.io/rome/RssAndAtOMUtilitiEsROMEV0.5AndAboveTutorialsAndArticles/RssAndAtOMUtilitiEsROMEPluginsMechanism.html">rometools.github.io</a>
 * @author Sébastien Binda
 */
public class GmlTimeModuleImpl extends GeoRSSModule implements GmlTimeModule, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Geo time start
     */
    private OffsetDateTime startDate;

    /**
     * Geo time stop
     */
    private OffsetDateTime stopDate;

    private Gson gson;

    public GmlTimeModuleImpl() {
        super(GmlTimeModuleImpl.class, GmlTimeModuleImpl.URI);
    }

    @Override
    public Class<? extends CopyFrom> getInterface() {
        return GmlTimeModule.class;
    }

    @Override
    public void copyFrom(CopyFrom obj) {
        GmlTimeModuleImpl mod = (GmlTimeModuleImpl) obj;
        mod.setStartDate(this.getStartDate());
        mod.setStopDate(this.getStopDate());
        mod.setGsonBuilder(this.getGsonBuilder());
    }

    @Override
    public OffsetDateTime getStartDate() {
        return this.startDate;
    }

    @Override
    public void setStartDate(OffsetDateTime startDate) {
        this.startDate = startDate;

    }

    @Override
    public OffsetDateTime getStopDate() {
        return this.stopDate;
    }

    @Override
    public void setStopDate(OffsetDateTime stopDate) {
        this.stopDate = stopDate;
    }

    @Override
    public Gson getGsonBuilder() {
        return this.gson;
    }

    @Override
    public void setGsonBuilder(Gson gson) {
        this.gson = gson;
    }

}
