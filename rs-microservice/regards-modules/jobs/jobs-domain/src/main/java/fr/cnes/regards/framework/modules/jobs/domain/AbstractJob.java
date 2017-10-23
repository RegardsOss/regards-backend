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
package fr.cnes.regards.framework.modules.jobs.domain;

import java.nio.file.Path;
import java.util.Observable;
import java.util.Observer;
import java.util.UUID;

/**
 * Abstract job, all jobs must inherit this class
 * @param <R> result type
 * @author oroussel
 * @author Léo Mieulet
 */
public abstract class AbstractJob<R> extends Observable implements IJob<R> {

    /**
     * JobInfo id
     */
    private UUID id;

    protected R result;

    /**
     * The workspace can be null, it should be cleaned after termination of a job
     */
    private Path workspace;

    /**
     * Current completion count
     */
    private int completion = 0;

    /**
     * When the JobHandler creates this job, it saves the jobId
     */
    @Override
    public void setId(final UUID pJobInfoId) {
        id = pJobInfoId;
    }

    protected void setResult(R result) {
        this.result = result;
    }

    @Override
    public R getResult() {
        return result;
    }

    /**
     * Add an observer
     */
    public void addObserver(Observer observer) {
        super.addObserver(observer);
    }

    @Override
    public void setWorkspace(Path pWorkspace) {
        workspace = pWorkspace;
    }

    @Override
    public Path getWorkspace() {
        return workspace;
    }

    @Override
    public void advanceCompletion() {
        this.completion++;
        super.setChanged();
        super.notifyObservers((this.completion * 100) / getCompletionCount());
    }
}
