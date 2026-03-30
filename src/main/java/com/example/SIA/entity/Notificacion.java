package com.example.SIA.entity;

import jakarta.persistence.*;
import java.util.Date;

@Entity
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idNotificacion;

    private String mensaje;

    private String tipo;       // ej: usuario_registro
    private String categoria;  // ej: usuarios, instructores, equipos
    private String prioridad;  // baja, media, alta, critica

    @Temporal(TemporalType.TIMESTAMP)
    private Date fecha = new Date();

    private boolean leida = false;

    public Integer getIdNotificacion() { return idNotificacion; }
    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public Date getFecha() { return fecha; }

    public boolean isLeida() { return leida; }
    public void setLeida(boolean leida) { this.leida = leida; }
}