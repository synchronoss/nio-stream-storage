/*
 * Copyright 2015 Synchronoss Technologies
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.synchronoss.cloud.nio.stream.storage;


import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * <p> Unit test for {@link DeferredFileStreamStorage}
 *
 * @author Silvano Riz.
 */
public class DeferredFileStreamStorageTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testConstructor() throws IOException{
        assertNotNull(new DeferredFileStreamStorage(new File(tempFolder.getRoot(), "testConstructor1.tmp"), 100));
        assertNotNull(new DeferredFileStreamStorage(new File(tempFolder.getRoot(), "testConstructor2.tmp"), -1));
        assertNotNull(new DeferredFileStreamStorage(new File(tempFolder.getRoot(), "testConstructor3.tmp"), 0));
        assertNotNull(new DeferredFileStreamStorage(new File(tempFolder.getRoot(), "testConstructor4.tmp"), 100, false));
        assertNotNull(new DeferredFileStreamStorage(new File(tempFolder.getRoot(), "testConstructor5.tmp"), false));
        assertNotNull(new DeferredFileStreamStorage(new File(tempFolder.getRoot(), "testConstructor6.tmp")));
    }

    @Test
    public void testWrite() throws IOException {

        File file = new File(tempFolder.getRoot(), "testWrite.tmp");

        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertTrue(deferredFileStreamStorage.isInMemory());
        assertFalse(file.exists());
        assertEquals(0, deferredFileStreamStorage.byteArrayOutputStream.size());

        deferredFileStreamStorage.write(0x01);
        deferredFileStreamStorage.write(0x02);
        deferredFileStreamStorage.write(0x03);

        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(3, deferredFileStreamStorage.byteArrayOutputStream.size());

        deferredFileStreamStorage.write(0x04);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.DISK);
        assertTrue(file.exists());
        assertEquals(4, file.length());
        assertNull(deferredFileStreamStorage.byteArrayOutputStream);

    }

    @Test
    public void testWrite1() throws IOException {

        File file = new File(tempFolder.getRoot(), "testWrite1.tmp");

        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(0, deferredFileStreamStorage.byteArrayOutputStream.size());

        deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03});

        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(3, deferredFileStreamStorage.byteArrayOutputStream.size());

        deferredFileStreamStorage.write(new byte[]{0x04, 0x05, 0x06});
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.DISK);
        assertTrue(file.exists());
        assertEquals(6, file.length());
        assertNull(deferredFileStreamStorage.byteArrayOutputStream);

    }

    @Test
    public void testWrite2() throws IOException {

        File file = new File(tempFolder.getRoot(), "testWrite2.tmp");

        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(0, deferredFileStreamStorage.byteArrayOutputStream.size());

        deferredFileStreamStorage.write(new byte[]{0x00, 0x01, 0x02, 0x03, 0x00}, 1, 3);

        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(3, deferredFileStreamStorage.byteArrayOutputStream.size());

        deferredFileStreamStorage.write(new byte[]{0x00, 0x04, 0x05, 0x06, 0x00}, 1, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.DISK);
        assertTrue(file.exists());
        assertEquals(6, file.length());
        assertNull(deferredFileStreamStorage.byteArrayOutputStream);

    }

    @Test
    public void testGetInputStream_memory() throws IOException {

        File file = new File(tempFolder.getRoot(), "testGetInputStream_memory.tmp");

        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(0, deferredFileStreamStorage.byteArrayOutputStream.size());

        // Write just 3 bytes. Still in the threshold so data should leave in memory...
        deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03});

        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(3, deferredFileStreamStorage.byteArrayOutputStream.size());

        deferredFileStreamStorage.close();

        InputStream inputStream = deferredFileStreamStorage.getInputStream();
        assertNotNull(inputStream);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03}, IOUtils.toByteArray(inputStream));

    }

    @Test
    public void testGetInputStream_file_purgeOnClose() throws IOException {

        File file = new File(tempFolder.getRoot(), "testGetInputStream_file_purgeOnClose.tmp");

        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(0, deferredFileStreamStorage.byteArrayOutputStream.size());

        // Write 5 bytes (2 bytes more than the threshold). It should switch to use a file
        deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});

        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.DISK);
        assertTrue(file.exists());
        assertEquals(5, file.length());

        deferredFileStreamStorage.close();

        InputStream inputStream = deferredFileStreamStorage.getInputStream();
        assertNotNull(inputStream);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5}, IOUtils.toByteArray(inputStream));

        // On close the temp file should be deleted
        assertTrue(file.exists());
        IOUtils.closeQuietly(inputStream);
        assertFalse(file.exists());

    }

    @Test
    public void testGetInputStream_file() throws IOException {

        File file = new File(tempFolder.getRoot(), "testGetInputStream_file.tmp");

        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3, false);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(0, deferredFileStreamStorage.byteArrayOutputStream.size());

        // Write 5 bytes (2 bytes more than the threshold). It should switch to use a file
        deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});

        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.DISK);
        assertTrue(file.exists());
        assertEquals(5, file.length());

        deferredFileStreamStorage.close();

        InputStream inputStream = deferredFileStreamStorage.getInputStream();
        assertNotNull(inputStream);

        assertArrayEquals(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5}, IOUtils.toByteArray(inputStream));

        // On close the temp file should be deleted
        assertTrue(file.exists());
        IOUtils.closeQuietly(inputStream);
        assertTrue(file.exists());

    }

    @Test
    public void testGetInputStream_OutputStreamNotClosed() throws IOException {

        File file = new File(tempFolder.getRoot(), "testGetInputStream_OutputStreamNotClosed.tmp");

        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertEquals(0, deferredFileStreamStorage.byteArrayOutputStream.size());

        // Write 5 bytes (2 bytes more than the threshold). It should switch to use a file
        deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});

        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.DISK);
        assertTrue(file.exists());
        assertEquals(5, file.length());

        Exception expected = null;
        try {
            InputStream inputStream = deferredFileStreamStorage.getInputStream();
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);


    }

    @Test
    public void testDismiss_inMemory(){

        File file = new File(tempFolder.getRoot(), "testCloseQuietlyAndPurgeFile_inMemory.tmp");
        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());
        assertTrue(deferredFileStreamStorage.dismiss());

        Exception expected = null;
        try {
            deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);

    }

    @Test
    public void testDismiss() throws IOException {

        File file = new File(tempFolder.getRoot(), "testCloseQuietlyAndPurgeFile.tmp");
        DeferredFileStreamStorage deferredFileStreamStorage = new DeferredFileStreamStorage(file, 3);
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.MEMORY);
        assertFalse(file.exists());

        deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});
        assertEquals(deferredFileStreamStorage.storageMode, DeferredFileStreamStorage.StorageMode.DISK);
        assertTrue(file.exists());
        assertTrue(deferredFileStreamStorage.dismiss());
        assertEquals(deferredFileStreamStorage.readWriteStatus, DeferredFileStreamStorage.ReadWriteStatus.DISMISSED);

        // Try to write
        Exception expected = null;
        try {
            deferredFileStreamStorage.write(new byte[]{0x01, 0x02, 0x03, 0x4, 0x5});
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);

        // try to read
        expected = null;
        try {
            deferredFileStreamStorage.getInputStream();
        }catch (Exception e){
            expected = e;
        }
        assertNotNull(expected);

    }

}