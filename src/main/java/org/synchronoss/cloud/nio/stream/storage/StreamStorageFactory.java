package org.synchronoss.cloud.nio.stream.storage;


public interface StreamStorageFactory {

    StreamStorage create();

    StreamStorage create(int threshold);

}
