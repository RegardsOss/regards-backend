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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.regards.impl;

import java.io.Serializable;

import com.google.gson.Gson;
import com.rometools.rome.feed.CopyFrom;
import com.rometools.rome.feed.module.ModuleImpl;

import fr.cnes.regards.modules.dam.domain.entities.feature.EntityFeature;
import fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter.atom.modules.regards.RegardsModule;

/**
 * Module to handle specific REGARDS models opensearch parameters into ATOM format responses.
 * com.rometools.rome module implementation to handle specifics regards model attributes.
 * This module handles all Opensearch parameters with regards namespace or not configured.
 *
 * @see <a href="https://rometools.github.io/rome/RssAndAtOMUtilitiEsROMEV0.5AndAboveTutorialsAndArticles/RssAndAtOMUtilitiEsROMEPluginsMechanism.html">rometools.github.io</a>
 * @author Sébastien Binda
 */
public class RegardsModuleImpl extends ModuleImpl implements RegardsModule, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Regards {@link EntityFeature} to format.
     */
    private EntityFeature entity;

    /**
     * {@link Gson} to serialize attributes values.
     */
    private Gson gson;

    public RegardsModuleImpl() {
        super(RegardsModuleImpl.class, RegardsModuleImpl.URI);
    }

    @Override
    public Class<? extends CopyFrom> getInterface() {
        return RegardsModule.class;
    }

    @Override
    public void copyFrom(CopyFrom obj) {
        RegardsModuleImpl regardsModule = (RegardsModuleImpl) obj;
        regardsModule.setGsonBuilder(this.gson);
        regardsModule.setEntity(this.entity);
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
    public EntityFeature getEntity() {
        return this.entity;
    }

    @Override
    public void setEntity(EntityFeature entity) {
        this.entity = entity;
    }

}
