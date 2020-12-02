/* Copyright 2017-2020 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.processing.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TODO : Class description
 *
 * @author Guillaume Andrieu
 *
 */
@Configuration
public class ProcessingStorageConfiguration {

    @Value("${regards.processing.sharedStorage.basePath}")
    private String sharedStorageBasePath;

    @Value("${regards.processing.executionWorkdir.basePath}")
    private String executionWorkdirBasePath;

    @Bean(name = "executionWorkdirParentPath")
    public Path executionWorkdirParentPath() throws IOException {
        return Files.createDirectories(Paths.get(executionWorkdirBasePath));
    }

    @Bean(name = "sharedStorageBasePath")
    public Path sharedStorageBasePath() throws IOException {
        return Files.createDirectories(Paths.get(sharedStorageBasePath));
    }

}
