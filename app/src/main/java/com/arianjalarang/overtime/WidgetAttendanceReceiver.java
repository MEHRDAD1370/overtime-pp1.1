package com.arianjalarang.overtime;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Receives private widget-button taps without launching the activity. */
public class WidgetAttendanceReceiver extends BroadcastReceiver {
  public static final String ACTION = "com.arianjalarang.overtime.WIDGET_ATTENDANCE";
  private static final String PREFS = "overtime_widget";
  private static final String QUEUE_KEY = "widget_attendance_queue";

  @Override public void onReceive(Context context, Intent intent) {
    if (intent == null || !ACTION.equals(intent.getAction())) return;
    String type = intent.getStringExtra("attendance_type");
    if (!"in".equals(type) && !"out".equals(type)) return;
    SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    String person = prefs.getString("person", "");
    Date now = new Date();
    String date = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now);
    String time = new SimpleDateFormat("HH:mm", Locale.US).format(now);
    try {
      JSONArray queue;
      try { queue = new JSONArray(prefs.getString(QUEUE_KEY, "[]")); }
      catch (Exception ignored) { queue = new JSONArray(); }
      JSONObject event = new JSONObject();
      event.put("type", type); event.put("person", person); event.put("date", date); event.put("time", time);
      queue.put(event);
      prefs.edit().putString(QUEUE_KEY, queue.toString())
          .putString("widget_status", ("in".equals(type) ? "ورود" : "خروج") + " ثبت شد ✓  " + time).apply();
    } catch (Exception ignored) { return; }
    AppWidgetManager manager = AppWidgetManager.getInstance(context);
    int[] ids = manager.getAppWidgetIds(new ComponentName(context, OvertimeWidgetProvider.class));
    if (ids != null && ids.length > 0) {
      Intent refresh = new Intent(context, OvertimeWidgetProvider.class);
      refresh.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
      refresh.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
      context.sendBroadcast(refresh);
    }
  }
}
