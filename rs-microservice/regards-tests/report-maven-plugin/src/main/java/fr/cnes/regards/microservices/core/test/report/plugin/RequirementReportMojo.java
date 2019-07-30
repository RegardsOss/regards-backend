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
package fr.cnes.regards.microservices.core.test.report.plugin;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import fr.cnes.regards.framework.test.report.exception.ReportException;
import fr.cnes.regards.framework.test.report.xls.XlsxHelper;
import fr.cnes.regards.framework.test.report.xml.XmlHelper;
import fr.cnes.regards.framework.test.report.xml.XmlRequirements;

/**
 * Scan all microservices file tree to retrieve all requirement reports and generate an aggregated XSLX report.
 * @author msordi
 */
@Mojo(name = "gen")
public class RequirementReportMojo extends AbstractMojo {

    /**
     * Prefix to detect all requirement reports
     */
    @Parameter(property = "prefix", defaultValue = "RQMT-")
    private String prefix;

    /**
     * Scan base directory
     */
    @Parameter(property = "basedir", defaultValue = ".")
    private String basedir;

    /**
     * Target aggregated file
     */
    @Parameter(property = "target", defaultValue = "./target/requirements.xlsx")
    private String target;

    @Override
    public void execute() throws MojoExecutionException {

        getLog().info("Generating requirement report.");

        try {
            XmlRequirements rqmts = XmlHelper.aggregateReports(Paths.get(basedir));
            Path targetPath = Paths.get(target);
            if (!targetPath.getParent().toFile().exists()) {
                getLog().info("Creating target directory");
                targetPath.getParent().toFile().mkdirs();
            }
            XlsxHelper.write(targetPath, rqmts, "sheet");
        } catch (ReportException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
