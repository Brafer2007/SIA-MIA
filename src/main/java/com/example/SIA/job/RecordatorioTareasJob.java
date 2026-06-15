package com.example.SIA.job;

import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.Tarea;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.TareaRepository;
import com.example.SIA.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Job que se ejecuta cada hora y envía recordatorios de tareas
 * que vencen en las próximas 24 horas.
 */
@Component
public class RecordatorioTareasJob {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioTareasJob.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final TareaRepository tareaRepository;
    private final AprendizRepository aprendizRepository;
    private final EmailService emailService;

    public RecordatorioTareasJob(TareaRepository tareaRepository,
                                  AprendizRepository aprendizRepository,
                                  EmailService emailService) {
        this.tareaRepository   = tareaRepository;
        this.aprendizRepository = aprendizRepository;
        this.emailService       = emailService;
    }

    /** Ejecutar cada hora en punto */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void enviarRecordatorios() {
        LocalDateTime ahora  = LocalDateTime.now();
        LocalDateTime en24h  = ahora.plusHours(24);

        List<Tarea> tareas = tareaRepository.findTareasProximasAVencer(ahora, en24h);
        if (tareas.isEmpty()) return;

        log.info("[RecordatorioJob] Procesando {} tarea(s) próximas a vencer", tareas.size());

        for (Tarea tarea : tareas) {
            try {
                List<Aprendiz> aprendices = aprendizRepository.findByFichaContainedIn(tarea.getNombreFicha());
                String fechaStr = tarea.getFechaLimite().format(FMT);
                String asunto   = "⏰ Recordatorio: tu tarea vence pronto — " + tarea.getTitulo();

                for (Aprendiz a : aprendices) {
                    if (a.getUsuario() == null || a.getUsuario().getCorreo() == null
                            || a.getUsuario().getCorreo().isBlank()) continue;

                    String nombre = (a.getUsuario().getNombres() + " " + a.getUsuario().getApellidos()).trim();
                    String cuerpo = "Hola " + nombre + ",\n\n"
                            + "Te recordamos que la tarea \"" + tarea.getTitulo() + "\""
                            + " vence el " + fechaStr + ".\n\n"
                            + "Instructor: " + tarea.getInstructor().getNombreCompleto() + "\n"
                            + "Ficha: " + tarea.getNombreFicha() + "\n\n"
                            + "Ingresa al sistema SIA antes de que venza el plazo.\n\n"
                            + "— Sistema SIA (mensaje automático)";

                    emailService.enviarCorreoIndividual(
                            a.getUsuario().getCorreo(), asunto, cuerpo,
                            tarea.getInstructor().getUsuario() != null
                                ? tarea.getInstructor().getUsuario().getCorreo() : null);
                }

                // Marcar como enviado para no volver a notificar
                tarea.setRecordatorioEnviado(true);
                tareaRepository.save(tarea);
                log.info("[RecordatorioJob] Recordatorio enviado para tarea id={} ficha={}",
                        tarea.getId(), tarea.getNombreFicha());

            } catch (Exception e) {
                log.error("[RecordatorioJob] Error procesando tarea id={}: {}", tarea.getId(), e.getMessage());
            }
        }
    }
}
