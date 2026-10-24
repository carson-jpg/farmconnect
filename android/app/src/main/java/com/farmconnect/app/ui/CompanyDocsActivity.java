package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.CompanyDocument;
import com.farmconnect.app.data.Models.CompanyView;
import com.farmconnect.app.util.ImageUtil;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CompanyViewModel;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** The company's verification documents: JPG, PNG or PDF. */
public class CompanyDocsActivity extends BaseListActivity {
    private static final int MAX_BYTES = 6 * 1024 * 1024;
    private CompanyViewModel cvm;
    private CompanyView company;
    private String pendingType;

    private final ActivityResultLauncher<String> picker =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> { if (uri != null) upload(uri); });

    @Override protected String screenTitle() { return I18n.t("Documents"); }
    @Override protected String fabText() { return I18n.t("Upload document"); }
    @Override protected String emptyText() { return I18n.t("No documents yet.\nUpload your registration certificate and KRA PIN certificate."); }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        cvm = new ViewModelProvider(this).get(CompanyViewModel.class);
    }

    @Override protected void load() {
        Ui.watch(this, cvm.mine(), b.progress, c -> { company = c; showDocs(); });
    }

    private void showDocs() {
        boolean removable = !"VERIFIED".equals(company.status) && !"UNDER_REVIEW".equals(company.status);
        List<Row> rows = new ArrayList<>();
        if (company.documents != null) for (CompanyDocument d : company.documents) {
            Row r = Row.of("📄", CompanyLabels.name(CompanyLabels.DOC_TYPES, CompanyLabels.DOC_NAMES, d.type))
                    .sub(d.fileName).meta(Ui.dateTime(d.uploadedAt)).click(() -> view(d));
            if (removable) r.action(I18n.t("Delete"), () -> delete(d));
            rows.add(r);
        }
        show(rows, company.name);
    }

    private void view(CompanyDocument d) {
        Ui.watch(this, cvm.doc(false, company.id, d.id), b.progress, bm -> {
            ImageView iv = new ImageView(this);
            iv.setAdjustViewBounds(true);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageBitmap(bm);
            new AlertDialog.Builder(this).setView(iv).setPositiveButton(I18n.t("Close"), null).show();
        });
    }

    private void delete(CompanyDocument d) {
        new AlertDialog.Builder(this).setTitle(I18n.t("Delete this document?"))
                .setPositiveButton(I18n.t("Delete"), (x, y) -> Ui.watch(this, cvm.deleteDoc(d.id), b.progress, c -> { company = c; showDocs(); }))
                .setNegativeButton(I18n.t("Cancel"), null).show();
    }

    @Override protected void onFab() {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.choice("type", I18n.t("Document type"), true, CompanyLabels.DOC_TYPES, CompanyLabels.DOC_NAMES));
        Forms.show(this, I18n.t("Upload document"), I18n.t("Choose file"), f, v -> {
            pendingType = v.get("type");
            picker.launch("*/*");
        });
    }

    private void upload(Uri uri) {
        try {
            String mime = getContentResolver().getType(uri);
            byte[] data;
            String sendMime, name;
            if ("application/pdf".equals(mime)) {
                try (InputStream in = getContentResolver().openInputStream(uri)) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while (in != null && (n = in.read(buf)) > 0) {
                        out.write(buf, 0, n);
                        if (out.size() > MAX_BYTES) { Ui.toast(this, I18n.t("File too large (max 6 MB)")); return; }
                    }
                    data = out.toByteArray();
                }
                sendMime = "application/pdf";
                name = "document.pdf";
            } else if (mime != null && mime.startsWith("image/")) {
                data = ImageUtil.compress(this, uri);
                sendMime = "image/jpeg";
                name = "document.jpg";
            } else {
                Ui.toast(this, I18n.t("Choose a JPG, PNG or PDF file"));
                return;
            }
            Ui.watch(this, cvm.uploadDoc(pendingType, data, sendMime, name), b.progress, c -> {
                company = c;
                Ui.toast(this, I18n.t("Uploaded"));
                showDocs();
            });
        } catch (Exception e) {
            Ui.toast(this, I18n.t("Could not read that file"));
        }
    }
}
