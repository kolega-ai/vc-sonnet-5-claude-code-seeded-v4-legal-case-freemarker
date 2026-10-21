package com.lawfirm.lcm.controller;

import com.lawfirm.lcm.domain.Client;
import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.dto.ClientForm;
import com.lawfirm.lcm.repository.ClientRepository;
import com.lawfirm.lcm.repository.UserRepository;
import com.lawfirm.lcm.security.CurrentUser;
import com.lawfirm.lcm.service.OutboxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    @GetMapping
    public String list(Model model) {
        staffOnly();
        model.addAttribute("clients", clientRepository.findAll());
        return "matter/clients";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        staffOnly();
        model.addAttribute("clientForm", new ClientForm());
        return "matter/client-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute ClientForm clientForm, BindingResult bindingResult, Model model) {
        staffOnly();
        if (bindingResult.hasErrors()) {
            return "matter/client-form";
        }
        Client client = new Client();
        client.setName(clientForm.getName());
        client.setType(Client.ClientType.valueOf(clientForm.getType()));
        client.setNationalId(clientForm.getNationalId());
        client.setEmail(clientForm.getEmail());
        client.setPhone(clientForm.getPhone());
        client.setAddress(clientForm.getAddress());
        client.setConflictCheckNotes(clientForm.getConflictCheckNotes());
        clientRepository.save(client);
        return "redirect:/clients";
    }

    // Gives an existing client record access to the client portal: creates the portal
    // account if it does not exist yet and mails the activation link.
    @PostMapping("/{id}/portal-invite")
    public String invitePortal(@PathVariable Long id) {
        staffOnly();
        Client client = clientRepository.findById(id).orElseThrow();
        if (client.getPortalUser() == null) {
            User portalUser = new User();
            portalUser.setEmail(client.getEmail());
            portalUser.setFullName(client.getName());
            portalUser.setRole(User.Role.CLIENT);
            portalUser.setPasswordHash(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
            client.setPortalUser(userRepository.save(portalUser));
            clientRepository.save(client);
        }
        outboxService.send(client.getEmail(), "Your client portal invitation",
                "Activate your portal access here: /portal-invite?e=" + client.getEmail(), "PORTAL_INVITE");
        return "redirect:/clients";
    }

    private void staffOnly() {
        User user = currentUser.get();
        if (user.getRole() == User.Role.CLIENT) {
            throw new AccessDeniedException("Clients cannot manage client records");
        }
    }
}
