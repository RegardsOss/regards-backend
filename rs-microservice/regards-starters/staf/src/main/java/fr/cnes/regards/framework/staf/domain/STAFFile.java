/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

/**
 * Informations about a file stored in STAF System.
 * @author CS
 */
public class STAFFile {

    /**
     * STAF File path
     */
    private String filePath;

    /**
     * STAF File size
     */
    private Integer fileSize;

    /**
     * STAF Archive
     */
    private STAFArchive stafArchive;

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String pFilePath) {
        filePath = pFilePath;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public void setFileSize(Integer pFileSize) {
        fileSize = pFileSize;
    }

    public STAFArchive getStafArchive() {
        return stafArchive;
    }

    public void setStafArchive(STAFArchive pStafArchive) {
        stafArchive = pStafArchive;
    }

}
