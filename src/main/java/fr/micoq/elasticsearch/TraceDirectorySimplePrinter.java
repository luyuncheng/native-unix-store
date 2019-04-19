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

import java.nio.file.Path;
import java.util.Collection;

import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IOContext.Context;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;

public class TraceDirectorySimplePrinter implements TraceDirectoryEventHandler {

  private String contextToString(IOContext context) 
  {
    String con = null;
    if(context.context == Context.DEFAULT) {
      con = "DEFAULT";
    } else if(context.context == Context.FLUSH) {
      con = "FLUSH";
    } else if(context.context == Context.MERGE) {
      con = "MERGE";
    } else if(context.context == Context.READ) {
      if(context.readOnce) {
        con = "READONCE";
      } else {
        con = "READ";
      }
    } else {
      con = "other";
    }
    return con;
  }
  
  @Override
  public void traceCloseDirectory(Path location) {
    System.out.println(String.format("%s Directory.close()",
        Thread.currentThread().getName()));
  }
  
  @Override
  public void traceSync(Path location, Collection<String> files) {
    System.out.println(String.format("%s Directory.sync()",
        Thread.currentThread().getName()));
  }

  @Override
  public void traceSyncMetaData(Path location) {
    System.out.println(String.format("%s Directory.syncMetaData()",
        Thread.currentThread().getName()));
  }

  @Override
  public void traceRename(Path location, String fromName, String toName) {
    System.out.println(String.format("%s Directory.rename(%s,%s)",
        Thread.currentThread().getName(), fromName, toName));
  }

  @Override
  public void traceListAll(Path location) {
    System.out.println(String.format("%s Directory.listAll()",
        Thread.currentThread().getName()));
  }
  
  @Override
  public void traceDeleteFile(Path location, String name) {
    System.out.println(String.format("%s Directory.deleteFile(%s)",
        Thread.currentThread().getName(), name));
  }

  @Override
  public void traceFileLength(Path location, String name, long l) {
    System.out.println(String.format("%s Directory.fileLength(%s) = %d",
        Thread.currentThread().getName(), name, l));
  }

  // Lock
  
  @Override
  public void traceObtainLock(Path location, String name) {
    System.out.println(String.format("%s Directory.obtainLock(%s)",
        Thread.currentThread().getName(), name));
  }

  @Override
  public void traceCloseLock() {
    System.out.println("Lock.close()");
  }
  
  @Override
  public void traceEnsureValidLock() {
    System.out.println("Lock.ensureValid()");
  }
  
  // Output
  
  @Override
  public void traceCreateOutput(Path location, IndexOutput output, String name, IOContext context) {
    System.out.println(String.format("%s Directory.createOutput(%s,%s) = 0x%08x",
        Thread.currentThread().getName(), name, contextToString(context), output.hashCode()));
  }
  
  @Override
  public void traceCreateTempOutput(Path location, IndexOutput output, String prefix, String suffix, IOContext context) {
    System.out.println(String.format("%s Directory.createTempOutput() = 0x%08x",
        Thread.currentThread().getName(), contextToString(context), output.hashCode()));
  }
  
  @Override
  public void traceCloseOutput(String outputName, int outputId) {
    System.out.println(String.format("%s [%s,0x%08x] IndexOutput.close()",
        Thread.currentThread().getName(), outputName, outputId));
  }

  @Override
  public void traceChecksumIndexOutput(String outputName, int outputId, long c) {
    System.out.println(String.format("%s [%s,0x%08x] IndexOutput.getChecksum() = 0x%016x",
        Thread.currentThread().getName(), outputName, outputId, c));
    
  }

  @Override
  public void traceFilePointerOutput(String outputName, int outputId, long p) {
    System.out.println(String.format("%s [%s,0x%08x] IndexOutput.getFilePointer() = %d",
        Thread.currentThread().getName(), outputName, outputId, p));
  }

  @Override
  public void traceWriteByte(String outputName, int outputId, byte b) {
    System.out.println(String.format("%s [%s,0x%08x] IndexOutput.writeByte(0x%02x)",
        Thread.currentThread().getName(), outputName, outputId, b));
  }

  @Override
  public void traceWriteBytes(String outputName, int outputId, byte[] data, int offset, int length) {
    System.out.println(String.format("%s [%s,0x%08x] IndexOutput.writeBytes(%d)",
        Thread.currentThread().getName(), outputName, outputId, length));
  }
  
  // Input
  
  @Override
  public void traceOpenInput(Path location, IndexInput input, String name, IOContext context) {
    System.out.println(String.format("%s Directory.openInput(%s,%s) = 0x%08x",
        Thread.currentThread().getName(), name, contextToString(context), input.hashCode()));
  }

  @Override
  public void traceReadByte(String inputName, int inputId, long currentOffset, byte b) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.readByte() = 0x%02x",
        Thread.currentThread().getName(), inputName, inputId, b));
  }
  
  @Override
  public void traceReadBytes(String inputName, int inputId, long currentOffset, byte[] data, int offset, int length) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.readBytes(%d)",
        Thread.currentThread().getName(), inputName, inputId, length));
  }

  @Override
  public void traceCloneInput(String inputName, int inputId) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.clone()",
        Thread.currentThread().getName(), inputName, inputId));
  }

  @Override
  public void traceSlice(String inputName, int inputId, String name, long offset, long length) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.slice(name=%s, offset=%d, length=%d)",
        Thread.currentThread().getName(), inputName, inputId, name, offset, length));
  }

  @Override
  public void traceCloseInput(String inputName, int inputId) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.close()",
        Thread.currentThread().getName(), inputName, inputId));
  }

  @Override
  public void traceFilePointerInput(String inputName, int inputId, long p) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.getFilePointer() = %d",
        Thread.currentThread().getName(), inputName, inputId, p));
  }

  @Override
  public void traceLength(String inputName, int inputId, long l) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.length() = %d",
        Thread.currentThread().getName(), inputName, inputId, l));
  }

  @Override
  public void traceSeek(String inputName, int inputId, long offset) {
    System.out.println(String.format("%s [%s,0x%08x] IndexInput.seek(%d)",
        Thread.currentThread().getName(), inputName, inputId, offset));
  }

}
