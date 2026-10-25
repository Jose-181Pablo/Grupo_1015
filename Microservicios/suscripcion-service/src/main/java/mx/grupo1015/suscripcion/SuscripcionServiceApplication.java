package mx.grupo1015.suscripcion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SuscripcionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuscripcionServiceApplication.class, args);
	}

}
