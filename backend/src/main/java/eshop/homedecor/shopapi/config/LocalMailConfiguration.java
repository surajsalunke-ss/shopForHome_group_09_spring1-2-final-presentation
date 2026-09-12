package eshop.homedecor.shopapi.config;

import javax.mail.internet.MimeMessage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** Local development must never open an SMTP connection. */
@Configuration
@Profile("local")
public class LocalMailConfiguration {
    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl() {
            @Override
            public void send(SimpleMailMessage... messages) {
                throw new MailSendException("Mail delivery is disabled in the local profile");
            }

            @Override
            protected void doSend(MimeMessage[] messages, Object[] originalMessages) {
                throw new MailSendException("Mail delivery is disabled in the local profile");
            }

            @Override
            public void testConnection() {
                throw new MailSendException("Mail delivery is disabled in the local profile");
            }
        };
    }
}
