package com.farmconnect.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.Post;
import com.farmconnect.app.databinding.ActivityPostDetailBinding;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CommunityViewModel;

public class PostDetailActivity extends AppCompatActivity {
    private ActivityPostDetailBinding b;
    private CommunityViewModel vm;
    private Post post;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        b = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        vm = new ViewModelProvider(this).get(CommunityViewModel.class);
        b.btnBack.setOnClickListener(v -> finish());
        long id = getIntent().getLongExtra("id", -1);
        if (id < 0) { finish(); return; }
        Ui.watch(this, vm.post(id), b.progress, this::render);
    }

    private void render(Post p) {
        post = p;
        b.tvType.setText(Labels.postIcon(p.type) + "  " + Labels.name(Labels.POST_TYPES, Labels.POST_NAMES, p.type));
        b.tvPostTitle.setText(p.title);
        b.tvMeta.setText((p.authorName == null ? "County" : p.authorName) + " · " + Ui.dateOnly(p.createdAt) + " · 👁 " + p.views);
        b.tvBody.setText(p.body);

        StringBuilder ev = new StringBuilder();
        if (p.eventDate != null) ev.append("📅 ").append(Ui.dateOnly(p.eventDate));
        if (p.location != null && !p.location.isEmpty()) ev.append(ev.length() > 0 ? "\n" : "").append("📍 ").append(p.location);
        b.tvEvent.setVisibility(ev.length() > 0 ? View.VISIBLE : View.GONE);
        b.tvEvent.setText(ev.toString());

        boolean hasContact = p.contact != null && !p.contact.trim().isEmpty();
        b.tvContact.setVisibility(hasContact ? View.VISIBLE : View.GONE);
        if (hasContact) b.tvContact.setText("Contact: " + p.contact);

        boolean registrable = "PROGRAMME".equals(p.type) || "TRAINING".equals(p.type) || "OPPORTUNITY".equals(p.type);
        b.btnRegister.setVisibility(registrable ? View.VISIBLE : View.GONE);
        if (registrable) {
            b.btnRegister.setText(p.registered ? "✓ Registered · tap to cancel" : "Register my interest  (" + p.registrations + " so far)");
            b.btnRegister.setOnClickListener(v -> Ui.watch(this, vm.togglePostRegistration(p.id), b.progress, u -> {
                Ui.toast(this, u.registered ? "You are registered" : "Registration cancelled");
                render(u);
            }));
        }
        String digits = hasContact ? p.contact.replaceAll("[^0-9+]", "") : "";
        boolean phone = digits.length() >= 9;
        b.btnCall.setVisibility(phone ? View.VISIBLE : View.GONE);
        b.btnCall.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + digits))));

        boolean staff = Labels.isStaff();
        b.btnDelete.setVisibility(staff ? View.VISIBLE : View.GONE);
        b.btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("Delete this post?")
                .setMessage("It disappears for everyone. Notifications already sent stay in people's inbox.")
                .setPositiveButton("Delete", (d, w) -> Ui.watch(this, vm.deletePost(p.id), b.progress, x -> {
                    Ui.toast(this, "Post deleted");
                    finish();
                }))
                .setNegativeButton("Cancel", null).show());
        b.actionBar.setVisibility(registrable || phone || staff ? View.VISIBLE : View.GONE);
    }
}
