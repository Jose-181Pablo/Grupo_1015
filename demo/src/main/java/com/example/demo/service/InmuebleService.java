package com.example.demo.service;

import com.example.demo.model.Inmueble;
import com.example.demo.repository.InmuebleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class InmuebleService {

    @Autowired
    private InmuebleRepository repository;

    public List<Inmueble> obtenerTodos() {
        return repository.findAll();
    }

    public Inmueble guardar(Inmueble inmueble) {
        return repository.save(inmueble);
    }

    // NUEVO: Busca un inmueble por su ID para poder editarlo
    public Inmueble obtenerPorId(String id) {
        return repository.findById(id).orElse(null);
    }

    // NUEVO: Elimina un inmueble por su ID
    public void eliminar(String id) {
        repository.deleteById(id);
    }
}