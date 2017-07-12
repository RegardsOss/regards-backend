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
package fr.cnes.regards.framework.modules.jobs.service.communication;

import fr.cnes.regards.framework.amqp.event.Event;
import fr.cnes.regards.framework.amqp.event.IPollable;
import fr.cnes.regards.framework.amqp.event.Target;

/**
 * @author Léo Mieulet
 */
@Event(target = Target.MICROSERVICE)
public class NewJobEvent implements IPollable {

    /**
     * the jobInfo id
     */
    private long jobInfoId;

    /**
     * @param pJobInfoId
     *            the jobInfo id
     */
    public NewJobEvent(final long pJobInfoId) {
        super();
        jobInfoId = pJobInfoId;
    }

    /**
     * @return the jobId
     */
    public long getJobInfoId() {
        return jobInfoId;
    }

    /**
     * @param pJobId
     *            the jobId to set
     */
    public void setJobInfoId(final long pJobId) {
        jobInfoId = pJobId;
    }

}
