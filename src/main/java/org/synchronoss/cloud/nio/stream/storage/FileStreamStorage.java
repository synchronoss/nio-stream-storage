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

import java.io.*;

/**
 * <p> A configurable {@code StreamStorage} that:
 * <ul>
 *     <li>Allows a a combination of memory and file to store the data. In fact a threshold can be set and if the data is smaller than the threshold
 *         the data is kept in memory, if the threshold is reached the bytes are flushed to disk.</li>
 *     <li>Allows to automatically delete the file after the object is disposed via the {@link #dispose()} method</li>
 *     <li>Allows to automatically delete the file after the {@link InputStream} returned by the {@link #getInputStream()} method is closed.</li>
 *     <li>Allows to append the data to the underlying file (useful for resuming writes).</li>
 * </ul>
 *
 * <p> The {@code FileStreamStorage} has two distinct states:
 * <ul>
 *     <li><i>write</i>: The {@code StreamStorage} is ONLY writable and NOT readable.</li>
 *     <li><i>read</i>: The {@code StreamStorage} is ONLY readable and NOT writable.</li>
 * </ul>
 *
 * <p> A new instance will always start in a <i>write</i> state, ready to accept bytes and any call to the {@link #getInputStream()} will fail.
 *     Once all the data has been written, the {@link #close()} method needs to be called to close the write channel and switch the
 *     {@code FileStreamStorage} to the <i>read</i> state. At that point the data can be read via {@link #getInputStream()}.
 *
 * @author Silvano Riz
 */
public class FileStreamStorage extends StreamStorage {

    private static final Logger log = LoggerFactory.getLogger(FileStreamStorage.class);

    enum ReadWriteStatus {
        READ, WRITE, DISMISSED
    }

    enum StorageMode {
        MEMORY, DISK;
    }

    volatile File file = null;
    volatile int threshold;
    volatile boolean append;
    volatile boolean deleteFilesOnClose = false;
    volatile boolean deleteFilesOnDispose = false;

    volatile ReadWriteStatus readWriteStatus;
    volatile StorageMode storageMode;
    volatile ByteArrayOutputStream byteArrayOutputStream;
    volatile FileOutputStream fileOutputStream;

    /**
     * <p> Returns a reference to a {@link FileStreamStorage} where the data is written to a file when the data is greater in
     * size than the threshold specified.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to the file.
     *
     * @return FileStreamStorage
     */
    public static FileStreamStorage deferred(final File file, final int threshold){
        return new FileStreamStorage(file, threshold, false);
    }

    /**
     * <p> Returns a reference to a {@link FileStreamStorage} where the data is always written to the file specified.
     *
     * @param file The file that will be used to store the data.
     * @param append boolean that indicates whether any existing data in the file is going to be appended to or overridden.
     *
     * @return FileStreamStorage
     */
    public static FileStreamStorage directToFile(final File file, boolean append){
        return new FileStreamStorage(file, 0, append);
    }

    /**
     * <p> Configures the current {@link FileStreamStorage} to delete the underlying file after calling the {@link #close()} method.
     *
     * @return The current object
     */
    public FileStreamStorage deleteFilesOnClose(){
        this.deleteFilesOnClose = true;
        return this;
    }

    /**
     * <p> Configures the current {@link FileStreamStorage} to delete the underlying file after calling the {@link #dispose()} ()} method.
     *
     * @return The current object
     */
    public FileStreamStorage deleteFilesOnDispose(){
        this.deleteFilesOnDispose = true;
        return this;
    }

    // ------------
    // CONSTRUCTORS
    // ------------

    /**
     * <p> Constructor that sets the threshold to the user specified value.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to disk.
     * @param deleteFilesOnClose boolean indicating whether the file should be deleted after been read.
     * @param deleteFilesOnDispose boolean indicating whether the file should be deleted after dismiss has been called.
     *
     */
    protected FileStreamStorage(final File file, final int threshold, final boolean deleteFilesOnClose, final boolean deleteFilesOnDispose, final boolean append) {
        this(file, threshold, append);
        this.deleteFilesOnClose = deleteFilesOnClose;
        this.deleteFilesOnDispose = deleteFilesOnDispose;
    }

    /**
     * <p> Constructor that sets the threshold to the user specified value.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to disk.
     */
     protected FileStreamStorage(final File file, final int threshold, final boolean append){
        this.file = file;
        this.threshold = threshold;
        readWriteStatus = ReadWriteStatus.WRITE;
        this.append = append;
        if(threshold <= 0){
            storageMode = StorageMode.DISK;
            fileOutputStream = newFileOutputStream();
        }else{
            storageMode = StorageMode.MEMORY;
            byteArrayOutputStream = new ByteArrayOutputStream();
        }
    }

    File getFile() {
        return this.file;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(int b) throws IOException {
        assertIsWritable();
        if (checkThreshold(1)){
            byteArrayOutputStream.write(b);
        }else{
            fileOutputStream.write(b);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        assertIsWritable();
        if (checkThreshold(len)){
            byteArrayOutputStream.write(b, off, len);
        }else{
            fileOutputStream.write(b, off, len);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(byte[] b) throws IOException {
        assertIsWritable();
        if (checkThreshold(b.length)){
            byteArrayOutputStream.write(b);
        }else{
            fileOutputStream.write(b);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
        assertIsWritable();
        if (fileOutputStream != null) {
            fileOutputStream.flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        close(ReadWriteStatus.READ);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public InputStream getInputStream() {
        if (readWriteStatus.equals(ReadWriteStatus.READ)) {
            if (storageMode.equals(StorageMode.MEMORY)) {
                return new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            } else {
                return newFileInputStream();
            }
        }else{
            throw new IllegalStateException("The DeferredFileStreamStorage is still in write mode. Call the close() method when all the data has been written before asking for the InputStream.");
        }
    }

    /**
     * <p> Returns if the data has been flushed to disk or if it's still in memory.
     *
     * @return true if the data is in memory, false otherwise
     */
    public boolean isInMemory() {
        return storageMode.equals(StorageMode.MEMORY);
    }

    /**
     * <p> Dismisses the {@code DeferredFileStreamStorage} closing quietly the {@code OutputStream} and deleting the underlying file if it exists.
     *     This method is useful just in case of errors to free the resources and once called the {@code DeferredFileStreamStorage} is not usable anymore.
     *
     * @return <code>true</code> if and only if the file was created and it has been deleted successfully; <code>false</code> otherwise.
     */
    @Override
    public boolean dispose() {
        try {
            close(ReadWriteStatus.DISMISSED);
        } catch (Exception e) {
            // Nothing to do
        }
        return !(file != null && file.exists()) || (deleteFilesOnDispose && file.delete());
    }

    void close(final ReadWriteStatus newReadWriteStatus) throws IOException {
        readWriteStatus = newReadWriteStatus;
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }
    }

    boolean checkThreshold(final int lengthToWrite) throws IOException {
        if (byteArrayOutputStream != null && byteArrayOutputStream.size() + lengthToWrite <= threshold){
            return true;
        }
        if (isInMemory()){
            switchToFile();
        }
        return false;
    }

    void assertIsWritable(){
        if (!readWriteStatus.equals(ReadWriteStatus.WRITE)){
            throw new IllegalStateException("OutputStream is closed");
        }
    }

    void switchToFile() throws IOException {

        if (log.isDebugEnabled()) log.debug("Switching to file");

        fileOutputStream = new FileOutputStream(file);
        fileOutputStream.write(byteArrayOutputStream.toByteArray());
        fileOutputStream.flush();
        byteArrayOutputStream.reset();
        byteArrayOutputStream = null;
        storageMode = StorageMode.DISK;
    }

    FileOutputStream newFileOutputStream(){
        try{
            return new FileOutputStream(file, append);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the outputStream.", e);
        }
    }

    NameAwarePurgableFileInputStream newFileInputStream(){
        try{
            return new NameAwarePurgableFileInputStream(file, deleteFilesOnClose);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the inputStream.", e);
        }
    }
}
