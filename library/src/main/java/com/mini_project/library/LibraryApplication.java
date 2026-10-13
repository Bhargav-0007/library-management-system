package com.mini_project.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

// Staff authenticate via JwtAuthFilter, so Boot's default in-memory user isn't needed.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class LibraryApplication {

	public static void main(String[] args) {
		SpringApplication.run(LibraryApplication.class, args);
	}

}
