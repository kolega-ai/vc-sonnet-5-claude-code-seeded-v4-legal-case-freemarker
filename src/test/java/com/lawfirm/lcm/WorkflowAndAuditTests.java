package com.lawfirm.lcm;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.repository.*;
import com.lawfirm.lcm.service.DocumentService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WorkflowAndAuditTests {

    @Autowired UserRepository userRepository;
    @Autowired ClientRepository clientRepository;
    @Autowired MatterRepository matterRepository;
    @Autowired DocumentService documentService;
    @Autowired AuditLogRepository auditLogRepository;
    @Autowired PasswordEncoder passwordEncoder;

    User attorney;
    Matter matter;

    @BeforeEach
    void setUp() {
        attorney = new User();
        attorney.setEmail("wf-attorney@lawfirm.test");
        attorney.setFullName("WF Attorney");
        attorney.setRole(User.Role.ATTORNEY);
        attorney.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
        attorney = userRepository.save(attorney);

        Client client = new Client();
        client.setName("WF Client");
        client.setType(Client.ClientType.PERSON);
        client = clientRepository.save(client);

        matter = new Matter();
        matter.setTitle("WF Matter");
        matter.setClient(client);
        matter.setAssignedAttorney(attorney);
        matter.setStatus(Matter.Status.OPEN);
        matter = matterRepository.save(matter);
    }

    @Test
    void documentVisibilityChangeIsAudited() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        Document doc = documentService.createDocument(matter, "Test Doc", Document.Category.CONTRACT,
                Document.PrivilegeLevel.NONE, file, attorney);

        long before = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("Document", doc.getId()).size();

        documentService.changeVisibility(doc, Document.Visibility.CLIENT_SHARED, attorney);

        var logs = auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("Document", doc.getId());
        assertTrue(logs.size() > before);
        assertTrue(logs.stream().anyMatch(l -> l.getAction().equals("DOCUMENT_VISIBILITY_CHANGE")));
        assertEquals(Document.Visibility.CLIENT_SHARED, doc.getVisibility());
    }
}
