package fr.cnes.regards.framework.staf;

public class STAFFile {

    private String filePath;

    private Integer fileSize;

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
