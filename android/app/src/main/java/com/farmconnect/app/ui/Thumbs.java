package com.farmconnect.app.ui;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.bumptech.glide.Glide;
import com.farmconnect.app.R;
import com.farmconnect.app.data.ApiClient;
import com.google.android.material.card.MaterialCardView;

/** Small rounded product photo used in order and cart lists. */
final class Thumbs {
    private Thumbs() {}

    static MaterialCardView make(Context c, String url, int sizeDp, int marginEndDp) {
        float d = c.getResources().getDisplayMetrics().density;
        MaterialCardView card = new MaterialCardView(c);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(sizeDp * d), Math.round(sizeDp * d));
        lp.setMarginEnd(Math.round(marginEndDp * d));
        card.setLayoutParams(lp);
        card.setRadius(Math.round(10 * d));
        card.setCardElevation(0);
        ImageView iv = new ImageView(c);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(iv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        if (url != null && !url.isEmpty()) {
            Glide.with(c).load(ApiClient.absolute(url)).centerCrop()
                    .placeholder(R.drawable.ph_product).error(R.drawable.ph_product).into(iv);
        } else {
            iv.setImageResource(R.drawable.ph_product);
        }
        return card;
    }
}
