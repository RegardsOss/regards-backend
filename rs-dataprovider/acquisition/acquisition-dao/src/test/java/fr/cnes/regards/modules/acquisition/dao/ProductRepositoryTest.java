/*
 * Copyright 2017-2018 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
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
package fr.cnes.regards.modules.acquisition.dao;

import java.util.List;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import com.google.common.collect.Sets;

import fr.cnes.regards.framework.jpa.multitenant.test.AbstractDaoTest;
import fr.cnes.regards.framework.multitenant.IRuntimeTenantResolver;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductSIPState;
import fr.cnes.regards.modules.acquisition.domain.chain.AcquisitionProcessingChain;

/**
 * Test complex queries
 * @author Marc Sordi
 *
 */
@Ignore("Development testing for complex queries")
// @TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=jason2idgr")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.default_schema=acquisition_it")
public class ProductRepositoryTest extends AbstractDaoTest {

    @Autowired
    private IProductRepository productRepository;

    @Autowired
    private IRuntimeTenantResolver runtimeTenantResolver;

    @Autowired
    private IAcquisitionProcessingChainRepository processingChainRepository;

    @Test
    public void test() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        List<Product> products = productRepository.findAll();
        Assert.assertNotNull(products);
        Assert.assertTrue(!products.isEmpty());

        Page<Product> productByState = productRepository
                .findByProcessingChainIngestChainAndSessionAndSipState("DefaultIngestChain", "NO_SESSION",
                                                                       ProductSIPState.SUBMITTED,
                                                                       new PageRequest(0, 10));
        Assert.assertNotNull(productByState);

    }

    @Test
    public void jobReportTest() {
        runtimeTenantResolver.forceTenant(getDefaultTenant());

        AcquisitionProcessingChain chain = processingChainRepository.findCompleteById(1L);
        Assert.assertNotNull(chain);

        // AcquisitionJobReport report = new AcquisitionJobReport();
        // report.setScheduleDate(OffsetDateTime.now());
        // jobReportRepository.save(report);
        //
        // chain.setLastProductAcquisitionJobReport(report);
        // processingChainRepository.save(chain);

        // SIP submission report
        // String session = "SESS1";
        // String session = null;

        // First step : remove existing report if any
        // AcquisitionJobReport reportToRemove = null;
        // for (AcquisitionJobReport report : chain.getLastSIPSubmissionJobReports()) {
        // // Manage null session
        // if (((session == null) && (report.getSession() == null))
        // || ((session != null) && session.equals(report.getSession()))) {
        // reportToRemove = report;
        // break;
        // }
        // }
        // if (reportToRemove != null) {
        // chain.getLastSIPSubmissionJobReports().remove(reportToRemove);
        // // processingChainRepository.save(chain);
        // }
        //
        // // Second step : add new report
        // AcquisitionJobReport newReport = new AcquisitionJobReport();
        // newReport.setScheduleDate(OffsetDateTime.now());
        // newReport.setStartDate(OffsetDateTime.now());
        // newReport.setSession(session);
        // jobReportRepository.save(newReport);
        // chain.getLastSIPSubmissionJobReports().add(newReport);
        //
        // processingChainRepository.save(chain);

        // AcquisitionJobReport newReport2 = new AcquisitionJobReport();
        // newReport2.setScheduleDate(OffsetDateTime.now());
        // newReport2.setStartDate(OffsetDateTime.now());
        // newReport2.setSession(session);
        // jobReportRepository.save(newReport2);
        // chain.getLastSIPSubmissionJobReports().add(newReport2);

        // Clean reports
        // chain.getLastSIPSubmissionJobReports().r
        // chain.getLastSIPSubmissionJobReports().
        // processingChainRepository.save(chain);

        // newReport = new AcquisitionJobReport();
        // newReport.setScheduleDate(OffsetDateTime.now());
        // newReport.setStartDate(OffsetDateTime.now());
        // jobReportRepository.save(newReport);
        //
        // chain.getLastSIPSubmissionJobReports().add(newReport);
        // processingChainRepository.save(chain);
    }
}
