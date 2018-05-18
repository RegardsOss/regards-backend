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
package fr.cnes.regards.framework.utils.file.compression;

import java.io.File;

/**
 * Cette exception est levee lorsqu'un fichier cree lors de la compression ou de la decompression existe deja et qu'il
 * risquerai d'etre ecrase
 */
public class FileAlreadyExistException extends CompressionException {

    /**
     * File name
     */
    private final File fileName;

    /**
     * Constructeur de copie permettant d'ajouter un message.
     * @param pMessage Le message a ajouter
     * @param pException L'exception a copier.
     */
    public FileAlreadyExistException(String pMessage, Throwable pException) {
        super(pMessage, pException);
        fileName = null;
    }

    /**
     * Constructeur.
     * @param pMessage Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     */
    public FileAlreadyExistException(String pMessage) {
        super(pMessage);
        fileName = null;
    }

    /**
     * Constructeur.
     * @param pMessage Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     * @param pFileName Le fichier qui pose probleme.
     */
    public FileAlreadyExistException(String pMessage, File pFileName) {
        super(pMessage);
        fileName = pFileName;
    }
}
