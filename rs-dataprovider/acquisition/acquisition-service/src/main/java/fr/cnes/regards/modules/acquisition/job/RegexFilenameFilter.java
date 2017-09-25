package fr.cnes.regards.modules.acquisition.job;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Fournit un filtre permettant de selectionner des fichiers designes par un motif de type "*.xml" Ex :
 * file.listFiles(new RegexFilenameFilter("(\\w)+.xml"));
 * 
 * @author CS
 * @since 1.0
 */
public final class RegexFilenameFilter implements FilenameFilter {

    /**
     * Le motif de filtrage du nom
     * 
     * @since 1.0
     */
    private Pattern pattern_ = null;

    /**
     * Si <code>TRUE</code>, definit que les fichiers qui repondent au pattern doivent etre exclus.
     * 
     * @since 1.0
     */
    private Boolean patternExclusion_ = Boolean.FALSE;

    /**
     * indique si on matche egalement les noms de repertoire
     * 
     * @since 3.2
     */
    private Boolean matchDirectoryToo_ = Boolean.FALSE;

    /**
     * Constructeur
     * 
     * @param pRegex
     *            expression reguliere
     * @since 1.0
     */
    public RegexFilenameFilter(final String pRegex) {
        this.pattern_ = Pattern.compile(pRegex);
    }

    /**
     * Constructeur
     * 
     * @param pRegex
     *            expression reguliere
     * @param pCaseSensitive
     *            indique si l'expression est sensible a la case
     * @since 3.1
     * @DM SIPNG-DM-0020-CN : modification signature du constructeur
     */
    public RegexFilenameFilter(final String pRegex, Boolean pCaseSensitive) {
        if (!pCaseSensitive.booleanValue()) {
            this.pattern_ = Pattern.compile(pRegex, Pattern.CASE_INSENSITIVE);
        }
        else {
            this.pattern_ = Pattern.compile(pRegex);
        }
    }

    /**
     * Constructeur
     * 
     * @param pRegex
     *            expression reguliere
     * @param pCaseSensitive
     *            indique si l'expression est sensible a la case
     * @param pMatchDir
     *            indique si on matche les nom de repertoires
     * @since 3.1
     */
    public RegexFilenameFilter(final String pRegex, Boolean pCaseSensitive, Boolean pMatchDir) {

        matchDirectoryToo_ = pMatchDir;

        if (!pCaseSensitive.booleanValue()) {
            this.pattern_ = Pattern.compile(pRegex, Pattern.CASE_INSENSITIVE);
        }
        else {
            this.pattern_ = Pattern.compile(pRegex);
        }
    }

    /***
     * Test si un fichier respectant l'expression reguliere peut etre incluse dans la liste des fichiers.
     * 
     * @param pDir
     *            le repertoire analyse
     * @param pName
     *            le nom du fichier
     * @return <code>true</code> si et seulement si le nom du fichier correspond au pattern.
     */
    @Override
    public final boolean accept(final File pDir, final String pName) {
        boolean ret = true;
        File file = new File(pDir, pName);
        if ((file.isFile()) || ((file.isDirectory()) && (matchDirectoryToo_.booleanValue()))) {
            ret = pattern_.matcher(pName).matches();
            if (patternExclusion_ == Boolean.TRUE) {
                ret = !ret;
            }
        }
        else {
            ret = false;
        }

        return ret;
    }

    // === accessors ===/

    /**
     * Constructeur
     * 
     * @since 1.0
     * @param pBoolean
     *            boolean
     */
    public void setPatternExclusion(Boolean pBoolean) {
        patternExclusion_ = pBoolean;
    }

    public Boolean getMatchDirectoryToo() {
        return this.matchDirectoryToo_;
    }

    public void setMatchDirectoryToo(Boolean pMatchDirectoryToo) {
        this.matchDirectoryToo_ = pMatchDirectoryToo;
    }
}