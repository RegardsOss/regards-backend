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
package fr.cnes.regards.framework.staf;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import fr.cnes.regards.framework.staf.domain.ArchiveAccessModeEnum;
import fr.cnes.regards.framework.staf.domain.STAFConfiguration;

/**
 * @author oroussel
 */
@RunWith(JUnit4.class)
public class STAFSessionManagerTest {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    private STAFSessionManager sessionMgr;

    @Before
    public void init() {
        STAFConfiguration config = new STAFConfiguration();
        config.setMaxSessionsArchivingMode(3);
        config.setMaxSessionsRestitutionMode(3);
        sessionMgr = STAFSessionManager.getInstance(config);
    }

    public void reset() throws InterruptedException {
        sessionMgr.releaseAllCurrentlyBlockingReservations();
    }

    @Test
    public void test1() throws InterruptedException, ExecutionException {
        reset();
        Integer sessionId = sessionMgr.getReservation(ArchiveAccessModeEnum.ARCHIVE_MODE);
        Assert.assertNotNull(sessionId);
        sessionId = sessionMgr.getReservation(ArchiveAccessModeEnum.ARCHIVE_MODE);
        Assert.assertNotNull(sessionId);
        sessionId = sessionMgr.getReservation(ArchiveAccessModeEnum.ARCHIVE_MODE);
        Assert.assertNotNull(sessionId);

        // Ask for a reservation in blocking mode => block
        Future<Integer> future = executor.submit(() -> sessionMgr.getReservation(ArchiveAccessModeEnum.ARCHIVE_MODE));
        try {
            sessionId = future.get(1, TimeUnit.SECONDS);
            Assert.fail("Timeout should have been thrown");
        } catch (TimeoutException e) {
        }
        // Releasing currently blocking (also could have called freeReservation on all reservations)
        reset();
        // To wait until its end
        sessionId = future.get();
    }

    @Test
    public void test3() throws ExecutionException, InterruptedException {
        reset();
        // Ask for 4 session but only 3 are available
        List<Integer> ids = sessionMgr.getReservations(4, true, ArchiveAccessModeEnum.ARCHIVE_MODE);
        Assert.assertEquals(3, ids.size());

        // Ask for 1 session but none are available and not blocking
        ids = sessionMgr.getReservations(1, false, ArchiveAccessModeEnum.ARCHIVE_MODE);
        Assert.assertEquals(0, ids.size());

        // Ask for 1 session but none are available and blocking
        Future<List<Integer>> future = executor
                .submit(() -> sessionMgr.getReservations(1, true, ArchiveAccessModeEnum.ARCHIVE_MODE));
        try {
            ids = future.get(1, TimeUnit.SECONDS);
            Assert.fail("Timeout should have been thrown");
        } catch (TimeoutException e) {
        }
        // Releasing currently blocking (also could have called freeReservation on all reservations)
        reset();
        // to wait until its end
        ids = future.get();
    }

    @Test
    public void test4() throws ExecutionException, InterruptedException {
        reset();
        // Ask for 4 session but only 3 are available
        List<Integer> ids = sessionMgr.getReservations(4, true, ArchiveAccessModeEnum.ARCHIVE_MODE);
        Assert.assertEquals(3, ids.size());

        // Ask for 1 session but none are available and not blocking
        ids = sessionMgr.getReservations(1, false, ArchiveAccessModeEnum.ARCHIVE_MODE);
        Assert.assertEquals(0, ids.size());

        // Ask for 1 session but none are available and blocking
        Future<List<Integer>> future = executor
                .submit(() -> sessionMgr.getReservations(1, true, ArchiveAccessModeEnum.ARCHIVE_MODE));
        // future is now blocking if asked (cf. test3)...

        // ... So free 1 reservation
        sessionMgr.freeReservation(1, ArchiveAccessModeEnum.ARCHIVE_MODE);
        // Check that previous asked reservation is now available
        ids = future.get();
        Assert.assertEquals(1, ids.size());

    }
}
