/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.microservices.core.test.report.plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import fr.cnes.regards.microservices.core.test.report.exception.ReportException;
import fr.cnes.regards.microservices.core.test.report.xls.XlsxHelper;
import fr.cnes.regards.microservices.core.test.report.xml.XmlHelper;
import fr.cnes.regards.microservices.core.test.report.xml.XmlRequirement;
import fr.cnes.regards.microservices.core.test.report.xml.XmlRequirements;

/**
 * Scan all microservices file tree to retrieve all requirement reports and generate an aggregated XSLX report.
 *
 * @author msordi
 *
 */
@Mojo(name = "gen")
public class RequirementReportMojo extends AbstractMojo {

    @Parameter(property = "prefix", defaultValue = "RQMT-")
    private String prefix_;

    @Parameter(property = "basedir", defaultValue = ".")
    private String basedir_;

    @Parameter(property = "target", defaultValue = "./target/requirements.xlsx")
    private String target_;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        getLog().info("Generating requirement report.");
        XmlRequirements rqmts = aggregateReports(scanFileTree());
        try {
            Path targetPath = Paths.get(target_);
            if (!targetPath.getParent().toFile().exists()) {
                getLog().info("Creating target directory");
                targetPath.getParent().toFile().mkdirs();
            }
            XlsxHelper.write(targetPath, rqmts, "sheet");
        }
        catch (ReportException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    /**
     * Scan all file tree from base directory
     *
     * @return list of requirement reports path
     * @throws MojoExecutionException
     */
    private List<Path> scanFileTree() throws MojoExecutionException {
        try (Stream<Path> paths = Files.walk(Paths.get(basedir_))) {
            List<Path> targetPaths = paths.filter(path -> path.toFile().getName().startsWith(prefix_))
                    .collect(Collectors.toList());
            for (Path target : targetPaths) {
                getLog().info(target.toString());
            }
            return targetPaths;
        }
        catch (IOException e) {
            String message = "Error scanning file tree";
            getLog().error("Error scanning file tree", e);
            throw new MojoExecutionException(message);
        }
    }

    /**
     * Aggregate all reports in a single one
     *
     * @param pReports
     *            list of reports
     * @return aggregated report
     * @throws MojoExecutionException
     */
    private XmlRequirements aggregateReports(List<Path> pReports) throws MojoExecutionException {
        // Init aggregation map
        Map<String, XmlRequirement> rqmtMap = new HashMap<>();

        if (pReports != null) {
            for (Path reportPath : pReports) {
                try {
                    XmlRequirements tmp = XmlHelper.read(reportPath, XmlRequirements.class);
                    if ((tmp != null) && (tmp.getRequirements() != null)) {
                        for (XmlRequirement rqmt : tmp.getRequirements()) {
                            aggregateTests(rqmtMap, rqmt);
                        }
                    }
                }
                catch (ReportException e) {
                    throw new MojoExecutionException(e.getMessage());
                }
            }
        }

        // Compute result
        XmlRequirements rqmts = new XmlRequirements();
        for (XmlRequirement rqmt : rqmtMap.values()) {
            rqmts.addRequirement(rqmt);
        }
        return rqmts;
    }

    /**
     * Aggregate identical requirement tests in a single wrapper
     *
     * @param pRqmtMap
     *            working map
     * @param pXmlRequirement
     *            requirement to aggregate
     */
    private void aggregateTests(Map<String, XmlRequirement> pRqmtMap, XmlRequirement pXmlRequirement) {
        XmlRequirement rqmt = pRqmtMap.get(pXmlRequirement.getRequirement());
        if (rqmt == null) {
            pRqmtMap.put(pXmlRequirement.getRequirement(), pXmlRequirement);
        }
        else {
            rqmt.addAllTests(pXmlRequirement.getTests());
        }
    }
}
