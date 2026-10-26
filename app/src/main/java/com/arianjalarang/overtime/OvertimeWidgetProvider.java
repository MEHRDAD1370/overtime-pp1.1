package com.arianjalarang.overtime;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.graphics.Color;
import android.app.PendingIntent;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class OvertimeWidgetProvider extends AppWidgetProvider {
  public static final String ACTION_WIDGET_ATTENDANCE = "com.arianjalarang.overtime.WIDGET_ATTENDANCE";
  private static final String PREFS = "overtime_widget";
  private static final String QUEUE_KEY = "widget_attendance_queue";

  @Override public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);
  }

  @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
    SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    String overtime = p.getString("overtime", "۰:۰۰");
    String amount = p.getString("amount", "۰ ریال");
    String status = p.getString("widget_status", "ثبت سریع ورود و خروج");
    int tileColor; try { tileColor = Color.parseColor(p.getString("accent", "#13E0A2")); } catch (Exception e) { tileColor = Color.parseColor("#13E0A2"); }
    for (int id : ids) {
      RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.overtime_widget);
      v.setInt(R.id.widget_root, "setBackgroundColor", tileColor);
      v.setTextViewText(R.id.widget_overtime, overtime);
      v.setTextViewText(R.id.widget_amount, "مبلغ دریافتی: " + amount);
      v.setTextViewText(R.id.widget_status, status);
      v.setTextColor(R.id.widget_overtime, Color.WHITE);
      v.setTextColor(R.id.widget_title, Color.WHITE);
      v.setInt(R.id.widget_clock, "setColorFilter", Color.WHITE);
      v.setTextColor(R.id.widget_amount, Color.WHITE);
      v.setTextColor(R.id.widget_status, Color.WHITE);

      Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
      if (launch != null) {
        PendingIntent pi = PendingIntent.getActivity(context, 712, launch, PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        v.setOnClickPendingIntent(R.id.widget_root, pi);
      }
      v.setOnClickPendingIntent(R.id.widget_check_in, attendancePendingIntent(context, "in", 721));
      v.setOnClickPendingIntent(R.id.widget_check_out, attendancePendingIntent(context, "out", 722));
      manager.updateAppWidget(id, v);
    }
  }

  private PendingIntent attendancePendingIntent(Context context, String type, int requestCode) {
    Intent intent = new Intent(context, WidgetAttendanceReceiver.class);
    intent.setAction(WidgetAttendanceReceiver.ACTION);
    intent.putExtra("attendance_type", type);
    return PendingIntent.getBroadcast(context, requestCode, intent,
      PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
  }
}
