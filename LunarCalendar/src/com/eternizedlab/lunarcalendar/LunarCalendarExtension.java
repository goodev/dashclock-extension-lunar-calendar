//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
package com.eternizedlab.lunarcalendar;

import java.util.Calendar;
import java.util.Locale;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;

import com.eternizedlab.lunarcalendar.LunarCalendar.LunarDate;
import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

public class LunarCalendarExtension extends DashClockExtension {
  private static final String TAG = LunarCalendarExtension.class
      .getSimpleName();
  private LunarCalendar calendar = new LunarCalendar();

  public static final String PREF_STATUS_NUMBER_OF_LINES = "pref_status_number_of_lines";
  public static final String PREF_NEXT_SPECIAL = "pref_next_special";
  public static final String PREF_LANGUAGE = "pref_language";
  public static final String PREF_SHORTCUT = "pref_shortcut";

  private static final Intent BUILTIN_CALENDAR_INTENT = new Intent(
      Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,
          Intent.CATEGORY_APP_CALENDAR))
      .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  private static final Intent ONLINE_CALENDAR_INTENT = new Intent(
      Intent.ACTION_VIEW,
      Uri.parse("http://www.baidu.com/s?wd=%E4%B8%87%E5%B9%B4%E5%8E%86"));

  private LunarRenderer setupRenderer(SharedPreferences sp) {
    String numStatusLines = sp.getString(PREF_STATUS_NUMBER_OF_LINES, "1");
    String language = sp.getString(PREF_LANGUAGE,
        getString(R.string.pref_language_default_value));
    LunarRenderer renderer = "cn_traditional".equals(language) ? TraditionalLunarRenderer
        .getInstance() : DigitalLunarRenderer.getInstance();
    renderer.setNumStatusLine("1".equals(numStatusLines) ? 1 : 2);
    RenderHelper.setLocale(getDisplayLocale(language));
    return renderer;
  }

  private String getNextSpecialDay(SharedPreferences sp, LunarRenderer renderer) {
    if (sp.getBoolean(PREF_NEXT_SPECIAL, true)) {
      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.DAY_OF_MONTH, 1);
      LunarDate ld = calendar.transformLunarDate(cal);
      int daysLeft = 1;
      while (ld.holidayIdx == -1 && ld.termIdx == -1) {
        daysLeft++;
        ld = calendar.nextDay(ld, cal);
        cal.add(Calendar.DAY_OF_MONTH, 1);
      }
      return renderer.getNextSpecialDay(ld, daysLeft);
    } else {
      return "";
    }
  }

  @Override
  protected void onUpdateData(int reason) {
    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

    Intent clickIntent = AppChooserPreference.getIntentValue(
        sp.getString(PREF_SHORTCUT, null), ONLINE_CALENDAR_INTENT);

    LunarRenderer renderer = setupRenderer(sp);
    LunarDate date = calendar.transformLunarDate(Calendar.getInstance());
    String status = renderer.getDisplayStatus(date);
    String expandTitle = renderer.getDisplayExpandedTitle(date);
    String expandBody = renderer.getDisplayExpandedBody(date)
        + getNextSpecialDay(sp, renderer);

    Log.v(TAG, status + "|" + expandTitle + "|" + expandBody);
    // Publish the extension data update.
    publishUpdate(new ExtensionData().visible(true)
        .icon(R.drawable.ic_extension_lunarcalendar).status(status)
        .expandedTitle(expandTitle).expandedBody(expandBody)
        .clickIntent(clickIntent));
  }

  private Locale getDisplayLocale(String language) {
    if ("en".equals(language)) {
      return Locale.ENGLISH;
    } else {
      return Locale.CHINESE;
    }
  }

  @Override
  protected void onInitialize(boolean isReconnect) {
    super.onInitialize(isReconnect);
    setUpdateWhenScreenOn(true);
    RenderHelper.initialize(this);
  }

}
