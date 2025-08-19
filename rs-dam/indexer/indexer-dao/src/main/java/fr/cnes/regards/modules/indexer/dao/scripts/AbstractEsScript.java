/*
 * Copyright 2017-2025 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
 * along with REGARDS. If not, see `<http://www.gnu.org/licenses/>`.
 */
package fr.cnes.regards.modules.indexer.dao.scripts;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Base class for Elasticsearch scripts.
 * It reads the script content from a file located in the classpath.
 * The script ID is used to identify the script when registering it with Elasticsearch.
 * <p>
 * Tips : read .painless scripts in vscode. For syntax highlighting, and formatter, go to settings:
 * search for "Files: Associations" setting, and click add Item and set values "*.painless": "javascript" to add the file association.
 * Tips : use the "Painless Lab" of the Elastic dev tools to "test" your scripts in a web interface.
 *
 * @author tguillou
 */
public abstract class AbstractEsScript {

    private final String scriptId;

    private final String scriptContent;

    public AbstractEsScript(String scriptId, String scriptFile) throws IOException {
        this.scriptId = scriptId;
        // read the script content from a resource file
        try (InputStream in = AbstractEsScript.class.getClassLoader().getResourceAsStream(scriptFile)) {
            if (in == null) {
                throw new IllegalStateException("Script file not found: " + scriptFile);
            }
            scriptContent = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public String getScriptId() {
        return scriptId;
    }
}
