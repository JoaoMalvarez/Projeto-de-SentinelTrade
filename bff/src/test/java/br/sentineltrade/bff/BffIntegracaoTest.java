package br.sentineltrade.bff;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testa o BFF de ponta a ponta contra um "backend falso" (um servidor HTTP minimo).
 * Assim da para testar o BFF mesmo antes de o backend real ficar pronto.
 */
@SpringBootTest(properties = "bff.limite.auth-por-minuto=1000") // os testes fazem muitos logins seguidos
@AutoConfigureMockMvc
class BffIntegracaoTest {

    private static final HttpServer BACKEND_FALSO;
    static volatile String ultimoAuthorization;
    static volatile String ultimaChaveIdempotencia;
    static volatile String ultimoCorrelationId;

    static {
        try {
            BACKEND_FALSO = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        BACKEND_FALSO.createContext("/", BffIntegracaoTest::responder);
        BACKEND_FALSO.start();
    }

    @DynamicPropertySource
    static void apontarParaBackendFalso(DynamicPropertyRegistry registro) {
        registro.add("bff.backend-url", () -> "http://127.0.0.1:" + BACKEND_FALSO.getAddress().getPort());
    }

    @AfterAll
    static void desligar() {
        BACKEND_FALSO.stop(0);
    }

    private static void responder(HttpExchange troca) throws IOException {
        String metodo = troca.getRequestMethod();
        String caminho = troca.getRequestURI().getPath();
        String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        ultimoAuthorization = troca.getRequestHeaders().getFirst("Authorization");
        ultimoCorrelationId = troca.getRequestHeaders().getFirst("X-Correlation-Id");

        int status = 200;
        String resposta;
        if (metodo.equals("POST") && caminho.equals("/api/auth/login")) {
            if (corpo.contains("errada")) {
                status = 401;
                resposta = "{\"message\":\"Credenciais inválidas.\"}";
            } else {
                resposta = "{\"mfaToken\":\"mfa-1\"}";
            }
        } else if (metodo.equals("POST") && caminho.equals("/api/auth/mfa")) {
            String perfil = corpo.contains("111111") ? "AUDITOR" : "INVESTIDOR";
            resposta = "{\"token\":\"jwt-do-backend\",\"usuario\":{\"id\":\"u1\",\"nome\":\"Ana\","
                    + "\"email\":\"ana@orion.test\",\"perfil\":\"" + perfil + "\"}}";
        } else if (caminho.equals("/api/carteira")) {
            resposta = "{\"saldoDisponivel\":5000.00,\"saldoBloqueado\":0,"
                    + "\"posicoes\":[{\"ticker\":\"PETR4\",\"valorAtual\":3850.00}]}";
        } else if (caminho.equals("/api/ordens") && metodo.equals("GET")) {
            resposta = "[]";
        } else if (caminho.equals("/api/ordens") && metodo.equals("POST")) {
            ultimaChaveIdempotencia = troca.getRequestHeaders().getFirst("Idempotency-Key");
            status = 201;
            resposta = "{\"id\":\"o1\",\"status\":\"PENDENTE\"}";
        } else if (caminho.equals("/api/ativos")) {
            resposta = "[{\"ticker\":\"PETR4\",\"mercado\":\"ABERTO\",\"variacao\":1.05}]";
        } else if (caminho.equals("/api/notificacoes")) {
            status = 503;
            resposta = "{\"message\":\"Sistema em modo seguro.\"}";
        } else if (caminho.equals("/api/ordens/o1") && metodo.equals("DELETE")) {
            status = 409;
            resposta = "{\"mensagem\":\"Esta ordem não pode mais ser cancelada.\"}";
        } else {
            status = 404;
            resposta = "{\"message\":\"Não encontrado.\"}";
        }

        byte[] bytes = resposta.getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json");
        troca.sendResponseHeaders(status, bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    @Autowired
    private MockMvc mvc;

    private String logar(String codigo) throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@orion.test\",\"senha\":\"Teste@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mfaToken").value("mfa-1"));

        MvcResult r = mvc.perform(post("/api/auth/mfa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mfaToken\":\"mfa-1\",\"codigo\":\"" + codigo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.email").value("ana@orion.test"))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.token");
    }

    @Test
    void loginCriaSessaoSemExporOJwtDoBackend() throws Exception {
        String token = logar("123456");
        assertNotEquals("jwt-do-backend", token);

        mvc.perform(get("/api/carteira").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicoes[0].ticker").value("PETR4"));
        // O BFF trocou o token opaco pelo JWT do backend
        assertEquals("Bearer jwt-do-backend", ultimoAuthorization);
    }

    @Test
    void dashboardAgregaTresChamadas() throws Exception {
        String token = logar("123456");
        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"patrimonio\":8850.00")))
                .andExpect(jsonPath("$.destaques[0].ticker").value("PETR4"));
    }

    @Test
    void semSessaoDa401() throws Exception {
        mvc.perform(get("/api/carteira"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void perfilErradoDa403() throws Exception {
        String token = logar("123456"); // investidor
        mvc.perform(get("/api/auditoria").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        String auditor = logar("111111");
        mvc.perform(get("/api/dashboard").header("Authorization", "Bearer " + auditor))
                .andExpect(status().isForbidden());
    }

    @Test
    void ordemExigeIdempotencyKeyERepassaSemAlterar() throws Exception {
        String token = logar("123456");
        String ordem = "{\"ticker\":\"PETR4\",\"tipo\":\"COMPRA\",\"quantidade\":10,\"precoLimite\":38.50}";

        mvc.perform(post("/api/ordens").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(ordem))
                .andExpect(status().isBadRequest());

        String chave = "3f2a7c1e-9d2b-4b7e-8a51-0c6f2e1d9b44";
        mvc.perform(post("/api/ordens").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", chave)
                        .header("X-Correlation-Id", "teste-correlacao-123")
                        .contentType(MediaType.APPLICATION_JSON).content(ordem))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(header().string("X-Correlation-Id", "teste-correlacao-123"));
        assertEquals(chave, ultimaChaveIdempotencia);
        assertEquals("teste-correlacao-123", ultimoCorrelationId);
    }

    @Test
    void quantidadeFracionadaOuCampoExtraDa400() throws Exception {
        String token = logar("123456");
        mvc.perform(post("/api/ordens").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "3f2a7c1e-9d2b-4b7e-8a51-0c6f2e1d9b45")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"tipo\":\"COMPRA\",\"quantidade\":1.5,\"precoLimite\":38.5}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/ordens").header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "3f2a7c1e-9d2b-4b7e-8a51-0c6f2e1d9b46")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticker\":\"PETR4\",\"tipo\":\"COMPRA\",\"quantidade\":1,\"precoLimite\":38.5,\"status\":\"EXECUTADA\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void errosDoBackendSaoTraduzidos() throws Exception {
        String token = logar("123456");
        mvc.perform(get("/api/notificacoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Sistema em modo seguro."));
        // backend usa "mensagem"; o BFF devolve sempre "message"
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/ordens/o1").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Esta ordem não pode mais ser cancelada."));
    }

    @Test
    void credencialErradaDa401ComMensagemGenerica() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@orion.test\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas."));
    }

    @Test
    void parametroMaliciosoNaoChegaAoBackend() throws Exception {
        String token = logar("123456");
        mvc.perform(get("/api/ordens").param("aba", "todas'; DROP TABLE ordem;--")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutEncerraASessao() throws Exception {
        String token = logar("123456");
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/carteira").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cabecalhosDeSegurancaPresentes() throws Exception {
        mvc.perform(get("/api/carteira"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().exists("X-Correlation-Id"));
    }
}
