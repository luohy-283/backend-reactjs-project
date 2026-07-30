package com.company.bookingroom;

import com.company.bookingroom.config.AsyncSyncConfiguration;
import com.company.bookingroom.config.DatabaseTestcontainer;
import com.company.bookingroom.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.context.ImportTestcontainers;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(
    classes = {
        BackendBookMeetingRoomApp.class,
        JacksonConfiguration.class,
        AsyncSyncConfiguration.class,
        com.company.bookingroom.config.JacksonHibernateConfiguration.class,
    }
)
@ImportTestcontainers(DatabaseTestcontainer.class)
public @interface IntegrationTest {}
