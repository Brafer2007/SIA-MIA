package com.example.SIA.observer;

import java.util.Date;

public abstract class Evento {
    private final Date fecha = new Date();

    public Date getFecha() {
        return fecha;
    }
}