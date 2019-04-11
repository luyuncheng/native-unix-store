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

import java.util.HashMap;
import java.util.Map;

public class TraceDirectoryStatsPrinter implements TraceDirectoryEventHandler {

  private Map<String,StatEntry> stats;
  
  public TraceDirectoryStatsPrinter() {
    this.stats = new HashMap<String,StatEntry>();
  }
  
  private synchronized void addStat(String inputName, long offset, int length) {
    StatEntry stat = stats.get(inputName);
    if(stat == null) {
      stat = new StatEntry();
      stats.put(inputName, stat);
    }
    stat.addBytes(length);
    System.out.println(String.format("%s %d @ %d (total %d)", inputName, length, offset, stat.getReadBytes()));
  }
  
  @Override
  public void traceReadByte(String inputName, int inputId, long currentOffset, byte b) {
    this.addStat(inputName, currentOffset, 1);
  }
  
  @Override
  public void traceReadBytes(String inputName, int inputId, long currentOffset, byte[] data, int offset, int length) {
    this.addStat(inputName, currentOffset, length);
  }
  
  private class StatEntry {
    private long readBytes;
    
    StatEntry() {
      this.readBytes = 0;
    }
    
    public void addBytes(int bytes) {
      this.readBytes += bytes;
    }
    
    public long getReadBytes() {
      return this.readBytes;
    }
  }
  
}
