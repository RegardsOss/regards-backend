/*
 * Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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

package fr.cnes.regards.framework.dump;

/**
 *
 * @author Iliana Ghazali
 */

public enum DumpMessages {

    //Error messages
    ERROR_DELETE_DIR("Could not delete destination directory at {}"),
    ERROR_CREATE_DIR("Could not create destination directory(ies) at {}"),
    ERROR_CREATE_ZIP("Could not create zip at {}"),
    ERROR_CREATE_FILE("Could not create file at {}"),
    ERROR_WRITE_FILE("An error occurred during the writing of the file {}"),

    //Debug messages
    FILE_CREATED("File created: {} at {}"),
    FILE_ALREADY_EXISTS("File already exists at {}."),
    SUCCESS_WRITE("Successfully wrote to the file {}");
    private final String msg;

    DumpMessages(String msg){
        this.msg = msg;
    }

    public String getMsg() {
        return this.msg;
    }
}
