package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.*;
import com.lawfirm.lcm.dto.MatterForm;
import com.lawfirm.lcm.repository.*;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.MatterAccessService;
import com.lawfirm.lcm.service.MatterAssistantService;
import com.lawfirm.lcm.service.MatterService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/matters")
@RequiredArgsConstructor
public class MatterController {

    private final MatterRepository matterRepository;
    private final ClientRepository clientRepository;
    private final PracticeAreaRepository practiceAreaRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final TaskRepository taskRepository;
    private final DeadlineRepository deadlineRepository;
    private final NoteRepository noteRepository;
    private final ClientMessageRepository clientMessageRepository;
    private final AuditLogRepository auditLogRepository;
    private final MatterAccessService matterAccessService;
    private final MatterService matterService;
    private final MatterAssistantService matterAssistantService;
    private final CurrentUser currentUser;

    @GetMapping
    public String list(@RequestParam(required = false) Matter.Status status,
                        @RequestParam(required = false) Long practiceAreaId,
                        @RequestParam(required = false) Long attorneyId,
                        @RequestParam(required = false) Long clientId,
                        @RequestParam(required = false) MatterService.SortColumn sort,
                        Model model) {
        User user = currentUser.get();
        List<Matter> matters;
        if (user.getRole() == User.Role.CLIENT) {
            Client client = clientRepository.findByPortalUser_Id(user.getId()).orElseThrow();
            matters = matterService.search(status, practiceAreaId, attorneyId, client.getId(), sort);
        } else if (user.getRole() == User.Role.ATTORNEY || user.getRole() == User.Role.PARALEGAL) {
            matters = matterService.search(status, practiceAreaId, user.getId(), clientId, sort);
        } else {
            matters = matterService.search(status, practiceAreaId, attorneyId, clientId, sort);
        }
        model.addAttribute("matters", matters);
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("practiceAreas", practiceAreaRepository.findAll());
        model.addAttribute("attorneys", userRepository.findAll().stream().filter(u -> u.getRole() == User.Role.ATTORNEY).toList());
        model.addAttribute("statuses", Matter.Status.values());
        return "matter/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("matterForm", new MatterForm());
        model.addAttribute("clients", clientRepository.findAll());
        model.addAttribute("practiceAreas", practiceAreaRepository.findAll());
        model.addAttribute("attorneys", userRepository.findAll().stream().filter(u -> u.getRole() == User.Role.ATTORNEY).toList());
        return "matter/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute MatterForm matterForm, BindingResult bindingResult, Model model) {
        User user = currentUser.get();
        if (user.getRole() == User.Role.CLIENT) {
            throw new AccessDeniedException("Clients cannot create matters");
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("clients", clientRepository.findAll());
            model.addAttribute("practiceAreas", practiceAreaRepository.findAll());
            model.addAttribute("attorneys", userRepository.findAll().stream().filter(u -> u.getRole() == User.Role.ATTORNEY).toList());
            return "matter/form";
        }
        Matter matter = new Matter();
        matter.setTitle(matterForm.getTitle());
        matter.setDescription(matterForm.getDescription());
        matter.setClient(clientRepository.findById(matterForm.getClientId()).orElseThrow());
        if (matterForm.getPracticeAreaId() != null) {
            matter.setPracticeArea(practiceAreaRepository.findById(matterForm.getPracticeAreaId()).orElse(null));
        }
        if (matterForm.getAssignedAttorneyId() != null) {
            userRepository.findById(matterForm.getAssignedAttorneyId())
                    .filter(u -> u.getRole() == User.Role.ATTORNEY)
                    .ifPresent(matter::setAssignedAttorney);
        }
        matter.setSensitivityLevel(Matter.Sensitivity.valueOf(matterForm.getSensitivityLevel()));
        matter = matterService.create(matter);
        return "redirect:/matters/" + matter.getId();
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, @RequestParam(defaultValue = "overview") String tab,
                       @RequestParam(required = false) String ask, Model model) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(id, user);
        model.addAttribute("matter", matter);
        model.addAttribute("tab", tab);
        model.addAttribute("isStaff", matterAccessService.isStaff(user));
        model.addAttribute("isFirmAdmin", user.getRole() == User.Role.FIRM_ADMIN);
        model.addAttribute("documents", documentRepository.findByMatter_IdOrderByCreatedAtDesc(id).stream()
                .filter(d -> matterAccessService.isStaff(user) || d.getVisibility() == Document.Visibility.CLIENT_SHARED || d.getVisibility() == Document.Visibility.PUBLIC_FILING)
                .toList());
        model.addAttribute("tasks", taskRepository.findByMatter_IdOrderByDueDateAsc(id));
        model.addAttribute("deadlines", deadlineRepository.findByMatter_IdOrderByDueDateAsc(id));
        if (matterAccessService.isStaff(user)) {
            model.addAttribute("notes", noteRepository.findByMatter_IdOrderByCreatedAtDesc(id));
            model.addAttribute("auditHistory", auditLogRepository.findByEntityTypeAndEntityIdOrderByTimestampDesc("Matter", id));
        }
        List<ClientMessage> messages = clientMessageRepository.findByMatter_IdOrderByCreatedAtAsc(id);
        if (user.getRole() == User.Role.CLIENT) {
            messages = messages.stream().filter(m -> m.getStatus() != ClientMessage.Status.DRAFT).toList();
        }
        model.addAttribute("messages", messages);
        model.addAttribute("staffUsers", userRepository.findAll().stream()
                .filter(u -> u.getRole() != User.Role.CLIENT).toList());
        if (ask != null && !ask.isBlank()) {
            try {
                model.addAttribute("assistantAnswer", matterAssistantService.answer(matter, user, ask));
            } catch (Exception e) {
                model.addAttribute("assistantProblem", e.getMessage());
            }
        }
        return "matter/detail";
    }

    @PostMapping("/{id}/transition")
    public String transition(@PathVariable Long id, @RequestParam Matter.Status target, Model model) {
        User user = currentUser.get();
        Matter matter = matterAccessService.assertCanAccess(id, user);
        matterService.transition(matter, target, user);
        return "redirect:/matters/" + id;
    }

    // Printable card the front desk hands to a courier as a court case reference.
    @GetMapping(value = "/case-ref/{caseRef}/card", produces = MediaType.TEXT_HTML_VALUE)
    public void caseRefCard(@PathVariable String caseRef, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=UTF-8");
        resp.getWriter().write("<div class=\"case-ref-card\"><h3>Case reference: " + caseRef + "</h3>"
                + "<p>Present this card at the court filing window.</p></div>");
    }

    // Case-number search over the firm's docket index. Staff see everything; a client's
    // search is scoped to their own matters by the predicate below.
    @GetMapping("/docket/search/{caseRef}")
    @ResponseBody
    public List<String> docketSearch(@PathVariable String caseRef) throws Exception {
        User user = currentUser.get();
        String scope = "";
        if (user.getRole() == User.Role.CLIENT) {
            scope = clientRepository.findByPortalUser_Id(user.getId()).map(Client::getName).orElse("");
        }
        org.w3c.dom.Document docketDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element root = docketDoc.createElement("docket");
        docketDoc.appendChild(root);
        for (Matter m : matterRepository.findAll()) {
            Element entry = docketDoc.createElement("matter");
            entry.setAttribute("client", m.getClient().getName());
            Element ref = docketDoc.createElement("ref");
            ref.setTextContent(String.valueOf(m.getId()));
            entry.appendChild(ref);
            Element title = docketDoc.createElement("title");
            title.setTextContent(m.getTitle());
            entry.appendChild(title);
            root.appendChild(entry);
        }
        String expr = scope.isBlank()
                ? "//matter[ref='" + caseRef + "']/title/text()"
                : "//matter[client='" + scope + "' and ref='" + caseRef + "']/title/text()";
        XPath xp = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xp.evaluate(expr, docketDoc, XPathConstants.NODESET);
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            titles.add(nodes.item(i).getNodeValue());
        }
        return titles;
    }
}
