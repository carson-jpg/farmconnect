package com.farmconnect.service;

import com.farmconnect.dto.Dtos.DraftRequest;
import com.farmconnect.model.*;
import com.farmconnect.repository.FarmerVerificationRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.Crypto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VerificationService {
    public static final List<String> SUB_COUNTIES = List.of("Cherangany", "Endebess", "Kiminini", "Kwanza", "Saboti");
    private static final Set<String> TYPES = Set.of("CROP", "LIVESTOCK", "POULTRY", "MIXED", "AQUACULTURE", "OTHER");
    private static final Pattern PHONE = Pattern.compile("^(?:\\+?254|0)([17]\\d{8})$");
    private static final Pattern NATIONAL_ID = Pattern.compile("^\\d{7,8}$");
    private static final SecureRandom RNG = new SecureRandom();

    private final FarmerVerificationRepository repo;
    private final UserRepository users;
    private final FileStorageService files;
    private final SmsService sms;
    private final PasswordEncoder encoder;

    // ---------------- farmer side ----------------

    @Transactional
    public FarmerVerification mine(User u) {
        return repo.findByFarmerId(u.getId()).orElseGet(() -> {
            FarmerVerification v = new FarmerVerification();
            v.setFarmer(users.getReferenceById(u.getId()));
            return repo.save(v);
        });
    }

    @Transactional
    public FarmerVerification saveDraft(User u, DraftRequest r) {
        FarmerVerification v = mine(u);
        requireEditable(v);
        if (notBlank(r.fullName())) v.setFullName(r.fullName().trim());
        if (notBlank(r.nationalId())) {
            String id = r.nationalId().trim();
            if (!NATIONAL_ID.matcher(id).matches()) throw bad("National ID must be 7 or 8 digits");
            String hash = Crypto.hmac(id);
            if (repo.existsByNationalIdHashAndFarmerIdNot(hash, u.getId()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This National ID is already registered to another account");
            v.setNationalId(id);
            v.setNationalIdHash(hash);
        }
        if (notBlank(r.subCounty())) {
            if (!SUB_COUNTIES.contains(r.subCounty())) throw bad("Choose a sub-county in Trans Nzoia");
            v.setSubCounty(r.subCounty());
        }
        if (r.ward() != null) v.setWard(r.ward().trim());
        if (r.village() != null) v.setVillage(r.village().trim());
        if (r.farmLat() != null && r.farmLng() != null) {
            // generous box around Trans Nzoia, just a sanity check
            if (r.farmLat() < 0.6 || r.farmLat() > 1.5 || r.farmLng() < 34.4 || r.farmLng() > 35.6)
                throw bad("Farm location must be inside Trans Nzoia County");
            v.setFarmLat(r.farmLat());
            v.setFarmLng(r.farmLng());
        }
        if (notBlank(r.farmingType())) {
            String t = r.farmingType().trim().toUpperCase();
            if (!TYPES.contains(t)) throw bad("Unknown farming type");
            v.setFarmingType(t);
        }
        if (r.mainProduce() != null) v.setMainProduce(r.mainProduce().trim());
        if (r.farmSize() != null) {
            if (r.farmSize() <= 0) throw bad("Farm size must be greater than 0");
            v.setFarmSize(r.farmSize());
        }
        if (r.termsAccepted() != null) {
            if (r.termsAccepted() && !v.isTermsAccepted()) v.setTermsAcceptedAt(Instant.now());
            v.setTermsAccepted(r.termsAccepted());
        }
        return repo.save(v);
    }

    @Transactional
    public void sendOtp(User u, String rawPhone) {
        FarmerVerification v = mine(u);
        requireEditable(v);
        String phone = normalizePhone(rawPhone);
        if (repo.existsByPhoneAndPhoneVerifiedTrueAndFarmerIdNot(phone, u.getId()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This phone number is already verified on another account");
        Instant now = Instant.now();
        if (v.getOtpSentAt() != null && now.isBefore(v.getOtpSentAt().plusSeconds(60)))
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Wait 60 seconds before requesting another code");
        String code = String.format("%06d", RNG.nextInt(1_000_000));
        v.setOtpHash(encoder.encode(code));
        v.setOtpPhone(phone);
        v.setOtpSentAt(now);
        v.setOtpExpiresAt(now.plusSeconds(600));
        v.setOtpAttempts(0);
        repo.save(v);
        sms.send(phone, "Your FarmConnect verification code is " + code + ". It expires in 10 minutes. Never share it.");
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public FarmerVerification verifyOtp(User u, String rawPhone, String code) {
        FarmerVerification v = mine(u);
        requireEditable(v);
        String phone = normalizePhone(rawPhone);
        if (v.getOtpHash() == null || !phone.equals(v.getOtpPhone())) throw bad("Request a code for this number first");
        if (Instant.now().isAfter(v.getOtpExpiresAt())) throw bad("Code expired. Request a new one");
        if (v.getOtpAttempts() >= 5) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many wrong attempts. Request a new code");
        v.setOtpAttempts(v.getOtpAttempts() + 1);
        if (!encoder.matches(code.trim(), v.getOtpHash())) {
            repo.save(v);
            throw bad("Wrong code");
        }
        v.setPhone(phone);
        v.setPhoneVerified(true);
        v.setOtpHash(null);
        v.setOtpPhone(null);
        v.setOtpExpiresAt(null);
        return repo.save(v);
    }

    @Transactional
    public FarmerVerification upload(User u, String type, MultipartFile file) {
        FarmerVerification v = mine(u);
        requireEditable(v);
        String t = type == null ? "" : type.toUpperCase();
        String old;
        String path;
        switch (t) {
            case "ID_FRONT": old = v.getIdFrontPath(); path = files.save(u.getId(), "id-front", file); v.setIdFrontPath(path); break;
            case "ID_BACK": old = v.getIdBackPath(); path = files.save(u.getId(), "id-back", file); v.setIdBackPath(path); break;
            case "SELFIE": old = v.getSelfiePath(); path = files.save(u.getId(), "selfie", file); v.setSelfiePath(path); break;
            case "PROOF": old = v.getProofPath(); path = files.save(u.getId(), "proof", file); v.setProofPath(path); break;
            default: throw bad("Unknown document type");
        }
        files.delete(old);
        return repo.save(v);
    }

    @Transactional
    public FarmerVerification submit(User u) {
        FarmerVerification v = mine(u);
        requireEditable(v);
        List<String> m = new ArrayList<>();
        if (!notBlank(v.getFullName())) m.add("full name");
        if (v.getNationalId() == null) m.add("National ID number");
        if (v.getIdFrontPath() == null) m.add("ID photo (front)");
        if (v.getIdBackPath() == null) m.add("ID photo (back)");
        if (!v.isPhoneVerified()) m.add("verified phone number");
        if (v.getSubCounty() == null) m.add("sub-county");
        if (!notBlank(v.getWard())) m.add("ward");
        if (!notBlank(v.getVillage())) m.add("village/location");
        if (v.getFarmLat() == null || v.getFarmLng() == null) m.add("farm GPS location");
        if (v.getFarmingType() == null) m.add("type of farming");
        if (!notBlank(v.getMainProduce())) m.add("main crops/livestock");
        if (v.getFarmSize() == null) m.add("farm size");
        if (v.getProofPath() == null) m.add("proof of farming activity");
        if (v.getSelfiePath() == null) m.add("selfie");
        if (!v.isTermsAccepted()) m.add("acceptance of the terms");
        if (!m.isEmpty()) throw bad("Please provide: " + String.join(", ", m));
        v.setStatus(VerificationStatus.PENDING);
        v.setSubmittedAt(Instant.now());
        v.setRejectionReason(null);
        v.setReviewedAt(null);
        v.setReviewedBy(null);
        return repo.save(v);
    }

    // ---------------- reviewer side (ADMIN / OFFICER) ----------------

    public List<FarmerVerification> list(VerificationStatus status) {
        return repo.findByStatusOrderBySubmittedAtAsc(status);
    }

    public FarmerVerification get(Long id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Verification not found"));
    }

    @Transactional
    public FarmerVerification startReview(Long id, User reviewer) {
        FarmerVerification v = get(id);
        if (v.getStatus() != VerificationStatus.PENDING) throw conflict("Only pending verifications can be started");
        v.setStatus(VerificationStatus.UNDER_REVIEW);
        v.setReviewedBy(users.getReferenceById(reviewer.getId()));
        return repo.save(v);
    }

    @Transactional
    public FarmerVerification approve(Long id, User reviewer) {
        FarmerVerification v = get(id);
        if (v.getStatus() != VerificationStatus.UNDER_REVIEW) throw conflict("Start the review before approving");
        v.setStatus(VerificationStatus.VERIFIED);
        v.setReviewedAt(Instant.now());
        v.setReviewedBy(users.getReferenceById(reviewer.getId()));
        v.setRejectionReason(null);
        User farmer = v.getFarmer();
        farmer.setVerified(true);
        users.save(farmer);
        return repo.save(v);
    }

    @Transactional
    public FarmerVerification reject(Long id, User reviewer, String reason) {
        FarmerVerification v = get(id);
        if (v.getStatus() != VerificationStatus.UNDER_REVIEW) throw conflict("Start the review before rejecting");
        v.setStatus(VerificationStatus.REJECTED);
        v.setReviewedAt(Instant.now());
        v.setReviewedBy(users.getReferenceById(reviewer.getId()));
        v.setRejectionReason(reason.trim());
        User farmer = v.getFarmer();
        farmer.setVerified(false);
        users.save(farmer);
        return repo.save(v);
    }

    public static String pathFor(FarmerVerification v, String type) {
        switch (type == null ? "" : type.toUpperCase()) {
            case "ID_FRONT": return v.getIdFrontPath();
            case "ID_BACK": return v.getIdBackPath();
            case "SELFIE": return v.getSelfiePath();
            case "PROOF": return v.getProofPath();
            default: return null;
        }
    }

    // ---------------- helpers ----------------

    private void requireEditable(FarmerVerification v) {
        if (v.getStatus() != VerificationStatus.DRAFT && v.getStatus() != VerificationStatus.REJECTED)
            throw conflict("Your verification is " + v.getStatus().name().toLowerCase().replace('_', ' ') + " and cannot be edited");
    }

    static String normalizePhone(String raw) {
        var m = PHONE.matcher(raw == null ? "" : raw.replaceAll("[\\s-]", ""));
        if (!m.matches()) throw bad("Enter a valid Kenyan phone number (e.g. 0712345678)");
        return "+254" + m.group(1);
    }

    private static boolean notBlank(String s) { return s != null && !s.isBlank(); }
    private static ResponseStatusException bad(String m) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, m); }
    private static ResponseStatusException conflict(String m) { return new ResponseStatusException(HttpStatus.CONFLICT, m); }
}