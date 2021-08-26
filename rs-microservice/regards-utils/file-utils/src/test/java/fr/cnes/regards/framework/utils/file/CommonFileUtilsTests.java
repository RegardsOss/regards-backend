package fr.cnes.regards.framework.utils.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Sylvain VISSIERE-GUERINET
 */
public class CommonFileUtilsTests {

    @Test
    public void testGetAvailableFileNameWithExtension() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target", "CommonFileUtilsTests", "testGetAvailableFileNameWithExtension");
        String fileName = "toto.titi";
        Path alreadyExistingFile = dirWithFile.resolve(fileName);
        Files.createDirectories(dirWithFile);
        Files.createFile(alreadyExistingFile);
        String availableFileName = CommonFileUtils.getAvailableFileName(dirWithFile, fileName);
        Assert.assertEquals(
                fileName + " already exist so next available file name for " + fileName + " should be toto_1.titi",
                "toto_1.titi",
                availableFileName);
    }

    @Test
    public void testGetAvailableFileNameWithExtensionAndDotInMiddle() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target", "CommonFileUtilsTests", "testGetAvailableFileNameWithExtensionAndDotInMiddle");
        String fileName = "to.to.titi";
        Path alreadyExistingFile = dirWithFile.resolve(fileName);
        Files.createDirectories(dirWithFile);
        Files.createFile(alreadyExistingFile);
        String availableFileName = CommonFileUtils.getAvailableFileName(dirWithFile, fileName);
        Assert.assertEquals(
                fileName + " already exist so next available file name for " + fileName + " should be to.to_1.titi",
                "to.to_1.titi",
                availableFileName);
    }

    @Test
    public void testGetAvailableFileNameWithExtensionStartsWithDot() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths
                .get("target", "CommonFileUtilsTests", "testGetAvailableFileNameWithExtensionStartsWithDot");
        String fileName = ".toto.titi";
        Path alreadyExistingFile = dirWithFile.resolve(fileName);
        Files.createDirectories(dirWithFile);
        Files.createFile(alreadyExistingFile);
        String availableFileName = CommonFileUtils.getAvailableFileName(dirWithFile, fileName);
        Assert.assertEquals(
                fileName + " already exist so next available file name for " + fileName + " should be .toto_1.titi",
                ".toto_1.titi",
                availableFileName);
    }

    @Test
    public void testGetAvailableFileNameWithoutExtension() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths.get("target", "CommonFileUtilsTests", "testGetAvailableFileNameWithoutExtension");
        String fileName = "toto";
        Path alreadyExistingFile = dirWithFile.resolve(fileName);
        Files.createDirectories(dirWithFile);
        Files.createFile(alreadyExistingFile);
        String availableFileName = CommonFileUtils.getAvailableFileName(dirWithFile, fileName);
        Assert.assertEquals(
                fileName + " already exist so next available file name for " + fileName + " should be toto_1",
                "toto_1",
                availableFileName);
    }

    @Test
    public void testGetAvailableFileNameWithoutExtensionStartsWithDot() throws IOException {
        // create new file in target for walk
        Path dirWithFile = Paths
                .get("target", "CommonFileUtilsTests", "testGetAvailableFileNameWithoutExtensionStartsWithDot");
        String fileName = ".toto";
        Path alreadyExistingFile = dirWithFile.resolve(fileName);
        Files.createDirectories(dirWithFile);
        Files.createFile(alreadyExistingFile);
        String availableFileName = CommonFileUtils.getAvailableFileName(dirWithFile, fileName);
        Assert.assertEquals(
                fileName + " already exist so next available file name for " + fileName + " should be .toto_1",
                ".toto_1",
                availableFileName);
    }

}
