package com.farmconnect.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class CropDoctor {
    private CropDoctor() {}

    public record SymptomInfo(String key, String icon, String label) {}
    public record GuideInfo(List<String> crops, List<SymptomInfo> symptoms, String disclaimer) {}

    public record DiagnoseRequest(@NotBlank @Size(max = 60) String crop, @Size(max = 12) List<String> symptoms) {}
    public record MatchInfo(String key, String name, String kind, String cause, List<String> advice, String urgency,
                            int score, String confidence, boolean seeOfficer) {}
    public record DiagnoseResponse(String crop, List<MatchInfo> matches, String disclaimer) {}

    public record CaseRequest(@NotBlank @Size(max = 60) String crop, @Size(max = 12) List<String> symptoms,
                              @Size(max = 1000) String description, Long farmId) {}
    public record ReviewRequest(@NotBlank @Size(max = 1500) String note, @Size(max = 60) String confirmedProblem) {}

    public record CaseResponse(Long id, Long farmerId, String farmerName, String farmerPhone, String crop,
                               List<String> symptomKeys, List<String> symptomLabels, String description,
                               String subCounty, String county, String status, List<MatchInfo> matches,
                               List<String> photos, String reviewerName, String reviewerNote,
                               String confirmedProblem, String confirmedName, Instant reviewedAt, Instant createdAt) {}
}
