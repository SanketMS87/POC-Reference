package com.rukamini.mail.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "API REST Ing Mail Application",
                        description = "Mail Application",
                        version = "1.0",
                        contact = @Contact(name = "Team", email = "Charan@swissre.com"),
                        license =
                                @License(
                                        name = "Apache License Version 2.0",
                                        url = "https://www.apache.org/licenses/LICENSE-2.0")))
public class SpringFoxConfig {
    // SpringDoc OpenAPI auto-discovers all REST endpoints — no Docket bean needed.
}
