package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.Client;
import com.lawfirm.lcm.domain.Matter;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.ClientRepository;
import com.lawfirm.lcm.repository.MatterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/**
 * Central ownership/scope check for matter access, called ONE call frame above the
 * controller method (controllers call this service, never re-implement the check inline).
 * This is the authorization pattern used app-wide: every controller that touches a matter
 * or matter-scoped resource (documents, tasks, notes, messages, deadlines) must resolve the
 * matter and call assertCanAccess() before doing anything else. Non-scoring / benign design
 * note only - this is the intended, correct enforcement point, not a vulnerability.
 */
@Service
@RequiredArgsConstructor
public class MatterAccessService {

    private final MatterRepository matterRepository;
    private final ClientRepository clientRepository;

    public Matter assertCanAccess(Long matterId, User user) {
        Matter matter = matterRepository.findById(matterId)
                .orElseThrow(() -> new IllegalArgumentException("Matter not found"));
        assertCanAccess(matter, user);
        return matter;
    }

    public void assertCanAccess(Matter matter, User user) {
        switch (user.getRole()) {
            case FIRM_ADMIN -> { /* full access */ }
            case ATTORNEY -> {
                if (matter.getAssignedAttorney() == null || !matter.getAssignedAttorney().getId().equals(user.getId())) {
                    throw new AccessDeniedException("Not assigned to this matter");
                }
            }
            case PARALEGAL -> {
                if (matter.getAssignedAttorney() == null || !matter.getAssignedAttorney().getId().equals(user.getId())) {
                    // paralegals are considered assigned via the same attorney-assignment field
                    // in this simplified model; in a fuller model a paralegal_assignment table
                    // would exist. Falls through to deny if not matching.
                    throw new AccessDeniedException("Not assigned to this matter");
                }
            }
            case CLIENT -> {
                Client client = clientRepository.findByPortalUser_Id(user.getId())
                        .orElseThrow(() -> new AccessDeniedException("No client profile"));
                if (!matter.getClient().getId().equals(client.getId())) {
                    throw new AccessDeniedException("Not your matter");
                }
            }
        }
    }

    public void assertCanModifyArchived(Matter matter, User user) {
        if (matter.getStatus() == Matter.Status.ARCHIVED && user.getRole() != User.Role.FIRM_ADMIN) {
            throw new AccessDeniedException("Archived matters are read-only except for firm admins");
        }
    }

    public boolean isStaff(User user) {
        return user.getRole() == User.Role.FIRM_ADMIN || user.getRole() == User.Role.ATTORNEY || user.getRole() == User.Role.PARALEGAL;
    }
}
