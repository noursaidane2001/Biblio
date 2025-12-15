package com.biblio.jobs;

import com.biblio.services.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationExpirationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ReservationExpirationScheduler.class);
    private final ReservationService reservationService;

    public ReservationExpirationScheduler(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // Vérifie les expirations toutes les heures par défaut (configurable)
    @Scheduled(fixedDelayString = "${app.reservations.expiration-check-ms:3600000}")
    public void runExpirationCheck() {
        int expired = reservationService.expirerReservations();
        if (expired > 0) {
            logger.info("Réservations expirées traitées: {}", expired);
        }
    }
}
