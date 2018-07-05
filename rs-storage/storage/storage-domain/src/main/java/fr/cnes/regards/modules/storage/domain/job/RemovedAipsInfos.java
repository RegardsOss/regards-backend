/*
 * Copyright 2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.job;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This object is used at the end of the RemoveAIPsJob to save how many entities were removed
 * @author Léo Mieulet
 */
public class RemovedAipsInfos {
    private int nbErrors;
    private int nbRemoved;

    public RemovedAipsInfos(AtomicInteger nbErrors, AtomicInteger nbEntityRemoved) {
        this.nbErrors = nbErrors.get();
        this.nbRemoved = nbEntityRemoved.get();
    }

    public int getNbErrors() {
        return nbErrors;
    }

    public int getNbRemoved() {
        return nbRemoved;
    }
}
