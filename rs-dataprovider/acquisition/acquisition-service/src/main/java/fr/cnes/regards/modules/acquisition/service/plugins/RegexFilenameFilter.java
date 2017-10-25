/*
 * Copyright 2017 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
 */
package fr.cnes.regards.modules.acquisition.service.plugins;

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
        if (caseSensitive) {
            this.pattern = Pattern.compile(regex);
        } else {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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

        if (caseSensitive) {
            this.pattern = Pattern.compile(regex);
        } else {
            this.pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
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