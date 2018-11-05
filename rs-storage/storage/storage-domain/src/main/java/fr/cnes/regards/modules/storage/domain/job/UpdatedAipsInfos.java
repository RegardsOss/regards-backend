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
 * This object is used at the end of the UpdateAIPsTagJob to save how many entities were updated
 * @author Léo Mieulet
 */
public class UpdatedAipsInfos {
    private int nbErrors;
    private int nbUpdated;

    public UpdatedAipsInfos(AtomicInteger nbErrors, AtomicInteger nbEntityUpdated) {
        this.nbErrors = nbErrors.get();
        this.nbUpdated = nbEntityUpdated.get();
    }

    public int getNbErrors() {
        return nbErrors;
    }

    public int getNbUpdated() {
        return nbUpdated;
    }
}
