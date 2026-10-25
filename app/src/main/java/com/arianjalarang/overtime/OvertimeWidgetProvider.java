package com.arianjalarang.overtime;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;
import android.graphics.Color;
import android.net.Uri;
import android.app.PendingIntent;

public class OvertimeWidgetProvider extends AppWidgetProvider {
  @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
    SharedPreferences p = context.getSharedPreferences("overtime_widget", Context.MODE_PRIVATE);
    String overtime = p.getString("overtime", "۰:۰۰");
    String amount = p.getString("amount", "۰ ریال");
    int accent = color(p.getString("accent", "#39d0b4"), Color.rgb(57,208,180));
    int bg = color(p.getString("background", "#101c2f"), Color.rgb(16,28,47));
    for (int id : ids) {
      RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.overtime_widget);
      v.setTextViewText(R.id.widget_overtime, overtime);
      v.setTextViewText(R.id.widget_amount, "مبلغ دریافتی: " + amount);
      v.setTextColor(R.id.widget_overtime, accent);
      v.setTextColor(R.id.widget_title, accent);
      v.setTextColor(R.id.widget_amount, Color.WHITE);
      v.setInt(R.id.widget_root, "setBackgroundColor", bg);
      Intent launch = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
      if (launch != null) {
        PendingIntent pi = PendingIntent.getActivity(context, 712, launch, PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
        v.setOnClickPendingIntent(R.id.widget_root, pi);
      }
      manager.updateAppWidget(id, v);
    }
  }
  private int color(String value, int fallback) { try { return Color.parseColor(value); } catch (Exception e) { return fallback; } }
}
