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

import java.util.Date;

import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;

/**
 * instance d'un MetaFile. Un enregistrement de File est créé dés que l'on détecte l'arrivée d'un fichier sur un des
 * repertoires d'acquisition. Ensuite les informations sont renseignees au fur et a mesure de l'application des
 * traitements sur ce fichier.
 * 
 * @author Christophe Mertz
 *
 */
public class AcquisitionFile {

    /**
     * longueur maximum d'un nom de fichier
     */
    public static int MAX_FILE_NAME_LENGTH = 126;

    /**
     * nom du fichier (max 63 chars)
     */
    protected String fileName_;

    /**
     * identifiant dans le catalogue de diffusion
     */
    protected String nodeIdentifier_;

    /**
     * identifiant interne du fichier
     */
    protected Integer fileId_;

    /**
     * taille du fichier en octets
     */
    protected Long size_;

    /**
     * statut du fichier
     */
    protected SsaltoFileStatus status_;

    /**
     * produit auquel est rattache le fichier
     */
    protected Product product_;

    /**
     * type de fichier
     */
    protected MetaFile metaFile_;

    /**
     * numero de version du fichier
     * 
     * @since 1.0
     */
    protected int version_;

    //    /**
    //     * fichier descripteur associe au AcquisitionFile
    //     */
    //    protected DescriptorFile metaDataFileName_;

    /**
     * informations sur l'acquisition de ce fichier
     */
    protected FileAcquisitionInformations acquisitionInformations_;

    /**
     * liste des processus de mise à jour du catalogue qui ont pris en compte ce fichier
     */
    protected int catalogueUpdateProcessList_;

    /**
     * etat du traitement du fichier
     */
    protected ErrorType errorType_;

    /**
     * Date a laquelle le process a traite le fichier
     */
    protected Date dateTraitement_;

    /**
     * Signature MD5 du fichier
     */
    protected String fileMD5Signature_ = null;

    /**
     * constructeur par defaut, il est necessaire pour pouvoir l'instancier dans le digester
     * 
     * @since 1.0
     */
    public AcquisitionFile() {
        super();
    }

    @Override
    public boolean equals(Object pArg0) {
        boolean result = false;
        if (((AcquisitionFile) pArg0).getFileId().equals(getFileId())) {
            result = true;
        }
        return result;
    }

    /**
     * indique si le fichier est un doublon grace au status du fichier
     */
    public boolean isDuplicate() {
        boolean result = false;
        if (status_.equals(SsaltoFileStatus.DUPLICATE)) {
            result = true;
        }
        return result;
    }

    /**
     * Methode verifiant si le fichier passe en parametre et le fichier courant sont des doublons.<br>
     * Un fichier ne peut etre un doublon que s'il est dans un etat stable : <li>TO_ARCHIVE <li>ARCHIVED <li>
     * IN_CATALOGUE <li>TAR_CURRENT <li>ACQUIRED
     * 
     * @param pFile
     *            le fichier doublon suppose
     * @return
     * @since 1.0
     */

    public boolean isADoublon(AcquisitionFile pFile) {
        boolean isADoublon = false;
        if (pFile.getStatus().equals(SsaltoFileStatus.TO_ARCHIVE) || pFile.getStatus().equals(SsaltoFileStatus.ARCHIVED)
                || pFile.getStatus().equals(SsaltoFileStatus.IN_CATALOGUE)
                || pFile.getStatus().equals(SsaltoFileStatus.TAR_CURRENT)
                || pFile.getStatus().equals(SsaltoFileStatus.ACQUIRED)) {
            if (pFile.getFileName().equals(fileName_) && (pFile.getVersion() == version_)
                    && (!pFile.getStatus().equals(status_))) {
                isADoublon = true;
            } else {
                isADoublon = false;
            }
        } else {
            isADoublon = false;
        }
        return isADoublon;
    }

    /**
     * Methode comparant les nom
     * 
     * @param pFile
     * @return
     * @since 1.0
     */
    public boolean isSameFile(AcquisitionFile pFile) {
        return (pFile.getFileName().equals(fileName_));
    }

    /**
     * permet de dupliquer l'objet
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone() {
        AcquisitionFile file = new AcquisitionFile();
        file.setVersion(version_);
        file.setDateTraitement(dateTraitement_);
        file.setErrorType(errorType_);
        file.setFileId(fileId_);
        file.setFileName(fileName_);
        file.setProduct(product_);
        file.setMetaFile(metaFile_);
        file.setProduct(product_);
        file.setSize(size_);
        file.setNodeIdentifier(nodeIdentifier_);
        file.setFileMD5Signature(fileMD5Signature_);
        return file;
    }

    /**
     * Indique si le fichier doit etre supprime de l'archive locale.
     */
    public boolean isDeletedFromLocalArchive() {
        return false;
        //        return (ARCHIVE_TYPE_BOTH.equalsIgnoreCase(archiveType_))
        //                || (ARCHIVE_TYPE_LOCAL.equalsIgnoreCase(archiveType_));
    }

    /**
     * Indique si le fichier doit etre supprime du STAF
     */
    public boolean isDeletedFromStafArchive() {
        return false;
    }

    /**
     * Indique si le fichier est archive au STAF
     */
    public boolean isStoredInStafArchive() {
        return false;
    }

    /**
     * indique si le ssaltoFile se trouve dans un tar courant ou non.
     */
    public boolean isInCurrentTar() {
        boolean result = false;
        //        if (status_.equals(SsaltoFileStatus.TAR_CURRENT)) {
        //            result = true;
        //        }
        return result;
    }

    /**
     * Enregistre une anomalie sur le fichier
     * 
     * @param pError
     */
    public void setAcqErrorForProcess() {
        getAcquisitionInformations().setError(ErrorType.ERROR);
    }

    public FileAcquisitionInformations getAcquisitionInformations() {
        return acquisitionInformations_;
    }

    public void setAcquisitionInformations(FileAcquisitionInformations pAcquisitionInformations) {
        acquisitionInformations_ = pAcquisitionInformations;
    }

    public int getCatalogueUpdateProcessList() {
        return catalogueUpdateProcessList_;
    }

    public String getFileName() {
        return fileName_;
    }

    public MetaFile getMetaFile() {
        return metaFile_;
    }

    public Product getProduct() {
        return product_;
    }

    public Long getSize() {
        return size_;
    }

    public SsaltoFileStatus getStatus() {
        return status_;
    }

    public int getVersion() {
        return version_;
    }

    public void setCatalogueUpdateProcessList(int pCatalogueUpdateProcessList) {
        catalogueUpdateProcessList_ = pCatalogueUpdateProcessList;
    }

    public void setFileName(String pFileName) {
        fileName_ = pFileName;
    }

    public void setMetaFile(MetaFile pMetaFile) {
        metaFile_ = pMetaFile;
    }

    public void setProduct(Product pProduct) {
        product_ = pProduct;
    }

    public void setSize(Long pSize) {
        size_ = pSize;
    }

    public void setStatus(SsaltoFileStatus pStatus) {
        status_ = pStatus;
    }

    public void setVersion(int pVersion) {
        version_ = pVersion;
    }

    public Integer getFileId() {
        return fileId_;
    }

    public void setFileId(Integer pFileId) {
        fileId_ = pFileId;
    }

    //    public DescriptorFile getMetaDataFileName() {
    //        return metaDataFileName;
    //    }
    //
    //    public void setMetaDataFileName(DescriptorFile pMetaDataFileName) {
    //        metaDataFileName_ = pMetaDataFileName;
    //    }

    public ErrorType getErrorType() {
        return errorType_;
    }

    public void setErrorType(ErrorType pErrorType) {
        errorType_ = pErrorType;
    }

    public Date getDateTraitement() {
        return dateTraitement_;
    }

    public void setDateTraitement(Date pDateTraitement) {
        dateTraitement_ = pDateTraitement;
    }

    public String getNodeIdentifier() {
        return nodeIdentifier_;
    }

    public void setNodeIdentifier(String pEntityId) {
        nodeIdentifier_ = pEntityId;
    }

    public String getFileMD5Signature() {
        return fileMD5Signature_;
    }

    public void setFileMD5Signature(String fileMD5Signature) {
        fileMD5Signature_ = fileMD5Signature;
    }
}
