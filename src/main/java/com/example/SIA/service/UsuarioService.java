package com.example.SIA.service;

import com.example.SIA.dto.*;
import com.example.SIA.entity.Usuario;

import java.util.List;

// PATRÓN STRATEGY

public interface UsuarioService {
    // 📌 CRUD
    List<UsuarioResponse> listarUsuarios();
    UsuarioResponse crearUsuario(UsuarioRequest request);
    UsuarioResponse actualizarUsuario(UsuarioUpdateRequest request);

    // 📌 Registro (controlador Registro)
    UsuarioResponse registrarUsuario(UsuarioRegistroRequest request);

    // 📌 Activar / Desactivar
    boolean desactivar(Integer id);
    boolean activar(Integer id);

    // 📌 Exportar Excel
    byte[] exportarExcel();

    // 📌 Dashboard y lógica interna
    Usuario findById(Integer id); //  para cargar datos del usuario desde sesión
    List<Usuario> findAll();      //  para panel administrador

    Usuario actualizar(Usuario usuario);




}