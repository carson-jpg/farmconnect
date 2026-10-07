package com.farmconnect.app.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Builds simple input dialogs in code: text, multi-line, number, phone, email, date and choice fields. */
public final class Forms {
    private Forms() {}

    public interface Submit { void ok(Map<String, String> values); }

    private static final int TEXT = 0, MULTI = 1, NUMBER = 2, DATE = 3, CHOICE = 4, PHONE = 5, EMAIL = 6;

    public static class Field {
        final String key, label; final int type; boolean required; String value = ""; String[] values, labels;
        private Field(String key, String label, int type, boolean required) {
            this.key = key; this.label = label; this.type = type; this.required = required;
        }
        public Field value(String v) { this.value = v == null ? "" : v; return this; }
    }

    public static Field text(String key, String label, boolean required) { return new Field(key, label, TEXT, required); }
    public static Field multi(String key, String label, boolean required) { return new Field(key, label, MULTI, required); }
    public static Field number(String key, String label, boolean required) { return new Field(key, label, NUMBER, required); }
    public static Field date(String key, String label, boolean required) { return new Field(key, label, DATE, required); }
    public static Field phone(String key, String label, boolean required) { return new Field(key, label, PHONE, required); }
    public static Field email(String key, String label, boolean required) { return new Field(key, label, EMAIL, required); }
    public static Field choice(String key, String label, boolean required, String[] values, String[] labels) {
        Field f = new Field(key, label, CHOICE, required);
        f.values = values;
        f.labels = labels;
        return f;
    }

    public static void show(Activity a, String title, String positive, List<Field> fields, Submit submit) {
        float d = a.getResources().getDisplayMetrics().density;
        ScrollView sv = new ScrollView(a);
        LinearLayout col = new LinearLayout(a);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = Math.round(20 * d);
        col.setPadding(pad, Math.round(8 * d), pad, 0);
        sv.addView(col);

        Map<Field, TextInputEditText> edits = new LinkedHashMap<>();
        Map<Field, TextInputLayout> tils = new LinkedHashMap<>();
        for (Field f : fields) {
            TextInputLayout til = new TextInputLayout(a, null, com.google.android.material.R.attr.textInputOutlinedStyle);
            til.setHint(f.label + (f.required ? " *" : ""));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Math.round(10 * d);
            col.addView(til, lp);
            TextInputEditText et = new TextInputEditText(til.getContext());
            til.addView(et, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            switch (f.type) {
                case MULTI:
                    et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                    et.setMinLines(4);
                    et.setGravity(android.view.Gravity.TOP | android.view.Gravity.START);
                    break;
                case NUMBER:
                    et.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                    break;
                case PHONE:
                    et.setInputType(InputType.TYPE_CLASS_PHONE);
                    break;
                case EMAIL:
                    et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
                    break;
                case CHOICE:
                case DATE:
                    et.setFocusable(false);
                    et.setClickable(true);
                    break;
                default:
                    et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            }
            if (f.type == CHOICE) {
                for (int i = 0; i < f.values.length; i++) if (f.values[i].equals(f.value)) et.setText(f.labels[i]);
                et.setOnClickListener(v -> new AlertDialog.Builder(a).setTitle(f.label)
                        .setItems(f.labels, (dlg, i) -> { f.value = f.values[i]; et.setText(f.labels[i]); til.setError(null); })
                        .show());
            } else if (f.type == DATE) {
                et.setText(f.value);
                et.setOnClickListener(v -> {
                    Calendar c = Calendar.getInstance();
                    new DatePickerDialog(a, (picker, y, m, day) -> {
                        f.value = String.format(java.util.Locale.US, "%04d-%02d-%02d", y, m + 1, day);
                        et.setText(f.value);
                        til.setError(null);
                    }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
                });
            } else {
                et.setText(f.value);
            }
            edits.put(f, et);
            tils.put(f, til);
        }

        AlertDialog dialog = new AlertDialog.Builder(a).setTitle(title).setView(sv)
                .setPositiveButton(positive, null).setNegativeButton("Cancel", null).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Map<String, String> out = new LinkedHashMap<>();
            boolean ok = true;
            for (Field f : fields) {
                String val = (f.type == CHOICE || f.type == DATE) ? f.value
                        : String.valueOf(edits.get(f).getText()).trim();
                if (f.required && val.isEmpty()) { tils.get(f).setError("Required"); ok = false; }
                out.put(f.key, val);
            }
            if (!ok) return;
            dialog.dismiss();
            submit.ok(out);
        });
    }

    /** Convenience for arrays used a lot in forms. */
    public static String[] arr(String... s) { return s; }

    static View.OnClickListener noop() { return v -> { }; }
}
