package co.edu.unicesar.sierap.service.impl;

import co.edu.unicesar.sierap.service.ReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara una vez al día el trabajo de recordatorios (ADR-022). Por defecto a las 7:00
 * hora de Bogotá; la hora se cambia con {@code sierap.reminders.cron} y el trabajo se
 * desactiva con {@code sierap.reminders.enabled=false}.
 */
@Component
@ConditionalOnProperty(prefix = "sierap.reminders", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ReminderScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderService reminderService;

    /**
     * Crea el programador.
     *
     * @param reminderService servicio que arma y encola los recordatorios
     */
    public ReminderScheduler(ReminderService reminderService) {
        this.reminderService = reminderService;
    }

    /** Encola los recordatorios de hoy; un error se registra y se reintenta al día siguiente. */
    @Scheduled(cron = "${sierap.reminders.cron:0 0 7 * * *}", zone = "America/Bogota")
    public void run() {
        try {
            int enqueued = reminderService.enqueueDueReminders();
            LOGGER.info("Daily reminders: {} emails enqueued", enqueued);
        } catch (RuntimeException exception) {
            LOGGER.error("Daily reminders failed: {}", exception.getMessage(), exception);
        }
    }
}
