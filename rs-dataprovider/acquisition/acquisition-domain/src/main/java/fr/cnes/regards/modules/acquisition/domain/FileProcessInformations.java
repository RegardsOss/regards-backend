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
package fr.cnes.regards.modules.acquisition.domain;

import java.time.OffsetDateTime;

/**
 * contient les informations sur un process specifique a un fichier
 * 
 * @author Christophe Mertz
 * 
 */
public class FileProcessInformations {

    /**
     * Date a laquelle le process a traite le fichier
     */
    protected OffsetDateTime date;

    /**
     * erreur rencontree lors du traitement du fichier par le process
     */
    protected ErrorType error;

    public OffsetDateTime getDate() {
        return date;
    }

    public void setDate(OffsetDateTime newDate) {
        this.date = newDate;
    }

    public ErrorType getError() {
        return error;
    }

    public void setError(ErrorType error) {
        this.error = error;
    }

}
