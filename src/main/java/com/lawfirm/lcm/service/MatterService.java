package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.domain.ApiKey;
import com.lawfirm.lcm.repository.ApiKeyRepository;
import com.lawfirm.lcm.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MatterService {

    private final MatterRepository matterRepository;
    private final ApiKeyRepository apiKeyRepository;
    private final AuditService auditService;
    private final HttpClient partnerClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    // Whitelisted, bounded sort columns for the matter list screen. The column name is
    // validated against this enum before being used to sort - never interpolated raw
    // from request params - values themselves are always bound query parameters.
    public enum SortColumn { TITLE, STATUS, OPENED_DATE, CLIENT }

    private static final java.util.Map<Matter.Status, Set<Matter.Status>> ALLOWED_TRANSITIONS = java.util.Map.of(
            Matter.Status.INTAKE, EnumSet.of(Matter.Status.CONFLICT_REVIEW),
            Matter.Status.CONFLICT_REVIEW, EnumSet.of(Matter.Status.OPEN),
            Matter.Status.OPEN, EnumSet.of(Matter.Status.ON_HOLD, Matter.Status.CLOSED),
            Matter.Status.ON_HOLD, EnumSet.of(Matter.Status.OPEN, Matter.Status.CLOSED),
            Matter.Status.CLOSED, EnumSet.of(Matter.Status.ARCHIVED),
            Matter.Status.ARCHIVED, EnumSet.noneOf(Matter.Status.class)
    );

    public List<Matter> search(Matter.Status status, Long practiceAreaId, Long attorneyId, Long clientId, SortColumn sortColumn) {
        List<Matter> results = matterRepository.search(status, practiceAreaId, attorneyId, clientId);
        // Sort in memory using the whitelisted column - avoids building dynamic SQL from
        // client input while still supporting arbitrary sort selection.
        Comparator<Matter> comparator = switch (sortColumn == null ? SortColumn.TITLE : sortColumn) {
            case TITLE -> Comparator.comparing(Matter::getTitle, String.CASE_INSENSITIVE_ORDER);
            case STATUS -> Comparator.comparing(Matter::getStatus);
            case OPENED_DATE -> Comparator.comparing(Matter::getOpenedDate, Comparator.nullsLast(Comparator.naturalOrder()));
            case CLIENT -> Comparator.comparing(m -> m.getClient().getName(), String.CASE_INSENSITIVE_ORDER);
        };
        results.sort(comparator);
        return results;
    }

    public Matter create(Matter matter) {
        matter.setStatus(Matter.Status.INTAKE);
        return matterRepository.save(matter);
    }

    public void transition(Matter matter, Matter.Status target, User actor) {
        Set<Matter.Status> allowed = ALLOWED_TRANSITIONS.getOrDefault(matter.getStatus(), EnumSet.noneOf(Matter.Status.class));
        if (!allowed.contains(target)) {
            throw new IllegalStateException("Cannot transition matter from " + matter.getStatus() + " to " + target);
        }
        if (matter.getStatus() == Matter.Status.ARCHIVED && actor.getRole() != User.Role.FIRM_ADMIN) {
            throw new AccessDeniedException("Only firm admins may modify archived matters");
        }
        String before = matter.getStatus().name();
        matter.setStatus(target);
        if (target == Matter.Status.OPEN && matter.getOpenedDate() == null) {
            matter.setOpenedDate(LocalDate.now());
        }
        if (target == Matter.Status.CLOSED) {
            matter.setClosedDate(LocalDate.now());
        }
        matterRepository.save(matter);
        auditService.log(actor, "MATTER_STATUS_CHANGE", "Matter", matter.getId(), before, target.name());
        notifyPartners(matter, target);
    }

    // Partner systems that registered a status callback with their integration key get a
    // ping whenever a matter they track moves to a new status.
    private void notifyPartners(Matter matter, Matter.Status target) {
        for (ApiKey key : apiKeyRepository.findAll()) {
            String callbackUrl = key.getCallbackUrl();
            if (!key.isActive() || callbackUrl == null || callbackUrl.isBlank()) continue;
            try {
                HttpRequest request = HttpRequest.newBuilder(URI.create(callbackUrl
                                + "?matter=" + matter.getId() + "&status=" + target.name()))
                        .timeout(Duration.ofSeconds(5))
                        .GET().build();
                partnerClient.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (Exception ex) {
                auditService.log(null, "MATTER_CALLBACK_FAILED", "Matter", matter.getId(), null, key.getName());
            }
        }
    }
}
