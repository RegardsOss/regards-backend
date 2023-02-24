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
package fr.cnes.regards.modules.dam.domain;

import fr.cnes.regards.framework.test.report.annotation.Purpose;
import fr.cnes.regards.modules.dam.domain.datasources.CrawlingCursor;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test for {@link CrawlingCursor}
 *
 * @author Iliana Ghazali
 **/
@Slf4j
class CrawlingCursorTest {

    @Test
    @Purpose("Test if all data are returned with all pages if data dont have a last update date")
    void crawl_page_without_date() {
        CrawlingCursor cursor = new CrawlingCursor(0, 2);
        List<DataObject> listData = initData(7, null, false);

        final int expectedPageNb = (int) Math.ceil((double) listData.size() / (double) cursor.getSize()) - 1;

        AtomicInteger iterationCounter = new AtomicInteger(0);
        List<DataObject> actualList = getAllElementsFromPage(listData, cursor, iterationCounter, false);

        Assertions.assertEquals(expectedPageNb,
                                iterationCounter.get(),
                                "Unexpected number of iterations performed by the crawling cursor");
        Assertions.assertEquals(listData,
                                actualList,
                                "Unexpected elements from pages returned, next() method from CrawledPage does not work with no date");
    }

    @Test
    @Purpose("Test if all data are returned if with all pages if data have the same last update date")
    void crawl_page_with_equals_date() {
        CrawlingCursor cursor = new CrawlingCursor(0, 2);
        OffsetDateTime referenceDate = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        List<DataObject> listData = initData(11, referenceDate, false);

        // with the current algorithm the same data are returned on page 0 and 1 when dates are equal. Hence, we expect cursor.getPageSize() more data.
        final int expectedNbData = listData.size() + cursor.getSize();
        final int expectedPosition = (int) Math.ceil((double) expectedNbData / (double) cursor.getSize()) - 1;

        AtomicInteger iterationCounter = new AtomicInteger(0);
        List<DataObject> actualList = getAllElementsFromPage(listData, cursor, iterationCounter, true);
        Assertions.assertEquals(expectedPosition,
                                iterationCounter.get(),
                                "Unexpected number of iterations performed by the crawling cursor");
        Assertions.assertEquals(expectedNbData,
                                actualList.size(),
                                "Unexpected number elements from pages returned next(). Method from CrawledPage does not work with equal dates.");
    }

    @Test
    @Purpose("Test if all data are returned with all pages if data have different last update dates")
    void crawl_page_with_different_date() {
        CrawlingCursor cursor = new CrawlingCursor(0, 2);
        OffsetDateTime referenceDate = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        List<DataObject> listData = initData(12, referenceDate, true);

        // with the current algorithm the last element of a page is always processed first in the next page.
        // Hence, we expect twice as much data minus the first and last processed items of all pages
        final int expectedNbData = listData.size() * 2 - cursor.getSize();
        final int expectedPosition = ((int) Math.ceil((double) expectedNbData / (double) cursor.getSize())) - 1;
        AtomicInteger iterationCounter = new AtomicInteger(0);

        List<DataObject> actualList = getAllElementsFromPage(listData, cursor, iterationCounter, true);
        Assertions.assertEquals(expectedPosition,
                                iterationCounter.get(),
                                "Unexpected number of iterations performed by the crawling cursor");
        Assertions.assertEquals(expectedNbData,
                                actualList.size(),
                                "Unexpected number elements from pages returned next(). Method from CrawledPage does not work with different dates.");
    }

    @Test
    @Purpose(
        "Test if all data are returned with all pages if some data have the same last update dates and some have different")
    void crawl_page_with_mixed_date() {
        CrawlingCursor cursor = new CrawlingCursor(0, 2);
        OffsetDateTime referenceDate = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);

        List<DataObject> listData = initData(11, referenceDate, true);
        listData.add(6, new DataObject("5-1", "label05-1", "cnes", listData.get(5).lastUpdateDate()));
        listData.add(7, new DataObject("5-2", "label05-2", "cnes", listData.get(5).lastUpdateDate()));
        listData.add(8, new DataObject("5-3", "label05-3", "cnes", listData.get(5).lastUpdateDate()));

        // see comments on tests equals_date and different_dates to understand the expectedNbData
        final int expectedNbData = listData.size() * 2 - 3 * cursor.getSize();
        final int expectedPosition = ((int) Math.ceil((double) expectedNbData / (double) cursor.getSize())) - 1;
        AtomicInteger iterationCounter = new AtomicInteger(0);

        List<DataObject> actualList = getAllElementsFromPage(listData, cursor, iterationCounter, true);
        Assertions.assertEquals(expectedPosition,
                                iterationCounter.get(),
                                "Unexpected number of iterations performed by the crawling cursor");
        Assertions.assertEquals(expectedNbData,
                                actualList.size(),
                                "Unexpected elements from pages returned, next() method from CrawledPage does not work with date equals");
    }

    @Test
    @Purpose("Test if overlap is properly applied once for a given starting ingestion date")
    void crawl_page_with_overlap() {

        // Init data
        OffsetDateTime referenceDate = OffsetDateTime.of(2020, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        List<DataObject> listData = initData(3, referenceDate, true);

        // Crawling from scratch : 3 elements returned
        CrawlingCursor cursor = simulateDatabaseNextCursor(null, false);
        List<DataObject> results = doCrawlWithOverlap(listData, cursor, "iteration 0 (from scratch)", true);
        Assert.assertEquals(3, results.size());
        Assert.assertNull(cursor.getPreviousLastEntityDate());
        Assert.assertNotNull(cursor.getLastEntityDate());

        // Crawling another time with same data : last element is returned another time
        cursor = simulateDatabaseNextCursor(cursor, results.isEmpty());
        results = doCrawlWithOverlap(listData, cursor, "iteration 1 (overlap)");
        Assert.assertEquals(1, results.size());
        Assert.assertNotNull(cursor.getPreviousLastEntityDate());
        Assert.assertNotNull(cursor.getLastEntityDate());

        // Crawling another time with same data : no data
        cursor = simulateDatabaseNextCursor(cursor, results.isEmpty());
        results = doCrawlWithOverlap(listData, cursor, "iteration 2 (no data)");
        Assert.assertTrue(results.isEmpty());
        Assert.assertNotNull(cursor.getPreviousLastEntityDate());
        Assert.assertNotNull(cursor.getLastEntityDate());

        // Crawling another time with same data : no data
        cursor = simulateDatabaseNextCursor(cursor, results.isEmpty());
        results = doCrawlWithOverlap(listData, cursor, "iteration 3 (no data)");
        Assert.assertTrue(results.isEmpty());
        Assert.assertNotNull(cursor.getPreviousLastEntityDate());
        Assert.assertNotNull(cursor.getLastEntityDate());

        // New data arrive newReferenceDate > referenceDate
        OffsetDateTime newReferenceDate = OffsetDateTime.of(2021, 1, 1, 1, 1, 1, 1, ZoneOffset.UTC);
        listData.addAll(initData(5, newReferenceDate, true));

        // Crawling with new data : 5 elements returned
        cursor = simulateDatabaseNextCursor(cursor, results.isEmpty());
        results = doCrawlWithOverlap(listData, cursor, "iteration 4 (new data)");
        Assert.assertEquals(5, results.size());
        Assert.assertNotNull(cursor.getPreviousLastEntityDate());
        Assert.assertNotNull(cursor.getLastEntityDate());

        // Crawling another time with same data : last element is returned another time
        cursor = simulateDatabaseNextCursor(cursor, results.isEmpty());
        results = doCrawlWithOverlap(listData, cursor, "iteration 5 (overlap)");
        Assert.assertEquals(1, results.size());
        Assert.assertNotNull(cursor.getPreviousLastEntityDate());
        Assert.assertNotNull(cursor.getLastEntityDate());

        // Crawling another time with same data : no data
        cursor = simulateDatabaseNextCursor(cursor, results.isEmpty());
        results = doCrawlWithOverlap(listData, cursor, "iteration 6 (no data)");
        Assert.assertTrue(results.isEmpty());
        Assert.assertNotNull(cursor.getPreviousLastEntityDate());
        Assert.assertNotNull(cursor.getLastEntityDate());
    }

    private CrawlingCursor simulateDatabaseNextCursor(CrawlingCursor previousCrawlingCursor, boolean noData) {
        CrawlingCursor nextCursor = new CrawlingCursor(0, 10);
        if (previousCrawlingCursor == null) {
            // No previous cursor, harvesting is starting from scratch
            return nextCursor;
        } else {
            // Propagate previous date, previous date is only mutated in overlap case so result cannot be empty
            // and database is always updated
            // See {@link CrawlingCursor#tryApplyOverlap(long)}
            nextCursor.setPreviousLastEntityDate(previousCrawlingCursor.getPreviousLastEntityDate());
            if (noData) {
                // If no data found, last entity date is not updated in database
                // Reset last date to unchanged previous one
                nextCursor.setLastEntityDate(previousCrawlingCursor.getPreviousLastEntityDate());
            } else {
                // Update date from previous cursor one
                nextCursor.setLastEntityDate(previousCrawlingCursor.getLastEntityDate());
            }
        }
        return nextCursor;
    }

    private List<DataObject> doCrawlWithOverlap(List<DataObject> listData, CrawlingCursor cursor, String message) {
        return this.doCrawlWithOverlap(listData, cursor, message, false);
    }

    private List<DataObject> doCrawlWithOverlap(List<DataObject> listData,
                                                CrawlingCursor cursor,
                                                String message,
                                                boolean start) {
        log.info("Cursor start {} : {}", message, cursor);
        AtomicInteger iterationCounter = new AtomicInteger(0);
        // Simulate applying overlap
        if (!start) {
            cursor.tryApplyOverlap(30);
        }
        List<DataObject> iterationList = getAllElementsFromPage(listData, cursor, iterationCounter, true);
        // Simulate crawling manager
        if (!iterationList.isEmpty()) {
            cursor.setLastEntityDate(cursor.getCurrentLastEntityDate());
        }
        log.info("Cursor end {} : {}", message, cursor);
        return iterationList;
    }

    private List<DataObject> initData(int numElements, OffsetDateTime referenceDate, boolean differentDates) {
        List<DataObject> listData = new ArrayList<>(numElements);
        for (int i = 0; i < numElements; i++) {
            if (referenceDate != null && differentDates) {
                referenceDate = referenceDate.plusMinutes(i);
            }
            listData.add(new DataObject(Integer.toString(i), "label" + i, "cnes", referenceDate));
        }
        return listData;
    }

    private List<DataObject> getAllElementsFromPage(List<DataObject> listData,
                                                    CrawlingCursor cursor,
                                                    AtomicInteger iterationCounter,
                                                    boolean lastUpdateDatePresent) {
        listData.sort(Comparator.comparing(DataObject::lastUpdateDate,
                                           Comparator.nullsLast(Comparator.naturalOrder())));

        List<DataObject> allElements = new ArrayList<>(getPage(listData, cursor, lastUpdateDatePresent));

        while (cursor.hasNext()) {
            cursor.next();
            allElements.addAll(getPage(listData, cursor, lastUpdateDatePresent));
            iterationCounter.incrementAndGet();
        }

        return allElements;
    }

    /**
     * Algorithm to simulate the result of a requested page
     */
    public List<DataObject> getPage(List<DataObject> listData, CrawlingCursor cursor, boolean lastUpdateDatePresent) {
        // in order to get the page requested, simulate the indexes of sublist extracted from the list which contains all data
        int offset = (cursor.getPosition()) * cursor.getSize();
        int maxSublist = Math.min(offset + cursor.getSize(), listData.size());

        List<DataObject> subListElements;
        if (cursor.getLastEntityDate() != null) {
            subListElements = listData.stream()
                                      .filter(data -> (data.lastUpdateDate().isAfter(cursor.getLastEntityDate())
                                                       || data.lastUpdateDate().isEqual(cursor.getLastEntityDate())))
                                      .toList();

            subListElements = subListElements.subList(offset, Math.min(subListElements.size(), maxSublist));
        } else {
            subListElements = listData.subList(offset, maxSublist);
        }

        log.info("---------------------------------------------------------");
        log.info("Page number {}", cursor.getPosition());
        log.info("{} Elements returned : {}", subListElements.size(), subListElements);
        log.info("Crawling cursor {}", cursor);
        // SIMULATE computations from plugins
        // lastUpdateDate
        if (lastUpdateDatePresent) {
            cursor.setCurrentLastEntityDate(getMaxLastUpdateDate(subListElements));
        }
        // hasNext()
        cursor.setHasNext(subListElements.size() != 0 && (subListElements.get(subListElements.size() - 1)
                                                          != listData.get(listData.size() - 1)));

        return subListElements;
    }

    private OffsetDateTime getMaxLastUpdateDate(List<DataObject> subList) {
        return subList.stream()
                      .max(Comparator.comparing(DataObject::lastUpdateDate,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                      .map(DataObject::lastUpdateDate)
                      .orElse(null);
    }

    private record DataObject(String identifier,
                              String name,
                              String provider,
                              OffsetDateTime lastUpdateDate) {

    }

}
