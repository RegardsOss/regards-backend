package fr.cnes.regards.modules.order.domain;

/**
 * In case a file is nearline, it is at one of these states :
 * - PENDING : asked to storage to make it available,
 * - AVAILABLE : available to be downloaded,
 * - DOWNLOADED : already downloaded (maybe no more available),
 * - ERROR : in error while asked to be made available.
 * BEWARE !!!
 * - ONLINE files are not stored into rs-storage BUT are managed by rs-storage. Hence, it is mandatory to
 * ask storage for their availability and storage respond they are immediately available.
 * So, from order point of view, an online data file is the same as a NEARLINE data file.
 * @author oroussel
 */
public enum FileState {
    AVAILABLE,
    DOWNLOADED,
    ERROR,
    PENDING,
}
