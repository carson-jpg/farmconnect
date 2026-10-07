package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/** Agricultural information and county programmes. Anyone logged in reads; admins and officers publish. */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    private static final Set<PostType> REGISTRABLE = Set.of(PostType.PROGRAMME, PostType.TRAINING, PostType.OPPORTUNITY);

    private final PostRepository posts;
    private final PostRegistrationRepository registrations;
    private final NotificationService notifier;

    @GetMapping
    @Transactional(readOnly = true)
    public List<PostResponse> list(@RequestParam(required = false) PostType type, @RequestParam(required = false) String topic,
                                   @RequestParam(required = false) String q, @AuthenticationPrincipal User viewer) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        return posts.findAllByOrderByCreatedAtDesc().stream()
                .filter(p -> visible(viewer, p))
                .filter(p -> type == null || p.getType() == type)
                .filter(p -> topic == null || topic.isBlank() || topic.equalsIgnoreCase(p.getTopic()))
                .filter(p -> needle.isEmpty() || p.getTitle().toLowerCase().contains(needle) || p.getBody().toLowerCase().contains(needle))
                .map(p -> toResponse(p, viewer)).toList();
    }

    /** Opening a post counts as one information interaction. */
    @GetMapping("/{id}")
    @Transactional
    public PostResponse one(@PathVariable Long id, @AuthenticationPrincipal User viewer) {
        Post p = find(id);
        if (!visible(viewer, p)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        p.setViews(p.getViews() + 1);
        return toResponse(posts.save(p), viewer);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public PostResponse create(@Valid @RequestBody PostRequest r, @AuthenticationPrincipal User u) {
        Post p = new Post();
        p.setAuthor(u);
        apply(p, r);
        p = posts.save(p);
        if (r.notifyUsers() == null || r.notifyUsers()) {
            notifier.broadcast(p.getAudience(), "POST", label(p.getType()) + ": " + p.getTitle(), excerpt(p.getBody()), p.getId(),
                    Boolean.TRUE.equals(r.sendSms()));
        }
        return toResponse(p, u);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Transactional
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostRequest r, @AuthenticationPrincipal User u) {
        Post p = find(id);
        apply(p, r);
        return toResponse(posts.save(p), u);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
    @Transactional
    public void delete(@PathVariable Long id) {
        Post p = find(id);
        registrations.deleteByPostId(id);
        posts.delete(p);
    }

    /** Register / un-register interest in a programme, training or opportunity. */
    @PostMapping("/{id}/register")
    @Transactional
    public PostResponse toggleRegister(@PathVariable Long id, @AuthenticationPrincipal User u) {
        Post p = find(id);
        if (!visible(u, p)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found");
        if (!REGISTRABLE.contains(p.getType()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration is only for programmes, trainings and opportunities");
        var existing = registrations.findByPostIdAndUserId(id, u.getId());
        if (existing.isPresent()) {
            registrations.delete(existing.get());
        } else {
            PostRegistration reg = new PostRegistration();
            reg.setPost(p);
            reg.setUser(u);
            registrations.save(reg);
            notifier.notify(p.getAuthor(), "POST", "New registration", u.getName() + " registered for \"" + p.getTitle() + "\"", p.getId());
        }
        return toResponse(p, u);
    }

    // ---------------- helpers ----------------

    private void apply(Post p, PostRequest r) {
        p.setType(r.type());
        p.setTitle(r.title().trim());
        p.setTopic(r.topic() == null || r.topic().isBlank() ? "GENERAL" : r.topic().trim().toUpperCase());
        p.setBody(r.body().trim());
        p.setLocation(r.location());
        p.setEventDate(r.eventDate());
        p.setContact(r.contact());
        String a = r.audience() == null ? "ALL" : r.audience().trim().toUpperCase();
        if (!a.equals("ALL") && !a.equals("FARMERS") && !a.equals("BUYERS"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Audience must be ALL, FARMERS or BUYERS");
        p.setAudience(a);
    }

    private Post find(Long id) {
        return posts.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Post not found"));
    }

    private static boolean visible(User viewer, Post p) {
        if (viewer != null && (viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.OFFICER)) return true;
        if ("FARMERS".equals(p.getAudience())) return viewer != null && viewer.getRole() == Role.FARMER;
        if ("BUYERS".equals(p.getAudience())) return viewer != null && viewer.getRole() == Role.BUYER;
        return true;
    }

    private static String excerpt(String s) { return s.length() > 160 ? s.substring(0, 157) + "..." : s; }

    static String label(PostType t) {
        return switch (t) {
            case TIP -> "Farming tip";
            case ARTICLE -> "Article";
            case ADVISORY -> "Advisory";
            case ANNOUNCEMENT -> "Announcement";
            case PROGRAMME -> "County programme";
            case TRAINING -> "Training";
            case OPPORTUNITY -> "Opportunity";
        };
    }

    private PostResponse toResponse(Post p, User viewer) {
        boolean reg = viewer != null && registrations.findByPostIdAndUserId(p.getId(), viewer.getId()).isPresent();
        return new PostResponse(p.getId(), p.getType(), p.getTitle(), p.getTopic(), excerpt(p.getBody()), p.getBody(),
                p.getLocation(), p.getEventDate(), p.getContact(), p.getAuthor().getName(), p.getCreatedAt(), p.getViews(),
                registrations.countByPostId(p.getId()), reg, p.getAudience());
    }
}
