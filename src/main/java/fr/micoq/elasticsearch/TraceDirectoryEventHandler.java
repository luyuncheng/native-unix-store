/*
 * Copyright [2018-2019] Michaël Coquard
 *
 * Licensed under the Apache License, Version 2.0 (the "License") {};
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.micoq.elasticsearch;

import java.nio.file.Path;
import java.util.Collection;

import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

public interface TraceDirectoryEventHandler {

  default void traceCloseDirectory(Path location) {};
  default void traceSync(Path location, Collection<String> files) {};
  default void traceSyncMetaData(Path location) {};
  default void traceRename(Path location, String fromName, String toName) {};
  default void traceListAll(Path location) {};
  default void traceDeleteFile(Path location, String name) {};
  default void traceFileLength(Path location, String name, long l) {};
  
  default void traceObtainLock(Path location, String name) {};
  default void traceCloseLock() {};
  default void traceEnsureValidLock() {};
  
  default void traceCreateOutput(Path location, IndexOutput output, String name, IOContext context) {};
  default void traceCreateTempOutput(Path location, IndexOutput output, String prefix, String suffix, IOContext context) {};
  default void traceCloseOutput(String outputName, int outputId) {};
  default void traceChecksumIndexOutput(String outputName, int outputId, long c) {};
  default void traceFilePointerOutput(String outputName, int outputId, long p) {};
  default void traceWriteByte(String outputName, int outputId, byte b) {};
  default void traceWriteBytes(String outputName, int outputId, byte[] data, int offset, int length) {};
  
  default void traceOpenInput(Path location, IndexInput input, String name, IOContext context) {};
  default void traceReadByte(String inputName, int inputId, long currentOffset, byte b) {};
  default void traceReadBytes(String inputName, int inputId, long currentOffset, byte[] data, int offset, int length) {};
  default void traceCloneInput(String inputName, int inputId) {};
  default void traceSlice(String inputName, int inputId, String name, long offset, long length) {}; 
  default void traceCloseInput(String inputName, int inputId) {};
  default void traceFilePointerInput(String inputName, int inputId, long p) {};
  default void traceLength(String inputName, int inputId, long l) {};
  default void traceSeek(String inputName, int inputId, long offset) {};

}
