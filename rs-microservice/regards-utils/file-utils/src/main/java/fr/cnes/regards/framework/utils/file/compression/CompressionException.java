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

/**
 * Cette classe designe l'exception que peut lever le paquetage de Compression. Elle doit etre traitee par le module ou
 * l'application appelant.
 */
public class CompressionException extends Exception {

    /**
     * serialVersionUID field.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructeur
     */
    public CompressionException() {

    }

    /**
     * Constructeur de copie.
     * @param pException L'exception a copier.
     */
    public CompressionException(Throwable pException) {
        super(pException);
    }

    /**
     * Constructeur de copie permettant d'ajouter un message.
     * @param pMessage Le message a ajouter
     * @param pException L'exception a copier.
     */
    public CompressionException(String pMessage, Throwable pException) {
        super(pMessage, pException);
    }

    /**
     * Constructeur.
     * @param pMessage Le message textuel a afficher, permettant d'ajouter de la semantique a l'erreur.
     */
    public CompressionException(String pMessage) {
        super(pMessage);
    }

}
