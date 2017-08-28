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
package fr.cnes.regards.modules.acquisition.plugins.ssalto.tools.compression;

import java.io.File;

/**
 * 
 * Class CompressManager
 * 
 * Gestionnaire de compression
 * 
 * @author CS
 * @since 5.4
 */
public class CompressManager {

    /**
     * 
     * Thread dans laquelle est lancée la compression, si la compression est asycnhrone
     * 
     * @return
     * @since 5.4
     */
    private Thread thread_ = null;

    /**
     * Pourcentage d'avancement de la compression
     * 
     * @return double
     * @since5.4
     */
    private double percentage_ = 0;

    /**
     * Ratio a appliquer lors du calcul du pourcentage d'avancement. Ce paramètre permet d'indiquer que la compression
     * en cours ne représente qu'une partie de la compression totale.
     */
    private double ratio_ = 1;

    /**
     * Liste des fichiers résultats de la compression
     */
    private File compressedFile_;

    public synchronized void setPercentage(double pPercentage) {
        percentage_ = pPercentage * ratio_;
    }

    public synchronized void upPercentage(long pPercentageToAdd) {
        percentage_ += pPercentageToAdd * ratio_;
    }

    public double getPercentage() {
        return percentage_;
    }

    public File getCompressedFile() {
        return compressedFile_;
    }

    public synchronized void setCompressedFile(File pCompressedFile) {
        compressedFile_ = pCompressedFile;
    }

    public double getRatio() {
        return ratio_;
    }

    public void setRatio(double pRatio) {
        ratio_ = pRatio;
    }

    public Thread getThread() {
        return thread_;
    }

    public synchronized void setThread(Thread pThread) {
        thread_ = pThread;
    }

}
