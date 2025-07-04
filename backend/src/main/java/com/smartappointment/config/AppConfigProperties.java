package com.smartappointment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfigProperties {

    private int minSlotDurationMinutes;
    private int maxSlotDurationMinutes;
    private int maxBookingsPerDay;
}
