package com.example.SIA.service.impl;

import com.example.SIA.dto.EquipoDTO;
import com.example.SIA.entity.Equipo;
import com.example.SIA.entity.Usuario;
import com.example.SIA.repository.EquipoRepository;
import com.example.SIA.repository.UsuarioRepository;
import com.example.SIA.service.EquipoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EquipoServiceImpl implements EquipoService {

    private final EquipoRepository equipoRepo;
    private final UsuarioRepository usuarioRepo;

    public EquipoServiceImpl(EquipoRepository equipoRepo, UsuarioRepository usuarioRepo) {
        this.equipoRepo = equipoRepo;
        this.usuarioRepo = usuarioRepo;
    }

    @Override
public Equipo registrarEquipo(EquipoDTO equipoDTO) {
    if (equipoRepo.existsByNumeroSerie(equipoDTO.getNumeroSerie())) {
        throw new RuntimeException("El número de serie ya está registrado ❌");
    }

    Usuario usuario = usuarioRepo.findById(equipoDTO.getIdUsuario())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

    Equipo equipo = new Equipo();
    equipo.setTipoEquipo(equipoDTO.getTipoEquipo());
    equipo.setMarcaModelo(equipoDTO.getMarcaModelo());
    equipo.setNumeroSerie(equipoDTO.getNumeroSerie());
    equipo.setEstado(1);
    equipo.setUsuario(usuario);

    return equipoRepo.save(equipo);
}


    @Override
    public List<Equipo> listarPorUsuario(Integer idUsuario) {
        // Solo equipos activos (estado=1)
        return equipoRepo.findByUsuario_IdUsuarioAndEstado(idUsuario, 1);
    }

    @Override
    public boolean eliminarEquipo(Integer idEquipo) {
        if (equipoRepo.existsById(idEquipo)) {
            equipoRepo.deleteById(idEquipo);
            return true;
        }
        return false;
    }

    @Override
    public void desactivarEquipo(Integer idEquipo, Integer idUsuario) {
        Equipo equipo = equipoRepo.findById(idEquipo)
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));
        if (!equipo.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new RuntimeException("No autorizado para desactivar este equipo");
        }
        equipo.setEstado(0);
        equipoRepo.save(equipo);
    }

    @Override
    public Equipo buscarPorIdYUsuario(Integer idEquipo, Integer idUsuario) {
        return equipoRepo.findById(idEquipo)
                .filter(e -> e.getUsuario().getIdUsuario().equals(idUsuario))
                .orElse(null);
    }

    @Override
    public Equipo actualizarEquipo(EquipoDTO equipoDTO) {
        Equipo equipo = equipoRepo.findById(equipoDTO.getIdEquipo())
                .orElseThrow(() -> new RuntimeException("Equipo no encontrado"));

        if (!equipo.getUsuario().getIdUsuario().equals(equipoDTO.getIdUsuario())) {
            throw new RuntimeException("No autorizado para editar este equipo");
        }

        if (!equipo.getNumeroSerie().equals(equipoDTO.getNumeroSerie())
                && equipoRepo.existsByNumeroSerie(equipoDTO.getNumeroSerie())) {
            throw new RuntimeException("El número de serie ya está registrado ❌");
        }

        equipo.setTipoEquipo(equipoDTO.getTipoEquipo());
        equipo.setMarcaModelo(equipoDTO.getMarcaModelo());
        equipo.setNumeroSerie(equipoDTO.getNumeroSerie());

        return equipoRepo.save(equipo);
    }
}
