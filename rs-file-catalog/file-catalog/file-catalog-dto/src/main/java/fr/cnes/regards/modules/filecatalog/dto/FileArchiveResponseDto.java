package fr.cnes.regards.modules.filecatalog.dto;

import java.util.Objects;

/**
 * Dto for the response of a file packaging request. It contains the original requestId, storage and the computed
 * fileUrl which is the final url where the file will be stored.
 *
 * @author Thibaud Michaudel
 **/
public class FileArchiveResponseDto {

    private final Long requestId;

    private final String storage;

    private final String checksum;

    private final String fileUrl;

    public FileArchiveResponseDto(Long requestId, String storage, String checksum, String fileUrl) {
        this.requestId = requestId;
        this.storage = storage;
        this.checksum = checksum;
        this.fileUrl = fileUrl;
    }

    public Long getRequestId() {
        return requestId;
    }

    public String getStorage() {
        return storage;
    }

    public String getChecksum() {
        return checksum;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FileArchiveResponseDto that = (FileArchiveResponseDto) o;
        return Objects.equals(requestId, that.requestId) && Objects.equals(storage, that.storage) && Objects.equals(
            checksum,
            that.checksum) && Objects.equals(fileUrl, that.fileUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestId, storage, checksum, fileUrl);
    }

    @Override
    public String toString() {
        return "FileArchiveResponseDto{"
               + "requestId="
               + requestId
               + ", storage='"
               + storage
               + '\''
               + ", checksum='"
               + checksum
               + '\''
               + ", fileUrl='"
               + fileUrl
               + '\''
               + '}';
    }
}
