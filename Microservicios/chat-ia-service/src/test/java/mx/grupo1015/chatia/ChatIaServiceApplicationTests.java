package mx.grupo1015.chatia;

import mx.grupo1015.chatia.client.CatalogoClient;
import mx.grupo1015.chatia.service.GeminiService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ChatIaServiceApplicationTests {

    @MockitoBean
    private GeminiService geminiService;

    @MockitoBean
    private CatalogoClient catalogoClient;

    @Test
    void contextLoads() {
    }
}
