package fr.cnes.regards.modules.storage.plugin.datastorage;

/**
 * Contains data storage information aggregated with some meta data on this data storage: plugin id, plugin description, plugin configuration label.
 *
 * @author Sylvain VISSIERE-GUERINET
 */
public class PluginStorageInfo {

    /**
     * The plugin configuration id
     */
    private Long confId;

    /**
     * The plugin description
     */
    private String description;

    /**
     * The plugin configuration label
     */
    private String label;

    /**
     * Total size allocated to the associated data storage
     */
    private String totalSize;

    /**
     * Used size by the associated data storage
     */
    private String usedSize;

    /**
     * The occupation ratio of the associated data storage
     */
    private Double ratio;

    /**
     * Constructor setting the parameters as attributes
     * @param confId
     * @param description
     * @param label
     */
    public PluginStorageInfo(Long confId, String description, String label) {
        this.confId = confId;
        this.description = description;
        this.label = label;
    }

    /**
     * @return the configuration id
     */
    public Long getConfId() {
        return confId;
    }

    /**
     * Set the configuration id
     * @param confId
     */
    public void setConfId(Long confId) {
        this.confId = confId;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description
     * @param description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Set the label
     * @param label
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * @return the total size
     */
    public String getTotalSize() {
        return totalSize;
    }

    /**
     * Set the total size
     * @param totalSize
     */
    public void setTotalSize(String totalSize) {
        this.totalSize = totalSize;
    }

    /**
     * @return the used size
     */
    public String getUsedSize() {
        return usedSize;
    }

    /**
     * Set the used size
     * @param usedSize
     */
    public void setUsedSize(String usedSize) {
        this.usedSize = usedSize;
    }

    /**
     * @return the ratio
     */
    public Double getRatio() {
        return ratio;
    }

    /**
     * Set the ratio
     * @param ratio
     */
    public void setRatio(Double ratio) {
        this.ratio = ratio;
    }
}
