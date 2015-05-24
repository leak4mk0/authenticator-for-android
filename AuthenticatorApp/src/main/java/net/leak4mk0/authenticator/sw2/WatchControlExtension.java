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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.Log;

import com.google.android.apps.authenticator.AccountDb;
import com.google.android.apps.authenticator.OtpSource;
import com.google.android.apps.authenticator.OtpSourceException;
import com.google.android.apps.authenticator.TotpClock;
import com.google.android.apps.authenticator.TotpCounter;
import com.google.android.apps.authenticator.testability.DependencyInjector;
import com.google.android.apps.authenticator2.R;
import com.sonyericsson.extras.liveware.aef.control.Control;
import com.sonyericsson.extras.liveware.extension.util.ExtensionUtils;
import com.sonyericsson.extras.liveware.extension.util.control.ControlExtension;
import com.sonyericsson.extras.liveware.extension.util.control.ControlListItem;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A control extension for SONY SmartWatch 2
 *
 * @author leak4mk0
 */
public class WatchControlExtension extends ControlExtension {
  private static final String TAG = "WatchControlExtension";

  private static final boolean DEBUG = true;

  private static final int TOTP_TIMER_PERIOD = 1000;

  private Context mContext;
  private AccountDb mAccountDb;
  private OtpSource mOtpProvider;
  private TotpCounter mTotpCounter;
  private TotpClock mTotpClock;
  private Account[] mAccounts;

  private int mSelectedPosition;
  private Timer mTotpTimer;
  private long mTotpCurrentValue;
  private byte[] mTotpPieChart;

  public WatchControlExtension(Context context, String hostAppPackageName) {
    super(context, hostAppPackageName);

    mContext = context;
    mAccountDb = DependencyInjector.getAccountDb();
    mOtpProvider = DependencyInjector.getOtpProvider();
    mTotpCounter = mOtpProvider.getTotpCounter();
    mTotpClock = mOtpProvider.getTotpClock();
  }

  @Override
  public void onResume() {
    if (DEBUG) Log.d(TAG, "onResume()");
    queryAccounts();
    queryAccountPinAll(true);
    showList(mAccounts.length);

    mTotpTimer = new Timer();
    mTotpTimer.schedule(new TotpTimerTask(), 0, TOTP_TIMER_PERIOD);
  }

  @Override
  public void onPause() {
    if (DEBUG) Log.d(TAG, "onPause()");
    mTotpTimer.cancel();
    mTotpTimer = null;
  }

  @Override
  public void onRequestListItem(int layoutReference, int listItemPosition) {
    if (DEBUG) Log.d(TAG, String.format("onRequestListItem(int: %d, int: %d)", layoutReference, listItemPosition));
    showListItem(layoutReference, listItemPosition);
  }

  @Override
  public void onListItemClick(ControlListItem listItem, int clickType, int itemLayoutReference) {
    if (DEBUG)
      Log.d(TAG, String.format("onListItemClick(ControlListItem, int: %d, int: %d)", clickType, itemLayoutReference));
    queryAccountPin(listItem.listItemPosition);
    showListItem(listItem.layoutReference, listItem.listItemPosition);
  }

  @Override
  public void onListItemSelected(ControlListItem listItem) {
    if (DEBUG) Log.d(TAG, "onListItemSelected(ControlListItem)");
    mSelectedPosition = listItem.listItemPosition;
  }

  private void showList(int listCount) {
    String titleText;
    Bundle[] layoutData;

    titleText = mContext.getString(R.string.app_name);
    layoutData = new Bundle[1];
    layoutData[0] = new Bundle();
    layoutData[0].putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.text_view_title);
    layoutData[0].putString(Control.Intents.EXTRA_TEXT, titleText);

    showLayout(R.layout.sw2_control_list, layoutData);
    sendListCount(R.id.list_view, listCount);
    sendListPosition(R.id.list_view, mSelectedPosition);
  }

  private void showListItem(int layoutReference, int listItemPosition) {
    ControlListItem item;

    if (listItemPosition < 0 || listItemPosition >= mAccounts.length) {
      return;
    }

    item = new ControlListItem();
    item.layoutReference = layoutReference;
    item.dataXmlLayout = R.layout.sw2_control_list_item;
    item.listItemId = listItemPosition;
    item.listItemPosition = listItemPosition;
    item.layoutData = new Bundle[3];
    item.layoutData[0] = new Bundle();
    item.layoutData[0].putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.text_view_title);
    item.layoutData[0].putString(Control.Intents.EXTRA_TEXT, mAccounts[listItemPosition].name);
    item.layoutData[1] = new Bundle();
    item.layoutData[1].putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.text_view_value);
    item.layoutData[1].putString(Control.Intents.EXTRA_TEXT, mAccounts[listItemPosition].pin);
    item.layoutData[2] = new Bundle();
    item.layoutData[2].putInt(Control.Intents.EXTRA_LAYOUT_REFERENCE, R.id.image_view);
    if (mAccounts[listItemPosition].isHotp) {
      String uri;

      uri = ExtensionUtils.getUriString(mContext, R.drawable.sw2_refresh);
      item.layoutData[2].putString(Control.Intents.EXTRA_DATA_URI, uri);
    } else {
      item.layoutData[2].putByteArray(Control.Intents.EXTRA_DATA, mTotpPieChart);
    }

    sendListItem(item);
  }

  private void queryAccounts() {
    List<String> names;

    names = new ArrayList<>();
    mAccountDb.getNames(names);
    mAccounts = new Account[names.size()];
    for (int i = 0; i < mAccounts.length; i++) {
      mAccounts[i] = new Account();
      mAccounts[i].name = names.get(i);
      mAccounts[i].pin = mContext.getString(R.string.empty_pin);
      mAccounts[i].isHotp = mAccountDb.getType(names.get(i)) == AccountDb.OtpType.HOTP;
    }
  }

  private void queryAccountPinAll(boolean skipHotp) {
    if (mAccounts == null) {
      return;
    }

    for (Account account : mAccounts) {
      if (skipHotp && account.isHotp) {
        continue;
      }

      try {
        account.pin = mOtpProvider.getNextCode(account.name);
      } catch (OtpSourceException ignored) {
      }
    }
  }

  private void queryAccountPin(int index) {
    if (mAccounts == null || index < 0 || index >= mAccounts.length) {
      return;
    }

    try {
      mAccounts[index].pin = mOtpProvider.getNextCode(mAccounts[index].name);
    } catch (OtpSourceException ignored) {
    }
  }

  private byte[] getPieChart(int color, int angle) {
    Bitmap bitmap;
    Canvas canvas;
    RectF rect;
    Paint paint;
    ByteArrayOutputStream stream;
    byte[] byteArray;

    bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
    canvas = new Canvas(bitmap);
    rect = new RectF(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1);
    paint = new Paint();
    paint.setAntiAlias(true);
    paint.setColor(color);

    paint.setStyle(Paint.Style.FILL);
    canvas.drawArc(rect, angle - 90, 360 - angle, true, paint);

    paint.setStyle(Paint.Style.STROKE);
    canvas.drawOval(rect, paint);

    stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    bitmap.recycle();
    byteArray = stream.toByteArray();

    return byteArray;
  }

  private static class Account {
    public String name;
    public String pin;
    public boolean isHotp;
  }

  private class TotpTimerTask extends TimerTask {
    @Override
    public void run() {
      long currentTime;
      long currentValue;
      long currentStartTime;
      long nextStartTime;
      int angle;

      currentTime = mTotpClock.currentTimeMillis() / 1000;
      currentValue = mTotpCounter.getValueAtTime(currentTime);
      currentStartTime = mTotpCounter.getValueStartTime(currentValue);
      nextStartTime = mTotpCounter.getValueStartTime(currentValue + 1);

      if (currentValue != mTotpCurrentValue) {
        mTotpCurrentValue = currentValue;
        queryAccountPinAll(true);
      }

      angle = (int) (360 * (currentTime - currentStartTime) / (nextStartTime - currentStartTime));
      mTotpPieChart = getPieChart(mContext.getResources().getColor(R.color.sw2_pie_chart), angle);

      sendListCount(R.id.list_view, mAccounts.length);
    }
  }
}
