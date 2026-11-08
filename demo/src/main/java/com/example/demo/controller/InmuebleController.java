package com.example.demo.controller;

import com.example.demo.model.Inmueble;
import com.example.demo.service.InmuebleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class InmuebleController {

    @Autowired
    private InmuebleService service;

    // READ: Muestra la lista
    @GetMapping("/")
    public String listar(Model model) {
        model.addAttribute("inmuebles", service.obtenerTodos());
        return "inmuebles-list";
    }

    // CREATE (Vista): Muestra el formulario vacío
    @GetMapping("/nuevo")
    public String mostrarFormularioDeCrear(Model model) {
        model.addAttribute("inmueble", new Inmueble());
        return "inmuebles-form";
    }

    // CREATE / UPDATE (Acción): Guarda en la base de datos
    @PostMapping("/guardar")
    public String guardarInmueble(Inmueble inmueble) {
        service.guardar(inmueble);
        return "redirect:/"; // Redirige a la lista
    }

    // UPDATE (Vista): Muestra el formulario con los datos cargados del Inmueble
    @GetMapping("/editar/{id}")
    public String mostrarFormularioDeEditar(@PathVariable String id, Model model) {
        model.addAttribute("inmueble", service.obtenerPorId(id));
        return "inmuebles-form";
    }

    // DELETE: Elimina el registro y redirige a la lista
    @GetMapping("/eliminar/{id}")
    public String eliminarInmueble(@PathVariable String id) {
        service.eliminar(id);
        return "redirect:/";
    }
}