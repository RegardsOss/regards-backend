/*
 * LICENSE_PLACEHOLDER
 */
package fr.cnes.regards.framework.test.report.xls;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.cnes.regards.framework.test.report.exception.ReportException;
import fr.cnes.regards.framework.test.report.xml.XmlRequirement;
import fr.cnes.regards.framework.test.report.xml.XmlRequirements;
import fr.cnes.regards.framework.test.report.xml.XmlTest;

/**
 *
 * Help to write xlsx report
 *
 * @author msordi
 *
 */
public final class XlsxHelper {

    /**
     * Class logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(XlsxHelper.class);

    /**
     * Error message
     */
    private static final String NO_REQUIREMENT_FOUND = "No requirements found";

    /**
     * Error message
     */
    private static final String MISSING_SHEET_NAME = "Missing sheet name";

    private XlsxHelper() {
    }

    /**
     * Write data to file with XLSX format
     *
     * @param pDirectory
     *            file directory
     * @param pFilename
     *            filename
     * @param pRequirements
     *            list of requirements
     * @param pSheetName
     *            sheet name
     * @throws ReportException
     *             If report cannot be created
     */
    public static void write(Path pDirectory, String pFilename, XmlRequirements pRequirements, String pSheetName)
            throws ReportException {
        // Validate
        assertNotNull(pDirectory, "Missing directory path");
        assertNotNull(pFilename, "Missing filename");
        assertNotNull(pRequirements, NO_REQUIREMENT_FOUND);
        assertNotNull(pSheetName, MISSING_SHEET_NAME);

        if (pRequirements.getRequirements() != null) {

            try (Workbook wb = new HSSFWorkbook();
                    FileOutputStream fileOut = new FileOutputStream(pDirectory.resolve(pFilename).toFile())) {

                final CreationHelper createHelper = wb.getCreationHelper();

                // Create sheet
                final String safeName = WorkbookUtil.createSafeSheetName(pSheetName);
                final Sheet sheet = wb.createSheet(safeName);

                // Init requirement style
                final CellStyle style = wb.createCellStyle();
                style.setFillBackgroundColor(IndexedColors.AQUA.getIndex());
                style.setFillPattern(FillPatternType.BIG_SPOTS);

                int rownum = 0;
                for (XmlRequirement req : pRequirements.getRequirements()) {
                    // Write requirement row
                    final Row row = sheet.createRow(rownum);
                    final Cell cell = row.createCell(0);
                    cell.setCellValue(createHelper.createRichTextString(req.getRequirement()));
                    cell.setCellStyle(style);
                    sheet.addMergedRegion(new CellRangeAddress(rownum, rownum, 0, 2));
                    rownum++;

                    // Write tests
                    if (req.getTests() != null) {
                        for (XmlTest test : req.getTests()) {
                            final Row testRow = sheet.createRow(rownum++);
                            testRow.createCell(0).setCellValue(createHelper.createRichTextString(test.getPurpose()));
                            testRow.createCell(1).setCellValue(createHelper.createRichTextString(test.getTestClass()));
                            testRow.createCell(2)
                                    .setCellValue(createHelper.createRichTextString(test.getTestMethodName()));
                        }
                    } else {
                        LOG.error("No test found for requirement " + req.getRequirement());
                    }
                }

                // Write file
                wb.write(fileOut);

            } catch (IOException e) {
                final String message = "Error while writing XLSX file";
                LOG.error(message, e);
                throw new ReportException(message);
            }
        }
    }

    /**
     * Write data to file with XLSX format
     *
     * @param pFilePath
     *            file path
     * @param pRequirements
     *            list of requirements
     * @param pSheetName
     *            sheet name
     * @throws ReportException
     *             if report cannot be write
     */
    public static void write(Path pFilePath, XmlRequirements pRequirements, String pSheetName) throws ReportException {
        // Validate
        assertNotNull(pFilePath, "Missing full file path");
        assertNotNull(pRequirements, NO_REQUIREMENT_FOUND);
        assertNotNull(pSheetName, MISSING_SHEET_NAME);
        write(pFilePath.getParent(), pFilePath.getFileName().toString(), pRequirements, pSheetName);
    }

    /**
     * Check if object is not null
     *
     * @param pObject
     *            objet to check
     * @param pMessage
     *            error message
     * @throws ReportException
     *             if a report parameter is null
     */
    private static void assertNotNull(Object pObject, String pMessage) throws ReportException {
        if (pObject == null) {
            LOG.error(pMessage);
            throw new ReportException(pMessage);
        }
    }
}
