package com.example.SIA.observer;

import com.example.SIA.entity.Usuario;

public class EventoCertificadoDescargado extends Evento {

    private final Usuario instructor;
    private final String tipoCertificado;

    public EventoCertificadoDescargado(Usuario instructor, String tipoCertificado) {
        this.instructor = instructor;
        this.tipoCertificado = tipoCertificado;
    }

    public Usuario getInstructor() {
        return instructor;
    }

    public String getTipoCertificado() {
        return tipoCertificado;
    }
}