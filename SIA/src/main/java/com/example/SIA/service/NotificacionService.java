package com.example.SIA.service;

import com.example.SIA.entity.Notificacion;
import com.example.SIA.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacionService {

    private final NotificacionRepository repo;

    public void crear(String mensaje, String tipo, String categoria, String prioridad) {
        Notificacion n = new Notificacion();
        n.setMensaje(mensaje);
        n.setTipo(tipo);
        n.setCategoria(categoria);
        n.setPrioridad(prioridad);
        repo.save(n);
    }

    public List<Notificacion> obtenerNoLeidas() {
        return repo.findByLeidaFalse();
    }

    public void marcarComoLeidas() {
        List<Notificacion> lista = repo.findByLeidaFalse();
        for (Notificacion n : lista) {
            n.setLeida(true);
        }
        repo.saveAll(lista);
    }
}