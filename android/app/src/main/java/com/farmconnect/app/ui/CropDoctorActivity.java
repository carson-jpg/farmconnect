package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.net.Uri;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.farmconnect.app.data.Models.CropCase;
import com.farmconnect.app.data.Models.CropDiagnosis;
import com.farmconnect.app.data.Models.CropGuide;
import com.farmconnect.app.data.Models.CropMatch;
import com.farmconnect.app.data.Models.CropSymptom;
import com.farmconnect.app.data.Session;
import com.farmconnect.app.util.ImageUtil;
import com.farmconnect.app.util.Ui;
import java.util.ArrayList;
import java.util.List;

/**
 * Crop doctor. Farmers tick the signs they see, get likely causes from the built-in guide at once, and can send the case
 * (with up to 3 photos) to the county officer. County staff see the cases from their own area and reply.
 */
public class CropDoctorActivity extends BaseListActivity {
    private CropGuide guide;
    private long photoCaseId;
    private final ActivityResultLauncher<String> picker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> { if (uri != null) upload(uri); });

    private static boolean isFarmer() { return "FARMER".equals(Session.role()); }

    @Override protected String screenTitle() { return isFarmer() ? I18n.t("Crop doctor") : I18n.t("Crop cases"); }
    @Override protected String emptyText() {
        return isFarmer() ? I18n.t("No cases yet. Tap the button to check a sick plant.") : I18n.t("No crop cases here");
    }
    @Override protected String[][] chips() {
        return isFarmer() ? null : new String[][]{{"OPEN", "Waiting"}, {"ANSWERED", "Answered"}, {"CLOSED", "Closed"}, {"ALL", "All"}};
    }
    @Override protected String fabText() { return isFarmer() ? I18n.t("🩺 Check a plant") : null; }
    @Override protected void onFab() { start(); }

    private static String statusName(String s) {
        if ("ANSWERED".equals(s)) return I18n.t("Answered");
        if ("CLOSED".equals(s)) return I18n.t("Closed");
        return isFarmer() ? I18n.t("Sent to officer") : I18n.t("Waiting");
    }

    private static int statusColor(String s) {
        if ("ANSWERED".equals(s)) return 0xFF2E7D32;
        if ("CLOSED".equals(s)) return 0xFF757575;
        return 0xFFF9A825;
    }

    @Override protected void load() {
        if (vm == null) return;
        Ui.watch(this, vm.cropCases(isFarmer() ? null : chip), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (CropCase c : list) {
                CropMatch top = c.matches == null || c.matches.isEmpty() ? null : c.matches.get(0);
                String sub = isFarmer() ? (top == null ? I18n.t("No guess from the guide") : top.name)
                        : c.farmerName + (c.subCounty == null ? "" : " · " + c.subCounty);
                rows.add(Row.of("🩺", c.crop).sub(sub)
                        .meta(Ui.dateTime(c.createdAt) + (c.photos == null || c.photos.isEmpty() ? "" : " · 📷 " + c.photos.size()))
                        .badge(statusName(c.status), statusColor(c.status))
                        .unread(isFarmer() && "ANSWERED".equals(c.status))
                        .click(() -> showCase(c)));
            }
            show(rows, list.size() + I18n.t(" cases"));
        });
    }

    // ------------------------------------------------------------ farmer: check a plant

    private void start() {
        if (guide == null) {
            Ui.watch(this, vm.cropGuide(), b.progress, g -> { guide = g; start(); });
            return;
        }
        List<String> names = new ArrayList<>(guide.crops);
        names.add(I18n.t("Another crop"));
        new AlertDialog.Builder(this).setTitle(I18n.t("Which crop is it?"))
                .setItems(names.toArray(new String[0]), (d, which) -> {
                    if (which < guide.crops.size()) pickSymptoms(guide.crops.get(which));
                    else {
                        List<Forms.Field> f = new ArrayList<>();
                        f.add(Forms.text("crop", I18n.t("Crop name"), true));
                        Forms.show(this, I18n.t("Another crop"), I18n.t("Next"), f, v -> pickSymptoms(v.get("crop")));
                    }
                })
                .setNegativeButton(I18n.t("Cancel"), null).show();
    }

    private void pickSymptoms(String crop) {
        String[] labels = new String[guide.symptoms.size()];
        boolean[] on = new boolean[labels.length];
        for (int i = 0; i < labels.length; i++) {
            CropSymptom s = guide.symptoms.get(i);
            labels[i] = s.icon + "  " + I18n.t(s.label);
        }
        new AlertDialog.Builder(this).setTitle(I18n.t("What do you see? (up to 12)"))
                .setMultiChoiceItems(labels, on, (d, i, checked) -> on[i] = checked)
                .setPositiveButton(I18n.t("Find the cause"), (d, w) -> {
                    List<String> keys = new ArrayList<>();
                    for (int i = 0; i < on.length; i++) if (on[i] && keys.size() < 12) keys.add(guide.symptoms.get(i).key);
                    if (keys.isEmpty()) { Ui.toast(this, I18n.t("Tick at least one sign")); return; }
                    Ui.watch(this, vm.cropDiagnose(crop, keys), b.progress, r -> showDiagnosis(crop, keys, r));
                })
                .setNegativeButton(I18n.t("Cancel"), null).show();
    }

    private void showDiagnosis(String crop, List<String> keys, CropDiagnosis r) {
        Sheet s = Sheet.of(this, I18n.t("Possible causes") + " · " + crop);
        if (r.matches == null || r.matches.isEmpty()) {
            s.note(I18n.t("The guide could not match these signs. Send the case to your county officer and add photos."));
        } else {
            for (CropMatch m : r.matches) {
                StringBuilder sb = new StringBuilder(m.cause);
                for (String a : m.advice) sb.append("\n• ").append(a);
                s.field(m.name + " · " + m.score + "% (" + I18n.t(m.confidence.substring(0, 1) + m.confidence.substring(1).toLowerCase()) + ")", sb.toString());
                if ("HIGH".equals(m.urgency)) s.badge("⚠ " + I18n.t("Act quickly"), 0xFFC62828);
            }
        }
        s.note(I18n.t(r.disclaimer));
        s.primary(I18n.t("Send to my county officer"), () -> sendCase(crop, keys));
        s.secondary(I18n.t("Close"), null);
        s.show();
    }

    private void sendCase(String crop, List<String> keys) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("note", I18n.t("Anything else the officer should know? (optional)"), false));
        Forms.show(this, I18n.t("Send to county officer"), I18n.t("Send"), f, v ->
                Ui.watch(this, vm.cropSend(crop, keys, v.get("note")), b.progress, c -> {
                    Ui.toast(this, I18n.t("Sent. Add photos so the officer can see the plant."));
                    load();
                    showCase(c);
                }));
    }

    // ------------------------------------------------------------ a case

    private void showCase(CropCase c) {
        boolean farmer = isFarmer();
        boolean open = !"CLOSED".equals(c.status);
        Sheet s = Sheet.of(this, c.crop).badge(statusName(c.status), statusColor(c.status));
        if (!farmer) s.field(I18n.t("Farmer"), c.farmerName + (c.farmerPhone == null ? "" : " · " + c.farmerPhone));
        s.field(I18n.t("Place"), c.subCounty == null ? null : c.subCounty + (c.county == null ? "" : ", " + c.county));
        if (c.symptomLabels != null && !c.symptomLabels.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String l : c.symptomLabels) sb.append(sb.length() == 0 ? "" : "\n").append("• ").append(I18n.t(l));
            s.field(I18n.t("Signs"), sb.toString());
        }
        s.field(I18n.t("Details"), c.description);
        s.photos(c.photos);
        if (c.matches != null && !c.matches.isEmpty()) {
            CropMatch m = c.matches.get(0);
            s.field(I18n.t("The guide thinks"), m.name + " (" + m.score + "%)").note(m.cause);
        }
        if (c.reviewerNote != null && !c.reviewerNote.isEmpty()) {
            s.divider().field(I18n.t("Answer from ") + (c.reviewerName == null ? I18n.t("the officer") : c.reviewerName), c.reviewerNote);
            s.field(I18n.t("Confirmed problem"), c.confirmedName);
        } else if (farmer && open) {
            s.note(I18n.t("Your county officer will reply here. You will get an alert."));
        }
        if (farmer && open && (c.photos == null || c.photos.size() < 3))
            s.secondary(I18n.t("📷 Add a photo"), () -> { photoCaseId = c.id; picker.launch("image/*"); });
        if (!farmer && open) s.primary(I18n.t("Reply to the farmer"), () -> reply(c));
        if (open) s.danger(I18n.t("Close this case"), () ->
                Ui.watch(this, vm.cropClose(c.id), b.progress, x -> { Ui.toast(this, I18n.t("Case closed")); load(); }));
        s.show();
    }

    private void reply(CropCase c) {
        List<String> values = new ArrayList<>(), labels = new ArrayList<>();
        values.add("");
        labels.add("Not sure yet");
        if (c.matches != null) for (CropMatch m : c.matches) { values.add(m.key); labels.add(m.name); }
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.choice("problem", I18n.t("What do you think it is?"), false, values.toArray(new String[0]), labels.toArray(new String[0])));
        f.add(Forms.multi("note", I18n.t("Your advice to the farmer"), true));
        Forms.show(this, I18n.t("Reply to the farmer"), I18n.t("Send"), f, v ->
                Ui.watch(this, vm.cropReview(c.id, v.get("note"), v.get("problem")), b.progress, r -> {
                    Ui.toast(this, I18n.t("Answer sent"));
                    load();
                }));
    }

    // ------------------------------------------------------------ photos

    private void upload(Uri uri) {
        final long id = photoCaseId;
        b.progress.setVisibility(View.VISIBLE);
        new Thread(() -> {
            try {
                byte[] data = ImageUtil.compress(this, uri);
                runOnUiThread(() -> Ui.watch(this, vm.cropPhoto(id, data), b.progress, c -> {
                    Ui.toast(this, I18n.t("Photo added"));
                    load();
                    showCase(c);
                }));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    b.progress.setVisibility(View.GONE);
                    Ui.toast(this, I18n.t("Could not read that image"));
                });
            }
        }).start();
    }
}
