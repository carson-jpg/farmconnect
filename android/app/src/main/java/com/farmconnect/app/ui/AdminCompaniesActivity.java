package com.farmconnect.app.ui;

import com.farmconnect.app.util.I18n;

import android.os.Bundle;
import android.widget.ImageView;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.farmconnect.app.data.Models.AdminCompanyDetail;
import com.farmconnect.app.data.Models.AdminCompanySummary;
import com.farmconnect.app.data.Models.CompanyDocument;
import com.farmconnect.app.data.Models.CompanyView;
import com.farmconnect.app.util.Ui;
import com.farmconnect.app.vm.CompanyViewModel;
import java.util.ArrayList;
import java.util.List;

/** Company verification. Only the super admin gets here (and the server refuses everyone else). */
public class AdminCompaniesActivity extends BaseListActivity {
    private CompanyViewModel cvm;

    @Override protected String screenTitle() { return I18n.t("Company verification"); }
    @Override protected String emptyText() { return I18n.t("No companies here"); }

    @Override protected String[][] chips() {
        String[][] c = new String[CompanyLabels.STATUSES.length][];
        for (int i = 0; i < c.length; i++) c[i] = new String[]{CompanyLabels.STATUSES[i], CompanyLabels.STATUS_NAMES[i]};
        return c;
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        cvm = new ViewModelProvider(this).get(CompanyViewModel.class);
    }

    @Override protected void load() {
        Ui.watch(this, cvm.adminList(chip), b.progress, list -> {
            List<Row> rows = new ArrayList<>();
            for (AdminCompanySummary c : list) {
                rows.add(Row.of("🏢", c.name)
                        .sub(CompanyLabels.name(CompanyLabels.CATEGORIES, CompanyLabels.CATEGORY_NAMES, c.category) + (c.county == null ? "" : " · " + c.county))
                        .meta(c.documents + I18n.t(" documents") + " · " + Ui.dateTime(c.submittedAt))
                        .badge(CompanyLabels.status(c.status), CompanyLabels.statusColor(c.status))
                        .click(() -> open(c.id)));
            }
            show(rows, list.size() + (list.size() == 1 ? I18n.t(" company") : I18n.t(" companies")));
        });
    }

    private void open(long id) {
        Ui.watch(this, cvm.adminDetail(id), b.progress, this::detail);
    }

    private void detail(AdminCompanyDetail d) {
        CompanyView c = d.company;
        int docs = c.documents == null ? 0 : c.documents.size();
        Sheet s = Sheet.of(this, c.name)
                .badge(CompanyLabels.status(c.status), CompanyLabels.statusColor(c.status))
                .field(I18n.t("Registration no."), c.businessRegNumber)
                .field(I18n.t("KRA PIN"), c.kraPin)
                .field(I18n.t("Category"), CompanyLabels.name(CompanyLabels.CATEGORIES, CompanyLabels.CATEGORY_NAMES, c.category))
                .field(I18n.t("Representative"), c.repName + " · " + c.repPhone)
                .field(I18n.t("Account"), d.ownerName + " · " + d.ownerEmail)
                .field(I18n.t("Address"), c.address + ", " + c.county)
                .field(I18n.t("Operates in"), c.operatingCounties == null ? null : android.text.TextUtils.join(", ", c.operatingCounties))
                .field(I18n.t("Documents"), String.valueOf(docs))
                .field(I18n.t("Rejected"), c.rejectionReason)
                .field(I18n.t("Suspended"), c.suspensionReason);
        switch (c.status) {
            case "PENDING_VERIFICATION": s.primary(I18n.t("Start review"), () -> act(cvm.startReview(c.id), I18n.t("Review started"))); break;
            case "UNDER_REVIEW": s.primary(I18n.t("Verify company"), () -> confirmVerify(c)); break;
            case "VERIFIED": s.secondary(I18n.t("Payout account"), () -> payoutDialog(c)); break;
            case "SUSPENDED": s.primary(I18n.t("Reinstate"), () -> act(cvm.reinstate(c.id), I18n.t("Reinstated"))); break;
            default: break;
        }
        if (docs > 0) s.secondary(I18n.t("View documents"), () -> chooseDoc(c));
        if ("UNDER_REVIEW".equals(c.status)) s.danger(I18n.t("Reject"), () -> askReason(I18n.t("Reason shown to the company"), r -> act(cvm.reject(c.id, r), I18n.t("Rejected"))));
        if ("VERIFIED".equals(c.status)) s.danger(I18n.t("Suspend"), () -> askReason(I18n.t("Reason for suspending"), r -> act(cvm.suspend(c.id, r), I18n.t("Suspended"))));
        s.show();
    }

    /** The company's payout account: the super admin approves it before any money is sent there. */
    private void payoutDialog(CompanyView c) {
        Sheet s = Sheet.of(this, I18n.t("Payout account"));
        if (c.payoutAccount == null) {
            s.note(I18n.t("This company has not entered a payout account yet.")).show();
            return;
        }
        s.badge(c.payoutApproved ? I18n.t("Approved") : I18n.t("Not approved"), c.payoutApproved ? 0xFF2E7D32 : 0xFFF9A825)
                .field(I18n.t("Type"), CompanyLabels.name(CompanyLabels.PAYOUT_TYPES, CompanyLabels.PAYOUT_TYPE_NAMES, c.payoutType))
                .field(I18n.t("Number"), c.payoutAccount)
                .field(I18n.t("Account no."), c.payoutReference)
                .note(I18n.t("Approve only if this account really belongs to the company."));
        if (c.payoutApproved) s.danger(I18n.t("Revoke approval"), () -> act(cvm.revokePayout(c.id), I18n.t("Payout approval revoked")));
        else s.primary(I18n.t("Approve payout account"), () -> act(cvm.approvePayout(c.id), I18n.t("Payout account approved")));
        s.show();
    }

    private void confirmVerify(CompanyView c) {
        new AlertDialog.Builder(this).setTitle(I18n.t("Verify ") + c.name + "?")
                .setMessage(I18n.t("Confirm that the registration number, KRA PIN and documents are genuine and match the company details."))
                .setPositiveButton(I18n.t("Verify"), (x, y) -> act(cvm.verify(c.id), I18n.t("Company verified")))
                .setNegativeButton(I18n.t("Cancel"), null).show();
    }

    private interface Reason { void ok(String reason); }

    private void askReason(String label, Reason done) {
        List<Forms.Field> f = new ArrayList<>();
        f.add(Forms.multi("reason", label, true));
        Forms.show(this, label, I18n.t("Send"), f, v -> {
            String r = v.get("reason").trim();
            if (r.length() < 5) Ui.toast(this, I18n.t("Please give a reason of at least 5 characters"));
            else done.ok(r);
        });
    }

    private void act(androidx.lifecycle.LiveData<com.farmconnect.app.data.Resource<AdminCompanyDetail>> call, String ok) {
        Ui.watch(this, call, b.progress, d -> { Ui.toast(this, ok); load(); });
    }

    private void chooseDoc(CompanyView c) {
        String[] names = new String[c.documents.size()];
        for (int i = 0; i < names.length; i++) {
            CompanyDocument d = c.documents.get(i);
            names[i] = CompanyLabels.name(CompanyLabels.DOC_TYPES, CompanyLabels.DOC_NAMES, d.type);
        }
        new AlertDialog.Builder(this).setTitle(I18n.t("Documents"))
                .setItems(names, (x, which) -> showDoc(c.id, c.documents.get(which).id))
                .setNegativeButton(I18n.t("Close"), null).show();
    }

    private void showDoc(long companyId, long docId) {
        Ui.watch(this, cvm.doc(true, companyId, docId), b.progress, bm -> {
            ImageView iv = new ImageView(this);
            iv.setAdjustViewBounds(true);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            iv.setImageBitmap(bm);
            new AlertDialog.Builder(this).setView(iv).setPositiveButton(I18n.t("Close"), null).show();
        });
    }
}
