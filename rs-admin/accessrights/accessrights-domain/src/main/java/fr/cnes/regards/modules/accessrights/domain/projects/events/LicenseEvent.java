/*
 * Copyright 2017-20XX CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.accessrights.domain.projects.events;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.ISubscribable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * An event about a license (acceptation or reset)
 *
 * @author Thomas Fache
 **/
@Event(target = Target.ALL)
public class LicenseEvent implements ISubscribable {

    // We use a generic event on license with an action
    // because of the Regards design
    // It creates a queue for each event
    // We try to reduce queue number.
    private LicenseAction action;

    private String user;

    private String licenseLink;

    public LicenseEvent() {
        // For deserialisation
    }

    public LicenseEvent(LicenseAction licenseAction, String userMail, String licenseLink) {
        // No need to add the tenant.
        // It is added by regards-amqp.
        action = licenseAction;
        user = userMail;
        this.licenseLink = licenseLink;
    }

    public LicenseAction getAction() {
        return action;
    }

    public String getUser() {
        return user;
    }

    public String getLicenseLink() {
        return licenseLink;
    }

}
