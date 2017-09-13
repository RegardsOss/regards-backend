/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cette classe regroupe les parametres regissant les acces au STAF. Les informations de cette classe sont
 * issues du fichier de parametrage STAF. L'emplacement de ce fichier est indique par une variable d'environnement. Les
 * informations du fichier de conf sont les suivantes :
 * <ul>
 * <li>Nombre max. de fichiers par flot (commande STAF) pour la restitution</li>
 * <li>Nombre max. de flots par session pour la restitution</li>
 * <li>Nombre max. de sessions STAF en parallele pour la restitution</li>
 * <li>Nombre max. de tentatives de connexion au STAF</li>
 * <li>Nombre max. de fichiers par flot (commande STAF) pour l archivage</li>
 * <li>Nombre max. de flots par session pour l archivage</li>
 * <li>Nombre max. de sessions STAF en parallele pour l archivage</li>
 * <li>Taille max. d un fichier a archiver</li>
 * <li>Taille min. d un fichier a archiver</li>
 * </ul>
 *
 */
@ConfigurationProperties(prefix = "regards.staf")
public class STAFConfiguration {

    /**
     * Nombre maximum de fichiers par flot pour le mode "Restitution"
     */
    private Integer maxStreamFilesRestitutionMode = null;

    /**
     * Nombre maximum de flots par session pour le mode "Restitution"
     */
    private Integer maxSessionStreamsRestitutionMode = null;

    /**
     * Nombre maximum de sessions pour le mode "Restitution"
     */
    private Integer maxSessionsRestitutionMode = null;

    /**
     * Nombre maximum de fichiers par flot pour le mode "Archivage"
     */
    private Integer maxStreamFilesArchivingMode = null;

    /**
     * Nombre maximum de flots par session pour le mode "Archivage"
     */
    private Integer maxSessionStreamsArchivingMode = null;

    /**
     * Nombre maximum de sessions pour le mode "Archivage"
     */
    private Integer maxSessionsArchivingMode = null;

    /**
     * Nombre maximum de tentatives de connexion au STAF
     */
    private Integer attemptsBeforeFail = null;

    /**
     * Taille minimal d'un fichier standard
     */
    private Long minFileSize = null;

    /**
     * Taille maximale d'un fichier standard
     */
    private Long maxFileSize = null;

    /**
     * Taille maximale d'un TAR pour archive au STAF.
     */
    private Long maxTarSize = null;

    /**
     * Taille minimal d'un TAR pour archivage au STAF.
     */
    private Long tarSizeThreshold = null;

    /**
     * Durée d'attente maximum avant l'archivage d'un TAR.
     */
    private Long maxTarArchivingHours = null;

    /**
     * classe de service petits fichiers ( STAF GF et generaliste ) (CS1 ou CS2) - Garantie de performance : le temps de
     * restitution du fichier est inférieur à 1 minute - Garantie de stockage : le fichier est sauvegardé (2 copies
     * disponibles) - Disponibilité : le fichier est accessible 24 heures sur 24 et 7 jours sur 7 - Taille maximum du
     * fichier : 50 mégaoctets
     */
    private String littleFileClass = null;

    /**
     * classe de service plus gros fichier ( STAF Generaliste ) (CS3 ou CS4) - Garantie de performance : le temps de
     * restitution du fichier est inférieur à 8 minutes - Garantie de stockage : le fichier est sauvegardé (2 copies
     * disponibles) - Disponibilité : le fichier est accessible 24 heures sur 24 et 7 jours sur 7 - Taille maximum du
     * fichier : 500 mégaoctets
     */
    private String biggerFileGenClass = null;

    /**
     * classe de service gros fichier ( STAF Gros fichiers ) (CS5 ou CS6) - Garantie de performance : le temps de
     * restitution du fichier est inférieur à 60 minutes - Garantie de stockage : le fichier est sauvegardé (2 copies
     * disponibles) - Disponibilité : le fichier est accessible 24 heures sur 24 et 7 jours sur 7 - Taille maximum du
     * fichier limitée en STAF V2R12 à 1 gigaoctet
     */

    private String biggerFileGFClass = null;

    public Integer getMaxStreamFilesRestitutionMode() {
        return maxStreamFilesRestitutionMode;
    }

    public void setMaxStreamFilesRestitutionMode(Integer pMaxStreamFilesRestitutionMode) {
        maxStreamFilesRestitutionMode = pMaxStreamFilesRestitutionMode;
    }

    public Integer getMaxSessionStreamsRestitutionMode() {
        return maxSessionStreamsRestitutionMode;
    }

    public void setMaxSessionStreamsRestitutionMode(Integer pMaxSessionStreamsRestitutionMode) {
        maxSessionStreamsRestitutionMode = pMaxSessionStreamsRestitutionMode;
    }

    public Integer getMaxSessionsRestitutionMode() {
        return maxSessionsRestitutionMode;
    }

    public void setMaxSessionsRestitutionMode(Integer pMaxSessionsRestitutionMode) {
        maxSessionsRestitutionMode = pMaxSessionsRestitutionMode;
    }

    public Integer getMaxStreamFilesArchivingMode() {
        return maxStreamFilesArchivingMode;
    }

    public void setMaxStreamFilesArchivingMode(Integer pMaxStreamFilesArchivingMode) {
        maxStreamFilesArchivingMode = pMaxStreamFilesArchivingMode;
    }

    public Integer getMaxSessionStreamsArchivingMode() {
        return maxSessionStreamsArchivingMode;
    }

    public void setMaxSessionStreamsArchivingMode(Integer pMaxSessionStreamsArchivingMode) {
        maxSessionStreamsArchivingMode = pMaxSessionStreamsArchivingMode;
    }

    public Integer getMaxSessionsArchivingMode() {
        return maxSessionsArchivingMode;
    }

    public void setMaxSessionsArchivingMode(Integer pMaxSessionsArchivingMode) {
        maxSessionsArchivingMode = pMaxSessionsArchivingMode;
    }

    public Integer getAttemptsBeforeFail() {
        return attemptsBeforeFail;
    }

    public void setAttemptsBeforeFail(Integer pAttemptsBeforeFail) {
        attemptsBeforeFail = pAttemptsBeforeFail;
    }

    public Long getMinFileSize() {
        return minFileSize;
    }

    public void setMinFileSize(Long pMinFileSize) {
        minFileSize = pMinFileSize;
    }

    public Long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Long pMaxFileSize) {
        maxFileSize = pMaxFileSize;
    }

    public String getLittleFileClass() {
        return littleFileClass;
    }

    public void setLittleFileClass(String pLittleFileClass) {
        littleFileClass = pLittleFileClass;
    }

    public String getBiggerFileGenClass() {
        return biggerFileGenClass;
    }

    public void setBiggerFileGenClass(String pBiggerFileGenClass) {
        biggerFileGenClass = pBiggerFileGenClass;
    }

    public String getBiggerFileGFClass() {
        return biggerFileGFClass;
    }

    public void setBiggerFileGFClass(String pBiggerFileGFClass) {
        biggerFileGFClass = pBiggerFileGFClass;
    }

    public Long getMaxTarSize() {
        return maxTarSize;
    }

    public void setMaxTarSize(Long pMaxTarSize) {
        maxTarSize = pMaxTarSize;
    }

    public Long getTarSizeThreshold() {
        return tarSizeThreshold;
    }

    public void setTarSizeThreshold(Long pTarSizeThreshold) {
        tarSizeThreshold = pTarSizeThreshold;
    }

    public Long getMaxTarArchivingHours() {
        return maxTarArchivingHours;
    }

    public void setMaxTarArchivingHours(Long pMaxTarArchivingHours) {
        maxTarArchivingHours = pMaxTarArchivingHours;
    }

}
