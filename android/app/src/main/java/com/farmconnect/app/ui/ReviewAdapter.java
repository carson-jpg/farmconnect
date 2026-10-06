package com.farmconnect.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.ReviewSummary;
import com.farmconnect.app.databinding.ItemReviewBinding;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.VH> {
    public interface Listener { void onOpen(ReviewSummary r); }

    private final Listener listener;
    private List<ReviewSummary> items = new ArrayList<>();

    public ReviewAdapter(Listener l) { listener = l; }

    public void set(List<ReviewSummary> list) { items = list == null ? new ArrayList<>() : list; notifyDataSetChanged(); }

    static class VH extends RecyclerView.ViewHolder {
        final ItemReviewBinding b;
        VH(ItemReviewBinding b) { super(b.getRoot()); this.b = b; }
    }

    @Override public VH onCreateViewHolder(ViewGroup parent, int type) {
        return new VH(ItemReviewBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(VH h, int pos) {
        ReviewSummary r = items.get(pos);
        h.b.tvName.setText(r.fullName == null || r.fullName.isEmpty() ? "(no name)" : r.fullName);
        h.b.tvLoc.setText((r.subCounty == null ? "" : r.subCounty) + (r.ward == null || r.ward.isEmpty() ? "" : " · " + r.ward));
        h.b.tvDate.setText(r.submittedAt == null ? "" : "Submitted " + Ui.dateTime(r.submittedAt));
        Ui.styleVerification(h.b.tvStatus, r.status);
        h.b.getRoot().setOnClickListener(v -> listener.onOpen(r));
    }

    @Override public int getItemCount() { return items.size(); }
}