package com.lawfirm.lcm.config;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Seeds demo data for local/docker use. Runs once at startup if the database is empty.
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PracticeAreaRepository practiceAreaRepository;
    private final MatterRepository matterRepository;
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final TaskRepository taskRepository;
    private final NoteRepository noteRepository;
    private final ClientMessageRepository clientMessageRepository;
    private final DeadlineRepository deadlineRepository;
    private final RetentionSettingRepository retentionSettingRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Value("${app.demo-password}")
    private String demoPassword;

    private final Random random = new Random(42);

    @PostConstruct
    public void seed() throws IOException {
        if (userRepository.count() > 0) {
            return;
        }
        Files.createDirectories(Paths.get(uploadDir));

        if (retentionSettingRepository.findById(1L).isEmpty()) {
            retentionSettingRepository.save(new RetentionSetting());
        }

        User admin = user("admin@lawfirm.test", "Ada Admin", User.Role.FIRM_ADMIN);
        User recordsOfficer = new User();
        recordsOfficer.setEmail("records.officer@lawfirm.test");
        recordsOfficer.setFullName("Records Officer");
        recordsOfficer.setRole(User.Role.FIRM_ADMIN);
        recordsOfficer.setPasswordHash(passwordEncoder.encode("Records#2024"));
        userRepository.save(recordsOfficer);
        List<User> attorneys = new ArrayList<>();
        attorneys.add(user("attorney@lawfirm.test", "Alice Attorney", User.Role.ATTORNEY));
        attorneys.add(user("attorney2@lawfirm.test", "Aaron Attorney", User.Role.ATTORNEY));
        attorneys.add(user("attorney3@lawfirm.test", "Amy Attorney", User.Role.ATTORNEY));
        List<User> paralegals = new ArrayList<>();
        paralegals.add(user("paralegal@lawfirm.test", "Paula Paralegal", User.Role.PARALEGAL));
        paralegals.add(user("paralegal2@lawfirm.test", "Pete Paralegal", User.Role.PARALEGAL));

        List<PracticeArea> areas = new ArrayList<>();
        for (String name : List.of("Corporate", "Litigation", "Family Law", "Real Estate", "Intellectual Property", "Employment")) {
            PracticeArea pa = new PracticeArea();
            pa.setName(name);
            pa.setDescription(name + " practice area");
            areas.add(practiceAreaRepository.save(pa));
        }

        List<Client> clients = new ArrayList<>();
        String[] personNames = {"John Carter", "Maria Gonzalez", "David Kim", "Sarah Chen", "Robert Lee", "Emily Davis", "Michael Brown", "Linda Wilson"};
        String[] orgNames = {"Acme Corp", "Globex LLC", "Initech Inc", "Umbrella Holdings"};
        int idx = 0;
        for (String name : personNames) {
            Client c = new Client();
            c.setName(name);
            c.setType(Client.ClientType.PERSON);
            c.setEmail("client" + (idx++) + "@example.com");
            c.setPhone("555-01" + (10 + idx));
            c.setAddress(idx + " Main St, Springfield");
            c.setStatus(Client.Status.ACTIVE);
            clients.add(clientRepository.save(c));
        }
        for (String name : orgNames) {
            Client c = new Client();
            c.setName(name);
            c.setType(Client.ClientType.ORGANIZATION);
            c.setEmail("contact" + (idx++) + "@example.com");
            c.setPhone("555-02" + (10 + idx));
            c.setAddress(idx + " Commerce Ave, Metropolis");
            c.setStatus(Client.Status.ACTIVE);
            clients.add(clientRepository.save(c));
        }
        // create a client portal login for the first client
        User clientUser = user("client@lawfirm.test", "John Carter", User.Role.CLIENT);
        clients.get(0).setPortalUser(clientUser);
        clientRepository.save(clients.get(0));
        User clientUser2 = user("client2@lawfirm.test", "Maria Gonzalez", User.Role.CLIENT);
        clients.get(1).setPortalUser(clientUser2);
        clientRepository.save(clients.get(1));

        Matter.Status[] statuses = Matter.Status.values();
        List<Matter> matters = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Matter m = new Matter();
            m.setTitle("Matter " + (i + 1) + " - " + areas.get(i % areas.size()).getName());
            m.setDescription("Sample matter description for demo purposes, item " + (i + 1) + ".");
            m.setClient(clients.get(i % clients.size()));
            m.setPracticeArea(areas.get(i % areas.size()));
            m.setAssignedAttorney(attorneys.get(i % attorneys.size()));
            Matter.Status status = statuses[i % statuses.length];
            m.setStatus(status);
            m.setSensitivityLevel(Matter.Sensitivity.values()[i % 3]);
            if (status != Matter.Status.INTAKE && status != Matter.Status.CONFLICT_REVIEW) {
                m.setOpenedDate(LocalDate.now().minusDays(30L + i));
            }
            if (status == Matter.Status.CLOSED || status == Matter.Status.ARCHIVED) {
                m.setClosedDate(LocalDate.now().minusDays(i));
            }
            matters.add(matterRepository.save(m));
        }

        // Documents + versions
        String[] categories = {"PLEADING", "CONTRACT", "CORRESPONDENCE", "EVIDENCE", "FILING", "INTERNAL_MEMO"};
        String[] privileges = {"NONE", "ATTORNEY_CLIENT", "WORK_PRODUCT"};
        String[] visibilities = {"INTERNAL_ONLY", "PRIVILEGED", "CLIENT_SHARED", "PUBLIC_FILING"};
        int docCount = 0;
        for (Matter m : matters) {
            int docsForMatter = 3;
            for (int j = 0; j < docsForMatter; j++) {
                Document doc = new Document();
                doc.setMatter(m);
                doc.setTitle(m.getTitle() + " - Document " + (j + 1));
                doc.setCategory(Document.Category.valueOf(categories[docCount % categories.length]));
                doc.setPrivilegeLevel(Document.PrivilegeLevel.valueOf(privileges[docCount % privileges.length]));
                doc.setVisibility(Document.Visibility.valueOf(visibilities[docCount % visibilities.length]));
                doc.setUploadedBy(attorneys.get(docCount % attorneys.size()));
                doc = documentRepository.save(doc);
                int versions = 1 + (docCount % 3);
                for (int v = 1; v <= versions; v++) {
                    String storedName = "seed-" + doc.getId() + "-v" + v + ".txt";
                    Path filePath = Paths.get(uploadDir, storedName);
                    Files.writeString(filePath, "Demo content for " + doc.getTitle() + " version " + v, StandardCharsets.UTF_8);
                    DocumentVersion dv = new DocumentVersion();
                    dv.setDocument(doc);
                    dv.setVersionNumber(v);
                    dv.setOriginalFilename(doc.getTitle().replaceAll("\\s+", "_") + "_v" + v + ".txt");
                    dv.setStoredFilename(storedName);
                    dv.setFileSize(Files.size(filePath));
                    dv.setUploadedBy(doc.getUploadedBy());
                    dv.setNotes(v == 1 ? "Initial upload" : "Revision " + v);
                    documentVersionRepository.save(dv);
                }
                docCount++;
                if (docCount >= 62) break;
            }
            if (docCount >= 62) break;
        }

        // Tasks
        Task.Priority[] priorities = Task.Priority.values();
        Task.Status[] taskStatuses = Task.Status.values();
        int taskCount = 0;
        for (Matter m : matters) {
            for (int j = 0; j < 2; j++) {
                Task t = new Task();
                t.setMatter(m);
                t.setTitle("Task " + (taskCount + 1) + " for " + m.getTitle());
                t.setDescription("Demo task description.");
                t.setAssignee(taskCount % 2 == 0 ? attorneys.get(taskCount % attorneys.size()) : paralegals.get(taskCount % paralegals.size()));
                t.setPriority(priorities[taskCount % priorities.length]);
                t.setStatus(taskStatuses[taskCount % 3]);
                t.setDueDate(LocalDate.now().plusDays(taskCount % 20 - 5));
                taskRepository.save(t);
                taskCount++;
                if (taskCount >= 42) break;
            }
            if (taskCount >= 42) break;
        }

        // Deadlines
        int deadlineCount = 0;
        for (Matter m : matters) {
            Deadline d = new Deadline();
            d.setMatter(m);
            d.setTitle("Deadline for " + m.getTitle());
            d.setDueDate(LocalDate.now().plusDays(deadlineCount % 15 - 2));
            d.setResponsible(m.getAssignedAttorney());
            d.setCompleted(deadlineCount % 5 == 0);
            deadlineRepository.save(d);
            deadlineCount++;
            if (deadlineCount >= 22) break;
        }

        // Notes (privileged) and client messages
        int noteCount = 0;
        int messageCount = 0;
        for (Matter m : matters) {
            Note n = new Note();
            n.setMatter(m);
            n.setAuthor(m.getAssignedAttorney());
            n.setBody("Privileged internal note regarding strategy for " + m.getTitle() + ".");
            noteRepository.save(n);
            noteCount++;

            ClientMessage cm1 = new ClientMessage();
            cm1.setMatter(m);
            cm1.setSender(m.getAssignedAttorney());
            cm1.setBody("Update regarding your matter: " + m.getTitle());
            cm1.setStatus(messageCount % 3 == 0 ? ClientMessage.Status.DRAFT : ClientMessage.Status.SENT);
            if (cm1.getStatus() == ClientMessage.Status.SENT) cm1.setApprovedBy(m.getAssignedAttorney());
            clientMessageRepository.save(cm1);
            messageCount++;
            if (noteCount >= 18) break;
        }
        // pad remaining notes/messages to hit minimums
        while (noteCount < 18) {
            Matter m = matters.get(noteCount % matters.size());
            Note n = new Note();
            n.setMatter(m);
            n.setAuthor(m.getAssignedAttorney());
            n.setBody("Additional privileged note #" + noteCount);
            noteRepository.save(n);
            noteCount++;
        }
        while (messageCount < 18) {
            Matter m = matters.get(messageCount % matters.size());
            ClientMessage cm = new ClientMessage();
            cm.setMatter(m);
            cm.setSender(m.getAssignedAttorney());
            cm.setBody("Additional client update #" + messageCount);
            cm.setStatus(ClientMessage.Status.SENT);
            cm.setApprovedBy(m.getAssignedAttorney());
            clientMessageRepository.save(cm);
            messageCount++;
        }
    }

    private User user(String email, String fullName, User.Role role) {
        User u = new User();
        u.setEmail(email);
        u.setFullName(fullName);
        u.setRole(role);
        u.setPasswordHash(passwordEncoder.encode(demoPassword));
        return userRepository.save(u);
    }
}
