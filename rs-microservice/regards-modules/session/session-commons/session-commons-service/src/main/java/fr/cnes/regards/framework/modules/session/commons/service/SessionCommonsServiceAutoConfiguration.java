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
package fr.cnes.regards.framework.modules.session.commons.service;

import fr.cnes.regards.framework.modules.session.commons.service.delete.DefaultSessionDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.DefaultSourceDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISessionDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.ISourceDeleteService;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SessionDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.delete.SourceDeleteEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.jobs.SnapshotJobEventHandler;
import fr.cnes.regards.framework.modules.session.commons.service.jobs.SnapshotJobEventService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * @author Iliana Ghazali
 **/

@Configuration
public class SessionCommonsServiceAutoConfiguration {

    /**
     * Delete
     */

    @Bean
    @Profile("!nohandler")
    public SessionDeleteEventHandler sessionDeleteEventHandler() {
        return new SessionDeleteEventHandler();
    }

    @Bean
    @Profile("!nohandler")
    public SourceDeleteEventHandler sourceDeleteEventHandler() {
        return new SourceDeleteEventHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ISessionDeleteService sessionDeleteService() {
        return new DefaultSessionDeleteService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ISourceDeleteService sourceDeleteService() {
        return new DefaultSourceDeleteService();
    }

    /**
     * Jobs
     */

    @Bean
    @Profile("!nohandler")
    public SnapshotJobEventHandler snapshotJobEventHandler() {
        return new SnapshotJobEventHandler();
    }

    @Bean
    public SnapshotJobEventService snapshotJobEventService() {
        return new SnapshotJobEventService();
    }
}