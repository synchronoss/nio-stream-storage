NIO Stream Storage
=============================

Overview
--------
The NIO Stream Storage project is a lightweight library to stream byte data using a combination of in memory and file storage.
If the data is smaller than a configurable threshold the data is kept in memory, if the threshold is reached the bytes are flushed to a file on disk.

Description
-----------
The **DeferredFileByteStore** has two distinct states:

 * The **ByteStore** is ONLY writable and NOT readable.
 * The **ByteStore** is ONLY readable and NOT writable.

A new instance will always start in a *write* state, ready to accept bytes and any call to the *getInputStream()* will fail.
Once all the data has been written, the *close()* method needs to be called to close the write channel and switch the **DeferredFileByteStore** to the *read* state.
When the *purgeFileAfterReadComplete* flag is set to true the **PurgeOnCloseFileInputStream** ensures the temporary storage file is deleted if it exists.
At that point the data can be read via *getInputStream()*.

