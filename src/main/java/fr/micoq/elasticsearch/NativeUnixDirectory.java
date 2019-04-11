/*
 * Copyright [2018-2019] Michaël Coquard
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IOContext.Context;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.FileSwitchDirectory;

import fr.micoq.unsafe.MappedMemory;

public class NativeUnixDirectory extends FSDirectory {
  
  public static final int DEFAULT_DIRECT_BUFFER_SIZE = 131072;
  public static final long DEFAULT_MIN_BYTES_DIRECT = 10*1024*1024;
  public static final long DEFAULT_MAX_BYTES_PRELOAD = 0;

  private final boolean memoryReadAhead;
  private final boolean mappedMemory;
  private final boolean directReadEnabled;
  private final boolean directWriteEnabled;
  private final int directReadBufferSize;
  private final int directWriteBufferSize;
  private final ForceIO forceIO;
  private final long minBytesDirect;
  private final long maxBytesPreload;
  private final Directory delegate;
  private final Set<String> preLoadExtensions;
  
  private static final Set<String> directExcludedExtensions = new HashSet<String>(Arrays.asList("fnm","fdt","fdx")) ;
  
  public NativeUnixDirectory(Path path) throws IOException {
    this(path, FSLockFactory.getDefault());
  }
  
  public NativeUnixDirectory(Path path, LockFactory lockFactory) throws IOException {
    this(
      path,
      lockFactory,
      true,
      false,
      false,
      false,
      DEFAULT_DIRECT_BUFFER_SIZE,
      DEFAULT_DIRECT_BUFFER_SIZE,
      ForceIO.None,
      DEFAULT_MIN_BYTES_DIRECT,
      DEFAULT_MAX_BYTES_PRELOAD,
      new HashSet<String>());
  }

  public enum ForceIO {
    Direct,
    MappedMemory,
    None
  }
  
  public NativeUnixDirectory(
      Path path,
      LockFactory lockFactory,
      boolean mappedMemory,
      boolean memoryReadAhead,
      boolean directReadEnabled,
      boolean directWriteEnabled,
      int directReadBufferSize,
      int directWriteBufferSize,
      ForceIO forceIO,
      long minBytesDirect,
      long maxBytesPreload,
      Set<String> preLoadExtensions) throws IOException {
    super(path, lockFactory);
    this.mappedMemory = mappedMemory;
    this.memoryReadAhead = memoryReadAhead;
    this.directReadEnabled = directReadEnabled;
    this.directWriteEnabled = directWriteEnabled;
    this.directReadBufferSize = directReadBufferSize;
    this.directWriteBufferSize = directWriteBufferSize;
    this.forceIO = forceIO;
    this.minBytesDirect = minBytesDirect;
    this.maxBytesPreload = maxBytesPreload;
    this.preLoadExtensions = preLoadExtensions;
    this.delegate = new NIOFSDirectory(path, lockFactory);
  }
  
  private IndexInput makeMappedIndexInput(Path path, boolean readahead) throws IOException {
    String fileName = path.toString();
    MappedMemory memory = MappedMemory.mapFile(fileName);
    String fileExt = FileSwitchDirectory.getExtension(fileName);
    if((this.preLoadExtensions == null || this.preLoadExtensions.contains(fileExt)) &&
       (this.maxBytesPreload == 0 || this.maxBytesPreload <= memory.getLength())) {
      memory.preload();
    }
    if(readahead) {
      memory.madviseSequential();
    }
    else {
      memory.madviseRandom();
    }
    final String resourceDescription = "MMapIndexInput(path=\"" + path.toString() + "\")";
    final MappedIndexInputGuard guard = new MappedIndexInputGuard(memory);
    return MappedIndexInput.makeInput(resourceDescription, guard);
  }
  
  private IndexInput maybeMakeDirectIndexInput(Path path, String name, IOContext context) throws IOException {
    if(this.directReadEnabled && (
        (context.context == Context.MERGE && context.mergeInfo.estimatedMergeBytes >= this.minBytesDirect) ||
        getFileSize(path) >= this.minBytesDirect
      )) {
      return makeDirectIndexInput(path);
    } else if(this.mappedMemory) {
      return makeMappedIndexInput(path, true); // Always read ahead here since we don't do search requests
    } else {
      return delegate.openInput(name, context);
    }
  }
  
  private IndexInput makeDirectIndexInput(Path path) throws IOException {
    return new DirectIndexInput(path, this.directReadBufferSize);
  }
  
  private IndexOutput maybeMakeDirectIndexOutput(Path path, String name, IOContext context) throws IOException {
    if(this.directWriteEnabled) {
      return makeDirectIndexOutput(path);
    } else {
      return delegate.createOutput(name, context);
    }
  }
  
  private IndexOutput makeDirectIndexOutput(Path path) throws IOException {
    return new DirectIndexOutput(path, this.directWriteBufferSize); 
  }
  
  private long getFileSize(Path path) {
    return path.toFile().length();
  }

  @Override
  public IndexInput openInput(String name, IOContext context) throws IOException {
    ensureOpen();
    ensureCanRead(name);
    Path path = this.getDirectory().resolve(name);
    if(this.forceIO == ForceIO.Direct)
      return makeDirectIndexInput(path);
    else if(this.forceIO == ForceIO.MappedMemory)
      return makeMappedIndexInput(path, this.memoryReadAhead);
    else if(context.context == Context.READ && !context.readOnce) {
      // Search operations needs to be cached
      if(this.mappedMemory) {
        return makeMappedIndexInput(path, this.memoryReadAhead);
      } else {
        return delegate.openInput(name, context);
      }
    }
    return maybeMakeDirectIndexInput(path, name, context);
  }
  
  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    ensureOpen();
    Path path = this.getDirectory().resolve(name);
    if(this.forceIO == ForceIO.Direct)
      return makeDirectIndexOutput(path);
    else if(context.context == Context.MERGE && context.mergeInfo.estimatedMergeBytes >= this.minBytesDirect) {
      return maybeMakeDirectIndexOutput(path, name, context);
    }
    else if(context.context == Context.DEFAULT) {
      /*
       * TODO
       * Since shard restoration uses Context.DEFAULT, we would like to use direct writes here.
       * But stored fields and fields infos (fdx/fdt/fnm) are written with the same context so
       * we choose to exclude them.
       */
      if(directExcludedExtensions.contains(FileSwitchDirectory.getExtension(name))) {
        return delegate.createOutput(name, context);
      }
      else {
        return maybeMakeDirectIndexOutput(path, name, context);
      }
    }
    return delegate.createOutput(name, context);
  }
}
