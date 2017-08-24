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
package fr.cnes.regards.modules.acquisition.domain.metadata;

/**
 * Represente un repertoire d'acquisition dans lequel on va aller chercher le fichiers deposes par les fournisseurs de
 * donnees.
 * @author Christophe Mertz
 *
 */
public class SupplyDirectory {

    /**
     * identifiant du metaFile
     */
    private Integer mFileId;

    /**
     * identifiant interne du SupplyDirectory
     */
    private Integer mSupplyDirId;

    /**
     * chemin du repertoire d'acquisition
     */
    private String supplyDir;

    /**
     * Date de modification systeme du fichier le plus recemment acquis
     */
    private long lastAcqDate;

    /**
     * constructeur par defaut
     */
    public SupplyDirectory() {
        super();
    }

    public Integer getMSupplyDirId() {
        return mSupplyDirId;
    }

    public String getSupplyDir() {
        return supplyDir;
    }

    public void setMSupplyDirId(Integer pSupplyDirId) {
        mSupplyDirId = pSupplyDirId;
    }

    public void setSupplyDir(String pSupplyDir) {
        supplyDir = pSupplyDir;
    }

    public Integer getMFileId() {
        return mFileId;
    }

    public void setMFileId(Integer pFileId) {
        mFileId = pFileId;
    }

    public long getLastAcqDate() {
        return lastAcqDate;
    }

    public void setLastAcqDate(long pLastAcqDate) {
        this.lastAcqDate = pLastAcqDate;
    }
}
