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
package fr.cnes.regards.modules.filecatalog.domain.request;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Julien Canches
 */
class FileDeletionRequestTest {

    @Test
    void lengthy_errors_are_truncated() {
        FileDeletionRequest req = new FileDeletionRequest();
        req.setErrorCause(StringUtils.repeat("a very long error", 2000)); // 2000*17=34000
        assertThat(req.getErrorCause()).hasSize(32768);
        assertThat(req.getErrorCause()).startsWith("a very long error");
    }

}
