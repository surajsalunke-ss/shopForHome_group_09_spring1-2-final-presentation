package eshop.homedecor.shopapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eshop.homedecor.shopapi.entity.ProductInfo;
import eshop.homedecor.shopapi.repository.ProductInfoRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import javax.sql.DataSource;
import javax.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.MediaType;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.config.location=file:config/local.properties",
    "spring.datasource.url=jdbc:h2:mem:shopforhome-tests;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("local")
@AutoConfigureMockMvc
@Transactional
class LocalBackendIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductInfoRepository products;
    @Autowired DataSource dataSource;
    @Autowired ConfigurableEnvironment environment;
    @Autowired JavaMailSender mailSender;
    @Autowired EntityManager entityManager;

    @Test
    void usesOnlyIsolatedDatabaseAndConfiguration() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:shopforhome-tests");
        }
        assertThat(environment.getProperty("server.address")).isEqualTo("127.0.0.1");
        assertThat(environment.getPropertySources()).noneMatch(source ->
            source.getName().contains("application.properties"));
    }

    @Test
    void readsPersistedProductsThroughApi() throws Exception {
        ProductInfo product = new ProductInfo();
        product.setProductId("local-test-lamp");
        product.setProductName("Local test lamp");
        product.setProductPrice(new BigDecimal("249.50"));
        product.setProductStock(10);
        product.setProductStatus(0);
        product.setCategoryType(1);
        products.saveAndFlush(product);
        mvc.perform(get("/product/local-test-lamp"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.productName").value("Local test lamp"))
            .andExpect(jsonPath("$.productPrice").value(249.50));
        mvc.perform(get("/product"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void registersAuthenticatesAndReadsOwnProfileUsingJwt() throws Exception {
        String account = "{\"email\":\"local-test@example.invalid\",\"password\":\"LocalTest123!\","
            + "\"name\":\"Local Test\",\"phone\":\"0000000000\",\"address\":\"Local only\","
            + "\"active\":true,\"role\":\"ROLE_CUSTOMER\"}";
        mvc.perform(post("/register").contentType(MediaType.APPLICATION_JSON).content(account))
            .andExpect(status().isOk());
        // JDBC authentication must see the JPA writes inside this rollback-only test transaction.
        entityManager.flush();
        String response = mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"local-test@example.invalid\",\"password\":\"LocalTest123!\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode login = mapper.readTree(response);
        assertThat(login.path("token").asText()).isNotEmpty();
        mvc.perform(get("/profile/local-test@example.invalid")
            .header("Authorization", "Bearer " + login.path("token").asText()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("local-test@example.invalid"));
        mvc.perform(post("/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"username\":\"local-test@example.invalid\",\"password\":\"wrong\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void protectsPrivateEndpoints() throws Exception {
        mvc.perform(get("/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void disablesBothMailDeliveryPaths() throws Exception {
        assertThatThrownBy(() -> mailSender.send(new SimpleMailMessage()))
            .isInstanceOf(MailSendException.class).hasMessageContaining("disabled");
        assertThatThrownBy(() -> mailSender.send(mailSender.createMimeMessage()))
            .isInstanceOf(MailSendException.class).hasMessageContaining("disabled");
        mvc.perform(post("/sendMail").contentType(MediaType.APPLICATION_JSON)
            .content("{\"recipient\":\"nobody@example.invalid\",\"subject\":\"Local test\",\"msgBody\":\"Test\"}"))
            .andExpect(status().isOk())
            .andExpect(content().string("Error while Sending Mail"));
    }
}
