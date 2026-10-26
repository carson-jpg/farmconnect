package com.farmconnect.controller;

import com.farmconnect.dto.GroupDeals.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.security.Staff;
import com.farmconnect.service.AreaScope;
import com.farmconnect.service.NotificationService;
import com.farmconnect.service.PoolMath;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Group deals: a farmer group pools produce to sell in bulk, or pools orders for seed, fertilizer and tools.
 * Record only: the app tracks who puts in how much and the price; money never passes through it.
 */
@RestController
@RequestMapping("/api/group-deals")
@RequiredArgsConstructor
public class GroupDealsController {
    private static final int MAX_OPEN_PER_GROUP = 30;

    private final GroupPoolRepository pools;
    private final GroupPoolShareRepository shares;
    private final FarmerGroupRepository groups;
    private final GroupMemberRepository members;
    private final NotificationService notifier;

    // ------------------------------------------------------------------ reading

    /** A farmer: the deals of their groups. A group page or staff: the deals of one group (or of their area). */
    @GetMapping
    @Transactional(readOnly = true)
    public List<PoolResponse> list(@RequestParam(required = false) Long groupId, @RequestParam(required = false) String kind,
                                   @RequestParam(required = false) String status, @AuthenticationPrincipal User me) {
        List<GroupPool> found;
        if (groupId != null) {
            FarmerGroup g = group(groupId);
            requireView(g, me);
            found = pools.findByGroupIdOrderByCreatedAtDesc(groupId);
        } else if (Staff.is(me)) {
            AreaScope scope = AreaScope.of(me);
            found = pools.findAll(Sort.by(Sort.Direction.DESC, "createdAt")).stream()
                    .filter(p -> scope.all() || scope.allows(p.getGroup().getSubCounty())).limit(300).toList();
        } else if (me.getRole() == Role.FARMER) {
            Set<Long> ids = members.findByUserId(me.getId()).stream().map(m -> m.getGroup().getId()).collect(Collectors.toSet());
            found = ids.isEmpty() ? List.of() : pools.findByGroupIdInOrderByCreatedAtDesc(ids);
        } else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Group deals are for farmers and county staff");
        }
        return respond(found.stream()
                .filter(p -> kind == null || kind.isBlank() || p.getKind().equalsIgnoreCase(kind))
                .filter(p -> status == null || status.isBlank() || "ALL".equalsIgnoreCase(status) || p.getStatus().equalsIgnoreCase(status))
                .toList(), me);
    }

    /** Open produce deals, for buyers and companies looking for bulk produce. No member details. */
    @GetMapping("/market")
    @Transactional(readOnly = true)
    public List<PoolResponse> market(@AuthenticationPrincipal User me) {
        LocalDate today = LocalDate.now();
        return respond(pools.findTop200ByKindAndStatusOrderByCreatedAtDesc(GroupPool.SELL, GroupPool.OPEN).stream()
                .filter(p -> p.getClosesAt() == null || !p.getClosesAt().isBefore(today)).toList(), me);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public PoolDetail one(@PathVariable Long id, @AuthenticationPrincipal User me) {
        return detail(find(id), me, true);
    }

    // ------------------------------------------------------------------ writing

    @PostMapping
    @Transactional
    @ResponseStatus(HttpStatus.CREATED)
    public PoolDetail create(@Valid @RequestBody PoolRequest r, @AuthenticationPrincipal User me) {
        FarmerGroup g = group(r.groupId());
        requireManager(g, me);
        if (r.closesAt() != null && r.closesAt().isBefore(LocalDate.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The closing date is in the past");
        long open = pools.findByGroupIdOrderByCreatedAtDesc(g.getId()).stream().filter(p -> GroupPool.OPEN.equals(p.getStatus())).count();
        if (open >= MAX_OPEN_PER_GROUP) throw new ResponseStatusException(HttpStatus.CONFLICT, "This group has too many open deals. Close some first.");
        GroupPool p = new GroupPool();
        p.setGroup(g);
        p.setCreatedBy(me);
        p.setKind(r.kind());
        p.setItem(r.item().trim());
        p.setUnit(r.unit().trim());
        p.setTargetQty(r.targetQty());
        p.setUnitPrice(r.unitPrice());
        p.setListPrice(GroupPool.BUY.equals(r.kind()) ? r.listPrice() : null);
        p.setClosesAt(r.closesAt());
        p.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
        p = pools.save(p);
        String what = GroupPool.SELL.equals(p.getKind()) ? "Selling together: " : "Buying together: ";
        for (GroupMember m : members.findByGroupIdOrderByLeaderDescJoinedAtAsc(g.getId()))
            if (!m.getUser().getId().equals(me.getId()))
                notifier.notify(m.getUser(), "GROUP_POOL", what + p.getItem(), g.getName() + ": say how much " + p.getUnit()
                        + (GroupPool.SELL.equals(p.getKind()) ? " you will bring" : " you want"), p.getId());
        return detail(p, me, true);
    }

    @PostMapping("/{id}/share")
    @Transactional
    public PoolDetail share(@PathVariable Long id, @Valid @RequestBody ShareRequest r, @AuthenticationPrincipal User me) {
        GroupPool p = find(id);
        if (me.getRole() != Role.FARMER || members.findByGroupIdAndUserId(p.getGroup().getId(), me.getId()).isEmpty())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only members of this group can join its deals");
        if (!GroupPool.OPEN.equals(p.getStatus()) || (p.getClosesAt() != null && p.getClosesAt().isBefore(LocalDate.now())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This deal is no longer taking quantities");
        Optional<GroupPoolShare> have = shares.findByPoolIdAndUserId(id, me.getId());
        if (r.qty().signum() == 0) {
            have.ifPresent(shares::delete);
        } else {
            GroupPoolShare s = have.orElseGet(GroupPoolShare::new);
            s.setPool(p);
            s.setUser(me);
            s.setQty(r.qty());
            s.setUpdatedAt(java.time.Instant.now());
            shares.save(s);
        }
        return detail(p, me, true);
    }

    @PostMapping("/{id}/close")
    @Transactional
    public PoolDetail close(@PathVariable Long id, @AuthenticationPrincipal User me) {
        GroupPool p = find(id);
        requireManager(p.getGroup(), me);
        if (!GroupPool.OPEN.equals(p.getStatus())) throw new ResponseStatusException(HttpStatus.CONFLICT, "This deal is not open");
        p.setStatus(GroupPool.CLOSED);
        p.setClosedAt(java.time.Instant.now());
        pools.save(p);
        tellContributors(p, "Deal closed: " + p.getItem(), "No more quantities are being taken. The leader will share the final price.");
        return detail(p, me, true);
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public PoolDetail complete(@PathVariable Long id, @Valid @RequestBody CompleteRequest r, @AuthenticationPrincipal User me) {
        GroupPool p = find(id);
        requireManager(p.getGroup(), me);
        if (GroupPool.DONE.equals(p.getStatus()) || GroupPool.CANCELLED.equals(p.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This deal is already finished");
        if (r.finalUnitPrice() != null) p.setUnitPrice(r.finalUnitPrice());
        p.setFinalNote(r.note() == null || r.note().isBlank() ? null : r.note().trim());
        p.setStatus(GroupPool.DONE);
        p.setClosedAt(java.time.Instant.now());
        pools.save(p);
        for (GroupPoolShare s : shares.findByPoolIdOrderByUpdatedAtAsc(id)) {
            BigDecimal amt = PoolMath.amount(s.getQty(), p.getUnitPrice());
            notifier.notify(s.getUser(), "GROUP_POOL", "Deal done: " + p.getItem(),
                    "Your share: " + s.getQty().stripTrailingZeros().toPlainString() + " " + p.getUnit()
                            + (amt == null ? "" : " = KES " + amt.stripTrailingZeros().toPlainString()) + ". Money is settled between members and the buyer or dealer.", p.getId());
        }
        return detail(p, me, true);
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public PoolDetail cancel(@PathVariable Long id, @AuthenticationPrincipal User me) {
        GroupPool p = find(id);
        requireManager(p.getGroup(), me);
        if (GroupPool.DONE.equals(p.getStatus()) || GroupPool.CANCELLED.equals(p.getStatus()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This deal is already finished");
        p.setStatus(GroupPool.CANCELLED);
        p.setClosedAt(java.time.Instant.now());
        pools.save(p);
        tellContributors(p, "Deal cancelled: " + p.getItem(), "The group leader cancelled this deal.");
        return detail(p, me, true);
    }

    // ------------------------------------------------------------------ helpers

    private void tellContributors(GroupPool p, String title, String body) {
        for (GroupPoolShare s : shares.findByPoolIdOrderByUpdatedAtAsc(p.getId())) notifier.notify(s.getUser(), "GROUP_POOL", title, body, p.getId());
    }

    private GroupPool find(Long id) {
        return pools.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Deal not found"));
    }

    private FarmerGroup group(Long id) {
        return groups.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
    }

    private static boolean inArea(User me, FarmerGroup g) {
        AreaScope scope = AreaScope.of(me);
        return scope.all() || scope.allows(g.getSubCounty());
    }

    private boolean isLeader(FarmerGroup g, User me) {
        return members.findByGroupIdAndUserId(g.getId(), me.getId()).map(GroupMember::isLeader).orElse(false);
    }

    private boolean canManage(FarmerGroup g, User me) {
        return Staff.is(me) ? inArea(me, g) : isLeader(g, me);
    }

    private boolean isMember(FarmerGroup g, User me) { return members.findByGroupIdAndUserId(g.getId(), me.getId()).isPresent(); }

    private void requireView(FarmerGroup g, User me) {
        if (isMember(g, me) || (Staff.is(me) && inArea(me, g))) return;
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You cannot see this group's deals");
    }

    private void requireManager(FarmerGroup g, User me) {
        if (!canManage(g, me)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the group leader or county staff can do this");
    }

    private List<PoolResponse> respond(List<GroupPool> list, User me) {
        if (list.isEmpty()) return List.of();
        Map<Long, List<GroupPoolShare>> byPool = shares.findByPoolIdIn(list.stream().map(GroupPool::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getPool().getId()));
        Map<Long, GroupMember> leaders = new HashMap<>();
        List<PoolResponse> out = new ArrayList<>();
        for (GroupPool p : list) out.add(response(p, me, byPool.getOrDefault(p.getId(), List.of()), leaders));
        return out;
    }

    private PoolResponse response(GroupPool p, User me, List<GroupPoolShare> mine, Map<Long, GroupMember> leaders) {
        FarmerGroup g = p.getGroup();
        GroupMember leader = leaders.computeIfAbsent(g.getId(), k ->
                members.findByGroupIdOrderByLeaderDescJoinedAtAsc(k).stream().filter(GroupMember::isLeader).findFirst().orElse(null));
        BigDecimal committed = PoolMath.total(mine.stream().map(GroupPoolShare::getQty).toList());
        BigDecimal myQty = mine.stream().filter(s -> s.getUser().getId().equals(me.getId())).map(GroupPoolShare::getQty).findFirst().orElse(null);
        return new PoolResponse(p.getId(), g.getId(), g.getName(), g.getSubCounty(), p.getKind(), p.getItem(), p.getUnit(),
                p.getTargetQty(), committed, mine.size(), PoolMath.progressPct(committed, p.getTargetQty()),
                p.getUnitPrice(), p.getListPrice(), PoolMath.discountPct(p.getListPrice(), p.getUnitPrice()), p.getStatus(), p.getClosesAt(),
                p.getNotes(), p.getFinalNote(),
                leader != null ? leader.getUser().getId() : g.getCreatedBy().getId(),
                leader != null ? leader.getUser().getName() : g.getCreatedBy().getName(),
                myQty, PoolMath.amount(myQty, p.getUnitPrice()), canManage(g, me), isMember(g, me), p.getCreatedAt());
    }

    private PoolDetail detail(GroupPool p, User me, boolean check) {
        FarmerGroup g = p.getGroup();
        if (check) requireView(g, me);
        List<GroupPoolShare> all = shares.findByPoolIdOrderByUpdatedAtAsc(p.getId());
        boolean manager = canManage(g, me);
        List<ShareInfo> infos = all.stream().map(s -> new ShareInfo(s.getUser().getId(), s.getUser().getName(),
                manager ? s.getUser().getPhone() : null, s.getQty(), PoolMath.amount(s.getQty(), p.getUnitPrice()))).toList();
        return new PoolDetail(response(p, me, all, new HashMap<>()), infos);
    }
}
