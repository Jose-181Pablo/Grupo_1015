package mx.grupo1015.suscripcion;

import mx.grupo1015.suscripcion.client.AutenticacionClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class SuscripcionServiceApplicationTests {

	@MockitoBean
	private AutenticacionClient autenticacionClient;

	@Test
	void contextLoads() {
	}

}
