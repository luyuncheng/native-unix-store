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

import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.shard.ShardPath;
import org.elasticsearch.index.store.DirectoryService;
import org.elasticsearch.index.store.IndexStore;

public class NativeUnixIndexStore extends IndexStore {

  public NativeUnixIndexStore(IndexSettings indexSettings) {
    super(indexSettings);
  }
  
  @Override
  public DirectoryService newDirectoryService(ShardPath path) {
    return new NativeUnixFsDirectoryService(this.indexSettings, this, path);
  }

}
