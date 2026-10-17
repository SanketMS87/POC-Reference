package com.rukamini.eat.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Rukmini Eat API", version = "v1",
                description = """
                        Restaurant discovery, local ordering, and admin APIs. \
                        Log in or register to obtain a JWT, then use **Authorize**. \
                        Provider integrations (Zomato/Swiggy) are not connected.

                        ## Frontend routes
                        The Angular app runs at `http://localhost:4200`. Its routes are screens, \
                        not REST endpoints; they call the backend operations below.

                        | Frontend route | Screen | Backend operations |
                        |---|---|---|
                        | `/login` | Sign in / create account (guests only) | `POST /api/auth/login`, `POST /api/auth/register` |
                        | `/restaurants` | Dashboard, search, filters, sorting | `GET /api/restaurants` |
                        | `/restaurants/:id` | Restaurant menu | `GET /api/restaurants/{id}` |
                        | `/cart` | Cart and checkout | `POST /api/orders` |
                        | `/orders` | Order history | `GET /api/orders` |
                        | `/` or unknown route | Redirects to `/restaurants` | None |

                        All routes except `/login` require sign-in. No frontend screens exist for the \
                        administration or provider-integration operations; use this Swagger UI.
                        """),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
