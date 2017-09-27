package fr.cnes.regards.modules.acquisition.plugins;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

/**
 * Fournit un filtre permettant de selectionner des fichiers designes par un motif de type "*.xml" Ex :
 * file.listFiles(new RegexFilenameFilter("(\\w)+.xml"));
 * 
 * @author Christophe Mertz
 */
public final class RegexFilenameFilter implements FilenameFilter {

    /**
     * Le motif de filtrage du nom
     */
    private Pattern pattern = null;

    /**
     * Si <code>TRUE</code>, definit que les fichiers qui repondent au pattern doivent etre exclus.
     * 
     * @since 1.0
     */
    private Boolean patternExclusion = Boolean.FALSE;

    /**
     * indique si on matche egalement les noms de repertoire
     */
    private Boolean matchDirectoryToo = Boolean.FALSE;

    /**
     * Constructeur
     * 
     * @param regex
     *            expression reguliere
     */
    public RegexFilenameFilter(final String regex) {
        this.pattern = Pattern.compile(regex);
    }

    /**
     * Constructeur
     * 
     * @param regex
     *            expression reguliere
     * @param caseSensitive
     *            indique si l'expression est sensible a la case
     */
    public RegexFilenameFilter(final String regex, Boolean caseSensitive) {
        if (!caseSensitive.booleanValue()) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } else {
            this.pattern = Pattern.compile(regex);
        }
    }

    /**
     * Constructeur
     * 
     * @param regex
     *            expression reguliere
     * @param caseSensitive
     *            indique si l'expression est sensible a la case
     * @param matchDir
     *            indique si on matche les nom de repertoires
     */
    public RegexFilenameFilter(final String regex, Boolean caseSensitive, Boolean matchDir) {

        matchDirectoryToo = matchDir;

        if (!caseSensitive.booleanValue()) {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        } else {
            this.pattern = Pattern.compile(regex);
        }
    }

    /***
     * Test si un fichier respectant l'expression reguliere peut etre incluse dans la liste des fichiers.
     * 
     * @param dir
     *            le repertoire analyse
     * @param name
     *            le nom du fichier
     * @return <code>true</code> si et seulement si le nom du fichier correspond au pattern.
     */
    @Override
    public final boolean accept(final File dir, final String name) {
        boolean ret = true;
        File file = new File(dir, name);
        if ((file.isFile()) || ((file.isDirectory()) && (matchDirectoryToo.booleanValue()))) {
            ret = pattern.matcher(name).matches();
            if (patternExclusion.equals(Boolean.TRUE)) {
                ret = !ret;
            }
        } else {
            ret = false;
        }

        return ret;
    }

    public void setPatternExclusion(Boolean exclude) {
        patternExclusion = exclude;
    }

    public Boolean getMatchDirectoryToo() {
        return this.matchDirectoryToo;
    }

    public void setMatchDirectoryToo(Boolean matchDir) {
        this.matchDirectoryToo = matchDir;
    }
}