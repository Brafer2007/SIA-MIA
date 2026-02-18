package com.example.SIA.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "aprendiz")
public class Aprendiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_aprendiz")
    private Integer idAprendiz;

    @OneToOne
    @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", unique = true)
    private Usuario usuario;

    @Column(name = "ficha_formacion")
    private String fichaFormacion;

    @Column(name = "programa_formacion")
    private String programaFormacion;

    @Column(name = "perfil_completo")
    private Integer perfilCompleto = 0; // 0 = incompleto, 1 = completo

    // Getters y Setters
    public Integer getIdAprendiz() { return idAprendiz; }
    public void setIdAprendiz(Integer idAprendiz) { this.idAprendiz = idAprendiz; }

    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public String getFichaFormacion() { return fichaFormacion; }
    public void setFichaFormacion(String fichaFormacion) { this.fichaFormacion = fichaFormacion; }

    public String getProgramaFormacion() { return programaFormacion; }
    public void setProgramaFormacion(String programaFormacion) { this.programaFormacion = programaFormacion; }

    public Integer getPerfilCompleto() { return perfilCompleto; }
    public void setPerfilCompleto(Integer perfilCompleto) { this.perfilCompleto = perfilCompleto; }
}
