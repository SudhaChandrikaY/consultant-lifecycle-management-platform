package com.ensar.clmp.reference.service;

import java.time.Clock;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ensar.clmp.auth.CurrentUser;
import com.ensar.clmp.common.error.BusinessException;
import com.ensar.clmp.common.web.NamedRef;
import com.ensar.clmp.reference.domain.Client;
import com.ensar.clmp.reference.domain.ClientRepository;
import com.ensar.clmp.reference.domain.Vendor;
import com.ensar.clmp.reference.domain.VendorRepository;

/** Vendor and client lookup with find-or-create by normalized name (FR-051). */
@Service
public class CounterpartyService {

    private final VendorRepository vendors;
    private final ClientRepository clients;
    private final Clock clock;

    public CounterpartyService(VendorRepository vendors, ClientRepository clients, Clock clock) {
        this.vendors = vendors;
        this.clients = clients;
        this.clock = clock;
    }

    /** Exactly one of {@code id} or {@code name} must be given. */
    @Transactional
    public Vendor resolveVendor(Long id, String name, CurrentUser actor) {
        checkExactlyOne(id, name, "vendorName", "vendor");
        if (id != null) {
            return vendors.findById(id).orElseThrow(() -> BusinessException.fieldError("vendorId",
                    "The selected vendor does not exist."));
        }
        checkLength(name, "vendorName");
        return vendors.findByNormalizedName(NameNormalizer.normalize(name))
                .orElseGet(() -> vendors.save(new Vendor(name, actor.userId(), clock.instant())));
    }

    @Transactional
    public Client resolveClient(Long id, String name, CurrentUser actor) {
        checkExactlyOne(id, name, "clientName", "client");
        if (id != null) {
            return clients.findById(id).orElseThrow(() -> BusinessException.fieldError("clientId",
                    "The selected client does not exist."));
        }
        checkLength(name, "clientName");
        return clients.findByNormalizedName(NameNormalizer.normalize(name))
                .orElseGet(() -> clients.save(new Client(name, actor.userId(), clock.instant())));
    }

    @Transactional(readOnly = true)
    public List<NamedRef> searchVendors(String q) {
        return vendors.findTop20ByNormalizedNameContainingOrderByName(fragment(q)).stream()
                .map(v -> new NamedRef(v.getId(), v.getName())).toList();
    }

    @Transactional(readOnly = true)
    public List<NamedRef> searchClients(String q) {
        return clients.findTop20ByNormalizedNameContainingOrderByName(fragment(q)).stream()
                .map(c -> new NamedRef(c.getId(), c.getName())).toList();
    }

    private static String fragment(String q) {
        String n = NameNormalizer.normalize(q);
        return n == null ? "" : n;
    }

    private static void checkExactlyOne(Long id, String name, String field, String label) {
        boolean hasName = name != null && !name.isBlank();
        if ((id == null) == !hasName) {
            throw BusinessException.fieldError(field, "Choose an existing " + label + " or enter a new name.");
        }
    }

    private static void checkLength(String name, String field) {
        if (name.trim().length() > 120) {
            throw BusinessException.fieldError(field, "Name is too long.");
        }
    }
}
