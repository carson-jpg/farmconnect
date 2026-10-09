package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.Review;
import com.farmconnect.app.data.Models.SellerReviewSummary;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.Ui;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Ratings and reviews of one seller. Sellers can reply once; admins can remove abusive reviews. */
public class ReviewsActivity extends BaseListActivity {
    private long sellerId;

    @Override protected String screenTitle() { return I18n.t("Seller reviews"); }
    @Override protected String emptyText() { return I18n.t("No reviews yet.\nBuyers can rate a seller after receiving an order."); }

    @Override protected void load() {
        sellerId = getIntent().getLongExtra("sellerId", -1);
        if (sellerId < 0) { finish(); return; }
        Ui.watch(this, vm.sellerReviews(sellerId), b.progress, this::render);
    }

    private void render(SellerReviewSummary s) {
        b.tvTitle.setText(s.sellerName == null ? I18n.t("Seller reviews") : s.sellerName);
        boolean mine = Session.id() == sellerId;
        boolean admin = "ADMIN".equals(Session.role());

        LinearLayout top = b.topArea;
        top.removeAllViews();
        top.setPadding(dp(14), dp(10), dp(14), 0);
        MaterialCardView card = Cards.card(this, top, 0);
        LinearLayout in = Cards.inner(card);
        if (s.count == 0) {
            in.addView(Cards.text(this, I18n.t("No ratings yet"), 16, 0xFF5E7061, true));
        } else {
            TextView avg = Cards.text(this, String.format(Locale.US, "%.1f", s.average) + "  ★", 34, 0xFFB26A00, true);
            avg.setTypeface(android.graphics.Typeface.SERIF, android.graphics.Typeface.BOLD);
            in.addView(avg);
            in.addView(Cards.text(this, s.count + (s.count == 1 ? I18n.t(" review") : I18n.t(" reviews")) + (s.verified ? I18n.t("  ·  ✓ Verified seller") : ""), 13, 0xFF5E7061, false));
            in.addView(Cards.spacer(this, 8));
            java.util.Map<String, Integer> bars = new java.util.LinkedHashMap<>();
            for (int star = 5; star >= 1; star--) bars.put(star + " ★", s.distribution == null ? 0 : s.distribution[star - 1]);
            Cards.bars(this, in, bars, 0xFFF9A825, null);
        }

        List<Row> rows = new ArrayList<>();
        for (Review r : s.reviews) {
            StringBuilder sub = new StringBuilder();
            if (r.comment != null && !r.comment.isEmpty()) sub.append(r.comment);
            if (r.reply != null && !r.reply.isEmpty()) sub.append(sub.length() > 0 ? "\n" : "").append(I18n.t("↩ Seller: ")).append(r.reply);
            Row row = Row.of("👤", Labels.stars(r.rating) + "  " + r.buyerName)
                    .sub(sub.length() == 0 ? null : sub.toString()).meta(Ui.dateOnly(r.createdAt));
            if (mine && (r.reply == null || r.reply.isEmpty())) row.action(I18n.t("Reply"), () -> reply(r));
            else if (admin) row.action(I18n.t("Remove"), () -> new AlertDialog.Builder(this)
                    .setTitle(I18n.t("Remove this review?"))
                    .setPositiveButton(I18n.t("Remove"), (d, w) -> Ui.watch(this, vm.deleteReview(r.id), b.progress, x -> load()))
                    .setNegativeButton(I18n.t("Cancel"), null).show());
            rows.add(row);
        }
        show(rows, s.count == 0 ? "" : String.format(Locale.US, I18n.t("%.1f ★ · %d reviews"), s.average, s.count));
    }

    private void reply(Review r) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("reply", I18n.t("Your reply (public, one reply only)"), true));
        Forms.show(this, I18n.t("Reply to ") + r.buyerName, I18n.t("Send"), f, v ->
                Ui.watch(this, vm.replyReview(r.id, v.get("reply")), b.progress, x -> { Ui.toast(this, I18n.t("Reply posted")); load(); }));
    }
}
