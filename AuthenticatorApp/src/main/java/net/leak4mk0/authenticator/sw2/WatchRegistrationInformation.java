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

import android.content.ContentValues;
import android.content.Context;

import com.google.android.apps.authenticator.AuthenticatorActivity;
import com.google.android.apps.authenticator2.R;
import com.sonyericsson.extras.liveware.aef.registration.Registration;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.registration.RegistrationInformation;

/**
 * A registration information for SONY SmartWatch 2
 *
 * @author leak4mk0
 */
public class WatchRegistrationInformation extends RegistrationInformation {
  private static final String EXTENSION_UUID = "3c801594-e30a-4f04-902a-55e7b5f00f49";

  private Context mContext;

  public WatchRegistrationInformation(Context context) {
    mContext = context;
  }

  @Override
  public int getRequiredControlApiVersion() {
    return 2;
  }

  @Override
  public int getRequiredSensorApiVersion() {
    return API_NOT_REQUIRED;
  }

  @Override
  public int getRequiredNotificationApiVersion() {
    return API_NOT_REQUIRED;
  }

  @Override
  public int getRequiredWidgetApiVersion() {
    return API_NOT_REQUIRED;
  }

  @Override
  public ContentValues getExtensionRegistrationConfiguration() {
    ContentValues values;

    values = new ContentValues();
    values.put(Registration.ExtensionColumns.CONFIGURATION_ACTIVITY, AuthenticatorActivity.class.getName());
    values.put(Registration.ExtensionColumns.CONFIGURATION_TEXT, mContext.getString(R.string.app_name_short));
    values.put(Registration.ExtensionColumns.NAME, mContext.getString(R.string.app_name_short));
    values.put(Registration.ExtensionColumns.EXTENSION_KEY, getExtensionKey());
    values.put(Registration.ExtensionColumns.HOST_APP_ICON_URI, ExtensionUtils.getUriString(mContext, R.drawable.ic_launcher_authenticator));
    values.put(Registration.ExtensionColumns.EXTENSION_48PX_ICON_URI, ExtensionUtils.getUriString(mContext, R.drawable.ic_launcher_sw2_authenticator));
    values.put(Registration.ExtensionColumns.NOTIFICATION_API_VERSION, getRequiredNotificationApiVersion());
    values.put(Registration.ExtensionColumns.PACKAGE_NAME, mContext.getPackageName());

    return values;
  }

  @Override
  public boolean isDisplaySizeSupported(int width, int height) {
    return true;
  }

  @Override
  public boolean controlInterceptsBackButton() {
    return false;
  }

  @Override
  public String getExtensionKey() {
    return EXTENSION_UUID;
  }
}
