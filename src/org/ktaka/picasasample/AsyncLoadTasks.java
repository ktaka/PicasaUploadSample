/*
 * Copyright (c) 2013 Kenichi Takahashi
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.ktaka.picasasample;

import java.io.IOException;
import android.util.Log;

/**
 * Asynchronously load the tasks.
 * 
 * @author Yaniv Inbar
 */
class AsyncLoadTasks extends CommonAsyncTask {

  AsyncLoadTasks(PicasaUploadActivity tasksSample) {
    super(tasksSample);
  }

  @Override
  protected void doInBackground() throws IOException {
	  activity.showAlbums();
  }

  static void run(PicasaUploadActivity tasksSample) {
    new AsyncLoadTasks(tasksSample).execute();
  }
}
