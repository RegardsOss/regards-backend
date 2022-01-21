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
package fr.cnes.regards.modules.dam.domain.dataaccess.accessright.event;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;
import fr.cnes.regards.framework.urn.UniformResourceName;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.AccessLevel;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.AccessRight;
import fr.cnes.regards.modules.dam.domain.dataaccess.accessright.DataAccessLevel;

/**
 * Access right event.
 *
 * @author Sylvain Vissiere-Guerinet
 * @author oroussel
 */
@Event(target = Target.ALL)
public class AccessRightEvent implements ISubscribable {

    /**
     * This is used by update logic
     */
    private UniformResourceName datasetIpId;

    /**
     * This is used for notification purposes
     */
    private String datasetLabel;

    private String accessGroupName;

    private AccessLevel accessLevel;

    private DataAccessLevel dataAccessLevel;

    private String dataAccessPluginLabel;

    private AccessRightEventType eventType;

    private String roleToNotify;

    @SuppressWarnings("unused")
    private AccessRightEvent() {
        super();
    }

    public AccessRightEvent(AccessRight accessRight, AccessRightEventType eventType, String roleToNotify) {
        this.datasetIpId = accessRight.getConstrained().getIpId();
        this.datasetLabel = accessRight.getConstrained().getLabel();
        this.accessGroupName = accessRight.getAccessGroup().getName();
        this.accessLevel = accessRight.getAccessLevel();
        this.dataAccessLevel = accessRight.getDataAccessLevel();
        this.dataAccessPluginLabel =
                accessRight.getDataAccessPlugin() == null ? null : accessRight.getDataAccessPlugin().getLabel();
        this.eventType = eventType;
        this.roleToNotify = roleToNotify;
    }

    public UniformResourceName getDatasetIpId() {
        return datasetIpId;
    }

    public String getDatasetLabel() {
        return datasetLabel;
    }

    public String getAccessGroupName() {
        return accessGroupName;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public DataAccessLevel getDataAccessLevel() {
        return dataAccessLevel;
    }

    public String getDataAccessPluginLabel() {
        return dataAccessPluginLabel;
    }

    public AccessRightEventType getEventType() {
        return eventType;
    }

    public String getRoleToNotify() {
        return roleToNotify;
    }
}
