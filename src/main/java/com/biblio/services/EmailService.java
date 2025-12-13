package com.biblio.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service pour l'envoi d'emails.
 * Gère l'envoi d'emails de vérification et autres notifications.
 */
@Service
public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username}")
    private String fromEmail;
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }
    
    /**
     * Envoie un email de vérification à l'utilisateur.
     * 
     * @param toEmail L'email du destinataire
     * @param nom Le nom de l'utilisateur
     * @param prenom Le prénom de l'utilisateur
     * @param token Le token de vérification
     */
    public void sendVerificationEmail(String toEmail, String nom, String prenom, String token) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Vérification de votre adresse email - Biblio");
            
            String verificationUrl = baseUrl + "/register/verify?token=" + token;
            String body = String.format(
                "Bonjour %s %s,\n\n" +
                "Merci de vous être inscrit sur Biblio !\n\n" +
                "Pour activer votre compte, veuillez cliquer sur le lien suivant :\n" +
                "%s\n\n" +
                "Ce lien est valide pendant 24 heures.\n\n" +
                "Si vous n'avez pas créé de compte, veuillez ignorer cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe Biblio",
                prenom, nom, verificationUrl
            );
            
            message.setText(body);
            mailSender.send(message);
            logger.info("Email de vérification envoyé à : {}", toEmail);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de vérification à {} : {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de vérification", e);
        }
    }
}

