/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.utils.file.compression;

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
    private Thread thread = null;

    /**
     * Pourcentage d'avancement de la compression
     */
    private double percentage = 0;

    /**
     * Ratio a appliquer lors du calcul du pourcentage d'avancement. Ce paramètre permet d'indiquer que la compression
     * en cours ne représente qu'une partie de la compression totale.
     */
    private double ratio = 1;

    /**
     * Liste des fichiers résultats de la compression
     */
    private File compressedfile;

    // Getters & setters

    public synchronized void setPercentage(double pPercentage) {
        percentage = pPercentage * ratio;
    }

    public synchronized void upPercentage(long pPercentageToAdd) {
        percentage += pPercentageToAdd * ratio;

    }

    public double getPercentage() {
        return percentage;
    }

    public File getCompressedFile() {
        return compressedfile;
    }

    public synchronized void setCompressedFile(File pCompressedFile) {
        compressedfile = pCompressedFile;
    }

    public double getRatio() {
        return ratio;
    }

    public void setRatio(double pRatio) {
        ratio = pRatio;
    }

    public Thread getThread() {
        return thread;
    }

    public synchronized void setThread(Thread pThread) {
        thread = pThread;
    }

}
