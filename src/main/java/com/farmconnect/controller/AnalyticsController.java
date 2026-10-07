package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/** County analytics and the programme monitoring indicators. Admin and officers only. */
@RestController
@RequestMapping("/api/analytics")
@PreAuthorize("hasAnyRole('ADMIN','OFFICER')")
@RequiredArgsConstructor
public class AnalyticsController {
    private static final String UNSPECIFIED = "Not specified";

    private final UserRepository users;
    private final FarmRepository farms;
    private final ProductRepository products;
    private final OrderRepository orders;
    private final PostRepository posts;
    private final PostRegistrationRepository registrations;
    private final ChatMessageRepository messages;
    private final FarmerGroupRepository groups;
    private final GroupMemberRepository members;
    private final FarmRecordRepository records;
    private final FeedbackRepository feedback;

    @GetMapping
    @Transactional(readOnly = true)
    public Analytics overview() { return compute(); }

    @GetMapping(value = "/report.csv", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> csv() {
        Analytics a = compute();
        Indicators i = a.indicators();
        StringBuilder sb = new StringBuilder("Section,Item,Value\n");
        row(sb, "Indicator", "Registered farmers", i.registeredFarmers());
        row(sb, "Indicator", "Verified farmers", i.verifiedFarmers());
        row(sb, "Indicator", "Registered buyers", i.registeredBuyers());
        row(sb, "Indicator", "Active users (30 days)", i.activeUsers30d());
        row(sb, "Indicator", "Farmer groups onboarded", i.groupsOnboarded());
        row(sb, "Indicator", "Group members", i.groupMembers());
        row(sb, "Indicator", "Agricultural information interactions", i.informationInteractions());
        row(sb, "Indicator", "Market listings", i.marketListings());
        row(sb, "Indicator", "Farmer-to-buyer connections", i.farmerBuyerConnections());
        row(sb, "Indicator", "Opportunities accessed", i.opportunitiesAccessed());
        row(sb, "Indicator", "Officers using the platform (30 days)", i.officersActive30d() + " of " + i.officersTotal());
        row(sb, "Indicator", "Programmes communicated", i.programmesCommunicated());
        row(sb, "Indicator", "Farmer satisfaction (1-5)", String.format(Locale.US, "%.2f from %d responses", i.satisfactionAverage(), i.satisfactionResponses()));
        row(sb, "Indicator", "Farmers using digital records", i.farmersUsingRecords());
        row(sb, "Indicator", "Farm records created", i.recordsTotal());
        a.farmersBySubCounty().forEach((k, v) -> row(sb, "Farmers by sub-county", k, v));
        a.verifiedBySubCounty().forEach((k, v) -> row(sb, "Verified farmers by sub-county", k, v));
        a.productsByCategory().forEach((k, v) -> row(sb, "Products by category", k, v));
        a.harvestKgByCrop().forEach((k, v) -> row(sb, "Recorded harvest by crop (kg)", k, v));
        a.groupsByType().forEach((k, v) -> row(sb, "Groups by type", k, v));
        a.membersByGroup().forEach((k, v) -> row(sb, "Members per group", k, v));
        a.ordersByMonth().forEach((k, v) -> row(sb, "Orders by month", k, v));
        a.salesByMonth().forEach((k, v) -> row(sb, "Sales by month (KES)", k, v));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=farmconnect-report.csv")
                .contentType(MediaType.parseMediaType("text/csv")).body(sb.toString());
    }

    private static void row(StringBuilder sb, String section, String item, Object v) {
        sb.append('"').append(section).append("\",\"").append(item.replace("\"", "'")).append("\",\"").append(v).append("\"\n");
    }

    private Analytics compute() {
        List<User> all = users.findAll();
        List<Farm> farmList = farms.findAll();
        List<Product> prods = products.findAll();
        List<CustomerOrder> ords = orders.findAll();
        List<Post> postList = posts.findAll();
        List<ChatMessage> msgs = messages.findAll();
        List<FarmRecord> recs = records.findAll();
        List<FarmerGroup> grps = groups.findAll();
        List<GroupMember> mems = members.findAll();
        List<Feedback> fb = feedback.findAll();
        Instant since = Instant.now().minusSeconds(30L * 24 * 3600);

        List<User> farmers = all.stream().filter(u -> u.getRole() == Role.FARMER).toList();
        List<User> officers = all.stream().filter(u -> u.getRole() == Role.OFFICER).toList();
        java.util.function.Predicate<User> active = u -> u.getLastActiveAt() != null && u.getLastActiveAt().isAfter(since);

        // farmer -> sub-county (a farmer is counted once, in the sub-county of their first farm that has one)
        Map<Long, String> farmerCounty = new HashMap<>();
        for (Farm f : farmList) {
            if (f.getSubCounty() != null && !f.getSubCounty().isBlank()) farmerCounty.putIfAbsent(f.getOwner().getId(), f.getSubCounty());
        }
        Map<String, Long> bySub = new TreeMap<>();
        Map<String, Long> verifiedBySub = new TreeMap<>();
        for (User f : farmers) {
            String c = farmerCounty.getOrDefault(f.getId(), UNSPECIFIED);
            bySub.merge(c, 1L, Long::sum);
            if (f.isVerified()) verifiedBySub.merge(c, 1L, Long::sum);
        }

        Map<String, Long> prodByCat = new TreeMap<>();
        prods.stream().filter(p -> p.isActive() && p.getFarm().getOwner().isVerified())
                .forEach(p -> prodByCat.merge(p.getCategory() == null || p.getCategory().isBlank() ? "Other" : p.getCategory(), 1L, Long::sum));

        Map<String, Long> grpByType = new TreeMap<>();
        grps.forEach(g -> grpByType.merge(g.getType().name(), 1L, Long::sum));
        Map<Long, Long> memberCount = mems.stream().collect(Collectors.groupingBy(m -> m.getGroup().getId(), Collectors.counting()));
        Map<String, Long> membersByGroup = grps.stream()
                .sorted((x, y) -> Long.compare(memberCount.getOrDefault(y.getId(), 0L), memberCount.getOrDefault(x.getId(), 0L)))
                .limit(8).collect(Collectors.toMap(FarmerGroup::getName, g -> memberCount.getOrDefault(g.getId(), 0L),
                        (a, b) -> a, LinkedHashMap::new));

        // last 6 months of orders and sales
        Map<String, Long> ordersByMonth = new LinkedHashMap<>();
        Map<String, BigDecimal> salesByMonth = new LinkedHashMap<>();
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        for (int i = 5; i >= 0; i--) { String k = now.minusMonths(i).toString(); ordersByMonth.put(k, 0L); salesByMonth.put(k, BigDecimal.ZERO); }
        for (CustomerOrder o : ords) {
            String k = YearMonth.from(o.getCreatedAt().atZone(ZoneOffset.UTC)).toString();
            if (!ordersByMonth.containsKey(k)) continue;
            ordersByMonth.merge(k, 1L, Long::sum);
            if (o.getStatus() != OrderStatus.CANCELLED) salesByMonth.merge(k, o.getTotal(), BigDecimal::add);
        }

        Map<String, Long> postsByType = new TreeMap<>();
        postList.forEach(p -> postsByType.merge(p.getType().name(), 1L, Long::sum));

        Map<String, Long> harvest = new TreeMap<>();
        recs.stream().filter(r -> r.getType() == RecordType.HARVEST && r.getQuantity() != null)
                .forEach(r -> harvest.merge(r.getTitle(), r.getQuantity().longValue(), Long::sum));

        // farmer <-> buyer connections: pairs that ordered together or exchanged messages
        Set<String> pairs = new HashSet<>();
        for (CustomerOrder o : ords) {
            for (OrderItem i : o.getItems()) pairs.add(o.getBuyer().getId() + ":" + i.getProduct().getFarm().getOwner().getId());
        }
        for (ChatMessage m : msgs) {
            User a = m.getSender(), b = m.getRecipient();
            if (a.getRole() == Role.FARMER && b.getRole() == Role.BUYER) pairs.add(b.getId() + ":" + a.getId());
            if (b.getRole() == Role.FARMER && a.getRole() == Role.BUYER) pairs.add(a.getId() + ":" + b.getId());
        }

        long views = postList.stream().mapToLong(Post::getViews).sum();
        long regs = registrations.count();
        long opportunities = registrations.findAll().stream()
                .filter(r -> r.getPost().getType() == PostType.OPPORTUNITY || r.getPost().getType() == PostType.TRAINING
                        || r.getPost().getType() == PostType.PROGRAMME).count();
        long communicated = postList.stream().filter(p -> p.getType() == PostType.ANNOUNCEMENT || p.getType() == PostType.PROGRAMME
                || p.getType() == PostType.TRAINING || p.getType() == PostType.OPPORTUNITY).count();
        double avg = fb.isEmpty() ? 0 : fb.stream().mapToInt(Feedback::getRating).average().orElse(0);
        long recordFarmers = recs.stream().map(r -> r.getOwner().getId()).distinct().count();

        Indicators ind = new Indicators(farmers.size(), farmers.stream().filter(User::isVerified).count(),
                all.stream().filter(u -> u.getRole() == Role.BUYER).count(), all.stream().filter(active).count(),
                grps.size(), mems.size(), views + regs,
                prods.stream().filter(p -> p.isActive() && p.getFarm().getOwner().isVerified()).count(), pairs.size(),
                opportunities, officers.size(), officers.stream().filter(active).count(), communicated,
                Math.round(avg * 100) / 100.0, fb.size(), recordFarmers, recs.size(), msgs.size());
        return new Analytics(ind, bySub, verifiedBySub, prodByCat, grpByType, membersByGroup, ordersByMonth, salesByMonth, postsByType, harvest);
    }
}
