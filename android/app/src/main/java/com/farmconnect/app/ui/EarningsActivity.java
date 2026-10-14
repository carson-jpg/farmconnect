package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Earning;
import com.farmconnect.app.data.Models.Earnings;
import com.farmconnect.app.data.Models.PayoutAccount;
import com.farmconnect.app.databinding.ActivitySimplePageBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.FarmerViewModel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/** A farmer's money: what has been paid out, what is on the way, each sale's state, and the payout phone number. */
public class EarningsActivity extends AppCompatActivity {
    private ActivitySimplePageBinding b;
    private FarmerViewModel vm;
    private LinearLayout statsBox, accountBox, recentBox;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivitySimplePageBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(FarmerViewModel.class);
        b.tvTitle.setText(I18n.t("My earnings"));
        b.tvSubtitle.setText(I18n.t("Sales paid for in the app"));
        b.btnBack.setOnClickListener(v -> finish());
        statsBox = box();
        accountBox = box();
        recentBox = box();
    }

    @Override protected void onResume() {
        super.onResume();
        Ui.watch(this, vm.earnings(), b.progress, this::renderEarnings);
        Ui.watch(this, vm.payoutAccount(), null, this::renderAccount);
    }

    private LinearLayout box() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        b.content.addView(l, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return l;
    }

    private void renderEarnings(Earnings e) {
        statsBox.removeAllViews();
        LinearLayout row = Cards.statRow(this, statsBox);
        row.addView(Cards.stat(this, Ui.kes(e.paidKes), I18n.t("Paid to you")));
        row.addView(Cards.stat(this, Ui.kes(e.waitingKes), I18n.t("On the way to you")));

        recentBox.removeAllViews();
        MaterialCardView card = Cards.card(this, recentBox, 12);
        LinearLayout in = Cards.inner(card);
        in.addView(Cards.title(this, I18n.t("Recent sales")));
        in.addView(Cards.spacer(this, 6));
        if (e.recent == null || e.recent.isEmpty()) {
            in.addView(Cards.text(this, I18n.t("Nothing yet. When a buyer pays through FarmConnect, the sale shows here."), 14, 0xFF6B7A6D, false));
        } else {
            for (Earning x : e.recent) {
                LinearLayout line = new LinearLayout(this);
                line.setOrientation(LinearLayout.HORIZONTAL);
                line.setGravity(Gravity.CENTER_VERTICAL);
                line.setPadding(0, Cards.dp(this, 7), 0, Cards.dp(this, 7));
                LinearLayout left = new LinearLayout(this);
                left.setOrientation(LinearLayout.VERTICAL);
                left.addView(Cards.text(this, I18n.t("Order #") + x.orderId, 14, 0xFF1C2B1E, true));
                left.addView(Cards.text(this, statusText(x.status) + (x.date == null ? "" : " · " + Ui.dateOnly(x.date)), 12, 0xFF6B7A6D, false));
                line.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                line.addView(Cards.text(this, Ui.kes(x.amountKes), 14, 0xFF2E7D32, true));
                in.addView(line);
            }
        }
        in.addView(Cards.spacer(this, 6));
        in.addView(Cards.text(this, I18n.t("Each sale is split automatically. You are paid the farmer share to your M-Pesa after the order is delivered and a short waiting period."), 12, 0xFF6B7A6D, false));
    }

    private void renderAccount(PayoutAccount a) {
        accountBox.removeAllViews();
        MaterialCardView card = Cards.card(this, accountBox, 12);
        LinearLayout in = Cards.inner(card);
        in.addView(Cards.title(this, I18n.t("Payout number")));
        in.addView(Cards.spacer(this, 6));
        String st = a.status == null ? "NONE" : a.status;
        String msg;
        switch (st) {
            case "VERIFIED": msg = I18n.t("Ready. Your payments go to this M-Pesa number."); break;
            case "PENDING": msg = I18n.t("Waiting for approval. Payments are held until it is approved."); break;
            case "REJECTED": msg = I18n.t("This number was not approved. Please enter another one."); break;
            default: msg = I18n.t("No payout number yet. Verify your phone, or add one here.");
        }
        if (a.phone != null && !a.phone.isEmpty()) in.addView(Cards.text(this, "📱 " + a.phone, 16, 0xFF1C2B1E, true));
        in.addView(Cards.text(this, msg, 13, 0xFF6B7A6D, false));
        MaterialButton change = new MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        change.setText(I18n.t("Change number"));
        change.setAllCaps(false);
        change.setOnClickListener(v -> askPhone());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Cards.dp(this, 10);
        in.addView(change, lp);
    }

    private void askPhone() {
        EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_PHONE);
        et.setHint("07XXXXXXXX");
        new AlertDialog.Builder(this)
                .setTitle(I18n.t("New payout number"))
                .setMessage(I18n.t("A new number must be approved by FarmConnect before we send money to it."))
                .setView(et)
                .setPositiveButton(I18n.t("Save"), (d, w) -> {
                    String p = et.getText().toString().replaceAll("[\\s-]", "");
                    if (!p.matches("\\+?\\d{9,13}")) { Ui.toast(this, "Enter a valid phone number"); return; }
                    Ui.watch(this, vm.setPayoutPhone(p), b.progress, this::renderAccount);
                })
                .setNegativeButton(I18n.t("Cancel"), null)
                .show();
    }

    private static String statusText(String s) {
        if (s == null) return "";
        switch (s) {
            case "WAITING_FOR_DELIVERY": return I18n.t("Waiting for delivery");
            case "PAYING": return I18n.t("Being paid out");
            case "PAID": return I18n.t("Paid");
            case "ON_HOLD": return I18n.t("On hold, contact support");
            case "REFUNDED": return I18n.t("Refunded to the buyer");
            default: return s;
        }
    }
}
