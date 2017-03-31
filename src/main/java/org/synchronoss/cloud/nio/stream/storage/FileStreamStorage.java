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
 * <p> A {@code StreamStorage} that uses a combination of memory and file to store the data.
 *    If the data is smaller than a configurable threshold the data is kept in memory, if the threshold is reached the
 *    bytes are flushed to disk.
 * <p> The {@code DeferredFileStreamStorage} has two distinct states:
 * <ul>
 *     <li><i>write</i>: The {@code StreamStorage} is ONLY writable and NOT readable.</li>
 *     <li><i>read</i>: The {@code StreamStorage} is ONLY readable and NOT writable.</li>
 * </ul>
 * <p> A new instance will always start in a <i>write</i> state, ready to accept bytes and any call to the {@link #getInputStream()} will fail.
 * Once all the data has been written, the {@link #close()} method needs to be called to close the write channel and switch the
 * {@code DeferredFileStreamStorage} to the <i>read</i> state. At that point the data can be read via {@link #getInputStream()}.
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

    private File file = null;
    private int threshold;
    private boolean append;
    private boolean purgeFileAfterReadComplete = false;
    private boolean deleteFilesOnDismiss = false;

    volatile ReadWriteStatus readWriteStatus;
    volatile StorageMode storageMode;
    volatile ByteArrayOutputStream byteArrayOutputStream;
    volatile FileOutputStream fileOutputStream;

    /**
     * <p> Returns a reference to a FileStreamStorage where the data is written to a file when the data is greater in
     * size than the threshold specified.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to the file.
     * @param append boolean that indicates whether the file is going to be appended to or overridden.
     *
     * @return FileStreamStorage
     */
    public static FileStreamStorage deferred(final File file, final int threshold, final boolean append){
        return new FileStreamStorage(file, threshold, append);
    }

    /**
     * <p> Returns a reference to a FileStreamStorage where the data is always written to the file specified.
     *
     * @param file The file that will be used to store the data.
     * @param append boolean that indicates whether any existing data in the file is going to be appended to or overridden.
     *
     * @return FileStreamStorage
     */
    public static FileStreamStorage directToFile(final File file, boolean append){
        return new FileStreamStorage(file, 0, append);
    }

    public FileStreamStorage purgeFileAfterReadComplete(){
        this.purgeFileAfterReadComplete = true;
        return this;
    }

    public FileStreamStorage doNotPurgeFileAfterReadComplete(){
        this.purgeFileAfterReadComplete = false;
        return this;
    }

    public FileStreamStorage deleteFilesOnDismiss(){
        this.deleteFilesOnDismiss = true;
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
     * @param purgeFileAfterReadComplete boolean indicating whether the file should be deleted after been read.
     * @param deleteFilesOnDismiss boolean indicating whether the file should be deleted after dismiss has been called.
     *
     */
    public FileStreamStorage(File file, int threshold, boolean purgeFileAfterReadComplete, boolean deleteFilesOnDismiss, boolean append) {
        this(file, threshold, append);
        this.purgeFileAfterReadComplete = purgeFileAfterReadComplete;
        this.deleteFilesOnDismiss = deleteFilesOnDismiss;
    }

    /**
     * <p> Constructor that sets the threshold to the user specified value.
     *
     * @param file The file that will be used to store the data if the threshold is reached.
     * @param threshold The threshold in bytes. Data smaller than the threshold are kept in memory. If the threshold is reached, the data is flushed to disk.
     */
     FileStreamStorage(final File file, final int threshold, boolean append){
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
        return !(file != null && file.exists()) || (deleteFilesOnDismiss && file.delete());
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
            return new NameAwarePurgableFileInputStream(file, purgeFileAfterReadComplete);
        }catch (Exception e){
            throw new IllegalStateException("Unable to create the inputStream.", e);
        }
    }
}
