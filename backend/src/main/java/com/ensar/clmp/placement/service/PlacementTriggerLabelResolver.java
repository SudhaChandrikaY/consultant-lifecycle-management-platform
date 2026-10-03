package com.ensar.clmp.placement.service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.ensar.clmp.history.domain.HistoryEntityType;
import com.ensar.clmp.history.service.TriggerLabelResolver;
import com.ensar.clmp.placement.domain.Placement;
import com.ensar.clmp.placement.domain.PlacementRepository;

/** "Placement at {client} via {vendor}"; HR gets the generic "Placement created" instead. */
@Component
public class PlacementTriggerLabelResolver implements TriggerLabelResolver {

    private final PlacementRepository placements;

    public PlacementTriggerLabelResolver(PlacementRepository placements) {
        this.placements = placements;
    }

    @Override
    public boolean supports(HistoryEntityType entityType) {
        return entityType == HistoryEntityType.PLACEMENT;
    }

    @Override
    public Map<Long, String> labels(Set<Long> ids) {
        return placements.findLabelSourcesByIdIn(ids).stream().collect(Collectors.toMap(Placement::getId,
                p -> "Placement at " + p.getClient().getName() + " via " + p.getVendor().getName()));
    }
}
