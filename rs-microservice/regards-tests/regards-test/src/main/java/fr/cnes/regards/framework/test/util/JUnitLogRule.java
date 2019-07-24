/*
 * Copyright 2017-2019 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.framework.test.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit rule to log start and end of each tests.
 * @author Sébastien Binda
 */
public class JUnitLogRule implements TestRule {

    private static final Logger LOG = LoggerFactory.getLogger(JUnitLogRule.class);

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {

            @Override
            public void evaluate() throws Throwable {
                before(description);
                try {
                    base.evaluate();
                } finally {
                    after(description);
                }
            }

            private void after(Description description) {
                LOG.info("-----------------------------");
                LOG.info("");
                LOG.info("END - {}", description);
                LOG.info("");
                LOG.info("-----------------------------");
            }

            private void before(Description description) {
                LOG.info("-----------------------------");
                LOG.info("");
                LOG.info("START - {}", description);
                LOG.info("");
                LOG.info("-----------------------------");
            }
        };
    }

}
