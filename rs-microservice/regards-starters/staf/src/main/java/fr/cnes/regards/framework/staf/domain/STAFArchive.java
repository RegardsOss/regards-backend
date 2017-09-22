/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.staf.domain;

/**
 * Cette classe regroupe les parametres regissant les acces au STAF
 * pour un projet STAF donne. Les informations de cette classe sont issues du
 * fichier de parametrage STAF.
 * Les informations sont les suivantes :
 * <ul>
 * <li>Nom de l'archive STAF a laquelle s'applique la configuration</li>
 * <li>Indicateur d'utilisation d'un compte STAF GF (Gros Fichiers)</li>
 * </ul>
 */
public class STAFArchive {

    /**
     * Nom de l'archive
     */
    private String archiveName = null;

    /**
     * Mot de passe du projet
     */
    private String password = null;

    /**
     * @return Retourne le nom du projet STAF auquel s'applique la configuration.
     */
    public String getArchiveName() {
        return archiveName;
    }

    /**
     * @param archiveName Le nom du projet STAF auquel s'applique la configuration.
     */
    public void setArchiveName(String pArchiveName) {
        archiveName = pArchiveName;
    }

    /**
     * @return Retourne le mot de passe de connexion a l'archive.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password Le mot de passe de connexion a l'archive.
     */
    public void setPassword(String pPassword) {
        password = pPassword;
    }

}
