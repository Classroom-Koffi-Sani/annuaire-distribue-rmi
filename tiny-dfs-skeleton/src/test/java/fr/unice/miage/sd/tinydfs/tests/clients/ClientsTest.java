package fr.unice.miage.sd.tinydfs.tests.clients;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import junitx.framework.FileAssert;
import fr.unice.miage.sd.tinydfs.nodes.Master;
import fr.unice.miage.sd.tinydfs.tests.config.Constants;

public class ClientsTest {

    private static String storageServiceName;
    private static String registryHost;
    private static Master master;
    private static long testStartTime;

    @BeforeClass
    /**
     * Reads the properties and sets up the master.
     */
    public static void setUp() {
        testStartTime = System.currentTimeMillis();
        Properties prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream(ClientsTest.class.getResource(
                    Constants.PROPERTIES_FILE_PATH).getFile());
            prop.load(input);
            storageServiceName = prop.getProperty(
                    Constants.SERVICE_NAME_PROPERTY_KEY);
            registryHost = prop.getProperty(
                    Constants.REGISTRY_HOST_PROPERTY_KEY);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            Registry registry = LocateRegistry.getRegistry(
                    registryHost, Registry.REGISTRY_PORT);
            master = (Master) registry.lookup(storageServiceName);
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
            System.err.println("[ClientsTest] No master found, exiting.");
            System.exit(1);
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    /**
     * Saves bytes from a file through the master, retrieves them, and checks
     * that the final and original files are equal.
     */
    public void binaryClientTest() {
        File expectedFile = new File(this.getClass().getResource(
                Constants.BINARY_SAMPLE_FILE_PATH).getFile());
        Path path = Paths.get(this.getClass().getResource(
                Constants.BINARY_SAMPLE_FILE_PATH).getFile());
        BufferedOutputStream bos = null;
        File retrievedFile = null;

        try {
            byte[] data = Files.readAllBytes(path);
            master.saveBytes(Constants.BINARY_SAMPLE_FILE_NAME, data);
            byte[] retrievedData = master.retrieveBytes(
                    Constants.BINARY_SAMPLE_FILE_NAME);
            retrievedFile = new File(master.getDfsRootFolder() + "/"
                    + Constants.BINARY_SAMPLE_FILE_NAME);
            retrievedFile.createNewFile();
            bos = new BufferedOutputStream(new FileOutputStream(retrievedFile));
            bos.write(retrievedData);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        FileAssert.assertBinaryEquals(expectedFile, retrievedFile);
    }

    @Test
    /**
     * Saves a file through the master, retrieves it, and checks that the final
     * and original files are equal.
     */
    public void textualClientTest() {
        try {
            File expectedFile = new File(this.getClass().getResource(
                    Constants.TEXTUAL_SAMPLE_FILE_PATH).getFile());
            master.saveFile(expectedFile);
            File retrievedFile = master.retrieveFile(
                    Constants.TEXTUAL_SAMPLE_FILE_NAME);
            retrievedFile.createNewFile();
            FileAssert.assertBinaryEquals(expectedFile, retrievedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterClass
    /**
     * Checks that the produced files are more recent than the test starting
     * time.
     */
    public static void tearDown() {
        try {
            String dfsRootFolderPath = master.getDfsRootFolder();
            File dfsRootFolder = new File(dfsRootFolderPath);
            File[] folderFiles = dfsRootFolder.listFiles();
            int expectedNbFiles = master.getNbSlaves() * 2 + 2;
            Assert.assertTrue(folderFiles.length == expectedNbFiles);;
            for (File file : folderFiles) {
                Assert.assertTrue(file.lastModified() > testStartTime);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
