package eshop.homedecor.shopapi.config;

import eshop.homedecor.shopapi.entity.*;
import eshop.homedecor.shopapi.repository.*;
import eshop.homedecor.shopapi.service.UserService;
import java.math.BigDecimal;
import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Opt-in fixtures, never a migration or a database reset. */
@Component
@Profile("local")
@ConditionalOnProperty(name = "shop.demo.enabled", havingValue = "true")
public class LocalDemoData implements ApplicationRunner {
    private final ProductInfoRepository products;
    private final ProductCategoryRepository categories;
    private final UserRepository users;
    private final UserService userService;
    private final DataSource dataSource;
    private final Environment environment;

    public LocalDemoData(ProductInfoRepository products, ProductCategoryRepository categories,
                         UserRepository users, UserService userService, DataSource dataSource,
                         Environment environment) {
        this.products = products; this.categories = categories; this.users = users;
        this.userService = userService; this.dataSource = dataSource; this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getURL().startsWith("jdbc:h2:") ||
                !"127.0.0.1".equals(environment.getProperty("server.address"))) {
                throw new IllegalStateException("Demo data requires local H2 and IPv4 loopback binding");
            }
        }
        if (categories.findByCategoryType(0) == null) categories.save(new ProductCategory("Shop Living Room", 0));
        product("demo-lamp", "Amber Table Lamp", "29.00", "A warm accent for a reading corner.", "lamp");
        product("demo-vase", "Sage Ceramic Vase", "18.00", "A simple ceramic vase for your living room.", "vase");
        product("demo-cushion", "Terracotta Cushion", "24.00", "A soft accent cushion for a sofa or chair.", "cushion");
        account("customer@example.invalid", "Demo Customer", "ROLE_CUSTOMER");
        account("manager@example.invalid", "Demo Manager", "ROLE_MANAGER");
    }

    private void product(String id, String name, String price, String description, String image) {
        if (products.existsById(id)) return;
        ProductInfo product = new ProductInfo();
        product.setProductId(id); product.setProductName(name); product.setProductPrice(new BigDecimal(price));
        product.setProductDescription(description); product.setProductIcon("/assets/demo/" + image + ".svg");
        product.setProductStock(20); product.setProductStatus(0); product.setCategoryType(0);
        products.save(product);
    }

    private void account(String email, String name, String role) {
        if (users.findByEmail(email) != null) return;
        User user = new User();
        user.setEmail(email); user.setName(name); user.setPassword("DemoOnly123!");
        user.setPhone("0000000000"); user.setAddress("Local demo address"); user.setActive(true); user.setRole(role);
        userService.save(user);
    }
}
