package org.synchronoss.cloud.nio.stream.storage;


import java.io.File;

public interface StreamStorageFactory {

    StreamStorage create();

    StreamStorage create(File file, int threshold);

}
