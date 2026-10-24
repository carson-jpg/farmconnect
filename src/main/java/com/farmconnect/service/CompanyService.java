package com.farmconnect.service;

import com.farmconnect.dto.CompanyDtos.*;
import com.farmconnect.dto.CompanyMapper;
import com.farmconnect.model.*;
import com.farmconnect.repository.CompanyDocumentRepository;
import com.farmconnect.repository.CompanyRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.Crypto;
import com.farmconnect.security.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Company accounts. The company registers and uploads documents; ONLY the super admin verifies, rejects,
 * suspends or reinstates. Everything here maps to DTOs inside the transaction (open-in-view is off in production).
 */
@Service
@RequiredArgsConstructor
public class CompanyService {
    private static final Pattern KRA_PIN = Pattern.compile("^[AP]\\d{9}[A-Z]$");
    private static final int MAX_DOCS = 15;

    private final CompanyRepository companies;
    private final CompanyDocumentRepository docs;
    private final UserRepository users;
    private final FileStorageService files;
    private final NotificationService notifications;

    // ---------------- the company's own side ----------------

    @Transactional(readOnly = true)
    public CompanyView mine(User owner) {
        Company c = find(owner);
        return CompanyMapper.view(c, docs.findByCompanyIdOrderByUploadedAtAsc(c.getId()));
    }

    @Transactional
    public CompanyView register(User owner, CompanyRequest r) {
        if (companies.findByOwnerId(owner.getId()).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your company is already registered");
        Company c = new Company();
        c.setOwner(owner);
        apply(c, r, true);
        c.setStatus(CompanyStatus.PENDING_VERIFICATION);
        c.setSubmittedAt(Instant.now());
        companies.save(c);
        return CompanyMapper.view(c, List.of());
    }

    @Transactional
    public CompanyView update(User owner, CompanyRequest r) {
        Company c = find(owner);
        if (c.getStatus() == CompanyStatus.SUSPENDED)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This company is suspended. Please contact FarmConnect support.");
        // The identity of the business cannot change once the super admin has verified it (or is reviewing it)
        boolean locked = c.getStatus() == CompanyStatus.VERIFIED || c.getStatus() == CompanyStatus.UNDER_REVIEW;
        apply(c, r, !locked);
        if (c.getStatus() == CompanyStatus.REJECTED) {          // fixing a rejected company sends it back for review
            c.setStatus(CompanyStatus.PENDING_VERIFICATION);
            c.setRejectionReason(null);
            c.setSubmittedAt(Instant.now());
        }
        c.setUpdatedAt(Instant.now());
        companies.save(c);
        return CompanyMapper.view(c, docs.findByCompanyIdOrderByUploadedAtAsc(c.getId()));
    }

    @Transactional
    public CompanyView addDocument(User owner, CompanyDocType type, MultipartFile file) {
        Company c = find(owner);
        if (c.getStatus() == CompanyStatus.SUSPENDED)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This company is suspended");
        if (docs.countByCompanyId(c.getId()) >= MAX_DOCS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Too many documents. Delete one first.");
        String path = files.saveCompanyDocument(c.getId(), type.name(), file);
        CompanyDocument d = new CompanyDocument();
        d.setCompany(c);
        d.setType(type);
        d.setPath(path);
        String n = file.getOriginalFilename();
        d.setFileName(n == null ? null : n.substring(0, Math.min(n.length(), 120)));
        docs.save(d);
        if (c.getStatus() == CompanyStatus.REJECTED) { /* stays rejected until they edit the details and resubmit */ }
        return CompanyMapper.view(c, docs.findByCompanyIdOrderByUploadedAtAsc(c.getId()));
    }

    @Transactional
    public CompanyView deleteDocument(User owner, Long docId) {
        Company c = find(owner);
        CompanyDocument d = docs.findById(docId).filter(x -> x.getCompany().getId().equals(c.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        if (c.getStatus() == CompanyStatus.VERIFIED || c.getStatus() == CompanyStatus.UNDER_REVIEW)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Documents cannot be removed while verified or under review");
        files.delete(d.getPath());
        docs.delete(d);
        return CompanyMapper.view(c, docs.findByCompanyIdOrderByUploadedAtAsc(c.getId()));
    }

    /** The file of one of MY documents. */
    @Transactional(readOnly = true)
    public CompanyDocument myDocument(User owner, Long docId) {
        Company c = find(owner);
        return docs.findById(docId).filter(x -> x.getCompany().getId().equals(c.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    /** The company says where its money should go. Any change needs the super admin's approval again. */
    @Transactional
    public CompanyView setPayoutAccount(User owner, PayoutAccountRequest r) {
        Company c = find(owner);
        if (c.getStatus() == CompanyStatus.SUSPENDED)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This company is suspended");
        String account = r.account().trim().replaceAll("\\s", "");
        String reference = r.reference() == null || r.reference().isBlank() ? null : r.reference().trim();
        switch (r.type()) {
            case MPESA_PHONE -> { account = MpesaService.normalizePhone(account); reference = null; }
            case PAYBILL, TILL -> {
                if (!account.matches("\\d{5,7}"))
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the paybill or till number (5 to 7 digits)");
                if (r.type() == PayoutAccountType.PAYBILL && reference == null)
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A paybill needs the account number to credit");
                if (r.type() == PayoutAccountType.TILL) reference = null;
            }
        }
        c.setPayoutType(r.type());
        c.setPayoutAccount(account);
        c.setPayoutReference(reference);
        c.setPayoutVerified(false);
        c.setPayoutVerifiedAt(null);
        c.setUpdatedAt(Instant.now());
        companies.save(c);
        return CompanyMapper.view(c, docs.findByCompanyIdOrderByUploadedAtAsc(c.getId()));
    }

    /** Super admin: the payout account belongs to this company and may be paid. */
    @Transactional
    public AdminCompanyDetail approvePayout(User admin, Long id) {
        requireSuper(admin);
        Company c = get(id);
        if (c.getStatus() != CompanyStatus.VERIFIED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Verify the company first");
        if (c.getPayoutType() == null || c.getPayoutAccount() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The company has not entered a payout account");
        c.setPayoutVerified(true);
        c.setPayoutVerifiedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        companies.save(c);
        notifications.notify(c.getOwner(), "COMPANY", "Payout account approved", "FarmConnect can now send your sales to your payout account.", c.getId());
        return detailOf(c);
    }

    @Transactional
    public AdminCompanyDetail revokePayout(User admin, Long id) {
        requireSuper(admin);
        Company c = get(id);
        c.setPayoutVerified(false);
        c.setPayoutVerifiedAt(null);
        c.setUpdatedAt(Instant.now());
        companies.save(c);
        return detailOf(c);
    }

    /** The company entity of this owner (any status). Used by supply orders, which stay workable after a suspension. */
    @Transactional(readOnly = true)
    public Company entityOf(User owner) { return find(owner); }

    /** Phase 2+ calls this before letting a company list products, quote or order. */
    @Transactional(readOnly = true)
    public Company requireVerified(User owner) {
        Company c = find(owner);
        if (c.getStatus() != CompanyStatus.VERIFIED)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your company is not verified yet");
        return c;
    }

    // ---------------- super admin only ----------------

    @Transactional(readOnly = true)
    public List<AdminCompanySummary> list(CompanyStatus status) {
        return companies.findByStatusOrderBySubmittedAtAsc(status).stream()
                .map(c -> CompanyMapper.summary(c, (int) docs.countByCompanyId(c.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public AdminCompanyDetail detail(User admin, Long id) {
        requireSuper(admin);
        return detailOf(get(id));
    }

    @Transactional(readOnly = true)
    public CompanyDocument document(User admin, Long companyId, Long docId) {
        requireSuper(admin);
        return docs.findById(docId).filter(x -> x.getCompany().getId().equals(companyId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
    }

    @Transactional
    public AdminCompanyDetail startReview(User admin, Long id) {
        requireSuper(admin);
        Company c = get(id);
        require(c, CompanyStatus.PENDING_VERIFICATION);
        c.setStatus(CompanyStatus.UNDER_REVIEW);
        stamp(c, admin);
        companies.save(c);
        return detailOf(c);
    }

    @Transactional
    public AdminCompanyDetail verify(User admin, Long id) {
        requireSuper(admin);
        Company c = get(id);
        require(c, CompanyStatus.UNDER_REVIEW);
        if (!docs.existsByCompanyIdAndType(c.getId(), CompanyDocType.REGISTRATION_CERT)
                || !docs.existsByCompanyIdAndType(c.getId(), CompanyDocType.KRA_PIN_CERT))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "The registration certificate and the KRA PIN certificate must both be uploaded first");
        c.setStatus(CompanyStatus.VERIFIED);
        c.setRejectionReason(null);
        c.setSuspensionReason(null);
        stamp(c, admin);
        setUserVerified(c, true);
        companies.save(c);
        notifications.notify(c.getOwner(), "COMPANY", "Company verified",
                c.getName() + " is verified. You can now use your full company portal.", c.getId());
        return detailOf(c);
    }

    @Transactional
    public AdminCompanyDetail reject(User admin, Long id, String reason) {
        requireSuper(admin);
        Company c = get(id);
        require(c, CompanyStatus.UNDER_REVIEW);
        c.setStatus(CompanyStatus.REJECTED);
        c.setRejectionReason(reason.trim());
        stamp(c, admin);
        setUserVerified(c, false);
        companies.save(c);
        notifications.notify(c.getOwner(), "COMPANY", "Company not verified",
                "Reason: " + reason.trim() + ". Fix the details and resubmit.", c.getId());
        return detailOf(c);
    }

    @Transactional
    public AdminCompanyDetail suspend(User admin, Long id, String reason) {
        requireSuper(admin);
        Company c = get(id);
        require(c, CompanyStatus.VERIFIED);
        c.setStatus(CompanyStatus.SUSPENDED);
        c.setSuspensionReason(reason.trim());
        stamp(c, admin);
        setUserVerified(c, false);
        companies.save(c);
        notifications.notify(c.getOwner(), "COMPANY", "Company suspended",
                "Reason: " + reason.trim() + ". Contact FarmConnect support.", c.getId());
        return detailOf(c);
    }

    @Transactional
    public AdminCompanyDetail reinstate(User admin, Long id) {
        requireSuper(admin);
        Company c = get(id);
        require(c, CompanyStatus.SUSPENDED);
        c.setStatus(CompanyStatus.VERIFIED);
        c.setSuspensionReason(null);
        stamp(c, admin);
        setUserVerified(c, true);
        companies.save(c);
        notifications.notify(c.getOwner(), "COMPANY", "Company reinstated", c.getName() + " is active again.", c.getId());
        return detailOf(c);
    }

    // ---------------- helpers ----------------

    private Company find(User owner) {
        return companies.findByOwnerId(owner.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "NO_COMPANY"));
    }

    private Company get(Long id) {
        return companies.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Company not found"));
    }

    private AdminCompanyDetail detailOf(Company c) {
        User o = c.getOwner();
        return new AdminCompanyDetail(CompanyMapper.view(c, docs.findByCompanyIdOrderByUploadedAtAsc(c.getId())),
                o.getName(), o.getEmail(), o.getPhone(), c.getReviewedBy() == null ? null : c.getReviewedBy().getName());
    }

    private static void requireSuper(User u) {
        if (!Staff.isSuper(u)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the super admin can do this");
    }

    private static void require(Company c, CompanyStatus expected) {
        if (c.getStatus() != expected)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This company is " + c.getStatus() + ", not " + expected);
    }

    private static void stamp(Company c, User admin) {
        c.setReviewedBy(admin);
        c.setReviewedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
    }

    private void setUserVerified(Company c, boolean v) {
        User o = c.getOwner();
        o.setVerified(v);
        users.save(o);
    }

    /** Copies the form onto the company. identity=false keeps name, registration number and KRA PIN as they are. */
    private void apply(Company c, CompanyRequest r, boolean identity) {
        if (!r.canBuy() && !r.canSell())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose whether the company buys, sells, or both");
        if (identity) {
            String pin = r.kraPin().trim().toUpperCase(Locale.ROOT);
            if (!KRA_PIN.matcher(pin).matches())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "KRA PIN looks wrong. Example: A123456789B");
            String hash = Crypto.hmac(pin);
            companies.findByKraPinHash(hash).filter(x -> !x.getId().equals(c.getId())).ifPresent(x -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A company with this KRA PIN is already registered");
            });
            String reg = r.businessRegNumber().trim();
            companies.findByBusinessRegNumberIgnoreCase(reg).filter(x -> !x.getId().equals(c.getId())).ifPresent(x -> {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A company with this registration number is already registered");
            });
            c.setName(r.name().trim());
            c.setBusinessRegNumber(reg);
            c.setKraPin(pin);
            c.setKraPinHash(hash);
        }
        c.setCategory(r.category());
        c.setCanBuy(r.canBuy());
        c.setCanSell(r.canSell());
        c.setRepName(r.repName().trim());
        c.setRepPhone(r.repPhone().trim());
        c.setRepEmail(r.repEmail() == null || r.repEmail().isBlank() ? null : r.repEmail().trim());
        c.setDescription(r.description() == null ? null : r.description().trim());
        c.setAddress(r.address().trim());
        c.setCounty(r.county().trim());
        c.setOperatingCounties(r.operatingCounties() == null ? "" : String.join(",",
                r.operatingCounties().stream().map(String::trim).filter(s -> !s.isEmpty()).distinct().toList()));
    }
}
