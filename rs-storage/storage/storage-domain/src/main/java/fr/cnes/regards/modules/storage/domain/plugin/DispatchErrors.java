/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.storage.domain.plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;

/**
 * POJO to handle errors during IAllocationStrategy plugin dispatching process.
 * @author Sébastien Binda
 */
public class DispatchErrors {

    private static class DispatchError {

        private final StorageDataFile fileInError;

        private final String errorCause;

        public DispatchError(StorageDataFile fileInError, String errorCause) {
            super();
            this.fileInError = fileInError;
            this.errorCause = errorCause;
        }

        public StorageDataFile getFileInError() {
            return fileInError;
        }

        public String getErrorCause() {
            return errorCause;
        }

    }

    /**
     * List of dataFile in error associated to a failure cause message
     */
    private final List<DispatchError> errors = new ArrayList<DispatchError>();

    /**
     * Add a failure cause for the given {@link StorageDataFile}
     * @param dataFile {@link StorageDataFile}
     * @param dispatchErrorCause {@link String}
     */
    public void addDispatchError(StorageDataFile dataFile, String dispatchErrorCause) {
        errors.add(new DispatchError(dataFile, dispatchErrorCause));
    }

    /**
     * Retrieve an optional failure cause for the given {@link StorageDataFile}
     * @param dataFile {@link StorageDataFile}
     * @return Optional {@link String}
     */
    public Optional<String> get(StorageDataFile dataFile) {
        Optional<DispatchError> error = this.errors.stream().filter(de -> de.getFileInError().equals(dataFile))
                .findFirst();
        if (error.isPresent()) {
            return Optional.ofNullable(error.get().getErrorCause());
        } else {
            return Optional.empty();
        }
    }

}
