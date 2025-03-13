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
package fr.cnes.regards.modules.dam.domain.entities.event;

import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.urn.UniformResourceName;

/**
 * Microservice specific abstract entity event (@see CrawlerService) sent to AMQP indicating that the concerned entity has been
 * created/modified/deleted
 *
 * @author oroussel
 * @author Sylvain Vissiere-Guerinet
 */
public abstract class AbstractEntityEvent implements ISubscribable {

    private String roleToNotify;

    private String userToNotify;

    /**
     * Business id identifying an entity
     */
    private UniformResourceName[] ipIds;

    protected AbstractEntityEvent() {
        super();
    }

    public AbstractEntityEvent(String userToNotify, String roleToNotify, UniformResourceName... ipIds) {
        this();
        this.ipIds = ipIds;
        this.userToNotify = userToNotify;
        this.roleToNotify = roleToNotify;
    }

    public UniformResourceName[] getIpIds() {
        return ipIds;
    }

    public String getRoleToNotify() {
        return roleToNotify;
    }

    public String getUserToNotify() {
        return userToNotify;
    }

    @SuppressWarnings("unused")
    private void setIpIds(UniformResourceName... pIpIds) {
        ipIds = pIpIds;
    }

}