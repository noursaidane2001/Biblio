package com.biblio.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setup() {
        // Injecter les @Value manuellement
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@biblio.com");
        ReflectionTestUtils.setField(emailService, "baseUrl", "http://localhost:8080");
    }

    // ===============================
    // TEST : sendVerificationEmail
    // ===============================
    @Test
    void sendVerificationEmail_OK() {
        // WHEN
        emailService.sendVerificationEmail(
                "user@test.com",
                "Raja",
                "Test",
                "token123"
        );

        // THEN
        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("noreply@biblio.com", message.getFrom());
        assertEquals("user@test.com", message.getTo()[0]);
        assertTrue(message.getSubject().contains("Vérification"));
        assertTrue(message.getText().contains("token123"));
    }

    @Test
    void sendVerificationEmail_Exception() {
        // GIVEN
        doThrow(new RuntimeException("SMTP error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // THEN
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                emailService.sendVerificationEmail(
                        "user@test.com",
                        "Raja",
                        "Test",
                        "token123"
                )
        );

        assertTrue(ex.getMessage().contains("Impossible d'envoyer"));
    }

    // ===============================
    // TEST : sendReservationConfirmationEmail
    // ===============================
    @Test
    void sendReservationConfirmationEmail_OK() {
        emailService.sendReservationConfirmationEmail(
                "user@test.com",
                "Raja",
                "Test",
                "Clean Code",
                "2025-01-10"
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // ===============================
    // TEST : sendPretRetraitReminderEmail
    // ===============================
    @Test
    void sendPretRetraitReminderEmail_OK() {
        emailService.sendPretRetraitReminderEmail(
                "user@test.com",
                "Raja",
                "Test",
                "Clean Code",
                "Bibliothèque Centrale",
                "2025-01-12"
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // ===============================
    // TEST : sendPretRetourReminderEmail
    // ===============================
    @Test
    void sendPretRetourReminderEmail_OK() {
        emailService.sendPretRetourReminderEmail(
                "user@test.com",
                "Raja",
                "Test",
                "Clean Code",
                3,
                "2025-01-20"
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    // ===============================
    // TEST : sendReservationRejectedEmail
    // ===============================
    @Test
    void sendReservationRejectedEmail_OK() {
        emailService.sendReservationRejectedEmail(
                "user@test.com",
                "Raja",
                "Test",
                "Clean Code",
                "Bibliothèque Centrale"
        );

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
