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
package fr.cnes.regards.modules.order.domain;

import fr.cnes.regards.framework.modules.jobs.domain.LeafTask;

import javax.persistence.*;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A sub-order task is a job that manage a set of data files.
 * <p>
 * This task specifically monitors the life cycle of instances of OrderDataFiles,
 * that is: files meant to be downloaded by the end user in the context of an order.
 * <p>
 * This task has an internal state allowing to prevent the rest of the order
 * to be processed. The "waitingForUser" flag means that no more StorageFilesJob
 * will be run until the user has downloaded the available files.
 * <p>
 * Associated job calls
 *
 * @author oroussel
 */
@Entity
@Table(name = "t_files_task")
@PrimaryKeyJoinColumn(foreignKey = @ForeignKey(name = "fk_task_id"))
@NamedEntityGraph(name = "graph.filesTask.complete", attributeNodes = @NamedAttributeNode(value = "files"))
public class FilesTask extends LeafTask {

    @OneToMany // dataFiles used on more than one DataObjects are considered as not identical.
    @JoinColumn(name = "files_task_id", foreignKey = @ForeignKey(name = "fk_files_task"))
    private final Set<OrderDataFile> files = new HashSet<>();

    /**
     * Does this task ended ? (=> jobInfo terminated and no file to be downloaded)
     */
    @Column(name = "ended")
    private boolean ended = false;

    @Column(name = "waiting_for_user")
    private boolean waitingForUser = false;

    @Column(name = "owner", length = 100, nullable = false)
    private String owner;

    /**
     * Mandatory orderId to know whose fileTask belongs to BUT without managing a ManyToOne relation (which will be a
     * mess you don't even want to understand, believe me as with orderDataFiles...)
     */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    public Set<OrderDataFile> getFiles() {
        return files;
    }

    public void addFile(OrderDataFile orderDataFile) {
        this.files.add(orderDataFile);
    }

    /**
     * Use a defensive copy to add DataFile
     */
    public void addAllFiles(Collection<OrderDataFile> files) {
        files.forEach(this::addFile);
    }

    public boolean isEnded() {
        return ended;
    }

    public void setEnded(boolean ended) {
        this.ended = ended;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public boolean isWaitingForUser() {
        return waitingForUser;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * Permit to know if a user action is mandatory to completely finish this task ie :
     * - if associated jobInfo has ended (=> no file with PENDING status)
     * - if any file is available to download (=> any file with AVAILABLE status)
     */
    public void computeWaitingForUser() {
        boolean anyPending = files.stream().anyMatch(file -> file.getState() == FileState.PENDING);
        boolean anyAvailable = files.stream().anyMatch(file -> file.getState() == FileState.AVAILABLE);

        this.waitingForUser = !anyPending && anyAvailable;
    }

    public void computeTaskEnded() {
        this.ended = allFilesHaveFinalStatus();
    }

    private boolean allFilesHaveFinalStatus() {
        return this.getFiles().stream().map(OrderDataFile::getState).allMatch(FileState::isFinalState);
    }
}
