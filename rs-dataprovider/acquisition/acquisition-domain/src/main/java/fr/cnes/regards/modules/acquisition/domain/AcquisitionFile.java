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

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import fr.cnes.regards.framework.jpa.IIdentifiable;
import fr.cnes.regards.framework.jpa.converters.OffsetDateTimeAttributeConverter;
import fr.cnes.regards.modules.acquisition.domain.metadata.MetaFile;
import fr.cnes.regards.modules.acquisition.plugins.IAcquisitionScanPlugin;

/**
 * This class represents a {@link MetaFile} instance.<br>
 * A data file is detected by a plugin {@link IAcquisitionScanPlugin}. 
 * 
 * @author Christophe Mertz
 *
 */
@Entity
@Table(name = "t_acquisition_file")
public class AcquisitionFile implements IIdentifiable<Long> {

    /**
     * Maximum file name size constraint with length 255
     */
    private static final int MAX_FILE_NAME_LENGTH = 255;

    /**
     * Maximum enum size constraint with length 16
     */
    private static final int MAX_ENUM_LENGTH = 16;

    /**
     * Unique id
     */
    @Id
    @SequenceGenerator(name = "ChainSequence", initialValue = 1, sequenceName = "seq_chain")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ChainSequence")
    protected Long id;

    /**
     * The data file name
     */
    @NotBlank
    @Column(name = "label", length = MAX_FILE_NAME_LENGTH, nullable = false)
    protected String fileName;

    //    /**
    //     * identifiant dans le catalogue de diffusion
    //     * TODO CMZ util ?
    //     */
    //    protected String nodeIdentifier_;

    /**
     * The data file's size in octets
     */
    @Column(name = "file_size")
    protected Long size;

    /**
     * The data file's status
     */
    @Column(name = "status", length = MAX_ENUM_LENGTH)
    @Enumerated(EnumType.STRING)
    protected AcquisitionFileStatus status;

    // TODO CMZ util ?
    //    protected Product product;

    /**
     * The {@link MetaFile}
     */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meta_file_id", foreignKey = @ForeignKey(name = "fk_meta_file_id"), nullable = true,
            updatable = false)
    protected MetaFile metaFile;

    //    /**
    //     * numero de version du fichier
    //     */
    //    protected int version_;

    /**
     * informations sur l'acquisition de ce fichier
     */
    @Embedded
    protected FileAcquisitionInformations acquisitionInformations;

    //    /**
    //     * liste des processus de mise à jour du catalogue qui ont pris en compte ce fichier
    //     */
    //    protected int catalogueUpdateProcessList_;

    /**
     * Processing state of the data file
     */
    @Column(name = "error", length = MAX_ENUM_LENGTH)
    @Enumerated(EnumType.STRING)
    protected ErrorType error;

    /**
     * Data file asquisition date
     */
    @Column(name = "acquisition_date")
    @Convert(converter = OffsetDateTimeAttributeConverter.class)
    protected OffsetDateTime acqDate;

    /**
     * Data file checksum
     */
    @Column(name = "check_sum")
    protected String checkSum = null;

    /**
     * Algorithm used to calculate data file checksum
     */
    @Column(name = "check_sum_algo")
    protected String checkSumAlgo = null;

    /**
     * Default constructor
     */
    public AcquisitionFile() {
        super();
    }

    /**
     * permet de dupliquer l'objet
     * TODO CMZ : util ?
     */
    public Object clone() {
        AcquisitionFile file = new AcquisitionFile();
        file.setAcqDate(acqDate);
        file.setError(error);
        file.setId(id);
        file.setFileName(fileName);
        // TODO CMZ : bof pas terrible ce clone avec le même metaFile
        file.setMetaFile(metaFile);
        file.setSize(size);
        file.setCheckSum(checkSum);
        file.setCheckSumAlgo(checkSumAlgo);
        return file;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AcquisitionFile other = (AcquisitionFile) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public AcquisitionFileStatus getStatus() {
        return status;
    }

    public void setStatus(AcquisitionFileStatus status) {
        this.status = status;
    }

    //    /**
    //     * liste des processus de mise à jour du catalogue qui ont pris en compte ce fichier
    //     */
    //    protected int catalogueUpdateProcessList_;

    public MetaFile getMetaFile() {
        return metaFile;
    }

    public void setMetaFile(MetaFile metaFile) {
        this.metaFile = metaFile;
    }

    public ErrorType getError() {
        return error;
    }

    public void setError(ErrorType error) {
        this.error = error;
    }

    public OffsetDateTime getAcqDate() {
        return acqDate;
    }

    public void setAcqDate(OffsetDateTime acqDate) {
        this.acqDate = acqDate;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(String checkSum) {
        this.checkSum = checkSum;
    }

    public String getCheckSumAlgo() {
        return checkSumAlgo;
    }

    public void setCheckSumAlgo(String chackSumAlgo) {
        this.checkSumAlgo = chackSumAlgo;
    }

    public FileAcquisitionInformations getAcquisitionInformations() {
        return acquisitionInformations;
    }

    public void setAcquisitionInformations(FileAcquisitionInformations acquisitionInformations) {
        this.acquisitionInformations = acquisitionInformations;
    }

    //    /**
    //     * indique si le fichier est un doublon grace au status du fichier
    //     */
    //    public boolean isDuplicate() {
    //        boolean result = false;
    //        if (status.equals(AcquisitionFileStatus.DUPLICATE)) {
    //            result = true;
    //        }
    //        return result;
    //    }
    //
    //    /**
    //     * Methode verifiant si le fichier passe en parametre et le fichier courant sont des doublons.<br>
    //     * Un fichier ne peut etre un doublon que s'il est dans un etat stable : <li>TO_ARCHIVE <li>ARCHIVED <li>
    //     * IN_CATALOGUE <li>TAR_CURRENT <li>ACQUIRED
    //     * 
    //     * @param pFile
    //     *            le fichier doublon suppose
    //     * @return
    //     * @since 1.0
    //     */
    //
    //    public boolean isADoublon(AcquisitionFile pFile) {
    //        boolean isADoublon = false;
    //        if (pFile.getStatus().equals(AcquisitionFileStatus.TO_ARCHIVE) || pFile.getStatus().equals(AcquisitionFileStatus.ARCHIVED)
    //                || pFile.getStatus().equals(AcquisitionFileStatus.IN_CATALOGUE)
    //                || pFile.getStatus().equals(AcquisitionFileStatus.TAR_CURRENT)
    //                || pFile.getStatus().equals(AcquisitionFileStatus.ACQUIRED)) {
    //            if (pFile.getFileName().equals(fileName) && (pFile.getVersion() == version_)
    //                    && (!pFile.getStatus().equals(status))) {
    //                isADoublon = true;
    //            } else {
    //                isADoublon = false;
    //            }
    //        } else {
    //            isADoublon = false;
    //        }
    //        return isADoublon;
    //    }
    //
    //    /**
    //     * Methode comparant les nom
    //     * 
    //     * @param pFile
    //     * @return
    //     * @since 1.0
    //     */
    //    public boolean isSameFile(AcquisitionFile pFile) {
    //        return (pFile.getFileName().equals(fileName));
    //    }
    //
    //    /**
    //     * Indique si le fichier doit etre supprime de l'archive locale.
    //     */
    //    public boolean isDeletedFromLocalArchive() {
    //        return false;
    //        //        return (ARCHIVE_TYPE_BOTH.equalsIgnoreCase(archiveType_))
    //        //                || (ARCHIVE_TYPE_LOCAL.equalsIgnoreCase(archiveType_));
    //    }
    //
    //    /**
    //     * Indique si le fichier doit etre supprime du STAF
    //     */
    //    public boolean isDeletedFromStafArchive() {
    //        return false;
    //    }
    //
    //    /**
    //     * Indique si le fichier est archive au STAF
    //     */
    //    public boolean isStoredInStafArchive() {
    //        return false;
    //    }
    //
    //    /**
    //     * indique si le ssaltoFile se trouve dans un tar courant ou non.
    //     */
    //    public boolean isInCurrentTar() {
    //        boolean result = false;
    //        //        if (status.equals(AcquisitionFileStatus.TAR_CURRENT)) {
    //        //            result = true;
    //        //        }
    //        return result;
    //    }

}
