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
package fr.cnes.regards.modules.storage.domain;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Léo Mieulet
 *         This object is returned on REST to inform how many entities were removed
 */
public class DeletedAipsInfos {

    @SuppressWarnings("unused")
    private final int nbErrors;

    @SuppressWarnings("unused")
    private final int nbDeleted;

    public DeletedAipsInfos(AtomicInteger nbErrors, AtomicInteger nbDeleted) {
        this.nbErrors = nbErrors.get();
        this.nbDeleted = nbDeleted.get();
    }
}
