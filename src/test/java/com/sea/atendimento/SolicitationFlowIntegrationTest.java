package com.sea.atendimento;

import com.jayway.jsonpath.JsonPath;
import com.sea.atendimento.solicitation.service.CepAddress;
import com.sea.atendimento.solicitation.service.CepLookup;
import com.sea.atendimento.user.domain.Uf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SolicitationFlowIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @MockitoBean
    private CepLookup cepLookup;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @DisplayName("Fluxo completo: o cliente cria e envia; o analista da UF analisa e aprova")
    void fullSolicitationFlow() throws Exception {
        when(cepLookup.findByCep("01310100")).thenReturn(Optional.of(
                new CepAddress("01310100", "Avenida Paulista", "Bela Vista", "São Paulo", Uf.SP)));

        mvc.perform(post("/auth/register").contentType(APPLICATION_JSON).content("""
                        {"name": "Cliente Teste", "email": "cliente@teste.com", "password": "senha12345"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENT"));

        String clientToken = login("cliente@teste.com", "senha12345");

        String adminToken = login("admin@atendimento.com", "admin12345");
        mvc.perform(post("/admin/users").header(AUTHORIZATION, bearer(adminToken))
                        .contentType(APPLICATION_JSON).content("""
                        {"name": "Analista SP", "email": "analista@teste.com", "password": "senha12345",
                         "role": "ANALYST", "coveredUfs": ["SP"]}
                        """))
                .andExpect(status().isCreated());

        String analystToken = login("analista@teste.com", "senha12345");

        mvc.perform(post("/admin/users").header(AUTHORIZATION, bearer(clientToken))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        String created = mvc.perform(post("/solicitations").header(AUTHORIZATION, bearer(clientToken))
                        .contentType(APPLICATION_JSON).content("""
                        {"serviceType": "INSTALLATION", "title": "Instalação de ar",
                         "description": "Instalar um aparelho split na sala de estar"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn().getResponse().getContentAsString();

        Integer id = JsonPath.read(created, "$.id");

        mvc.perform(post("/solicitations/" + id + "/submit").header(AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.fieldErrors.length()").value(2));

        mvc.perform(put("/solicitations/" + id + "/address").header(AUTHORIZATION, bearer(clientToken))
                        .contentType(APPLICATION_JSON).content("""
                        {"cep": "01310-100", "number": "1000"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(2))
                .andExpect(jsonPath("$.address.uf").value("SP"));

        String preferredDate = LocalDate.now().plusDays(10).toString();
        mvc.perform(put("/solicitations/" + id + "/confirmation").header(AUTHORIZATION, bearer(clientToken))
                        .contentType(APPLICATION_JSON).content("""
                        {"priority": "MEDIUM", "preferredDate": "%s", "estimatedValue": 350.00, "termsAccepted": true}
                        """.formatted(preferredDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStep").value(3));

        mvc.perform(post("/solicitations/" + id + "/submit").header(AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        mvc.perform(post("/analyst/solicitations/" + id + "/start").header(AUTHORIZATION, bearer(analystToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));

        mvc.perform(post("/analyst/solicitations/" + id + "/decide").header(AUTHORIZATION, bearer(analystToken))
                        .contentType(APPLICATION_JSON).content("""
                        {"decision": "APPROVE", "comment": "Endereço e orçamento conferidos."}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/solicitations/" + id).header(AUTHORIZATION, bearer(clientToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.analystId").isNotEmpty());
    }

    private String login(String email, String password) throws Exception {
        String response = mvc.perform(post("/auth/login").contentType(APPLICATION_JSON).content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.accessToken");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}