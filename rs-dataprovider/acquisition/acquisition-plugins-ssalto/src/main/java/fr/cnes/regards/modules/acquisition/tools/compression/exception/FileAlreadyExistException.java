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
package fr.cnes.regards.modules.acquisition.tools.compression.exception;

import java.io.File;

/**
 * Cette exception est levee lorsqu'un fichier cree lors de la compression ou de la decompression existe deja et qu'il
 * risquerai d'etre ecrase
 * 
 * @author Christophe Mertz
 *
 */
public class FileAlreadyExistException extends CompressionException {


    private static final long serialVersionUID = 5231569315276637940L;
    /**
     * File name
     */
    private File fileName = null;


    /**
     * Constructeur de copie
     * 
     * @param pException
     *            L'exception a copier.
     */
    public FileAlreadyExistException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructeur de copie permettant d'ajouter un message
     * 
     * @param pMessage
     *            Le message a ajouter
     * @param pException
     *            L'exception a copier.
     */
    public FileAlreadyExistException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur
     * 
     * @param pMessage
     *            Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     */
    public FileAlreadyExistException(String pMessage) {
        super(pMessage);
    }

    /**
     * Constructeur.
     * 
     * @param pMessage
     *            Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     * @param pFileName
     *            Le fichier qui pose probleme.
     */
    public FileAlreadyExistException(String pMessage, File pFileName) {
        super(pMessage);
        setFileName(pFileName);
    }

    public File getFileName() {
        return fileName;
    }

    public void setFileName(File fileName_) {
        this.fileName = fileName_;
    }

}
