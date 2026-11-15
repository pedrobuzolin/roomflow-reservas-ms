package br.com.roomflow.reservas_ms.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "br.com.roomflow.reservas_ms.http")
public class FeignConfig {
}