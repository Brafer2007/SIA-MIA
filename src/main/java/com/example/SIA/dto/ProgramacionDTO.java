package com.example.SIA.dto;

import com.example.SIA.entity.Programacion;

public class ProgramacionDTO {
    private String curso;
    private String aula;
    private String dia;
    private String horaInicio;
    private String horaFin;
    private String trimestre;
    private String nombreFicha;
    private String programa;

    public ProgramacionDTO() {
    }

    public ProgramacionDTO(Programacion p) {
        this.curso = p.getCurso();
        this.aula = p.getAula();
        this.dia = p.getDia();
        this.horaInicio = p.getHoraInicio();
        this.horaFin = p.getHoraFin();
        this.trimestre = p.getTrimestre();
        this.nombreFicha = p.getNombreFicha();
        this.programa = p.getPrograma();
    }

    // Getters y setters
    public String getCurso() {
        return curso;
    }

    public void setCurso(String curso) {
        this.curso = curso;
    }

    public String getAula() {
        return aula;
    }

    public void setAula(String aula) {
        this.aula = aula;
    }

    public String getDia() {
        return dia;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }

    public String getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(String horaInicio) {
        this.horaInicio = horaInicio;
    }

    public String getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(String horaFin) {
        this.horaFin = horaFin;
    }

    public String getTrimestre() {
        return trimestre;
    }

    public void setTrimestre(String trimestre) {
        this.trimestre = trimestre;
    }

    public String getNombreFicha() {
        return nombreFicha;
    }

    public void setNombreFicha(String nombreFicha) {
        this.nombreFicha = nombreFicha;
    }

    public String getPrograma() {
        return programa;
    }

    public void setPrograma(String programa) {
        this.programa = programa;
    }
}