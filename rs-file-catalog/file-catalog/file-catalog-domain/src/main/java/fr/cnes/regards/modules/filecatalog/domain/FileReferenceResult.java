/*
 * Copyright 2017-2024 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.filecatalog.domain;

/**
 * POJO to represent a reference action result. A reference action can result with 3 types :
 * <ul>
 * <li>new reference is created</li>
 * <li>existing reference updated</li>
 * <li>existing reference not modified</li>
 * </ul>
 *
 * @author Stephane Cortine
 **/
public final class FileReferenceResult {

    FileReference fileReference;

    FileReferenceResultStatusEnum status;

    private FileReferenceResult() {
    }

    public static FileReferenceResult build(FileReference fileRef, FileReferenceResultStatusEnum status) {
        FileReferenceResult updateResult = new FileReferenceResult();
        updateResult.fileReference = fileRef;
        updateResult.status = status;
        return updateResult;
    }

    public FileReferenceResultStatusEnum getStatus() {
        return status;
    }

    public FileReference getFileReference() {
        return fileReference;
    }
}
