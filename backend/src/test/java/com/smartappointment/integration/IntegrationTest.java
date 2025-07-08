package com.smartappointment.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartappointment.repository.AppointmentRepository;
import com.smartappointment.repository.SlotRepository;
import com.smartappointment.repository.UserRepository;
import com.smartappointment.service.OtpService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = {
        "spring.kafka.topic.name=test-topic",
        "app.kafka.enabled=true"
})
@ActiveProfiles("test")
public class IntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("app.minSlotDurationMinutes", () -> "30");
        registry.add("app.maxSlotDurationMinutes", () -> "120");
        registry.add("app.maxBookingsPerDay", () -> "5");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private OtpService otpService;

    private static Long lifecycleSlotId;
    private static Long lifecycleAppointmentId;

    @BeforeEach
    void setUp() {

    }

    @Test
    @WithMockUser(username = "provider2", roles = "PROVIDER")
    void providerLifecycleActions() throws Exception {
        // Register user
        String userJson = "{" +
                "\"username\":\"testuser2\"," +
                "\"email\":\"testuser2@example.com\"," +
                "\"password\":\"password\"," +
                "\"role\":\"USER\"}";

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent. Please verify."));

        String otp = otpService.getOtpForUser("testuser2");

        mockMvc.perform(post("/user/verify")
                        .param("username", "testuser2")
                        .param("otp", otp))
                .andExpect(status().isCreated());

        // Register provider
        String providerJson = "{" +
                "\"username\":\"provider2\"," +
                "\"email\":\"provider2@example.com\"," +
                "\"password\":\"password\"," +
                "\"role\":\"PROVIDER\"," +
                "\"specialization\":\"Dentist\"}";

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(providerJson))
                .andExpect(status().isOk())
                .andExpect(content().string("OTP sent. Please verify."));

        String providerOtp = otpService.getOtpForUser("provider2");

        mockMvc.perform(post("/user/verify")
                        .param("username", "provider2")
                        .param("otp", providerOtp))
                .andExpect(status().isCreated());

        // Provider creates a slot
        String slotJson = "{" +
                "\"description\":\"Consultation\"," +
                "\"status\":\"AVAILABLE\"," +
                "\"startTime\":\"2030-01-01T11:00:00\"," +
                "\"endTime\":\"2030-01-01T11:30:00\"}";

        String slotResponse = mockMvc.perform(post("/slots/addSlot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        lifecycleSlotId = objectMapper.readTree(slotResponse).get("id").asLong();
    }

    @Test
    @WithMockUser(username = "testuser3", roles = "USER")
    void userLifecycleActions() throws Exception {
        // Register user
        String userJson = "{" +
                "\"username\":\"testuser3\"," +
                "\"email\":\"testuser3@example.com\"," +
                "\"password\":\"password\"," +
                "\"role\":\"USER\"}";

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        String otp = otpService.getOtpForUser("testuser3");
        mockMvc.perform(post("/user/verify")
                        .param("username", "testuser3")
                        .param("otp", otp))
                .andExpect(status().isCreated());
        // User books the slot
        String appointmentResponse = mockMvc.perform(post("/appointments/createAppointment/" + lifecycleSlotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(lifecycleSlotId))
                .andReturn().getResponse().getContentAsString();

        lifecycleAppointmentId = objectMapper.readTree(appointmentResponse).get("appointmentId").asLong();

        // Get appointment by ID
        mockMvc.perform(get("/appointments/getById/" + lifecycleAppointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(lifecycleAppointmentId));

        // Cancel appointment
        mockMvc.perform(delete("/appointments/cancel/" + lifecycleAppointmentId))
                .andExpect(status().isOk())
                .andExpect(content().string("Appointment deleted successfully."));

        // Confirm it's deleted
        mockMvc.perform(get("/appointments/getById/" + lifecycleAppointmentId))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void endToEndAppointmentLifecycleFlowCombined() throws Exception {
        appointmentRepository.deleteAll();
        slotRepository.deleteAll();
        userRepository.deleteAll();

        // Register USER
        String userJson = "{" +
                "\"username\":\"testuser2\"," +
                "\"email\":\"testuser2@example.com\"," +
                "\"password\":\"password\"," +
                "\"role\":\"USER\"}";

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        // Verify USER OTP
        String userOtp = otpService.getOtpForUser("testuser2");
        mockMvc.perform(post("/user/verify")
                        .param("username", "testuser2")
                        .param("otp", userOtp))
                .andExpect(status().isCreated());

        // Register PROVIDER
        String providerJson = "{" +
                "\"username\":\"provider2\"," +
                "\"email\":\"provider2@example.com\"," +
                "\"password\":\"password\"," +
                "\"role\":\"PROVIDER\"," +
                "\"specialization\":\"Dentist\"}";

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(providerJson))
                .andExpect(status().isOk());

        // Verify PROVIDER OTP
        String providerOtp = otpService.getOtpForUser("provider2");
        mockMvc.perform(post("/user/verify")
                        .param("username", "provider2")
                        .param("otp", providerOtp))
                .andExpect(status().isCreated());

        // Provider creates a slot (with security context)
        String slotJson = "{" +
                "\"description\":\"Consultation\"," +
                "\"status\":\"AVAILABLE\"," +
                "\"startTime\":\"2030-01-01T11:00:00\"," +
                "\"endTime\":\"2030-01-01T11:30:00\"}";

        String slotResponse = mockMvc.perform(post("/slots/addSlot")
                        .with(user("provider2").roles("PROVIDER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long slotId = objectMapper.readTree(slotResponse).get("id").asLong();

        // User books the slot
        String appointmentResponse = mockMvc.perform(post("/appointments/createAppointment/" + slotId)
                        .with(user("testuser2").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotId").value(slotId))
                .andReturn().getResponse().getContentAsString();

        Long appointmentId = objectMapper.readTree(appointmentResponse).get("appointmentId").asLong();

        // Get appointment by ID
        mockMvc.perform(get("/appointments/getById/" + appointmentId)
                        .with(user("testuser2").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(appointmentId));

        // Cancel appointment
        mockMvc.perform(delete("/appointments/cancel/" + appointmentId)
                        .with(user("testuser2").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string("Appointment deleted successfully."));

        // Try to get the cancelled appointment (should fail)
        mockMvc.perform(get("/appointments/getById/" + appointmentId)
                        .with(user("testuser2").roles("USER")))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(username = "provider1", roles = "PROVIDER")
    void testGetMySlotsAndAllSlotsAndSlotById() throws Exception {

        // Register provider first
        String providerJson = "{\"username\":\"provider1\",\"email\":\"provider1@example.com\",\"password\":\"password\",\"role\":\"PROVIDER\",\"specialization\":\"Dentist\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(providerJson))
                .andExpect(status().isOk());

        String otp = otpService.getOtpForUser("provider1");
        mockMvc.perform(post("/user/verify")
                        .param("username", "provider1")
                        .param("otp", otp))
                .andExpect(status().isCreated());

        // Create a slot
        String slotJson = "{\"description\":\"Consultation\",\"status\":\"AVAILABLE\",\"startTime\":\"2030-01-01T11:00:00\",\"endTime\":\"2030-01-01T11:30:00\"}";
        String slotResponse = mockMvc.perform(post("/slots/addSlot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.providerSpecialization").value("Dentist"))
                .andReturn().getResponse().getContentAsString();

        Long slotId = objectMapper.readTree(slotResponse).get("id").asLong();

        // Get slot by ID
        mockMvc.perform(get("/slots/" + slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerSpecialization").value("Dentist"));

        // Get all slots
        mockMvc.perform(get("/slots/getAllSlots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerSpecialization").value("Dentist"));

        // Get my slots
        mockMvc.perform(get("/slots/getMySlots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerSpecialization").value("Dentist"));

        // Update slot
        String updatedSlotJson = "{\"description\":\"Updated Consultation\",\"status\":\"AVAILABLE\",\"startTime\":\"2030-01-01T12:00:00\",\"endTime\":\"2030-01-01T12:30:00\"}";
        mockMvc.perform(put("/slots/updateSlot/" + slotId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatedSlotJson))
                .andExpect(status().isOk());

        // Delete slot
        mockMvc.perform(delete("/slots/" + slotId))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser4", roles = "USER")
    void testUserGetEndpointsAndSignin() throws Exception {
        // Register user
        String userJson = "{\"username\":\"testuser4\",\"email\":\"testuser4@example.com\",\"password\":\"password\",\"role\":\"USER\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        String otp = otpService.getOtpForUser("testuser4");

        mockMvc.perform(post("/user/verify")
                        .param("username", "testuser4")
                        .param("otp", otp))
                .andExpect(status().isCreated());

        // Sign in
        String signInJson = "{\"username\":\"testuser4\",\"password\":\"password\"}";
        mockMvc.perform(post("/user/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signInJson))
                .andExpect(status().isOk());

        // Get all users
        mockMvc.perform(get("/user/get-users"))
                .andExpect(status().isOk());

        // Get user by id
        Long userId = userRepository.findByUsername("testuser4").get().getId();
        mockMvc.perform(get("/user/" + userId))
                .andExpect(status().isOk());
    }

    @Test
    void testAdminEndpoints() throws Exception {
        // Register admin
        String adminJson = "{" +
                "\"username\":\"admin1\"," +
                "\"email\":\"admin1@example.com\"," +
                "\"password\":\"password\"," +
                "\"role\":\"ADMIN\"}";

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson))
                .andExpect(status().isOk());

        String adminOtp = otpService.getOtpForUser("admin1");
        mockMvc.perform(post("/user/verify")
                        .param("username", "admin1")
                        .param("otp", adminOtp))
                .andExpect(status().isCreated());

        // Register provider
        String providerJson = "{" +
                "\"username\":\"provider6\"," +
                "\"email\":\"provider6@example.com\"," +
                "\"password\":\"password\"," +
                "\"role\":\"PROVIDER\"," +
                "\"specialization\":\"Dentist\"}";

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(providerJson))
                .andExpect(status().isOk());

        String providerOtp = otpService.getOtpForUser("provider6");
        mockMvc.perform(post("/user/verify")
                        .param("username", "provider6")
                        .param("otp", providerOtp))
                .andExpect(status().isCreated());

        // Fetch provider's userId using get-users endpoint (only admin can access)
        String userListJson = mockMvc.perform(get("/user/get-users")
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Extract provider6's id
        Long providerId = null;
        var usersArray = objectMapper.readTree(userListJson);
        for (var userNode : usersArray) {
            if (userNode.get("username").asText().equals("provider6")) {
                providerId = userNode.get("id").asLong();
                break;
            }
        }
        assert providerId != null;

        // Now run admin-only endpoints as 'admin1' on this providerId
        mockMvc.perform(get("/admin/appointments/count/" + providerId)
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/appointments/getAppointmentList/" + providerId)
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/cancellations/count/" + providerId)
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/cancellations/rate/" + providerId)
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/appointments/peak-hours")
                        .with(user("admin1").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void testAppointmentEndpoints() throws Exception {
        // Register provider
        String providerJson = "{\"username\":\"provider4\",\"email\":\"provider4@example.com\",\"password\":\"password\",\"role\":\"PROVIDER\",\"specialization\":\"Dentist\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(providerJson))
                .andExpect(status().isOk());

        String providerOtp = otpService.getOtpForUser("provider4");
        mockMvc.perform(post("/user/verify")
                        .param("username", "provider4")
                        .param("otp", providerOtp))
                .andExpect(status().isCreated());

        // Register user
        String userJson = "{\"username\":\"testuser5\",\"email\":\"testuser5@example.com\",\"password\":\"password\",\"role\":\"USER\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        String userOtp = otpService.getOtpForUser("testuser5");
        mockMvc.perform(post("/user/verify")
                        .param("username", "testuser5")
                        .param("otp", userOtp))
                .andExpect(status().isCreated());

        LocalDateTime now = LocalDateTime.now().plusSeconds(2);
        LocalDateTime oneHourLater = now.plusHours(1);

        String slotJson = String.format(
                "{\"description\":\"Consultation\",\"status\":\"AVAILABLE\",\"startTime\":\"%s\",\"endTime\":\"%s\"}",
                now, oneHourLater
        );
        String slotResponse = mockMvc.perform(post("/slots/addSlot")
                        .with(user("provider4").roles("PROVIDER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long slotId = objectMapper.readTree(slotResponse).get("id").asLong();

        // Book appointment
        String appointmentResponse = mockMvc.perform(post("/appointments/createAppointment/" + slotId)
                        .with(user("testuser5").roles("USER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Long appointmentId = objectMapper.readTree(appointmentResponse).get("appointmentId").asLong();

        // Get by ID
        mockMvc.perform(get("/appointments/getById/" + appointmentId)
                        .with(user("testuser5").roles("USER")))
                .andExpect(status().isOk());

        // User appointments
        mockMvc.perform(get("/appointments/getUserAppointments")
                        .with(user("testuser5").roles("USER")))
                .andExpect(status().isOk());

        // Provider appointments
        mockMvc.perform(get("/appointments/getProviderAppointments")
                        .with(user("provider4").roles("PROVIDER")))
                .andExpect(status().isOk());

        Thread.sleep(6000); // 6 seconds
        // Complete appointment
        mockMvc.perform(put("/appointments/completeAppointment/" + appointmentId)
                        .with(user("provider4").roles("PROVIDER")))
                .andExpect(status().isOk());
    }


    @Test
    void testQueueEndpoints() throws Exception {
        // Register provider
        String providerJson = "{\"username\":\"provider5\",\"email\":\"provider5@example.com\",\"password\":\"password\",\"role\":\"PROVIDER\",\"specialization\":\"Dentist\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(providerJson))
                .andExpect(status().isOk());

        String providerOtp = otpService.getOtpForUser("provider5");
        mockMvc.perform(post("/user/verify")
                        .param("username", "provider5")
                        .param("otp", providerOtp))
                .andExpect(status().isCreated());

        // Register user
        String userJson = "{\"username\":\"testuser6\",\"email\":\"testuser6@example.com\",\"password\":\"password\",\"role\":\"USER\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isOk());

        String userOtp = otpService.getOtpForUser("testuser6");
        mockMvc.perform(post("/user/verify")
                        .param("username", "testuser6")
                        .param("otp", userOtp))
                .andExpect(status().isCreated());

        String userJson2 = "{\"username\":\"testuser7\",\"email\":\"testuser7@example.com\",\"password\":\"password\",\"role\":\"USER\"}";
        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson2))
                .andExpect(status().isOk());

        String userOtp2 = otpService.getOtpForUser("testuser7");
        mockMvc.perform(post("/user/verify")
                        .param("username", "testuser7")
                        .param("otp", userOtp2))
                .andExpect(status().isCreated());

        // Create slot with provider
        String slotJson = "{\"description\":\"Consultation\",\"status\":\"AVAILABLE\",\"startTime\":\"2030-01-01T11:00:00\",\"endTime\":\"2030-01-01T11:30:00\"}";
        String slotResponse = mockMvc.perform(post("/slots/addSlot")
                        .with(user("provider5").roles("PROVIDER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long slotId = objectMapper.readTree(slotResponse).get("id").asLong();

        mockMvc.perform(post("/appointments/createAppointment/" + slotId)
                        .with(user("testuser6").roles("USER")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        // Join queue
        mockMvc.perform(post("/queue/join/" + slotId)
                        .with(user("testuser7").roles("USER")))
                .andExpect(status().isOk());

        // View queue size
        mockMvc.perform(get("/queue/" + slotId))
                .andExpect(status().isOk());

        // Leave queue
        mockMvc.perform(post("/queue/leave/" + slotId)
                        .with(user("testuser7").roles("USER")))
                .andExpect(status().isOk());
    }


}
