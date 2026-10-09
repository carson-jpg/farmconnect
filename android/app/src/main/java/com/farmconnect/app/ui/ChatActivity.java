package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.farmconnect.app.data.Models.ChatMessage;
import com.farmconnect.app.data.Resource;
import com.farmconnect.app.databinding.ActivityChatBinding;
import com.farmconnect.app.databinding.ItemMessageBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CommunityViewModel;
import java.util.ArrayList;
import java.util.List;

/** One-to-one conversation. New messages are fetched every few seconds while the screen is open. */
public class ChatActivity extends AppCompatActivity {
    private static final long POLL_MS = 5000;

    private ActivityChatBinding b;
    private CommunityViewModel vm;
    private long otherId;
    private final Adapter adapter = new Adapter();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable poll = new Runnable() {
        @Override public void run() { refresh(false); handler.postDelayed(this, POLL_MS); }
    };

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CommunityViewModel.class);
        otherId = getIntent().getLongExtra("userId", -1);
        if (otherId < 0) { finish(); return; }
        String name = getIntent().getStringExtra("name");
        b.tvTitle.setText(name == null ? I18n.t("Chat") : name);
        b.tvSubtitle.setText(I18n.t("Messages are visible only to you two"));
        b.btnBack.setOnClickListener(v -> finish());
        LinearLayoutManager lm = new LinearLayoutManager(this);
        lm.setStackFromEnd(true);
        b.rv.setLayoutManager(lm);
        b.rv.setAdapter(adapter);
        b.btnSend.setOnClickListener(v -> send());
    }

    @Override protected void onResume() {
        super.onResume();
        handler.post(poll);
    }

    @Override protected void onPause() {
        super.onPause();
        handler.removeCallbacks(poll);
    }

    private void refresh(boolean scroll) {
        vm.thread(otherId).observe(this, res -> {
            if (res.status != Resource.Status.SUCCESS || res.data == null) return;
            boolean grew = res.data.size() != adapter.getItemCount();
            adapter.set(res.data);
            b.tvEmpty.setVisibility(res.data.isEmpty() ? View.VISIBLE : View.GONE);
            if (grew || scroll) b.rv.scrollToPosition(Math.max(0, adapter.getItemCount() - 1));
        });
    }

    private void send() {
        String text = b.etMessage.getText().toString().trim();
        if (text.isEmpty()) return;
        b.btnSend.setEnabled(false);
        vm.sendMessage(otherId, text).observe(this, res -> {
            if (res.status == Resource.Status.LOADING) return;
            b.btnSend.setEnabled(true);
            if (res.status == Resource.Status.ERROR) { Ui.toast(this, res.message); return; }
            b.etMessage.setText("");
            refresh(true);
        });
    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        private List<ChatMessage> items = new ArrayList<>();

        void set(List<ChatMessage> l) { items = l; notifyDataSetChanged(); }

        static class VH extends RecyclerView.ViewHolder {
            final ItemMessageBinding b;
            VH(ItemMessageBinding b) { super(b.getRoot()); this.b = b; }
        }

        @Override public VH onCreateViewHolder(ViewGroup p, int t) {
            return new VH(ItemMessageBinding.inflate(LayoutInflater.from(p.getContext()), p, false));
        }

        @Override public void onBindViewHolder(VH h, int pos) {
            ChatMessage m = items.get(pos);
            h.b.tvBubble.setText(m.body);
            h.b.tvTime.setText(Ui.dateTime(m.createdAt));
            h.b.tvBubble.setBackgroundTintList(android.content.res.ColorStateList.valueOf(m.mine ? 0xFF2E7D32 : 0xFFFFFFFF));
            h.b.tvBubble.setTextColor(m.mine ? 0xFFFFFFFF : 0xFF1C2B1E);
            int g = m.mine ? android.view.Gravity.END : android.view.Gravity.START;
            ((LinearLayout.LayoutParams) h.b.tvBubble.getLayoutParams()).gravity = g;
            ((LinearLayout.LayoutParams) h.b.tvTime.getLayoutParams()).gravity = g;
            h.b.tvBubble.requestLayout();
        }

        @Override public int getItemCount() { return items.size(); }
    }
}
