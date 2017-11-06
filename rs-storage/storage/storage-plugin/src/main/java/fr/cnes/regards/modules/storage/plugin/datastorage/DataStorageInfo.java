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
    private String totalSize;

    /**
     * This field contains the value and the unit. For example, 1234567925B ~= 1.177GiB ~= 1.235GB.
     */
    @FileSize
    private String usedSize;

    /**
     * Disk usage ratio in percent
     */
    private Double ratio;

    /**
     * Default constructor assuming that all numerical value given are expressed in Bytes.
     *
     * @param storagePhysicalId {@link DataStorageInfo#storagePhysicalId}
     * @param totalSize {@link DataStorageInfo#totalSize}
     * @param usedSize {@link DataStorageInfo#usedSize}
     */
    public DataStorageInfo(@NotEmpty String storagePhysicalId, long totalSize, long usedSize) {
        super();
        this.storagePhysicalId = storagePhysicalId;
        this.totalSize = totalSize + BYTES_UNIT;
        this.usedSize = usedSize + BYTES_UNIT;
        this.ratio = (new Double(usedSize) / totalSize) * 100;
    }

    public String getStoragePhysicalId() {
        return storagePhysicalId;
    }

    public void setStoragePhysicalId(String storagePhysicalId) {
        this.storagePhysicalId = storagePhysicalId;
    }

    public String getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(@FileSize String pTotalSize) {
        totalSize = pTotalSize;
    }

    public String getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(@FileSize String pUsedSize) {
        usedSize = pUsedSize;
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
