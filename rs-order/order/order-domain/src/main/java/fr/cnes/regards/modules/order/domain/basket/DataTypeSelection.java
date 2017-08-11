package fr.cnes.regards.modules.order.domain.basket;

/**
 * Data type selection (quilooks and/or raw data)
 * File types are from enum class DataType (from rs-dam/entities-domain)
 * @author oroussel
 */
public enum DataTypeSelection {
    ALL("RAWDATA", "QUICKLOOK_SD", "QUICKLOOK_MD", "QUICKLOOK_HD"),
    QUICKLOOKS("QUICKLOOK_SD", "QUICKLOOK_MD", "QUICKLOOK_HD"),
    RAWDATA("RAWDATA");

    private String[] fileTypes;

    DataTypeSelection(String... fileTypes) {
        this.fileTypes = fileTypes;
    }

    public String[] getFileTypes() {
        return this.fileTypes;
    }
}
