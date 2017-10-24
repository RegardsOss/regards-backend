/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.modules.storage.plugin.datastorage;

import org.hibernate.validator.constraints.NotEmpty;

import fr.cnes.regards.modules.storage.plugin.datastorage.validation.FileSize;

/**
 * Represents information on the current state of a data storage. <br/>
 * This will then be wrapped into a {@link PluginStorageInfo} which will have more informations on the plugin.
 *
 * @author Sylvain Vissiere-Guerinet
 */
public class DataStorageInfo {

    public static final String BYTES_UNIT = "B";

    /**
     * Identifier of the data storage being monitored. For example, the partition storagePhysicalId for {@link fr.cnes.regards.modules.storage.plugin.datastorage.local.LocalDataStorage}, the archive storagePhysicalId for {@link fr.cnes.regards.modules.storage.plugin.datastorage.staf.STAFDataStorage}.
     */
    @NotEmpty
    private String storagePhysicalId;

    /**
     * This field contains the value and the unit. For example, 1234567925B ~= 1.177GiB ~= 1.235GB.
     */
    @FileSize
    private String totalSpace;

    /**
     * This field contains the value and the unit. For example, 1234567925B ~= 1.177GiB ~= 1.235GB.
     */
    @FileSize
    private String usedSpace;

    /**
     * Occupation ratio in percent
     */
    private Double ratio;

    /**
     * Default constructor assuming that all numerical value given are expressed in Bytes.
     *
     * @param storagePhysicalId {@link DataStorageInfo#storagePhysicalId}
     * @param totalSpace {@link DataStorageInfo#totalSpace}
     * @param usedSpace {@link DataStorageInfo#usedSpace}
     */
    public DataStorageInfo(@NotEmpty String storagePhysicalId, long totalSpace, long usedSpace) {
        super();
        this.storagePhysicalId = storagePhysicalId;
        this.totalSpace = totalSpace + BYTES_UNIT;
        this.usedSpace = usedSpace + BYTES_UNIT;
        this.ratio = (new Double(usedSpace) / totalSpace) * 100;
    }

    public String getStoragePhysicalId() {
        return storagePhysicalId;
    }

    public void setStoragePhysicalId(String storagePhysicalId) {
        this.storagePhysicalId = storagePhysicalId;
    }

    public String getTotalSpace() {
        return totalSpace;
    }

    public void setTotalSpace(@FileSize String pTotalSize) {
        totalSpace = pTotalSize;
    }

    public String getUsedSpace() {
        return usedSpace;
    }

    public void setUsedSpace(@FileSize String pUsedSize) {
        usedSpace = pUsedSize;
    }

    public Double getRatio() {
        return ratio;
    }

    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataStorageInfo that = (DataStorageInfo) o;

        return storagePhysicalId != null ? storagePhysicalId.equals(that.storagePhysicalId) : that.storagePhysicalId == null;
    }

    @Override
    public int hashCode() {
        return storagePhysicalId != null ? storagePhysicalId.hashCode() : 0;
    }
}
