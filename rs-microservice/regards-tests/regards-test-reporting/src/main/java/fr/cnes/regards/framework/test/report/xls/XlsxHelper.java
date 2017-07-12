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
package fr.cnes.regards.framework.test.report.xls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
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
     * @param pFilePath
     *            file path
     * @param pRequirements
     *            list of requirements
     * @param pSheetName
     *            sheet name
     * @throws ReportException
     *             If report cannot be created
     */
    public static void write(Path pFilePath, XmlRequirements pRequirements, String pSheetName) throws ReportException {
        // Validate
        assertNotNull(pFilePath, "Missing file path");
        assertNotNull(pRequirements, NO_REQUIREMENT_FOUND);
        assertNotNull(pSheetName, MISSING_SHEET_NAME);

        if (pRequirements.getRequirements() != null) {

            Workbook wb;
            try {
                if (Files.exists(pFilePath)) {
                    wb = readFile(pFilePath);
                } else {
                    wb = new HSSFWorkbook();
                }
            } catch (IOException e) {
                final String message = "Error while reading XLSX file";
                LOG.error(message, e);
                throw new ReportException(message);
            }

            try (OutputStream out = Files.newOutputStream(pFilePath)) {

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
                wb.write(out);
                wb.close();
            } catch (IOException e) {
                final String message = "Error while writing XLSX file";
                LOG.error(message, e);
                throw new ReportException(message);
            }
        }
    }

    /**
     * Read an existing file
     *
     * @param pFilePath the file to read
     * @return an {@link HSSFWorkbook} representing the file
     * @throws IOException
     *             if problem occurs
     */
    private static HSSFWorkbook readFile(Path pFilePath) throws IOException {
        try (InputStream in = Files.newInputStream(pFilePath)) {
            return new HSSFWorkbook(in);
        }
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
