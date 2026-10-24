package com.farmconnect.service;

import com.farmconnect.dto.MarketDtos.MemberForm;
import com.farmconnect.dto.MarketDtos.MemberView;
import com.farmconnect.model.Company;
import com.farmconnect.model.CompanyMember;
import com.farmconnect.model.CompanyStatus;
import com.farmconnect.model.User;
import com.farmconnect.repository.CompanyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** The company's staff directory (owners, managers, drivers...). Contact records only: team members do not log in. */
@Service
@RequiredArgsConstructor
public class CompanyTeamService {
    private static final int MAX_MEMBERS = 50;
    private final CompanyMemberRepository members;
    private final CompanyService companies;

    @Transactional(readOnly = true)
    public List<MemberView> list(User owner) {
        return members.findByCompanyIdOrderByNameAsc(companies.entityOf(owner).getId()).stream().map(CompanyTeamService::view).toList();
    }

    @Transactional
    public MemberView create(User owner, MemberForm f) {
        Company c = writable(owner);
        if (members.countByCompanyId(c.getId()) >= MAX_MEMBERS)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A company can list up to " + MAX_MEMBERS + " team members");
        CompanyMember m = new CompanyMember();
        m.setCompany(c);
        apply(m, f);
        return view(members.save(m));
    }

    @Transactional
    public MemberView update(User owner, Long id, MemberForm f) {
        CompanyMember m = own(writable(owner), id);
        apply(m, f);
        return view(members.save(m));
    }

    @Transactional
    public void delete(User owner, Long id) {
        members.delete(own(writable(owner), id));
    }

    private Company writable(User owner) {
        Company c = companies.entityOf(owner);
        if (c.getStatus() == CompanyStatus.SUSPENDED) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This company is suspended");
        return c;
    }

    private CompanyMember own(Company c, Long id) {
        return members.findById(id).filter(m -> m.getCompany().getId().equals(c.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Team member not found"));
    }

    private static void apply(CompanyMember m, MemberForm f) {
        m.setName(f.name().trim());
        m.setPhone(f.phone() == null || f.phone().isBlank() ? null : f.phone().trim());
        m.setEmail(f.email() == null || f.email().isBlank() ? null : f.email().trim());
        m.setRole(f.role());
        m.setActive(f.active());
    }

    static MemberView view(CompanyMember m) {
        return new MemberView(m.getId(), m.getName(), m.getPhone(), m.getEmail(), m.getRole(), m.isActive(), m.getCreatedAt());
    }
}
