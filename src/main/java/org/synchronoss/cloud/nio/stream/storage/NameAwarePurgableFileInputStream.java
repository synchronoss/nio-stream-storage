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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * <p> Extension of {@code FileInputStream} that allows the file to be purged when {@link #close()} is called.
 *
 * <p> It also allows the user to obtain a handle to the underlying file by the {@link #getFile()}.
 *
 * @author Silvano Riz
 */
public class NameAwarePurgableFileInputStream extends FileInputStream {

    private static final Logger log = LoggerFactory.getLogger(NameAwarePurgableFileInputStream.class);

    private final File file;
    private final boolean purgeFileOnClose;

    /**
     * <p> Constructor.
     *
     * @param file The file.
     * @param purgeFileOnClose If set to {@code true} attempts to purge the file when the {@link #close()} is called.
     * @throws FileNotFoundException if the file does not exist, is a directory or it cannot be opened for reading.
     */
    public NameAwarePurgableFileInputStream(final File file, final boolean purgeFileOnClose) throws FileNotFoundException {
        super(file);
        this.file = file;
        this.purgeFileOnClose = purgeFileOnClose;
    }


    public NameAwarePurgableFileInputStream(final File file) throws FileNotFoundException {
        this(file, false);
    }

    public File getFile() {
        return file;
    }

    /**
     * <p> Closes the stream and deletes the file.
     *     The close will not fail if the file is already deleted or it cannot be deleted.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void close() throws IOException {
        super.close();
        if (purgeFileOnClose && file.exists()){
            if (!file.delete()) {
                log.warn("Failed to purge file: " + file.getAbsolutePath());
            }
        }
    }
}
