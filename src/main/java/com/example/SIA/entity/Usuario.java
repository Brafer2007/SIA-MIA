package com.example.SIA.entity;

import jakarta.persistence.*;

@Entity
public class Usuario {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id_usuario") // asegura que la columna se llame igual que en el JoinColumn
  private Integer idUsuario;
  // coherente con los impl

  private String nombreUsuario;
  private String nombres;
  private String apellidos;
  private String correo;
  private String noDocumento;
  private String passUsuario;
  private Integer estado; // 1=activo, 0=inactivo

  @ManyToOne
  @JoinColumn(name = "id_perfil")
  private Perfil perfil;

  @OneToOne(mappedBy = "usuario", cascade = CascadeType.ALL)
  private Instructor instructor;

  // Getters y setters
  public Integer getIdUsuario() {
    return idUsuario;
  }

  public void setIdUsuario(Integer idUsuario) {
    this.idUsuario = idUsuario;
  }

  public String getNombreUsuario() {
    return nombreUsuario;
  }

  public void setNombreUsuario(String nombreUsuario) {
    this.nombreUsuario = nombreUsuario;
  }

  public String getNombres() {
    return nombres;
  }

  public void setNombres(String nombres) {
    this.nombres = nombres;
  }

  public String getApellidos() {
    return apellidos;
  }

  public void setApellidos(String apellidos) {
    this.apellidos = apellidos;
  }

  public String getCorreo() {
    return correo;
  }

  public void setCorreo(String correo) {
    this.correo = correo;
  }

  public String getNoDocumento() {
    return noDocumento;
  }

  public void setNoDocumento(String noDocumento) {
    this.noDocumento = noDocumento;
  }

  public String getPassUsuario() {
    return passUsuario;
  }

  public void setPassUsuario(String passUsuario) {
    this.passUsuario = passUsuario;
  }

  public Integer getEstado() {
    return estado;
  }

  public void setEstado(Integer estado) {
    this.estado = estado;
  }

  public Perfil getPerfil() {
    return perfil;
  }

  public void setPerfil(Perfil perfil) {
    this.perfil = perfil;
  }

  public Instructor getInstructor() {
    return instructor;
  }

  public void setInstructor(Instructor instructor) {
    this.instructor = instructor;
  }
}