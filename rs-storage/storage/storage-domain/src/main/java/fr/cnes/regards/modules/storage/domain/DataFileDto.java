package fr.cnes.regards.modules.storage.domain;

import java.net.URL;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.util.MimeType;

import fr.cnes.regards.framework.oais.urn.DataType;
import fr.cnes.regards.modules.storage.domain.database.StorageDataFile;
import fr.cnes.regards.modules.storage.domain.database.DataFileState;
import fr.cnes.regards.modules.storage.domain.plugin.IOnlineDataStorage;

/**
 * DTO used to extract public useful information about files of an aip. To get instances of this class use {@link DataFileDto#fromDataFile(StorageDataFile)}.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class DataFileDto {

    /**
     * File url
     */
    private URL url;

    /**
     * File name
     */
    private String name;

    /**
     * Checksum
     */
    private String checksum;

    /**
     * Checksum algorithm
     */
    private String algorithm;

    /**
     * Data type
     */
    private DataType dataType;

    /**
     * File size
     */
    private Long fileSize;

    /**
     * File mime type
     */
    private MimeType mimeType;

    /**
     * Is the StorageDataFile stored online?
     */
    private boolean online = false;

    /**
     * Transform a {@link StorageDataFile} to a {@link DataFileDto}.
     * @param dataFile origin data file
     * @return dto
     */
    public static DataFileDto fromDataFile(StorageDataFile dataFile) {
        if (dataFile.getState() != DataFileState.STORED) {
            throw new IllegalArgumentException("DataFileDto cannot be created unless the data file is already stored");
        }
        DataFileDto dto = new DataFileDto();
        BeanUtils.copyProperties(dataFile, dto, "state", "id", "dataStorageUsed", "aipEntity");
        // lets compute the online attribute
        if (dataFile.getDataStorageUsed().getInterfaceNames().contains(IOnlineDataStorage.class.getName())) {
            dto.setOnline(true);
        }
        return dto;
    }

    /**
     * Default constructor
     */
    public DataFileDto() {}

    /**
     * @return the url
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Set the url
     * @param url
     */
    public void setUrl(URL url) {
        this.url = url;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the name
     * @param name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the checksum
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     * Set the checksum
     * @param checksum
     */
    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    /**
     * @return the checksum algorithm
     */
    public String getAlgorithm() {
        return algorithm;
    }

    /**
     * Set the algorithm
     * @param algorithm
     */
    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    /**
     * @return the data type
     */
    public DataType getDataType() {
        return dataType;
    }

    /**
     * Set the data type
     * @param dataType
     */
    public void setDataType(DataType dataType) {
        this.dataType = dataType;
    }

    /**
     * @return the file size
     */
    public Long getFileSize() {
        return fileSize;
    }

    /**
     * Set the file size
     * @param fileSize
     */
    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    /**
     * @return the mime type
     */
    public MimeType getMimeType() {
        return mimeType;
    }

    /**
     * Set the mime type
     * @param mimeType
     */
    public void setMimeType(MimeType mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return whether the data file is stored online or not
     */
    public boolean isOnline() {
        return online;
    }

    /**
     * Set whether the data file is stored online or not
     * @param online
     */
    public void setOnline(boolean online) {
        this.online = online;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DataFileDto that = (DataFileDto) o;
        return Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {

        return Objects.hash(url);
    }
}
