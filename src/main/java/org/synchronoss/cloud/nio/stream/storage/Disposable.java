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


/**
 * A {@code Disposable} is a destination of data that can be discarded.
 * The dispose method is invoked to dispose resources that the object is
 * holding (such as open files or streams).
 *
 */
public interface Disposable {

    /**
     * Dismiss and releases any system resources associated with this object. This includes closing streams and deleting
     * any temporary files. If the resources are already discarded then invoking this method has no effect.
     *
     * @return <code>true</code> if and only if the file was created and it has been deleted successfully; <code>false</code> otherwise.
     */
    boolean dispose();
}
