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
package fr.cnes.regards.framework.amqp.test.handler;

import fr.cnes.regards.framework.amqp.domain.TenantWrapper;
import fr.cnes.regards.framework.amqp.test.event.Info;

/**
 * @author Marc Sordi
 */
public class AbstractInfoReceiver extends AbstractReceiver<Info> {

    private TenantWrapper<Info> lastWrapper;

    private Info lastInfo;

    @Override
    protected void doHandle(TenantWrapper<Info> wrapper) {
        this.lastWrapper = wrapper;
        this.lastInfo = wrapper.getContent();
    }

    public Info getLastInfo() {
        return lastInfo;
    }

    public TenantWrapper<Info> getLastWrapper() {
        return lastWrapper;
    }
}
