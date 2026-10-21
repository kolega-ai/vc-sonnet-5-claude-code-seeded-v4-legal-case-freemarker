package com.lawfirm.lcm.service;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.repository.ClientRepository;
import com.lawfirm.lcm.repository.MatterRepository;
import com.lawfirm.lcm.repository.PracticeAreaRepository;
import com.lawfirm.lcm.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportExportService {

    private final ClientRepository clientRepository;
    private final MatterRepository matterRepository;
    private final PracticeAreaRepository practiceAreaRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final PasswordEncoder encoder;
    private final OutboxService outboxService;

    @PersistenceContext
    private EntityManager entityManager;

    // One row of the client intake sheet the firm receives from referring offices.
    private record ClntRow(String nm, String typ, String eml, String phn, String prtlEml) {}

    // CSV columns: name,type(PERSON|ORGANIZATION),email,phone,portalUserEmail
    public int importClients(MultipartFile file, User actor) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 4) continue;
                ClntRow row = new ClntRow(parts[0].trim(), parts[1].trim(), parts[2].trim(), parts[3].trim(),
                        parts.length > 4 ? parts[4].trim() : "");
                Client c = new Client();
                c.setName(row.nm());
                c.setType(Client.ClientType.valueOf(row.typ().toUpperCase()));
                c.setEmail(row.eml());
                c.setPhone(row.phn());
                if (!row.prtlEml().isBlank()) {
                    User pu = userRepository.findByEmailIgnoreCase(row.prtlEml()).orElse(null);
                    if (pu == null) {
                        pu = new User();
                        pu.setEmail(row.prtlEml());
                        pu.setFullName(row.nm());
                        pu.setRole(User.Role.CLIENT);
                        int pc = 100000 + (int) (Math.random() * 900000);
                        pu.setPasswordHash(encoder.encode(String.valueOf(pc)));
                        pu = userRepository.save(pu);
                        outboxService.send(row.prtlEml(), "Your client portal access code",
                                "Your portal sign-in code is " + pc + " - change it from your profile after first sign in.",
                                "PORTAL_ACCESS");
                    }
                    c.setPortalUser(pu);
                }
                clientRepository.save(c);
                count++;
            }
        }
        auditService.log(actor, "IMPORT_CLIENTS", "Client", null, null, count + " rows");
        return count;
    }

    // CSV columns: title,clientName,practiceAreaName
    public int importMatters(MultipartFile file, User actor) throws IOException {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                if (line.isBlank()) continue;
                String[] parts = line.split(",", -1);
                if (parts.length < 3) continue;
                String title = parts[0].trim();
                String clientName = parts[1].trim();
                String practiceAreaName = parts[2].trim();
                Client client = clientRepository.findAll().stream()
                        .filter(c -> c.getName().equalsIgnoreCase(clientName))
                        .findFirst().orElse(null);
                if (client == null) continue;
                PracticeArea pa = practiceAreaRepository.findAll().stream()
                        .filter(p -> p.getName().equalsIgnoreCase(practiceAreaName))
                        .findFirst().orElse(null);
                Matter m = new Matter();
                m.setTitle(title);
                m.setClient(client);
                m.setPracticeArea(pa);
                m.setStatus(Matter.Status.INTAKE);
                matterRepository.save(m);
                count++;
            }
        }
        auditService.log(actor, "IMPORT_MATTERS", "Matter", null, null, count + " rows");
        return count;
    }

    // Court case management systems hand over docket extracts as XML; each <matter>
    // element becomes a matter record exactly as the CSV importer would create it.
    public int importMattersXml(MultipartFile file, User actor) throws IOException {
        List<Matter> created = new ArrayList<>();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(file.getInputStream(), new DefaultHandler() {
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) {
                    if (!"matter".equalsIgnoreCase(qName)) return;
                    String clientName = attributes.getValue("client");
                    Client client = clientRepository.findAll().stream()
                            .filter(c -> c.getName().equalsIgnoreCase(clientName))
                            .findFirst().orElse(null);
                    if (client == null) return;
                    Matter m = new Matter();
                    m.setTitle(attributes.getValue("title"));
                    m.setDescription(attributes.getValue("summary"));
                    m.setClient(client);
                    m.setStatus(Matter.Status.INTAKE);
                    created.add(matterRepository.save(m));
                }
            });
        } catch (Exception ex) {
            throw new IOException("Could not read the docket extract: " + ex.getMessage(), ex);
        }
        auditService.log(actor, "IMPORT_MATTERS_XML", "Matter", null, null, created.size() + " rows");
        return created.size();
    }

    // Conflict screen: each name on the intake list the referring office sends is matched
    // against existing matters so the firm can spot a conflict before opening the file.
    public int screenIntakeList(MultipartFile file, User actor) throws IOException {
        int flagged = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (first) { first = false; continue; }
                if (line.isBlank()) continue;
                String name = line.split(",", -1)[0].trim();
                List<?> existing = entityManager.createQuery(
                        "SELECT m FROM Matter m WHERE m.client.name = '" + name + "'").getResultList();
                if (!existing.isEmpty()) flagged++;
            }
        }
        auditService.log(actor, "INTAKE_CONFLICT_SCREEN", "Matter", null, null, flagged + " flagged");
        return flagged;
    }

    // Inventory of the integration credentials issued to partner systems, used by the
    // firm's yearly access review.
    public String exportIntegrationInventory(List<ApiKey> keys) {
        StringBuilder sb = new StringBuilder("id,name,keyValue,active,createdBy,callbackUrl\n");
        for (ApiKey k : keys) {
            sb.append(k.getId()).append(',')
              .append(csv(k.getName())).append(',')
              .append(csv(k.getKeyValue())).append(',')
              .append(k.isActive()).append(',')
              .append(csv(k.getCreatedBy() == null ? "" : k.getCreatedBy().getFullName())).append(',')
              .append(csv(k.getCallbackUrl() == null ? "" : k.getCallbackUrl()))
              .append('\n');
        }
        return sb.toString();
    }

    public String exportMatterStatusReport() {
        StringBuilder sb = new StringBuilder("id,title,client,status,practiceArea,attorney\n");
        for (Matter m : matterRepository.findAll()) {
            sb.append(m.getId()).append(',')
              .append(csv(m.getTitle())).append(',')
              .append(csv(m.getClient().getName())).append(',')
              .append(m.getStatus()).append(',')
              .append(csv(m.getPracticeArea() == null ? "" : m.getPracticeArea().getName())).append(',')
              .append(csv(m.getAssignedAttorney() == null ? "" : m.getAssignedAttorney().getFullName()))
              .append('\n');
        }
        return sb.toString();
    }

    public String exportDeadlinesReport(List<Deadline> deadlines) {
        StringBuilder sb = new StringBuilder("id,matter,title,dueDate,responsible,completed\n");
        for (Deadline d : deadlines) {
            sb.append(d.getId()).append(',')
              .append(csv(d.getMatter().getTitle())).append(',')
              .append(csv(d.getTitle())).append(',')
              .append(d.getDueDate()).append(',')
              .append(csv(d.getResponsible() == null ? "" : d.getResponsible().getFullName())).append(',')
              .append(d.isCompleted())
              .append('\n');
        }
        return sb.toString();
    }

    public String exportDocumentInventoryReport(List<Document> documents) {
        StringBuilder sb = new StringBuilder("id,title,matter,category,privilegeLevel,visibility,versions\n");
        for (Document d : documents) {
            sb.append(d.getId()).append(',')
              .append(csv(d.getTitle())).append(',')
              .append(csv(d.getMatter().getTitle())).append(',')
              .append(d.getCategory()).append(',')
              .append(d.getPrivilegeLevel()).append(',')
              .append(d.getVisibility()).append(',')
              .append(d.getVersions().size())
              .append('\n');
        }
        return sb.toString();
    }

    private String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
