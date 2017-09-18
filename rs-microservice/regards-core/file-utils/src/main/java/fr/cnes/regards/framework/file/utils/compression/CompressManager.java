/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.file.utils.compression;

import java.io.File;

/**
 *
 * Class CompressManager
 *
 * Gestionnaire de compression
 */
public class CompressManager {

    /**
     * Thread dans laquelle est lancée la compression, si la compression est asycnhrone
     */
    private Thread thread_ = null;

    /**
     * Pourcentage d'avancement de la compression
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

    // Getters & setters

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
