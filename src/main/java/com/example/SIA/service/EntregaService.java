package com.example.SIA.service;

import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.dto.TareaAprendizDTO;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.EntregaTarea;
import com.example.SIA.entity.Tarea;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.EntregaTareaRepository;
import com.example.SIA.repository.TareaRepository;
import com.example.SIA.websocket.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class EntregaService {

    private final TareaRepository tareaRepository;
    private final EntregaTareaRepository entregaTareaRepository;
    private final AprendizRepository aprendizRepository;
    private final ArchivoService archivoService;

    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;

    public EntregaService(TareaRepository tareaRepository,
                          EntregaTareaRepository entregaTareaRepository,
                          AprendizRepository aprendizRepository,
                          ArchivoService archivoService) {
        this.tareaRepository = tareaRepository;
        this.entregaTareaRepository = entregaTareaRepository;
        this.aprendizRepository = aprendizRepository;
        this.archivoService = archivoService;
    }

    /**
     * Lista las tareas de una ficha con el estado de entrega calculado para el aprendiz dado.
     * Usa búsqueda por contenido para cubrir fichas con rangos (ej: "2996893 - 2996900").
     */
    public List<TareaAprendizDTO> listarTareasParaAprendiz(String fichaFormacion, Integer idAprendiz) {
        List<Tarea> tareas = tareaRepository.findByNombreFichaContaining(fichaFormacion);
        List<TareaAprendizDTO> resultado = new ArrayList<>();

        for (Tarea tarea : tareas) {
            Optional<EntregaTarea> entregaOpt =
                    entregaTareaRepository.findByTarea_IdAndAprendiz_IdAprendiz(tarea.getId(), idAprendiz);

            String estado;
            Double nota = null;
            String comentario = null;

            if (entregaOpt.isPresent()) {
                EntregaTarea entrega = entregaOpt.get();
                nota = entrega.getNota();
                comentario = entrega.getComentarioInstructor();
                if (nota != null) {
                    estado = "CALIFICADA";
                } else {
                    estado = "ENTREGADA";
                }
            } else {
                if (LocalDateTime.now().isBefore(tarea.getFechaLimite())) {
                    estado = "PENDIENTE";
                } else {
                    estado = "VENCIDA";
                }
            }

            TareaAprendizDTO dto = new TareaAprendizDTO();
            dto.setIdTarea(tarea.getId());
            dto.setTitulo(tarea.getTitulo());
            dto.setDescripcion(tarea.getDescripcion());
            dto.setNombreInstructor(tarea.getInstructor().getNombreCompleto());
            dto.setFechaLimite(tarea.getFechaLimite());
            dto.setEstadoEntrega(estado);
            dto.setNota(nota);
            dto.setComentarioInstructor(comentario);
            dto.setTieneArchivoTarea(tarea.getRutaArchivo() != null);
            dto.setRutaArchivoTarea(tarea.getRutaArchivo());

            resultado.add(dto);
        }

        return resultado;
    }

    /**
     * Procesa la entrega de un aprendiz para una tarea.
     * Reemplaza la entrega anterior si existe.
     */
    @Transactional
    public EntregaTarea entregar(Long idTarea, Integer idAprendiz, MultipartFile archivo) {
        Tarea tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + idTarea));

        if (!LocalDateTime.now().isBefore(tarea.getFechaLimite())) {
            throw new IllegalArgumentException("El plazo de entrega ha vencido");
        }

        archivoService.validarArchivo(archivo, 20 * 1024 * 1024L);

        Optional<EntregaTarea> entregaExistenteOpt =
                entregaTareaRepository.findByTarea_IdAndAprendiz_IdAprendiz(idTarea, idAprendiz);

        String nuevaRuta;
        try {
            nuevaRuta = archivoService.guardarArchivoEntrega(idTarea, idAprendiz, archivo);
        } catch (Exception e) {
            throw new RuntimeException("Error al guardar el archivo de entrega", e);
        }

        if (entregaExistenteOpt.isPresent()) {
            EntregaTarea entregaExistente = entregaExistenteOpt.get();
            String rutaAnterior = entregaExistente.getRutaArchivo();
            archivoService.eliminarArchivo(rutaAnterior);
            entregaExistente.setRutaArchivo(nuevaRuta);
            entregaExistente.setFechaEntrega(LocalDateTime.now());
            entregaExistente.setNota(null);
            entregaExistente.setComentarioInstructor(null);
            entregaExistente.setFechaCalificacion(null);
            try {
                EntregaTarea guardada = entregaTareaRepository.save(entregaExistente);
                notificarInstructorEntrega(tarea, aprendizRepository.findById(idAprendiz).orElse(null));
                return guardada;
            } catch (Exception e) {
                archivoService.eliminarArchivo(nuevaRuta);
                throw new RuntimeException("Error al persistir la entrega en base de datos: " + e.getMessage(), e);
            }
        } else {
            Aprendiz aprendiz = aprendizRepository.findById(idAprendiz)
                    .orElseThrow(() -> new IllegalArgumentException("Aprendiz no encontrado con id: " + idAprendiz));

            EntregaTarea nueva = new EntregaTarea();
            nueva.setTarea(tarea);
            nueva.setAprendiz(aprendiz);
            nueva.setRutaArchivo(nuevaRuta);
            nueva.setFechaEntrega(LocalDateTime.now());
            try {
                EntregaTarea guardada = entregaTareaRepository.save(nueva);
                notificarInstructorEntrega(tarea, aprendiz);
                return guardada;
            } catch (Exception e) {
                archivoService.eliminarArchivo(nuevaRuta);
                throw new RuntimeException("Error al persistir la entrega en base de datos: " + e.getMessage(), e);
            }
        }
    }

    /** Notifica al instructor cuando un aprendiz entrega una tarea */
    private void notificarInstructorEntrega(Tarea tarea, Aprendiz aprendiz) {
        try {
            if (tarea.getInstructor() == null || aprendiz == null) return;
            String idInstructor = String.valueOf(tarea.getInstructor().getId());
            String nombreAprendiz = aprendiz.getUsuario() != null
                    ? (aprendiz.getUsuario().getNombres() + " " + aprendiz.getUsuario().getApellidos()).trim()
                    : "Un aprendiz";
            NotificacionDTO dto = new NotificacionDTO(
                    "tarea_entregada",
                    "📋 Nueva entrega de tarea",
                    nombreAprendiz + " entregó: " + tarea.getTitulo(),
                    nombreAprendiz,
                    tarea.getNombreFicha()
            );
            dto.setSonar(true);
            notificationWebSocketHandler.notificarInstructor(idInstructor, dto);
        } catch (Exception e) {
            // No interrumpir el flujo principal si falla la notificación
        }
    }
}
