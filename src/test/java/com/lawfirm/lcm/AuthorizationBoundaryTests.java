package com.lawfirm.lcm;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.repository.*;
import com.lawfirm.lcm.service.MatterAccessService;
import com.lawfirm.lcm.service.MatterService;
import com.lawfirm.lcm.service.TaskService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorizationBoundaryTests {

    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MatterRepository matterRepository;
    @Autowired NoteRepository noteRepository;
    @Autowired MatterAccessService matterAccessService;
    @Autowired MatterService matterService;
    @Autowired TaskService taskService;
    @Autowired PasswordEncoder passwordEncoder;

    User attorney, otherAttorney, client1User, client2User;
    Client client1, client2;
    Matter matter1, matter2;

    @BeforeEach
    void setUp() {
        attorney = save(user("attorney-test@lawfirm.test", User.Role.ATTORNEY));
        otherAttorney = save(user("attorney-test-2@lawfirm.test", User.Role.ATTORNEY));
        client1User = save(user("client-test-1@lawfirm.test", User.Role.CLIENT));
        client2User = save(user("client-test-2@lawfirm.test", User.Role.CLIENT));

        client1 = new Client();
        client1.setName("Test Client One");
        client1.setType(Client.ClientType.PERSON);
        client1.setPortalUser(client1User);
        client1 = clientRepository.save(client1);

        client2 = new Client();
        client2.setName("Test Client Two");
        client2.setType(Client.ClientType.PERSON);
        client2.setPortalUser(client2User);
        client2 = clientRepository.save(client2);

        matter1 = new Matter();
        matter1.setTitle("Matter One");
        matter1.setClient(client1);
        matter1.setAssignedAttorney(attorney);
        matter1.setStatus(Matter.Status.OPEN);
        matter1 = matterRepository.save(matter1);

        matter2 = new Matter();
        matter2.setTitle("Matter Two");
        matter2.setClient(client2);
        matter2.setAssignedAttorney(otherAttorney);
        matter2.setStatus(Matter.Status.OPEN);
        matter2 = matterRepository.save(matter2);
    }

    private User user(String email, User.Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(email);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        return u;
    }

    private User save(User u) {
        return userRepository.save(u);
    }

    @Test
    void clientCannotAccessAnotherClientsMatter() {
        assertThrows(AccessDeniedException.class, () -> matterAccessService.assertCanAccess(matter2.getId(), client1User));
    }

    @Test
    void clientCanAccessOwnMatter() {
        assertDoesNotThrow(() -> matterAccessService.assertCanAccess(matter1.getId(), client1User));
    }

    @Test
    void attorneyCannotAccessUnassignedMatter() {
        assertThrows(AccessDeniedException.class, () -> matterAccessService.assertCanAccess(matter2.getId(), attorney));
    }

    @Test
    void archivedMatterIsReadOnlyExceptForAdmin() {
        matter1.setStatus(Matter.Status.ARCHIVED);
        matterRepository.save(matter1);
        assertThrows(AccessDeniedException.class, () -> matterAccessService.assertCanModifyArchived(matter1, attorney));

        User admin = save(user("admin-test@lawfirm.test", User.Role.FIRM_ADMIN));
        assertDoesNotThrow(() -> matterAccessService.assertCanModifyArchived(matter1, admin));
    }

    @Test
    void matterStatusTransitionRulesAreEnforced() {
        Matter matter = new Matter();
        matter.setTitle("Transition Matter");
        matter.setClient(client1);
        matter.setAssignedAttorney(attorney);
        matter.setStatus(Matter.Status.INTAKE);
        matter = matterRepository.save(matter);

        // Cannot skip straight to OPEN from INTAKE
        Matter finalMatter = matter;
        assertThrows(IllegalStateException.class, () -> matterService.transition(finalMatter, Matter.Status.OPEN, attorney));

        matterService.transition(matter, Matter.Status.CONFLICT_REVIEW, attorney);
        assertEquals(Matter.Status.CONFLICT_REVIEW, matter.getStatus());

        matterService.transition(matter, Matter.Status.OPEN, attorney);
        assertEquals(Matter.Status.OPEN, matter.getStatus());
    }

    @Test
    void taskStatusTransitionRulesAreEnforced() {
        Task task = new Task();
        task.setMatter(matter1);
        task.setTitle("Test Task");
        task.setStatus(Task.Status.OPEN);
        var saved = task;

        assertThrows(IllegalStateException.class, () -> taskService.transition(saved, Task.Status.COMPLETED, attorney));
    }
}
