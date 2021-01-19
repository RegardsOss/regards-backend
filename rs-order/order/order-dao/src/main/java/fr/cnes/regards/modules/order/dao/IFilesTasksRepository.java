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
package fr.cnes.regards.modules.order.dao;

import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.OrderDataFile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author oroussel
 */
public interface IFilesTasksRepository extends JpaRepository<FilesTask, Long> {

    @EntityGraph("graph.filesTask.complete")
    FilesTask findDistinctByFilesContaining(OrderDataFile file);

    @EntityGraph("graph.filesTask.complete")
    List<FilesTask> findDistinctByFilesIn(List<OrderDataFile> files);

    Stream<FilesTask> findByOrderId(Long orderId);

    long countByOwnerAndWaitingForUser(String user, Boolean waitingForUser);

    /**
     * Count filesTasks (or jobs because 1 filesTask = 1 job) with a false ended attribute value (associated jobInfo
     * not finished OR at least one remaining file to download) and a "finished" status
     * @param user user specific tasks and jobs
     */
    default long countWaitingForUserFilesTasks(String user) {
        return countByOwnerAndWaitingForUser(user, true);
    }

    // For tests
    @EntityGraph("graph.filesTask.complete")
    List<FilesTask> findDistinctByWaitingForUser(Boolean waitingForUser);
}
