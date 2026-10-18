package com.farmconnect.controller;

import com.farmconnect.dto.Community.*;
import com.farmconnect.model.*;
import com.farmconnect.repository.*;
import com.farmconnect.service.AreaScope;
import com.farmconnect.service.Locations;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@PreAuthorize("hasAnyRole('ADMIN','COUNTY_ADMIN','OFFICER')")
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
    private final FarmerVerificationRepository verifications;

    @GetMapping
    @Transactional(readOnly = true)
    public Analytics overview(@AuthenticationPrincipal User me) { return compute(AreaScope.of(me)); }

    /** Which area the logged-in officer covers (administrators and unassigned officers cover everything). */
    @GetMapping("/scope")
    public Map<String, Object> scope(@AuthenticationPrincipal User me) {
        AreaScope s = AreaScope.of(me);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("all", s.all());
        m.put("label", s.label());
        m.put("county", s.county() == null ? "" : s.county());
        m.put("subCounties", new ArrayList<>(s.subCounties()));
        return m;
    }

    @GetMapping(value = "/report.csv", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> csv(@AuthenticationPrincipal User me) {
        Analytics a = compute(AreaScope.of(me));
        Indicators i = a.indicators();
        StringBuilder sb = new StringBuilder("Section,Item,Value\n");
        row(sb, "Report", "Area", a.scopeLabel());
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
        a.farmersByCounty().forEach((k, v) -> row(sb, "Farmers by county", k, v));
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

    @GetMapping(value = "/report.pdf", produces = "application/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> pdf(@AuthenticationPrincipal User me) {
        byte[] bytes = com.farmconnect.report.ReportPdf.build(compute(AreaScope.of(me)), me == null ? "" : me.getName());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=farmconnect-report.pdf")
                .contentType(MediaType.APPLICATION_PDF).body(bytes);
    }

    /** Every registered farmer in the officer's area with county, sub-county and verification status. */
    @GetMapping(value = "/farmers.csv", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> farmersCsv(@AuthenticationPrincipal User me) {
        AreaScope sc = AreaScope.of(me);
        Map<Long, String> sub = farmerAreas(farms.findAll());
        StringBuilder sb = new StringBuilder("Name,Phone,County,Sub-county,Verified,Joined\n");
        users.findAll().stream().filter(u -> u.getRole() == Role.FARMER && sc.allows(sub.get(u.getId())))
                .sorted(Comparator.comparing(u -> u.getName() == null ? "" : u.getName().toLowerCase()))
                .forEach(u -> {
                    String s = sub.get(u.getId());
                    String c = s == null ? null : Locations.countyOf(s);
                    sb.append(cell(u.getName())).append(',').append(cell(u.getPhone())).append(',')
                            .append(cell(c == null ? UNSPECIFIED : c)).append(',').append(cell(s == null ? UNSPECIFIED : s)).append(',')
                            .append(u.isVerified() ? "Yes" : "No").append(',')
                            .append(u.getCreatedAt() == null ? "" : u.getCreatedAt().toString().substring(0, 10)).append('\n');
                });
        return csvFile("farmconnect-farmers.csv", sb);
    }

    /** Every order with its status and payment state. For an officer: only orders that include their area's farmers, counted for those items. */
    @GetMapping(value = "/orders.csv", produces = "text/csv")
    @Transactional(readOnly = true)
    public ResponseEntity<String> ordersCsv(@AuthenticationPrincipal User me) {
        Set<Long> area = farmerIdsIn(AreaScope.of(me));
        StringBuilder sb = new StringBuilder("Order,Date,Status,Total (KES),Payment method,Payment status\n");
        orders.findAll().stream().sorted(Comparator.comparing(CustomerOrder::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(o -> {
                    BigDecimal total = area == null ? o.getTotal() : areaTotal(o, area);
                    if (area != null && total == null) return;          // nothing in this order belongs to the area
                    sb.append(o.getId()).append(',')
                            .append(o.getCreatedAt() == null ? "" : o.getCreatedAt().toString().substring(0, 10)).append(',')
                            .append(o.getStatus()).append(',').append(total == null ? "" : total.toBigInteger()).append(',')
                            .append(cell(o.getPaymentMethod())).append(',')
                            .append(o.getPaymentStatus() == null ? "UNPAID" : o.getPaymentStatus()).append('\n');
                });
        return csvFile("farmconnect-orders.csv", sb);
    }

    /** farmer id -> sub-county: the sub-county of the farmer's first farm that has one, else the one on their verification. */
    private Map<Long, String> farmerAreas(List<Farm> farmList) {
        Map<Long, String> m = new HashMap<>();
        for (Farm f : farmList) {
            if (f.getSubCounty() != null && !f.getSubCounty().isBlank()) m.putIfAbsent(f.getOwner().getId(), f.getSubCounty());
        }
        for (FarmerVerification v : verifications.findAll()) {
            if (v.getSubCounty() != null && !v.getSubCounty().isBlank()) m.putIfAbsent(v.getFarmer().getId(), v.getSubCounty());
        }
        return m;
    }

    /** Ids of the farmers inside the area, or null when the officer sees everything. */
    private Set<Long> farmerIdsIn(AreaScope sc) {
        if (sc.all()) return null;
        Map<Long, String> sub = farmerAreas(farms.findAll());
        Set<Long> ids = new HashSet<>();
        for (User u : users.findAll()) if (u.getRole() == Role.FARMER && sc.allows(sub.get(u.getId()))) ids.add(u.getId());
        return ids;
    }

    /** What the area's farmers earn from one order (null when none of its items come from them). */
    private static BigDecimal areaTotal(CustomerOrder o, Set<Long> farmerIds) {
        BigDecimal sum = null;
        for (OrderItem i : o.getItems()) {
            if (farmerIds.contains(i.getProduct().getFarm().getOwner().getId()))
                sum = (sum == null ? BigDecimal.ZERO : sum).add(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
        }
        return sum;
    }

    private static ResponseEntity<String> csvFile(String name, StringBuilder body) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + name)
                .contentType(MediaType.parseMediaType("text/csv")).body(body.toString());
    }

    /** Quotes a CSV cell and defuses spreadsheet formulas (a name starting with = + - @ would otherwise run in Excel). */
    private static String cell(String s) {
        if (s == null) return "";
        String t = s.replace("\"", "\"\"");
        if (!t.isEmpty() && "=+-@\t\r".indexOf(t.charAt(0)) >= 0) t = "'" + t;
        return "\"" + t + "\"";
    }

    private static void row(StringBuilder sb, String section, String item, Object v) {
        sb.append('"').append(section).append("\",\"").append(item.replace("\"", "'")).append("\",\"").append(v).append("\"\n");
    }

    private Analytics compute(AreaScope sc) {
        final boolean scoped = !sc.all();
        List<User> all = users.findAll();
        List<Farm> farmList = farms.findAll();
        Map<Long, String> farmerSub = farmerAreas(farmList);

        List<User> farmers = all.stream().filter(u -> u.getRole() == Role.FARMER && sc.allows(farmerSub.get(u.getId()))).toList();
        final Set<Long> farmerIds = farmers.stream().map(User::getId).collect(Collectors.toSet());

        // everything below is limited to the officer's area (administrators see it all)
        final List<Product> prods = products.findAll().stream()
                .filter(p -> !scoped || farmerIds.contains(p.getFarm().getOwner().getId())).toList();
        final List<CustomerOrder> ords = orders.findAll().stream()
                .filter(o -> !scoped || areaTotal(o, farmerIds) != null).toList();
        final List<Post> postList = scoped ? List.<Post>of() : posts.findAll();
        final List<ChatMessage> msgs = messages.findAll().stream()
                .filter(m -> !scoped || farmerIds.contains(m.getSender().getId()) || farmerIds.contains(m.getRecipient().getId())).toList();
        final List<FarmRecord> recs = records.findAll().stream()
                .filter(r -> !scoped || farmerIds.contains(r.getOwner().getId())).toList();
        final List<FarmerGroup> grps = groups.findAll().stream().filter(g -> sc.allows(g.getSubCounty())).toList();
        final Set<Long> groupIds = grps.stream().map(FarmerGroup::getId).collect(Collectors.toSet());
        final List<GroupMember> mems = members.findAll().stream().filter(m -> groupIds.contains(m.getGroup().getId())).toList();
        final List<Feedback> fb = scoped ? List.<Feedback>of() : feedback.findAll();
        Instant since = Instant.now().minusSeconds(30L * 24 * 3600);

        List<User> officers = scoped ? List.<User>of() : all.stream().filter(u -> u.getRole() == Role.OFFICER).toList();
        java.util.function.Predicate<User> active = u -> u.getLastActiveAt() != null && u.getLastActiveAt().isAfter(since);

        Map<String, Long> bySub = new TreeMap<>(), verifiedBySub = new TreeMap<>();
        Map<String, Long> byCounty = new TreeMap<>(), verifiedByCounty = new TreeMap<>();
        for (User f : farmers) {
            String s = farmerSub.getOrDefault(f.getId(), UNSPECIFIED);
            String c = Locations.countyOf(s) == null ? UNSPECIFIED : Locations.countyOf(s);
            bySub.merge(s, 1L, Long::sum);
            byCounty.merge(c, 1L, Long::sum);
            if (f.isVerified()) { verifiedBySub.merge(s, 1L, Long::sum); verifiedByCounty.merge(c, 1L, Long::sum); }
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

        // last 6 months of orders and sales (for an officer: only what their farmers sold)
        Map<String, Long> ordersByMonth = new LinkedHashMap<>();
        Map<String, BigDecimal> salesByMonth = new LinkedHashMap<>();
        YearMonth now = YearMonth.now(ZoneOffset.UTC);
        for (int i = 5; i >= 0; i--) { String k = now.minusMonths(i).toString(); ordersByMonth.put(k, 0L); salesByMonth.put(k, BigDecimal.ZERO); }
        for (CustomerOrder o : ords) {
            String k = YearMonth.from(o.getCreatedAt().atZone(ZoneOffset.UTC)).toString();
            if (!ordersByMonth.containsKey(k)) continue;
            ordersByMonth.merge(k, 1L, Long::sum);
            if (o.getStatus() != OrderStatus.CANCELLED) salesByMonth.merge(k, scoped ? areaTotal(o, farmerIds) : o.getTotal(), BigDecimal::add);
        }

        Map<String, Long> postsByType = new TreeMap<>();
        postList.forEach(p -> postsByType.merge(p.getType().name(), 1L, Long::sum));

        Map<String, Long> harvest = new TreeMap<>();
        recs.stream().filter(r -> r.getType() == RecordType.HARVEST && r.getQuantity() != null)
                .forEach(r -> harvest.merge(r.getTitle(), r.getQuantity().longValue(), Long::sum));

        // farmer <-> buyer connections: pairs that ordered together or exchanged messages
        Set<String> pairs = new HashSet<>();
        for (CustomerOrder o : ords) {
            for (OrderItem i : o.getItems()) {
                Long fid = i.getProduct().getFarm().getOwner().getId();
                if (!scoped || farmerIds.contains(fid)) pairs.add(o.getBuyer().getId() + ":" + fid);
            }
        }
        for (ChatMessage m : msgs) {
            User a = m.getSender(), b = m.getRecipient();
            if (a.getRole() == Role.FARMER && b.getRole() == Role.BUYER && (!scoped || farmerIds.contains(a.getId()))) pairs.add(b.getId() + ":" + a.getId());
            if (b.getRole() == Role.FARMER && a.getRole() == Role.BUYER && (!scoped || farmerIds.contains(b.getId()))) pairs.add(a.getId() + ":" + b.getId());
        }

        long views = postList.stream().mapToLong(Post::getViews).sum();
        long regs = scoped ? 0 : registrations.count();
        long opportunities = scoped ? 0 : registrations.findAll().stream()
                .filter(r -> r.getPost().getType() == PostType.OPPORTUNITY || r.getPost().getType() == PostType.TRAINING
                        || r.getPost().getType() == PostType.PROGRAMME).count();
        long communicated = postList.stream().filter(p -> p.getType() == PostType.ANNOUNCEMENT || p.getType() == PostType.PROGRAMME
                || p.getType() == PostType.TRAINING || p.getType() == PostType.OPPORTUNITY).count();
        double avg = fb.isEmpty() ? 0 : fb.stream().mapToInt(Feedback::getRating).average().orElse(0);
        long recordFarmers = recs.stream().map(r -> r.getOwner().getId()).distinct().count();
        long buyers = scoped ? 0 : all.stream().filter(u -> u.getRole() == Role.BUYER).count();
        long activeUsers = scoped ? farmers.stream().filter(active).count() : all.stream().filter(active).count();

        Indicators ind = new Indicators(farmers.size(), farmers.stream().filter(User::isVerified).count(),
                buyers, activeUsers,
                grps.size(), mems.size(), views + regs,
                prods.stream().filter(p -> p.isActive() && p.getFarm().getOwner().isVerified()).count(), pairs.size(),
                opportunities, officers.size(), officers.stream().filter(active).count(), communicated,
                Math.round(avg * 100) / 100.0, fb.size(), recordFarmers, recs.size(), msgs.size());
        return new Analytics(ind, bySub, verifiedBySub, prodByCat, grpByType, membersByGroup, ordersByMonth, salesByMonth,
                postsByType, harvest, byCounty, verifiedByCounty, scoped, sc.label());
    }
}
