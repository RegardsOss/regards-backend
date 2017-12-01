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
package fr.cnes.regards.modules.order.dao;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import fr.cnes.regards.framework.modules.jobs.domain.JobStatus;
import fr.cnes.regards.modules.order.domain.FilesTask;
import fr.cnes.regards.modules.order.domain.OrderDataFile;

/**
 * @author oroussel
 */
public interface IFilesTasksRepository extends JpaRepository<FilesTask, Long> {

    @EntityGraph("graph.filesTask.complete")
    FilesTask findDistinctByFilesIn(OrderDataFile file);

    @EntityGraph("graph.filesTask.complete")
    List<FilesTask> findDistinctByFilesIn(Iterable<OrderDataFile> files);

    Stream<FilesTask> findByOrderId(Long orderId);

    long countByOwnerAndEndedAndJobInfoStatusStatusIn(String user, Boolean ended, JobStatus... statuses);

    default long countFinishedJobsOnNotEndedFilesTaskCount(String user) {
        return countByOwnerAndEndedAndJobInfoStatusStatusIn(user, false, JobStatus.SUCCEEDED, JobStatus.FAILED,
                                                            JobStatus.ABORTED);
    }

    // For tests
    @EntityGraph("graph.filesTask.complete")
    List<FilesTask> findDistinctByWaitingForUser(Boolean waitingForUser);
}
