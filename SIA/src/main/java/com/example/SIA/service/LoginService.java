package com.example.SIA.service;

import com.example.SIA.dto.LoginRequest;
import com.example.SIA.dto.LoginResponse;
import com.example.SIA.entity.Usuario;

public interface LoginService {
    LoginResponse login(LoginRequest request);
    Usuario obtenerUsuarioPorId(Integer idUsuario); // ✅ corregido a Integer
    void logout();
}