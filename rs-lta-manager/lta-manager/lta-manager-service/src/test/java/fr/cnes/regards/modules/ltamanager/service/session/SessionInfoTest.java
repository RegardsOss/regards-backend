/*
 * Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.ltamanager.service.session;

import fr.cnes.regards.modules.ltamanager.dto.submission.input.SubmissionRequestState;
import fr.cnes.regards.modules.ltamanager.dto.submission.session.SessionStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

/**
 * @author Thomas GUILLOU
 **/
public class SessionInfoTest {

    @Test
    public void testSessionStatusDone() {
        List<SubmissionRequestState> states = Arrays.asList(SubmissionRequestState.DONE,
                                                            SubmissionRequestState.DONE,
                                                            SubmissionRequestState.DONE);
        Assertions.assertEquals(SessionStatus.DONE, SessionInfoUtils.getSessionStatus(states));
    }

    @Test
    public void testSessionStatusRunning() {
        List<SubmissionRequestState> states = Arrays.asList(SubmissionRequestState.DONE,
                                                            SubmissionRequestState.GENERATION_PENDING,
                                                            SubmissionRequestState.DONE);
        Assertions.assertEquals(SessionStatus.RUNNING, SessionInfoUtils.getSessionStatus(states));
    }

    @Test
    public void testSessionStatusRunning2() {
        List<SubmissionRequestState> states = Arrays.asList(SubmissionRequestState.DONE,
                                                            SubmissionRequestState.INGESTION_PENDING,
                                                            SubmissionRequestState.DONE);
        Assertions.assertEquals(SessionStatus.RUNNING, SessionInfoUtils.getSessionStatus(states));
    }

    @Test
    public void testSessionStatusRunning3() {
        List<SubmissionRequestState> states = Arrays.asList(SubmissionRequestState.DONE,
                                                            SubmissionRequestState.GENERATED,
                                                            SubmissionRequestState.DONE);
        Assertions.assertEquals(SessionStatus.RUNNING, SessionInfoUtils.getSessionStatus(states));
    }

    @Test
    public void testSessionStatusError() {
        List<SubmissionRequestState> states = Arrays.asList(SubmissionRequestState.DONE,
                                                            SubmissionRequestState.INGESTION_ERROR,
                                                            SubmissionRequestState.DONE,
                                                            SubmissionRequestState.GENERATION_PENDING);
        Assertions.assertEquals(SessionStatus.ERROR, SessionInfoUtils.getSessionStatus(states));
    }

    @Test
    public void testSessionStatusError2() {
        List<SubmissionRequestState> states = Arrays.asList(SubmissionRequestState.DONE,
                                                            SubmissionRequestState.GENERATION_ERROR,
                                                            SubmissionRequestState.DONE,
                                                            SubmissionRequestState.INGESTION_PENDING);
        Assertions.assertEquals(SessionStatus.ERROR, SessionInfoUtils.getSessionStatus(states));
    }
}
