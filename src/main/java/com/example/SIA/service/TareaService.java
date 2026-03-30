package com.example.SIA.service;

import com.example.SIA.dto.EntregaResumenDTO;
import com.example.SIA.dto.NotificacionDTO;
import com.example.SIA.dto.TareaRequest;
import com.example.SIA.entity.Aprendiz;
import com.example.SIA.entity.EntregaTarea;
import com.example.SIA.entity.Instructor;
import com.example.SIA.entity.Notificacion;
import com.example.SIA.entity.Tarea;
import com.example.SIA.repository.AprendizRepository;
import com.example.SIA.repository.EntregaTareaRepository;
import com.example.SIA.repository.InstructorRepository;
import com.example.SIA.repository.NotificacionRepository;
import com.example.SIA.repository.ProgramacionRepository;
import com.example.SIA.repository.TareaRepository;
import com.example.SIA.websocket.NotificationWebSocketHandler;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TareaService {

    private final TareaRepository tareaRepository;
    private final InstructorRepository instructorRepository;
    private final ProgramacionRepository programacionRepository;
    private final AprendizRepository aprendizRepository;
    private final EntregaTareaRepository entregaTareaRepository;
    private final NotificacionRepository notificacionRepository;
    private final ArchivoService archivoService;

    @Autowired
    private NotificationWebSocketHandler notificationWebSocketHandler;

    public TareaService(TareaRepository tareaRepository,
                        InstructorRepository instructorRepository,
                        ProgramacionRepository programacionRepository,
                        AprendizRepository aprendizRepository,
                        EntregaTareaRepository entregaTareaRepository,
                        NotificacionRepository notificacionRepository,
                        ArchivoService archivoService) {
        this.tareaRepository = tareaRepository;
        this.instructorRepository = instructorRepository;
        this.programacionRepository = programacionRepository;
        this.aprendizRepository = aprendizRepository;
        this.entregaTareaRepository = entregaTareaRepository;
        this.notificacionRepository = notificacionRepository;
        this.archivoService = archivoService;
    }

    /**
     * Retorna las fichas asociadas al instructor autenticado.
     */
    public List<String> obtenerFichasDeInstructor(Integer idInstructor) {
        return programacionRepository.findFichasByInstructorId(idInstructor);
    }

    /**
     * Lista todas las tareas creadas por el instructor.
     */
    public List<Tarea> listarPorInstructor(Integer idInstructor) {
        return tareaRepository.findByInstructor_Id(idInstructor);
    }

    /**
     * Crea una nueva tarea, guarda el archivo adjunto si existe y notifica a los aprendices.
     */
    @Transactional
    public Tarea crearTarea(TareaRequest request, Integer idInstructor, MultipartFile archivo) {
        // Validaciones
        if (request.getTitulo() == null || request.getTitulo().isBlank()) {
            throw new IllegalArgumentException("El título es obligatorio");
        }
        if (request.getFechaLimite() == null) {
            throw new IllegalArgumentException("La fecha límite es obligatoria");
        }
        if (request.getNombreFicha() == null || request.getNombreFicha().isBlank()) {
            throw new IllegalArgumentException("La ficha es obligatoria");
        }

        Instructor instructor = instructorRepository.findById(idInstructor)
                .orElseThrow(() -> new IllegalArgumentException("Instructor no encontrado con id: " + idInstructor));

        // Construir y persistir la tarea
        Tarea tarea = new Tarea();
        tarea.setTitulo(request.getTitulo());
        tarea.setDescripcion(request.getDescripcion());
        tarea.setFechaLimite(request.getFechaLimite());
        tarea.setNombreFicha(request.getNombreFicha());
        tarea.setInstructor(instructor);

        tarea = tareaRepository.save(tarea);

        // Guardar archivo adjunto si existe
        if (archivo != null && !archivo.isEmpty()) {
            archivoService.validarArchivo(archivo, 10 * 1024 * 1024L);
            String rutaArchivo = null;
            try {
                rutaArchivo = archivoService.guardarArchivoTarea(tarea.getId(), archivo);
            } catch (IOException | RuntimeException e) {
                archivoService.eliminarArchivo(rutaArchivo);
                throw new RuntimeException("Error al guardar el archivo adjunto: " + e.getMessage(), e);
            }
            tarea.setRutaArchivo(rutaArchivo);
            final String rutaArchivoFinal = rutaArchivo;
            try {
                tarea = tareaRepository.save(tarea);
            } catch (Exception e) {
                archivoService.eliminarArchivo(rutaArchivoFinal);
                throw new RuntimeException("Error al persistir la tarea con el archivo adjunto: " + e.getMessage(), e);
            }
        }

        // Notificar a los aprendices de la ficha
        String ficha = tarea.getNombreFicha();
        List<Aprendiz> aprendices = aprendizRepository.findByFichaContainedIn(ficha);

        for (int i = 0; i < aprendices.size(); i++) {
            Notificacion notificacion = new Notificacion();
            notificacion.setMensaje("Nueva tarea: " + tarea.getTitulo());
            notificacion.setTipo("nueva_tarea");
            notificacion.setCategoria("tareas");
            notificacion.setPrioridad("media");
            notificacionRepository.save(notificacion);
        }

        // Notificar vía WebSocket
        NotificacionDTO notificacionDTO = new NotificacionDTO(
                "nueva_tarea",
                "Nueva tarea",
                "Nueva tarea: " + tarea.getTitulo(),
                instructor.getNombreCompleto(),
                ficha
        );
        notificationWebSocketHandler.notificarAprendicesDeFicha(ficha, notificacionDTO);

        return tarea;
    }

    /**
     * Lista las entregas de una tarea, verificando que pertenece al instructor.
     */
    @Transactional
    public List<EntregaResumenDTO> listarEntregasDeTarea(Long idTarea, Integer idInstructor) {
        Tarea tarea = tareaRepository.findById(idTarea)
                .orElseThrow(() -> new IllegalArgumentException("Tarea no encontrada con id: " + idTarea));

        if (!tarea.getInstructor().getId().equals(idInstructor)) {
            throw new IllegalArgumentException("La tarea no pertenece al instructor indicado");
        }

        String ficha = tarea.getNombreFicha();
        // Busca aprendices cuya fichaFormacion esté contenida en el nombreFicha de la tarea
        // (cubre rangos como "2996893 - 2996900" donde el aprendiz tiene "2996893")
        List<Aprendiz> aprendices = aprendizRepository.findByFichaContainedIn(ficha);

        List<EntregaResumenDTO> resultado = new ArrayList<>();
        for (Aprendiz aprendiz : aprendices) {
            EntregaResumenDTO dto = new EntregaResumenDTO();
            dto.setIdAprendiz(aprendiz.getIdAprendiz());

            String nombre = "";
            if (aprendiz.getUsuario() != null) {
                String nombres = aprendiz.getUsuario().getNombres() != null ? aprendiz.getUsuario().getNombres() : "";
                String apellidos = aprendiz.getUsuario().getApellidos() != null ? aprendiz.getUsuario().getApellidos() : "";
                nombre = (nombres + " " + apellidos).trim();
            }
            dto.setNombreAprendiz(nombre);

            Optional<EntregaTarea> entregaOpt = entregaTareaRepository
                    .findByTarea_IdAndAprendiz_IdAprendiz(idTarea, aprendiz.getIdAprendiz());

            if (entregaOpt.isPresent()) {
                EntregaTarea entrega = entregaOpt.get();
                dto.setEstadoEntrega("ENTREGADO");
                dto.setFechaEntrega(entrega.getFechaEntrega());
                dto.setIdEntrega(entrega.getId());
                dto.setRutaArchivo(entrega.getRutaArchivo());
                dto.setNota(entrega.getNota());
                dto.setComentario(entrega.getComentarioInstructor());
            } else {
                dto.setEstadoEntrega("PENDIENTE");
            }

            resultado.add(dto);
        }

        return resultado;
    }

    /**
     * Califica una entrega de tarea.
     */
    @Transactional
    public void calificar(Long idEntrega, Double nota, String comentario, Integer idInstructor) {
        EntregaTarea entrega = entregaTareaRepository.findById(idEntrega)
                .orElseThrow(() -> new IllegalArgumentException("Entrega no encontrada con id: " + idEntrega));

        if (!entrega.getTarea().getInstructor().getId().equals(idInstructor)) {
            throw new IllegalArgumentException("La tarea no pertenece al instructor indicado");
        }

        if (nota == null || nota < 0.0 || nota > 5.0) {
            throw new IllegalArgumentException("La nota debe estar entre 0.0 y 5.0");
        }

        entrega.setNota(nota);
        entrega.setComentarioInstructor(comentario);
        entrega.setFechaCalificacion(LocalDateTime.now());
        entregaTareaRepository.save(entrega);
    }
}
