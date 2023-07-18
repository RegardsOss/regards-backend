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
package fr.cnes.regards.modules.ingest.service.job;

import fr.cnes.regards.modules.ingest.domain.request.InternalRequestState;
import fr.cnes.regards.modules.ingest.dto.request.SearchRequestParameters;
import org.junit.Assert;
import org.junit.Test;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Léo Mieulet
 */
public class RequestDeletionJobTest {

    @Test
    public void testValidRequestStatesWithIncluded() {
        // INCLUDED + empty list
        Collection<InternalRequestState> requestStateEmptyList = getValidRequestStateIncluded(Collections.emptyList());
        Assert.assertFalse("should not contain RUNNING", requestStateEmptyList.contains(InternalRequestState.RUNNING));
        Assert.assertEquals("should get all states except RUNNING",
                            InternalRequestState.values().length - 1,
                            requestStateEmptyList.size());

        // INCLUDED + random list
        List<InternalRequestState> randomList = List.of(InternalRequestState.ERROR, InternalRequestState.ABORTED);
        Collection<InternalRequestState> requestStateRandomList = getValidRequestStateIncluded(randomList);
        Assert.assertFalse("should not contain RUNNING", requestStateRandomList.contains(InternalRequestState.RUNNING));
        Assert.assertEquals("should get same list", randomList, requestStateRandomList);

        // INCLUDED + list including RUNNING
        Collection<InternalRequestState> requestStateListWithRunning = getValidRequestStateIncluded(List.of(
            InternalRequestState.RUNNING,
            InternalRequestState.ERROR,
            InternalRequestState.ABORTED));
        Assert.assertFalse("should not contain RUNNING",
                           requestStateListWithRunning.contains(InternalRequestState.RUNNING));
        Assert.assertEquals("should get the same list than randomList", randomList, requestStateListWithRunning);

    }

    @Test
    public void testValidRequestStatesWithExcluded() {
        // INCLUDED + empty list
        Collection<InternalRequestState> requestStateEmptyList = getValidRequestStateExcluded(Collections.emptyList());
        Assert.assertTrue("should contain RUNNING", requestStateEmptyList.contains(InternalRequestState.RUNNING));
        Assert.assertEquals("should contain only RUNNING", 1, requestStateEmptyList.size());

        // INCLUDED + list including RUNNING
        List<InternalRequestState> listContainingRunning = List.of(InternalRequestState.ERROR,
                                                                   InternalRequestState.ABORTED,
                                                                   InternalRequestState.RUNNING);
        Collection<InternalRequestState> requestStateListWithRunning = getValidRequestStateExcluded(
            listContainingRunning);
        Assert.assertTrue("should contain RUNNING", requestStateListWithRunning.contains(InternalRequestState.RUNNING));
        Assert.assertEquals("should get the same list", listContainingRunning, requestStateListWithRunning);

        // INCLUDED + random list
        Collection<InternalRequestState> requestStateRandomList = getValidRequestStateExcluded(List.of(
            InternalRequestState.ERROR,
            InternalRequestState.ABORTED));
        Assert.assertTrue("should contain RUNNING", requestStateRandomList.contains(InternalRequestState.RUNNING));
        Assert.assertEquals("should get same list than listContainingRunning",
                            listContainingRunning,
                            requestStateRandomList);

    }

    @Test
    public void testValidLastUpdateBefore_validDate() {
        OffsetDateTime threeWeeksAgo = OffsetDateTime.now().minusWeeks(3);
        Assert.assertEquals("date should not change when valid",
                            threeWeeksAgo,
                            RequestDeletionJob.getValidLastUpdateBefore(threeWeeksAgo));
    }

    @Test
    public void testValidLastUpdateBefore_invalidDate() {
        OffsetDateTime threeWeeksAgo = OffsetDateTime.now().plusWeeks(3);
        Assert.assertNotEquals("date should be changed as it cannot exceed now",
                               threeWeeksAgo,
                               RequestDeletionJob.getValidLastUpdateBefore(threeWeeksAgo));
    }

    @Test
    public void testValidLastUpdateBefore_nullDate() {
        Assert.assertNotNull("date should be changed as it cannot exceed now",
                             RequestDeletionJob.getValidLastUpdateBefore(null));
    }

    private Collection<InternalRequestState> getValidRequestStateIncluded(Collection<InternalRequestState> list) {
        return RequestDeletionJob.getValidRequestStates(new SearchRequestParameters().withRequestStatesIncluded(list)
                                                                                     .getRequestStates());
    }

    private Collection<InternalRequestState> getValidRequestStateExcluded(Collection<InternalRequestState> list) {
        return RequestDeletionJob.getValidRequestStates(new SearchRequestParameters().withRequestStatesExcluded(list)
                                                                                     .getRequestStates());
    }

}
