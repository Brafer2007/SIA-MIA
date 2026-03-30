package com.example.SIA.pattern;

import com.example.SIA.entity.Instructor;
import com.example.SIA.entity.Programacion;

public class ProgramacionBuilder {

    private Programacion programacion;

    public ProgramacionBuilder() {
        this.programacion = new Programacion();
    }

    public ProgramacionBuilder conAula(String aula) {
        programacion.setAula(aula);
        return this;
    }

    public ProgramacionBuilder conCurso(String curso) {
        programacion.setCurso(curso);
        return this;
    }

    public ProgramacionBuilder conDia(String dia) {
        programacion.setDia(dia);
        return this;
    }

    public ProgramacionBuilder conHoraInicio(String horaInicio) {
        programacion.setHoraInicio(horaInicio);
        return this;
    }

    public ProgramacionBuilder conHoraFin(String horaFin) {
        programacion.setHoraFin(horaFin);
        return this;
    }

    public ProgramacionBuilder conNombreFicha(String nombreFicha) {
        programacion.setNombreFicha(nombreFicha);
        return this;
    }

    public ProgramacionBuilder conTrimestre(String trimestre) {
        programacion.setTrimestre(trimestre);
        return this;
    }

    public ProgramacionBuilder conInstructor(Instructor instructor) {
        programacion.setInstructor(instructor);
        return this;
    }

    public ProgramacionBuilder conPrograma(String programa) {
        programacion.setPrograma(programa);
        return this;
    }

    public Programacion build() {
        return programacion;
    }
}
