package com.example.SIA.observer;

import java.util.ArrayList;
import java.util.List;

public class SistemaEventos {

    private static final List<ObservadorEvento> observadores = new ArrayList<>();

    public static void registrarObservador(ObservadorEvento obs) {
        observadores.add(obs);
    }

    public static void emitir(Evento evento) {
        for (ObservadorEvento obs : observadores) {
            obs.notificar(evento);
        }
    }
}