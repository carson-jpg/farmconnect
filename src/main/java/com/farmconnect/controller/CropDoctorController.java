package com.farmconnect.controller;

import com.farmconnect.dto.CropDoctor.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.CropCaseRepository;
import com.farmconnect.repository.FarmRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.Staff;
import com.farmconnect.service.AreaScope;
import com.farmconnect.service.CropCalendar;
import com.farmconnect.service.CropDoctorGuide;
import com.farmconnect.service.CropPhotoSigner;
import com.farmconnect.service.FileStorageService;
import com.farmconnect.service.Locations;
import com.farmconnect.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Crop doctor. A farmer ticks what they see and gets likely causes from the built-in guide straight away; if they want a
 * person to look, they send it to the county officer with up to 3 photos. Staff only ever see cases from their own area.
 */
@RestController
@RequestMapping("/api/crop-doctor")
@RequiredArgsConstructor
public class CropDoctorController {
    private static final int MAX_PHOTOS = 3;
    private static final int MAX_CASES_PER_DAY = 10;

    private final CropCaseRepository cases;
    private final FarmRepository farms;
    private final UserRepository users;
    private final FileStorageService files;
    private final CropPhotoSigner signer;
    private final NotificationService notifier;

    // ------------------------------------------------------------------ the guide

    @GetMapping("/guide")
    public GuideInfo guide() {
        return new GuideInfo(CropDoctorGuide.crops(),
                CropDoctorGuide.symptoms().stream().map(s -> new SymptomInfo(s.key(), s.icon(), s.label())).toList(),
                CropDoctorGuide.DISCLAIMER);
    }

    @PostMapping("/diagnose")
    @PreAuthorize("hasRole('FARMER')")
    public DiagnoseResponse diagnose(@Valid @RequestBody DiagnoseRequest r) {
        return new DiagnoseResponse(r.crop().trim(), matches(r.crop(), r.symptoms()), CropDoctorGuide.DISCLAIMER);
    }

    // ------------------------------------------------------------------ cases

    @PostMapping("/cases")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public CaseResponse send(@Valid @RequestBody CaseRequest r, @AuthenticationPrincipal User me) {
        List<String> symptoms = cleanSymptoms(r.symptoms());
        String note = r.description() == null ? "" : r.description().trim();
        if (symptoms.isEmpty() && note.length() < 10)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pick at least one sign you can see, or describe the problem");
        if (cases.countByFarmerIdAndCreatedAtAfter(me.getId(), Instant.now().minus(Duration.ofDays(1))) >= MAX_CASES_PER_DAY)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "You have sent many cases today. Please wait until tomorrow.");

        CropCase c = new CropCase();
        c.setFarmer(me);
        CropCalendar.Crop known = CropCalendar.find(r.crop());
        c.setCrop(known != null ? known.name() : r.crop().trim());
        c.setSymptoms(String.join(",", symptoms));
        c.setDescription(note.isEmpty() ? null : note);
        placeOf(me, r.farmId(), c);
        List<CropDoctorGuide.Match> m = CropDoctorGuide.diagnose(r.crop(), symptoms);
        if (!m.isEmpty()) { c.setGuideProblemKey(m.get(0).key()); c.setGuideScore(m.get(0).score()); }
        c = cases.save(c);
        alertStaff(c, m.isEmpty() ? null : m.get(0));
        return toResponse(c, me);
    }

    @PostMapping("/cases/{id}/photos")
    @PreAuthorize("hasRole('FARMER')")
    @Transactional
    public CaseResponse addPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file, @AuthenticationPrincipal User me) {
        CropCase c = find(id);
        if (!c.getFarmer().getId().equals(me.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your case");
        if (CropCase.CLOSED.equals(c.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This case is closed");
        if (c.getPhotos().size() >= MAX_PHOTOS) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You can add up to " + MAX_PHOTOS + " photos");
        c.getPhotos().add(files.saveCropPhoto(c.getId(), file));
        return toResponse(cases.save(c), me);
    }

    @GetMapping("/cases")
    @Transactional(readOnly = true)
    public List<CaseResponse> list(@RequestParam(required = false) String status, @AuthenticationPrincipal User me) {
        List<CropCase> all;
        if (Staff.is(me)) {
            AreaScope scope = AreaScope.of(me);
            all = cases.findTop500ByOrderByCreatedAtDesc().stream().filter(c -> inArea(scope, c)).toList();
        } else if (me.getRole() == Role.FARMER) {
            all = cases.findByFarmerIdOrderByCreatedAtDesc(me.getId());
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Crop doctor is for farmers and county staff");
        }
        return all.stream().filter(c -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || c.getStatus().equalsIgnoreCase(status))
                .map(c -> toResponse(c, me)).toList();
    }

    @GetMapping("/cases/{id}")
    @Transactional(readOnly = true)
    public CaseResponse one(@PathVariable Long id, @AuthenticationPrincipal User me) {
        CropCase c = find(id);
        requireAccess(c, me);
        return toResponse(c, me);
    }

    @PostMapping("/cases/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN','OFFICER')")
    @Transactional
    public CaseResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRequest r, @AuthenticationPrincipal User me) {
        CropCase c = find(id);
        requireAccess(c, me);
        if (CropCase.CLOSED.equals(c.getStatus())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This case is closed");
        String confirmed = r.confirmedProblem() == null || r.confirmedProblem().isBlank() ? null : r.confirmedProblem().trim();
        if (confirmed != null && CropDoctorGuide.problem(confirmed) == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown problem");
        c.setReviewer(me);
        c.setReviewerNote(r.note().trim());
        c.setConfirmedProblem(confirmed);
        c.setReviewedAt(Instant.now());
        c.setStatus(CropCase.ANSWERED);
        cases.save(c);
        notifier.notifyAndSms(c.getFarmer(), "CROP_CASE", "Your crop doctor case has an answer",
                me.getName() + ": " + r.note().trim(), c.getId());
        return toResponse(c, me);
    }

    @PostMapping("/cases/{id}/close")
    @Transactional
    public CaseResponse close(@PathVariable Long id, @AuthenticationPrincipal User me) {
        CropCase c = find(id);
        requireAccess(c, me);
        c.setStatus(CropCase.CLOSED);
        return toResponse(cases.save(c), me);
    }

    // ------------------------------------------------------------------ helpers

    private CropCase find(Long id) {
        return cases.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case not found"));
    }

    private static boolean inArea(AreaScope scope, CropCase c) {
        if (scope.all()) return true;
        return scope.allows(c.getSubCounty());
    }

    /** The owner, or staff whose area covers the farmer's sub-county. */
    private void requireAccess(CropCase c, User me) {
        if (c.getFarmer().getId().equals(me.getId())) return;
        if (Staff.is(me) && inArea(AreaScope.of(me), c)) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot open this case");
    }

    private static List<String> cleanSymptoms(List<String> in) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (in != null) for (String s : in) if (s != null && CropDoctorGuide.knownSymptom(s.trim())) out.add(s.trim());
        return new ArrayList<>(out);
    }

    private static List<MatchInfo> matches(String crop, List<String> symptoms) {
        return CropDoctorGuide.diagnose(crop, cleanSymptoms(symptoms)).stream().map(CropDoctorController::info).toList();
    }

    private static MatchInfo info(CropDoctorGuide.Match m) {
        return new MatchInfo(m.key(), m.name(), m.kind(), m.cause(), m.advice(), m.urgency(), m.score(), m.confidence(), m.seeOfficer());
    }

    /** Where the farm is: the chosen farm, else the farmer's first farm that has a sub-county. */
    private void placeOf(User me, Long farmId, CropCase c) {
        String sub = null;
        List<Farm> mine = farms.findByOwnerId(me.getId());
        if (farmId != null) {
            for (Farm f : mine) if (f.getId().equals(farmId)) sub = f.getSubCounty();
        }
        if (sub == null) for (Farm f : mine) if (f.getSubCounty() != null && !f.getSubCounty().isBlank()) { sub = f.getSubCounty(); break; }
        c.setSubCounty(sub);
        c.setCounty(sub == null ? null : Locations.countyOf(sub));
    }

    /** Tell the officers and county admins whose area covers the case (urgent ones are marked). */
    private void alertStaff(CropCase c, CropDoctorGuide.Match top) {
        Set<Long> ids = new LinkedHashSet<>();
        for (User u : users.findByRoleIn(List.of(Role.OFFICER, Role.COUNTY_ADMIN))) {
            if (!u.isEnabled()) continue;
            if (inArea(AreaScope.of(u), c)) ids.add(u.getId());
        }
        boolean urgent = top != null && "HIGH".equals(top.urgency());
        String title = (urgent ? "URGENT crop case: " : "New crop case: ") + c.getCrop();
        String body = c.getFarmer().getName() + (c.getSubCounty() == null ? "" : ", " + c.getSubCounty())
                + (top == null ? "" : ". Guide thinks: " + top.name());
        notifier.broadcastToUsers(ids, "CROP_CASE", title, body, c.getId(), false);
    }

    private CaseResponse toResponse(CropCase c, User viewer) {
        List<String> keys = c.getSymptoms() == null || c.getSymptoms().isBlank() ? List.of() : Arrays.asList(c.getSymptoms().split(","));
        List<String> labels = keys.stream().map(CropDoctorGuide::symptomLabel).toList();
        List<MatchInfo> m = matches(c.getCrop(), keys);
        boolean staff = Staff.is(viewer);
        CropDoctorGuide.Problem confirmed = c.getConfirmedProblem() == null ? null : CropDoctorGuide.problem(c.getConfirmedProblem());
        return new CaseResponse(c.getId(), c.getFarmer().getId(), c.getFarmer().getName(),
                staff ? c.getFarmer().getPhone() : null, c.getCrop(), keys, labels, c.getDescription(), c.getSubCounty(), c.getCounty(),
                c.getStatus(), m, c.getPhotos().stream().map(signer::url).toList(),
                c.getReviewer() == null ? null : c.getReviewer().getName(), c.getReviewerNote(),
                c.getConfirmedProblem(), confirmed == null ? null : confirmed.name(), c.getReviewedAt(), c.getCreatedAt());
    }
}
