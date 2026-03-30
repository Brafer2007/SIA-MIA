package com.example.SIA.dto;

import com.example.SIA.entity.SolicitudAmbiente;
import java.time.LocalDateTime;

public class SolicitudAmbienteDTO {

    private Long id;
    private String aula;
    private String tipo;
    private String estado;
    private LocalDateTime fechaSolicitud;
    private String nombreInstructor;
    private Integer idInstructor;

    public SolicitudAmbienteDTO() {}

    public static SolicitudAmbienteDTO from(SolicitudAmbiente s) {
        SolicitudAmbienteDTO dto = new SolicitudAmbienteDTO();
        dto.id = s.getId();
        dto.aula = s.getAula();
        dto.tipo = s.getTipo();
        dto.estado = s.getEstado();
        dto.fechaSolicitud = s.getFechaSolicitud();
        if (s.getInstructor() != null) {
            dto.idInstructor = s.getInstructor().getId();
            if (s.getInstructor().getUsuario() != null) {
                dto.nombreInstructor = s.getInstructor().getUsuario().getNombres()
                        + " " + s.getInstructor().getUsuario().getApellidos();
            } else {
                dto.nombreInstructor = "Instructor #" + s.getInstructor().getId();
            }
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getAula() { return aula; }
    public String getTipo() { return tipo; }
    public String getEstado() { return estado; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public String getNombreInstructor() { return nombreInstructor; }
    public Integer getIdInstructor() { return idInstructor; }
}
