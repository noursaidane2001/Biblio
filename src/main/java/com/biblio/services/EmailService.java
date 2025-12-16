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

    public void sendReservationConfirmationEmail(String toEmail, String nom, String prenom, String titreRessource, String deadlineRetraitDisplay) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Votre livre est prêt à être emprunté - Biblio");
            String body = String.format(
                "Bonjour %s %s,\n\n" +
                "Bonne nouvelle ! Votre réservation pour \"%s\" a été confirmée.\n\n" +
                "Le livre est prêt à être retiré à la bibliothèque.\n" +
                "Date limite de retrait: %s\n\n" +
                "Passé ce délai, la réservation pourra être annulée.\n\n" +
                "Cordialement,\n" +
                "Votre bibliothèque",
                prenom, nom, titreRessource != null ? titreRessource : "Ressource", 
                deadlineRetraitDisplay != null ? deadlineRetraitDisplay : "bientôt"
            );
            message.setText(body);
            mailSender.send(message);
            logger.info("Email de confirmation de réservation envoyé à : {}", toEmail);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de confirmation à {} : {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de confirmation de réservation", e);
        }
    }

    public void sendPretRetraitReminderEmail(String toEmail, String nom, String prenom, String titreRessource, String bibliothequeNom, String deadlineRetraitDisplay) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Rappel de retrait de votre livre - Biblio");
            String body = String.format(
                "Bonjour %s %s,\n\n" +
                "Ceci est un rappel pour retirer votre livre \"%s\" à la bibliothèque%s.\n" +
                "%s\n\n" +
                "Si vous ne pouvez pas vous déplacer, vous pouvez annuler la réservation pour libérer l'exemplaire.\n\n" +
                "Cordialement,\n" +
                "Votre bibliothèque",
                prenom, nom,
                titreRessource != null ? titreRessource : "Ressource",
                bibliothequeNom != null ? " \"" + bibliothequeNom + "\"" : "",
                (deadlineRetraitDisplay != null && !deadlineRetraitDisplay.isBlank())
                        ? "Date limite de retrait: " + deadlineRetraitDisplay
                        : "La date limite de retrait sera bientôt atteinte."
            );
            message.setText(body);
            mailSender.send(message);
            logger.info("Email de rappel de retrait envoyé à : {}", toEmail);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de rappel à {} : {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de rappel de retrait", e);
        }
    }

    public void sendPretRetourReminderEmail(String toEmail, String nom, String prenom, String titreRessource, long joursRestants, String dateRetourPrevuDisplay) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("Rappel de retour de votre livre - Biblio");
            String body = String.format(
                "Bonjour %s %s,\n\n" +
                "Vous avez emprunté \"%s\".\n" +
                "Il vous reste %d jour(s) avant la date de retour prévue: %s.\n\n" +
                "Merci de retourner le livre à temps pour éviter le blocage de votre prêt.\n\n" +
                "Cordialement,\n" +
                "Votre bibliothèque",
                prenom, nom,
                titreRessource != null ? titreRessource : "Ressource",
                Math.max(joursRestants, 0),
                dateRetourPrevuDisplay != null ? dateRetourPrevuDisplay : "bientôt"
            );
            message.setText(body);
            mailSender.send(message);
            logger.info("Email de rappel de retour envoyé à : {}", toEmail);
        } catch (Exception e) {
            logger.error("Erreur lors de l'envoi de l'email de rappel de retour à {} : {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Impossible d'envoyer l'email de rappel de retour", e);
        }
    }
}
