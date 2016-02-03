package org.synchronoss.cloud.nio.stream.storage;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class DeferredFileStreamStorageFactoryTest {

    private static final String TEMP_TEST_FOLDER_PATH = "temp_test_folder_path";

    @After
    public void cleanUp() {
        File tempFolder = new File(TEMP_TEST_FOLDER_PATH);
        if (tempFolder.exists())
            tempFolder.delete();
    }

    @Test
    public void testConstructor() {
        assertNotNull(new DeferredFileStreamStorageFactory(TEMP_TEST_FOLDER_PATH, 0));
        assertNotNull(new DeferredFileStreamStorageFactory(TEMP_TEST_FOLDER_PATH, 1));
        assertNotNull(new DeferredFileStreamStorageFactory(TEMP_TEST_FOLDER_PATH, -1));
        assertNotNull(new DeferredFileStreamStorageFactory(TEMP_TEST_FOLDER_PATH, Integer.MAX_VALUE));
        assertNotNull(new DeferredFileStreamStorageFactory(TEMP_TEST_FOLDER_PATH, Integer.MIN_VALUE));
        assertNotNull(new DeferredFileStreamStorageFactory(0));
        assertNotNull(new DeferredFileStreamStorageFactory(1));
        assertNotNull(new DeferredFileStreamStorageFactory(-1));
        assertNotNull(new DeferredFileStreamStorageFactory(TEMP_TEST_FOLDER_PATH));
        assertNotNull(new DeferredFileStreamStorageFactory());
    }

    @Test
    public void testConstructorGivenMax_error() {
        try {
            new DeferredFileStreamStorageFactory("", 1);
        } catch (Exception e) {
            assertConstructorException(e);
        }

        try {
            new DeferredFileStreamStorageFactory(" ");
        } catch (Exception e) {
            assertConstructorException(e);
        }
    }

    @Test
    public void testCreate() throws IOException {
        DeferredFileStreamStorageFactory deferredFileStreamStorageFactory = new DeferredFileStreamStorageFactory(TEMP_TEST_FOLDER_PATH, 2);
        StreamStorage streamStorage = deferredFileStreamStorageFactory.create();
        assertTrue(streamStorage instanceof DeferredFileStreamStorage);

        DeferredFileStreamStorage deferredFileStreamStorage = (DeferredFileStreamStorage) streamStorage;
        deferredFileStreamStorage.assertIsWritable();
        assertTrue(deferredFileStreamStorage.isInMemory());

        byte[] testByteArray = "This is a string".getBytes();
        deferredFileStreamStorage.write(testByteArray[0]);
        deferredFileStreamStorage.write(testByteArray[1]);
        assertTrue(deferredFileStreamStorage.isInMemory());

        deferredFileStreamStorage.write(testByteArray[2]);
        assertFalse(deferredFileStreamStorage.isInMemory());

        deferredFileStreamStorage.dispose();
    }

    private static void assertConstructorException(Exception e) {
        assertTrue(e instanceof IllegalStateException);
        assertEquals(e.getMessage(), "Unable to create the temporary folder: ");
    }
}