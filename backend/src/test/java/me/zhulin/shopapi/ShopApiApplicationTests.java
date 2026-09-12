package me.zhulin.shopapi;

import eshop.homedecor.shopapi.ShopApiApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ShopApiApplication.class, properties = {
    "spring.config.location=file:config/local.properties",
    "spring.datasource.url=jdbc:h2:mem:shopforhome-legacy-tests;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("local")
public class ShopApiApplicationTests {
    @Test
    public void contextLoads() {
    }
}
