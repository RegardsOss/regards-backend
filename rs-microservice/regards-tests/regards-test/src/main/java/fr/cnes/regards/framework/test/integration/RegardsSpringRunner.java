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
package fr.cnes.regards.framework.test.integration;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import fr.cnes.regards.framework.test.report.RequirementMatrixReportListener;

/**
 * Custom spring runner to integrate custom reporter.<br/>
 * Allows to test listener locally.
 * @author msordi
 */
public final class RegardsSpringRunner extends SpringJUnit4ClassRunner {

    /**
     * @throws InitializationError If runner can't be initialized
     */
    public RegardsSpringRunner(Class<?> pClazz) throws InitializationError {
        super(pClazz);
    }

    @Override
    public void run(RunNotifier pNotifier) {
        pNotifier.addListener(new RequirementMatrixReportListener());
        super.run(pNotifier);
    }

}
