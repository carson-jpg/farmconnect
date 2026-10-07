package com.farmconnect.controller;

import com.farmconnect.dto.Community.FeedbackRequest;
import com.farmconnect.model.Feedback;
import com.farmconnect.model.User;
import com.farmconnect.repository.FeedbackRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** "How do you like FarmConnect?" - feeds the farmer satisfaction indicator. */
@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackRepository repo;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void submit(@Valid @RequestBody FeedbackRequest r, @AuthenticationPrincipal User u) {
        Feedback f = new Feedback();
        f.setUser(u);
        f.setRating(r.rating());
        f.setComment(r.comment() == null || r.comment().isBlank() ? null : r.comment().trim());
        repo.save(f);
    }
}
