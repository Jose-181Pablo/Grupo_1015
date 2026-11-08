package com.grupo1015.microservicioofertas.repository;

import com.grupo1015.microservicioofertas.model.Oferta;
import com.grupo1015.microservicioofertas.model.TipoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OfertaRepository extends JpaRepository<Oferta, Long> {
    List<Oferta> findByActivaTrue();
    List<Oferta> findByTipoAndActivaTrue(TipoPublicacion tipo);
    List<Oferta> findByPropiedadAndActivaTrue(String propiedad);
}