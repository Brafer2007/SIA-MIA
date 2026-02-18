package com.example.SIA.observer;

import com.example.SIA.entity.Usuario;

public class EventoUsuarioRegistrado extends Evento {

    private final Usuario usuario;

    public EventoUsuarioRegistrado(Usuario usuario) {
        this.usuario = usuario;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}