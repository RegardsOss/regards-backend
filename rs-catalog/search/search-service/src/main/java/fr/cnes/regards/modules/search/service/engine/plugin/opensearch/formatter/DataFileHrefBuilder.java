/*
 * Copyright 2017-2021 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.search.service.engine.plugin.opensearch.formatter;

import fr.cnes.regards.modules.indexer.domain.DataFile;

/**
 * Build an url from a {@link DataFile}
 *
 * @author Léo Mieulet
 */
public final class DataFileHrefBuilder {

    private DataFileHrefBuilder() {
    }

    /**
     * Convert a {@link DataFile} into an URL. Only for publicly available resource like QUICKLOOK and THUMBNAIL
     *
     * @param file  {@link DataFile}
     * @param scope the current tenant
     * @return {@link String}
     */
    public static String getDataFileHref(DataFile file, String scope) {
        switch (file.getDataType()) {
            case QUICKLOOK_SD:
            case QUICKLOOK_MD:
            case QUICKLOOK_HD:
            case THUMBNAIL:
                String href = file.getUri();
                if (file.isReference()) {
                    return href;
                }
                return getLinkWithScope(href, scope);
            case RAWDATA:
            case DOCUMENT:
            case OTHER:
            case AIP:
            case DESCRIPTION:
                return file.getUri();
            default:
                throw new IllegalArgumentException("Unsupported DataFile type");
        }
    }

    /**
     * Any {@link String} href that is not a reference can be accessed publicly and needs the scope inside the URL to do so
     */
    private static String getLinkWithScope(String href, String scope) {
        if (href.contains("?")) {
            return String.format("%s&scope=%s", href, scope);
        } else {
            return String.format("%s?scope=%s", href, scope);
        }
    }
}
