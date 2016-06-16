NIO Stream Storage
=============================

[![Master build Status](https://travis-ci.org/synchronoss/nio-stream-storage.svg?branch=master)](https://travis-ci.org/synchronoss/nio-stream-storage)

Overview
--------
The NIO Stream Storage project is a lightweight library to store streamed byte data using a combination of in memory and file storage.
`StreamStorage` (which extends `OutputStream`) is used as the destination to write incoming bytes as they are available.
It supplies an `InputStream` of the stored bytes when requested.
Automatic resource management is available to ensure any underlying files are automatically be deleted once the InputStream is closed.

Latest Release
--------------
```xml
<dependency>
    <groupId>org.synchronoss.cloud</groupId>
    <artifactId>nio-stream-storage</artifactId>
    <version>1.0.0</version>
</dependency>
```

Description
-----------
The implementation of `StreamStorage`, `DeferredFileStreamStorage` has two distinct states:

 * The `StreamStorage` is ONLY writable and NOT readable.
 * The `StreamStorage` is ONLY readable and NOT writable.

A new instance will always start in a *write* state, ready to accept bytes and any call to the `getInputStream()` will fail.
Once all the data has been written, the `close()` method needs to be called to close the write channel and switch the `DeferredFileStreamStorage` to the *read* state.
When the `purgeFileAfterReadComplete` flag is set to true the `PurgeOnCloseFileInputStream` ensures the temporary storage file is deleted if it exists.
At that point the data can be read via `getInputStream()`.

Usage
-----
Instantiate a StreamStorage object either directly or via a configured Factory:
```java
StreamStorageFactory streamStorageFactory = new DeferredFileStreamStorageFactory();
StreamStorage streamStorage = streamStorageFactory.create();
```
StreamStorage is an OutputStream so simply write bytes to it and close the stream once complete:
```java
streamStorage.write(new byte[]{0x03, 0x4, 0x5});
streamStorage.write(new byte[]{0x01, 0x02});
streamStorage.close();
```
Retrieve an InputStream of the stored bytes:
```java
InputStream inputStream = streamStorage.getInputStream();
```
Close the InputStream to cleanup the underlying file:
```java
inputStream.close()
```
Advanced Configuration
----------------------
It is possible to configure an in-memory threshold before storing data on disk. If configured the data is flushed to disk only
if that in-memory threshold is breached. This is provided as a potential optimisation to avoid unnecessary slow disk IO.
It should be noted however that even without this option (when flushing directly to disk) there will likely be a Page Cache
hit on the OS anyway, especially if the lifecycle of StreamStorage is relatively short-lived. So it may be worth bench-marking
any use of this in-memory threshold.


