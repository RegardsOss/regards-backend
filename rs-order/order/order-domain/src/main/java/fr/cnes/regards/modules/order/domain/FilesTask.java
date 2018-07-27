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
package fr.cnes.regards.modules.order.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import fr.cnes.regards.framework.modules.jobs.domain.LeafTask;

/**
 * A sub-order task is a job that manage a set of data files.
 * Associated job calls
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

    public void setWaitingForUser(boolean waitingForUser) {
        this.waitingForUser = waitingForUser;
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
     * - if no file has been downloaded (=> no file with DOWNLOADED status)
     */
    public void computeWaitingForUser() {
        Set<OrderDataFile> notInErrorFiles = files.stream()
                .filter(f -> (f.getState() != FileState.ERROR) && (f.getState() != FileState.DOWNLOAD_ERROR))
                .collect(Collectors.toSet());
        // Not in error nor download_error files are all available
        this.waitingForUser = !notInErrorFiles.isEmpty()
                && notInErrorFiles.stream().allMatch(f -> f.getState() == FileState.AVAILABLE);
    }
}
