/*
 * Copyright 2015 leak4mk0. All Rights Reserved.
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

package net.leak4mk0.authenticator.sw2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * A extension receiver for SONY SmartWatch 2
 *
 * @author leak4mk0
 */
public class WatchExtensionReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    intent.setClass(context, WatchExtensionService.class);
    context.startService(intent);
  }
}
