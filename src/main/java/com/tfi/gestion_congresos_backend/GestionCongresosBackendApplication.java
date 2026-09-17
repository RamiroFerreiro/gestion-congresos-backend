package com.tfi.gestion_congresos_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class GestionCongresosBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(GestionCongresosBackendApplication.class, args);
	}

}
