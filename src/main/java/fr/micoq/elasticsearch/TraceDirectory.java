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

// Like strace... for Lucene :)

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;

public class TraceDirectory extends Directory {

  private Directory delegate;
  private TraceDirectoryEventHandler handler;
  private Path location;
  
  public TraceDirectory(Path location, Directory delegate, TraceDirectoryEventHandler handler) {
    this.delegate = delegate;
    this.handler = handler;
    this.location = location;
  }
  
  @Override
  public void close() throws IOException {
    this.delegate.close();
    this.handler.traceCloseDirectory(this.location);
  }
  
  @Override
  public IndexOutput createOutput(String name, IOContext context) throws IOException {
    String resourceDescription = String.format("TraceIndexOutput(%s)",name);
    IndexOutput output = this.delegate.createOutput(name, context);
    this.handler.traceCreateOutput(this.location, output, name, context);
    return new TraceIndexOutput(this.handler, resourceDescription, name, context, output);
  }
  
  @Override
  public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
    IndexOutput output = this.delegate.createTempOutput(prefix, suffix, context);
    this.handler.traceCreateTempOutput(this.location, output, prefix, suffix, context);
    return new TraceIndexOutput(this.handler, suffix, prefix, context, output);
  }
  
  @Override
  public void deleteFile(String name) throws IOException {
    this.delegate.deleteFile(name);
    this.handler.traceDeleteFile(this.location, name);
  }
  
  @Override
  public long fileLength(String name) throws IOException {
    long l = this.delegate.fileLength(name);
    this.handler.traceFileLength(this.location, name,l);
    return l;
  }
  
  @Override
  public String[] listAll() throws IOException {
    this.handler.traceListAll(this.location);
    return this.delegate.listAll();
  }
  
  @Override
  public Lock obtainLock(String name) throws IOException {
    Lock lock = new TraceLock(this.handler, this.delegate.obtainLock(name));
    this.handler.traceObtainLock(this.location, name);
    return lock;
  }
  
  @Override
  public IndexInput openInput(String name, IOContext context) throws IOException {
    String resourceDescription = String.format("TraceIndexInput(%s)", name);
    IndexInput input = this.delegate.openInput(name, context);
    this.handler.traceOpenInput(this.location, input, name, context);
    return new TraceIndexInput(this.handler, resourceDescription, name, input);
  }
  
  @Override
  public void rename(String fromName, String toName) throws IOException {
    this.delegate.rename(fromName,toName);
    this.handler.traceRename(this.location, fromName, toName);
  }
  
  @Override
  public void sync(Collection<String> files) throws IOException {
    this.delegate.sync(files);
    this.handler.traceSync(this.location, files);
  }
  
  @Override
  public void syncMetaData() throws IOException {
    this.delegate.syncMetaData();
    this.handler.traceSyncMetaData(this.location);
  }
  
  private class TraceIndexOutput extends IndexOutput {
    private IndexOutput delegateOutput;
    private int outputId; 
    private TraceDirectoryEventHandler handler;
    private String outputName;
    
    protected TraceIndexOutput(
        TraceDirectoryEventHandler handler,
        String resourceDescription,
        String name,
        IOContext context,
        IndexOutput delegateOutput) {
      super(resourceDescription, name);
      this.delegateOutput = delegateOutput;
      this.handler = handler;
      this.outputName = name;
      this.outputId = delegateOutput.hashCode();
    }
    
    @Override
    public void close() throws IOException {
      this.delegateOutput.close();
      this.handler.traceCloseOutput(this.outputName, this.outputId);
    }
    
    @Override
    public long getChecksum() throws IOException {
      long c = this.delegateOutput.getChecksum();
      this.handler.traceChecksumIndexOutput(this.outputName, this.outputId, c);
      return c;
    }
    
    @Override
    public long getFilePointer() {
      long p = this.delegateOutput.getFilePointer(); 
      this.handler.traceFilePointerOutput(this.outputName, this.outputId, p);
      return p;
    }
    
    @Override
    public void writeByte(byte b) throws IOException {
      this.delegateOutput.writeByte(b);
      this.handler.traceWriteByte(this.outputName, this.outputId, b);
    }
  
    @Override
    public void writeBytes(byte[] data, int offset, int length) throws IOException {
      this.delegateOutput.writeBytes(data, offset, length);
      this.handler.traceWriteBytes(this.outputName, this.outputId, data, offset, length);
    }
  }

  private class TraceIndexInput extends IndexInput {
    private IndexInput delegateInput;
    private int inputId;
    private TraceDirectoryEventHandler handler;
    private String inputName;
    
    protected TraceIndexInput(TraceDirectoryEventHandler handler, String resourceDescription, String name, IndexInput delegateInput) {
      super(resourceDescription);
      this.delegateInput = delegateInput;
      this.handler = handler;
      this.inputName = name;
      this.inputId = delegateInput.hashCode();
    }
    
    @Override
    public void readBytes(byte[] data, int offset, int length) throws IOException {
      long currentOffset = this.delegateInput.getFilePointer();
      this.delegateInput.readBytes(data, offset, length);
      this.handler.traceReadBytes(this.inputName, this.inputId, currentOffset, data, offset, length);
    }
    
    @Override
    public byte readByte() throws IOException {
      long currentOffset = this.delegateInput.getFilePointer();
      byte b = this.delegateInput.readByte();
      this.handler.traceReadByte(this.inputName, this.inputId, currentOffset, b);
      return b;
    }
    
    @Override
    public IndexInput clone() {
      String resourceDescription = String.format("TraceIndexInput()");
      IndexInput input = new TraceIndexInput(this.handler, resourceDescription, this.inputName, delegateInput.clone());
      this.handler.traceCloneInput(this.inputName, this.inputId);
      return input;
    }
    
    @Override
    public IndexInput slice(String name, long offset, long length) throws IOException {
      String resourceDescription = String.format("TraceIndexInput()");
      IndexInput input = new TraceIndexInput(
          this.handler,
          resourceDescription,
          this.inputName,
          this.delegateInput.slice(name, offset, length));
      this.handler.traceSlice(this.inputName, this.inputId, name, offset, length);
      return input;
    }
    
    @Override
    public void seek(long offset) throws IOException {
      this.delegateInput.seek(offset);
      this.handler.traceSeek(this.inputName, this.inputId, offset);
    }
    
    @Override
    public long length() {
      long l = this.delegateInput.length();
      this.handler.traceLength(this.inputName, this.inputId, l);
      return l;
    }
    
    @Override
    public long getFilePointer() {
      long p = this.delegateInput.getFilePointer();
      this.handler.traceFilePointerInput(this.inputName, this.inputId, p);
      return p;
    }
    
    @Override
    public void close() throws IOException {
      this.delegateInput.close();
      this.handler.traceCloseInput(this.inputName, this.inputId);
    }
  }
  
  private class TraceLock extends Lock {
  
    private Lock delegateLock;
    private TraceDirectoryEventHandler handler;
    
    TraceLock(TraceDirectoryEventHandler handler, Lock delegateLock) {
      this.delegateLock = delegateLock;
      this.handler = handler;
    }
    
    @Override
    public void close() throws IOException {
      this.delegateLock.close();
      this.handler.traceCloseLock();
    }
    
    @Override
    public void ensureValid() throws IOException {
      this.delegateLock.ensureValid();
      this.handler.traceEnsureValidLock();
    }
  }
}
