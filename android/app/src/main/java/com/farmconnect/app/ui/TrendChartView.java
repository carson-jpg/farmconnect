package com.farmconnect.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import com.farmconnect.app.data.Models.TrendPoint;
import java.util.ArrayList;
import java.util.List;

/** A small line chart: past prices as a solid green line, the forecast as a dashed amber continuation. */
public class TrendChartView extends View {
    private List<TrendPoint> history = new ArrayList<>(), forecast = new ArrayList<>();
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG), dash = new Paint(Paint.ANTI_ALIAS_FLAG),
            dot = new Paint(Paint.ANTI_ALIAS_FLAG), grid = new Paint(Paint.ANTI_ALIAS_FLAG), label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float d;

    public TrendChartView(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        line.setColor(0xFF2E7D32); line.setStyle(Paint.Style.STROKE); line.setStrokeWidth(2.5f * d); line.setStrokeJoin(Paint.Join.ROUND);
        dash.setColor(0xFFF9A825); dash.setStyle(Paint.Style.STROKE); dash.setStrokeWidth(2.5f * d);
        dash.setPathEffect(new DashPathEffect(new float[]{8 * d, 6 * d}, 0));
        dot.setColor(0xFFF9A825); dot.setStyle(Paint.Style.FILL);
        grid.setColor(0xFFE3ECE0); grid.setStrokeWidth(1f * d);
        label.setColor(0xFF5E7061); label.setTextSize(10.5f * d);
    }

    public void set(List<TrendPoint> history, List<TrendPoint> forecast) {
        this.history = history == null ? new ArrayList<>() : history;
        this.forecast = forecast == null ? new ArrayList<>() : forecast;
        invalidate();
    }

    @Override protected void onMeasure(int w, int h) {
        setMeasuredDimension(resolveSize(0, w), Math.round(200 * d));
    }

    @Override protected void onDraw(Canvas cv) {
        super.onDraw(cv);
        List<TrendPoint> all = new ArrayList<>(history);
        all.addAll(forecast);
        if (history.size() < 2) {
            label.setTextAlign(Paint.Align.CENTER);
            cv.drawText("—", getWidth() / 2f, getHeight() / 2f, label);
            return;
        }
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (TrendPoint p : all) { lo = Math.min(lo, p.price); hi = Math.max(hi, p.price); }
        if (hi - lo < 1) { hi += 1; lo -= 1; }
        double pad = (hi - lo) * 0.12; lo -= pad; hi += pad;
        float left = 54 * d, right = 12 * d, top = 12 * d, bottom = 26 * d;
        float w = getWidth() - left - right, h = getHeight() - top - bottom;
        int n = all.size();
        label.setTextAlign(Paint.Align.RIGHT);
        for (int i = 0; i <= 3; i++) {
            float y = top + h * i / 3f;
            cv.drawLine(left, y, left + w, y, grid);
            cv.drawText(String.valueOf(Math.round(hi - (hi - lo) * i / 3.0)), left - 6 * d, y + 4 * d, label);
        }
        Path solid = new Path(), dashed = new Path();
        for (int i = 0; i < n; i++) {
            float x = left + w * i / (float) (n - 1);
            float y = top + (float) ((hi - all.get(i).price) / (hi - lo)) * h;
            if (i < history.size()) { if (i == 0) solid.moveTo(x, y); else solid.lineTo(x, y); }
            if (i == history.size() - 1) dashed.moveTo(x, y);
            else if (i >= history.size()) { dashed.lineTo(x, y); cv.drawCircle(x, y, 4 * d, dot); }
        }
        cv.drawPath(solid, line);
        if (!forecast.isEmpty()) cv.drawPath(dashed, dash);
        label.setTextAlign(Paint.Align.LEFT);
        cv.drawText(shortDate(all.get(0).date), left, getHeight() - 6 * d, label);
        label.setTextAlign(Paint.Align.RIGHT);
        cv.drawText(shortDate(all.get(n - 1).date), left + w, getHeight() - 6 * d, label);
    }

    private static String shortDate(String iso) {
        return iso != null && iso.length() >= 10 ? iso.substring(8, 10) + "/" + iso.substring(5, 7) : "";
    }
}
