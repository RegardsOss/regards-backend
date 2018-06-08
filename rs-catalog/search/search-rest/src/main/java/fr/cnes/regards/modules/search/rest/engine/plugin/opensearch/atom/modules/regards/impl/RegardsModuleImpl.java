/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.regards.impl;

import java.io.Serializable;

import com.google.gson.Gson;
import com.rometools.rome.feed.CopyFrom;
import com.rometools.rome.feed.module.ModuleImpl;

import fr.cnes.regards.modules.entities.domain.AbstractEntity;
import fr.cnes.regards.modules.search.rest.engine.plugin.opensearch.atom.modules.regards.RegardsModule;

/**
 * com.rometools.rome module implementation to handle specifics regards model attributes.
 * @author Sébastien Binda
 */
public class RegardsModuleImpl extends ModuleImpl implements RegardsModule, Serializable {

    private static final long serialVersionUID = 1L;

    private AbstractEntity entity;

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
    public AbstractEntity getEntity() {
        return this.entity;
    }

    @Override
    public void setEntity(AbstractEntity entity) {
        this.entity = entity;
    }

}
