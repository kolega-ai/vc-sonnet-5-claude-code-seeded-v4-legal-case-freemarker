package com.lawfirm.lcm;

import com.lawfirm.lcm.domain.User;
import com.lawfirm.lcm.repository.UserRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebSecurityBoundaryTests {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeAll
    void setUp() {
        if (userRepository.findByEmailIgnoreCase("paralegal-web-test@lawfirm.test").isEmpty()) {
            User u = new User();
            u.setEmail("paralegal-web-test@lawfirm.test");
            u.setFullName("Web Test Paralegal");
            u.setRole(User.Role.PARALEGAL);
            u.setPasswordHash(passwordEncoder.encode("Passw0rd!"));
            userRepository.save(u);
        }
    }

    @Test
    @WithUserDetails(value = "paralegal-web-test@lawfirm.test", userDetailsServiceBeanName = "appUserDetailsService")
    void paralegalCannotAccessRetentionSettings() throws Exception {
        mockMvc.perform(get("/admin/retention"))
                .andExpect(status().isForbidden());
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection());
    }
}
