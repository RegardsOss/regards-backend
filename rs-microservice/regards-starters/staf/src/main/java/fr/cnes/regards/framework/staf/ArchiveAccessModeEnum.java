package fr.cnes.regards.framework.staf;

public class ArchiveAccessModeEnum {

    /**
     * Definit une archive du STAF.
     */
    public static final ArchiveAccessModeEnum ARCHIVE_MODE = new ArchiveAccessModeEnum(0, "ARCHIVE_MODE");

    /**
     * Definit une archive en ligne (disque local).
     */
    public static final ArchiveAccessModeEnum RESTITUTION_MODE = new ArchiveAccessModeEnum(1, "RESTITUTION_MODE");

    /**
     * La representation textuelle representant l'enumere.
     */
    private String label = null;

    /**
     * la valeur de l'enumere
     */
    private int value;

    /**
     * Constructeur.
     */
    private ArchiveAccessModeEnum() {
        // explicit null
    }

    /**
     * Constructeur.
     *
     * @param pValue La valeur textuelle a affecter.
     * @since 4.1
     */
    private ArchiveAccessModeEnum(int pValue, String pLabel) {
        value = pValue;
        label = pLabel;
    }

    /**
     * Permet de recuperer la valeur de l'enum en tant que chaine de caracteres.
     *
     * @return La valeur de l'enum.
     * @see java.lang.Object#toString()
     * @since 4.1
     */
    @Override
    public String toString() {
        return label;
    }
}
