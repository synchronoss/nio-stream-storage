/*
 * Copyright (C) 2015 Synchronoss Technologies
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;


import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * <p> Unit tests for {@link NameAwarePurgableFileInputStream}
 *
 * @author Silvano Riz
 */
public class NameAwarePurgableFileInputStreamTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testClose() throws Exception {
        File file = tempFolder.newFile("testClose");
        assertTrue(file.exists());
        NameAwarePurgableFileInputStream nameAwarePurgableFileInputStream = new NameAwarePurgableFileInputStream(file, true);
        assertTrue(file.exists());
        nameAwarePurgableFileInputStream.close();
        assertFalse(file.exists());
    }

    @Test
    public void testClose_noFile() throws Exception {
        File file = tempFolder.newFile("testClose");
        assertTrue(file.exists());
        NameAwarePurgableFileInputStream nameAwarePurgableFileInputStream = new NameAwarePurgableFileInputStream(file);
        assertTrue(file.delete());
        nameAwarePurgableFileInputStream.close();
        assertFalse(file.exists());
    }

    @Test
    public void testClose_cannotDelete() throws Exception {

        File file = new File(tempFolder.getRoot(), "testClose_cannotDelete"){
            int deleteCalls = 0;

            @Override
            public boolean delete() {
                if (deleteCalls == 0){
                    deleteCalls++;
                    return false;
                }else {
                    return super.delete();
                }
            }
        };
        assertTrue(file.createNewFile());
        assertTrue(file.exists());
        NameAwarePurgableFileInputStream nameAwarePurgableFileInputStream = new NameAwarePurgableFileInputStream(file, true);
        nameAwarePurgableFileInputStream.close();
        assertTrue(file.exists());

        assertTrue(file.setReadable(true));
        assertTrue(file.setWritable(true));
        assertTrue(file.delete());
    }

        @Test
    public void testGetFile() throws Exception {
        final String fileName = "testGetFile";

        File file = tempFolder.newFile(fileName);
            NameAwarePurgableFileInputStream nameAwarePurgableFileInputStream = new NameAwarePurgableFileInputStream(file);

        assertTrue(nameAwarePurgableFileInputStream.getFile() != null);
        assertTrue(nameAwarePurgableFileInputStream.getFile().getName().contains(fileName));
    }
}