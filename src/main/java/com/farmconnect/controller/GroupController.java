package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Farmer groups, cooperatives and SACCOs. Farmers create / join; staff can manage everything. */
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {
    private final FarmerGroupRepository groups;
    private final GroupMemberRepository members;
    private final NotificationService notifier;

    @GetMapping
    @Transactional(readOnly = true)
    public List<GroupResponse> list(@RequestParam(required = false) Boolean mine, @RequestParam(required = false) String q,
                                    @RequestParam(required = false) GroupType type, @AuthenticationPrincipal User me) {
        Set<Long> myGroups = members.findByUserId(me.getId()).stream().map(m -> m.getGroup().getId()).collect(Collectors.toSet());
        String needle = q == null ? "" : q.trim().toLowerCase();
        return groups.findAllByOrderByNameAsc().stream()
                .filter(g -> !Boolean.TRUE.equals(mine) || myGroups.contains(g.getId()))
                .filter(g -> type == null || g.getType() == type)
                .filter(g -> needle.isEmpty() || g.getName().toLowerCase().contains(needle))
                .map(g -> toResponse(g, me)).toList();
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public GroupDetail one(@PathVariable Long id, @AuthenticationPrincipal User me) {
        FarmerGroup g = find(id);
        boolean privileged = staff(me) || isLeader(g, me);
        List<MemberResponse> list = members.findByGroupIdOrderByLeaderDescJoinedAtAsc(id).stream()
                .map(m -> new MemberResponse(m.getUser().getId(), m.getUser().getName(), m.isLeader(), m.getUser().isVerified(),
                        privileged ? m.getUser().getPhone() : null, m.getJoinedAt())).toList();
        return new GroupDetail(toResponse(g, me), list);
    }

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@Valid @RequestBody GroupRequest r, @AuthenticationPrincipal User me) {
        if (me.getRole() == Role.BUYER) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only farmers and county staff can create groups");
        FarmerGroup g = new FarmerGroup();
        g.setCreatedBy(me);
        apply(g, r);
        g = groups.save(g);
        if (me.getRole() == Role.FARMER) addMember(g, me, true);
        return toResponse(g, me);
    }

    @PutMapping("/{id}")
    @Transactional
    public GroupResponse update(@PathVariable Long id, @Valid @RequestBody GroupRequest r, @AuthenticationPrincipal User me) {
        FarmerGroup g = find(id);
        requireManager(g, me);
        apply(g, r);
        return toResponse(groups.save(g), me);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void delete(@PathVariable Long id, @AuthenticationPrincipal User me) {
        FarmerGroup g = find(id);
        requireManager(g, me);
        members.deleteByGroupId(id);
        groups.delete(g);
    }

    @PostMapping("/{id}/join")
    @Transactional
    public GroupResponse join(@PathVariable Long id, @AuthenticationPrincipal User me) {
        if (me.getRole() != Role.FARMER) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only farmers can join groups");
        FarmerGroup g = find(id);
        if (members.findByGroupIdAndUserId(id, me.getId()).isEmpty()) {
            addMember(g, me, false);
            members.findByGroupIdOrderByLeaderDescJoinedAtAsc(id).stream().filter(GroupMember::isLeader)
                    .forEach(l -> notifier.notify(l.getUser(), "GROUP", "New member in " + g.getName(), me.getName() + " joined your group", id));
        }
        return toResponse(g, me);
    }

    @PostMapping("/{id}/leave")
    @Transactional
    public GroupResponse leave(@PathVariable Long id, @AuthenticationPrincipal User me) {
        FarmerGroup g = find(id);
        GroupMember m = members.findByGroupIdAndUserId(id, me.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "You are not a member"));
        if (m.isLeader()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Group leaders cannot leave. Ask the county office to change the leader or delete the group.");
        members.delete(m);
        return toResponse(g, me);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Transactional
    public void removeMember(@PathVariable Long id, @PathVariable Long userId, @AuthenticationPrincipal User me) {
        FarmerGroup g = find(id);
        requireManager(g, me);
        GroupMember m = members.findByGroupIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (m.isLeader()) throw new ResponseStatusException(HttpStatus.CONFLICT, "The leader cannot be removed");
        members.delete(m);
    }

    /** Leader (or county staff) sends a notice to every member. */
    @PostMapping("/{id}/announce")
    @Transactional
    public void announce(@PathVariable Long id, @Valid @RequestBody AnnounceRequest r, @AuthenticationPrincipal User me) {
        FarmerGroup g = find(id);
        requireManager(g, me);
        members.findByGroupIdOrderByLeaderDescJoinedAtAsc(id).stream()
                .filter(m -> !m.getUser().getId().equals(me.getId()))
                .forEach(m -> notifier.notify(m.getUser(), "GROUP", g.getName() + ": " + r.title().trim(), r.body().trim(), id));
    }

    // ---------------- helpers ----------------

    private void addMember(FarmerGroup g, User u, boolean leader) {
        GroupMember m = new GroupMember();
        m.setGroup(g);
        m.setUser(u);
        m.setLeader(leader);
        members.save(m);
    }

    private FarmerGroup find(Long id) {
        return groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private boolean staff(User u) { return u.getRole() == Role.ADMIN || u.getRole() == Role.OFFICER; }

    private boolean isLeader(FarmerGroup g, User u) {
        return members.findByGroupIdAndUserId(g.getId(), u.getId()).map(GroupMember::isLeader).orElse(false);
    }

    private void requireManager(FarmerGroup g, User u) {
        if (!staff(u) && !isLeader(g, u)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group leader or county staff can do this");
    }

    private void apply(FarmerGroup g, GroupRequest r) {
        g.setName(r.name().trim());
        g.setType(r.type());
        g.setDescription(r.description());
        g.setSubCounty(r.subCounty());
        g.setLocation(r.location());
        g.setContactPhone(r.contactPhone());
    }

    private GroupResponse toResponse(FarmerGroup g, User me) {
        List<GroupMember> all = members.findByGroupIdOrderByLeaderDescJoinedAtAsc(g.getId());
        GroupMember leader = all.stream().filter(GroupMember::isLeader).findFirst().orElse(null);
        boolean member = all.stream().anyMatch(m -> m.getUser().getId().equals(me.getId()));
        boolean iAmLeader = leader != null && leader.getUser().getId().equals(me.getId());
        return new GroupResponse(g.getId(), g.getName(), g.getType(), g.getDescription(), g.getSubCounty(), g.getLocation(),
                g.getContactPhone(), all.size(), leader != null ? leader.getUser().getName() : g.getCreatedBy().getName(),
                leader != null ? leader.getUser().getId() : g.getCreatedBy().getId(), member, iAmLeader);
    }
}
