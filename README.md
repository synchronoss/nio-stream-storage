NIO Stream Storage
=============================

[![Master build Status](https://travis-ci.org/synchronoss/nio-stream-storage.svg?branch=master)](https://travis-ci.org/synchronoss/nio-stream-storage)

Overview
--------
The NIO Stream Storage project is a lightweight library to store streamed byte data and read them back.
The core component, `StreamStorage` (which extends `OutputStream`) is used as the destination to write incoming bytes as they are available.
The `StreamStorage` is required to supply an `InputStream` of the stored bytes when requested.
The `StreamStorage` is implementing the `Disposable` interface for automatic resource management.

Latest Release
--------------
```xml
<dependency>
    <groupId>org.synchronoss.cloud</groupId>
    <artifactId>nio-stream-storage</artifactId>
    <version>1.1.1</version>
</dependency>
```

Description
-----------
The default implementation of `StreamStorage` provided by the library is the `FileStreamStorage`. The implementation enforces two distinct states:

 * The `StreamStorage` is ONLY writable and NOT readable.
 * The `StreamStorage` is ONLY readable and NOT writable.

A new instance will always start in a *write* state, ready to accept bytes and any call to the `getInputStream()` will fail.
Once all the data has been written, the `close()` method needs to be called to close the write channel and switch the `FileStreamStorage` to the *read* state.

Additionally, the `FileStreamStorage` can be configured to:

 * Use a combination of in memory and file storage. In fact a memory threshold can be set and the bytes will be kept in memory until the threshold is reached. Once the threshold is reached, the `FileStreamStorage` will flush the in memory data to file and it will keep writing directly to that file.
 * Delete the underlying file after the `FileStreamStorage` is disposed.
 * Delete the underlying file after the `InputStream` supplied by the `FileStreamStorage` is closed.
 * Append the data to the underlying file. This option is only available when the threshold is set to 0 and it is made available to support scenarios where the write needs to be resumed.

Usage
-----
Instantiate a StreamStorage object either directly or via a configured Factory:
```java
StreamStorageFactory streamStorageFactory = new DeferredFileStreamStorageFactory();
// Coinfigure the streamStorageFactory if needed
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
if that in-memory threshold is reached. This is provided as a potential optimisation to avoid unnecessary slow disk IO.
It should be noted however that even without this option (when flushing directly to disk) there will likely be a Page Cache
hit on the OS anyway, especially if the lifecycle of StreamStorage is relatively short-lived. So it may be worth bench-marking
any use of this in-memory threshold.


