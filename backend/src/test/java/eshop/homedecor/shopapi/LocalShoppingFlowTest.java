package eshop.homedecor.shopapi;

import com.fasterxml.jackson.databind.JsonNode;
import eshop.homedecor.shopapi.config.LocalDemoData;
import eshop.homedecor.shopapi.entity.ProductInfo;
import eshop.homedecor.shopapi.repository.*;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.config.location=file:config/local.properties",
    "spring.datasource.url=jdbc:h2:mem:shopping-flow;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop", "shop.demo.enabled=true"
})
@ActiveProfiles("local")
class LocalShoppingFlowTest {
    @Autowired TestRestTemplate http;
    @Autowired LocalDemoData demo;
    @Autowired ProductInfoRepository products;
    @Autowired UserRepository users;

    @Test
    void demoIsRepeatableAndPreservesExistingRecords() throws Exception {
        long productCount = products.count(), userCount = users.count();
        ProductInfo vase = products.findByProductId("demo-vase");
        vase.setProductName("Locally edited vase"); products.saveAndFlush(vase);
        demo.run(new DefaultApplicationArguments(new String[0]));
        demo.run(new DefaultApplicationArguments(new String[0]));
        assertThat(products.count()).isEqualTo(productCount);
        assertThat(users.count()).isEqualTo(userCount);
        assertThat(products.findByProductId("demo-vase").getProductName()).isEqualTo("Locally edited vase");
    }

    @Test
    void customerCanBuyLastItemsAndOnlyReadOwnOrder() {
        String email = "flow@example.invalid";
        ResponseEntity<JsonNode> registration = http.postForEntity("/register", Map.of(
            "email", email, "password", "FlowOnly123!", "name", "Flow Test", "phone", "0000000000",
            "address", "Local test address", "active", true, "role", "ROLE_MANAGER"), JsonNode.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registration.getBody().path("role").asText()).isEqualTo("ROLE_CUSTOMER");
        JsonNode login = http.postForObject("/login", Map.of("username", email, "password", "FlowOnly123!"), JsonNode.class);
        HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(login.path("token").asText());
        ProductInfo lamp = products.findByProductId("demo-lamp"); lamp.setProductStock(2); products.saveAndFlush(lamp);
        assertThat(http.exchange("/cart/add", HttpMethod.POST,
            new HttpEntity<>(Map.of("productId", "demo-lamp", "quantity", 1), headers), Boolean.class).getBody()).isTrue();
        assertThat(http.exchange("/cart/demo-lamp", HttpMethod.PUT, new HttpEntity<>(0, headers), String.class)
            .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.exchange("/cart/demo-lamp", HttpMethod.PUT, new HttpEntity<>(2, headers), JsonNode.class)
            .getBody().path("count").asInt()).isEqualTo(2);
        assertThat(http.exchange("/cart/checkout", HttpMethod.POST, new HttpEntity<>(headers), String.class)
            .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(products.findByProductId("demo-lamp").getProductStock()).isZero();
        assertThat(http.exchange("/cart", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class)
            .getBody().path("products").size()).isZero();
        JsonNode order = http.exchange("/order", HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class)
            .getBody().path("content").get(0);
        assertThat(order.path("orderAmount").decimalValue()).isEqualByComparingTo("58.00");
        String orderPath = "/order/" + order.path("orderId").asLong();
        assertThat(http.exchange(orderPath, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class)
            .getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode otherLogin = http.postForObject("/login", Map.of("username", "customer@example.invalid", "password", "DemoOnly123!"), JsonNode.class);
        HttpHeaders other = new HttpHeaders(); other.setBearerAuth(otherLogin.path("token").asText());
        assertThat(http.exchange(orderPath, HttpMethod.GET, new HttpEntity<>(other), String.class)
            .getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.exchange("/cart/checkout", HttpMethod.POST, new HttpEntity<>(headers), String.class)
            .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
