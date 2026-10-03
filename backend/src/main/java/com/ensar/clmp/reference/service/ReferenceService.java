package com.ensar.clmp.reference.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.consultant.domain.ConsultantStatus;
import com.ensar.clmp.marketing.domain.MarketingStatus;
import com.ensar.clmp.reference.domain.RegionRepository;
import com.ensar.clmp.reference.domain.TeamRepository;
import com.ensar.clmp.reference.domain.VisaType;
import com.ensar.clmp.reference.web.ReferenceResponse;

@Service
@Transactional(readOnly = true)
public class ReferenceService {

    private final TeamRepository teams;
    private final RegionRepository regions;

    public ReferenceService(TeamRepository teams, RegionRepository regions) {
        this.teams = teams;
        this.regions = regions;
    }

    public ReferenceResponse reference() {
        return new ReferenceResponse(
                teams.findAllByOrderByName().stream().filter(t -> t.isActive())
                        .map(t -> new ReferenceResponse.Item(t.getId(), t.getCode(), t.getName())).toList(),
                regions.findAllByOrderByName().stream().filter(r -> r.isActive())
                        .map(r -> new ReferenceResponse.Item(r.getId(), r.getCode(), r.getName())).toList(),
                List.of(VisaType.values()),
                List.of(ConsultantStatus.values()),
                List.of(MarketingStatus.values()));
    }
}
