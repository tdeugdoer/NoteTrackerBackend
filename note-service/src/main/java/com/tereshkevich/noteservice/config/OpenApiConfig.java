package com.tereshkevich.noteservice.config;

import com.tereshkevich.noteservice.util.OpenApiDocs;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI notesOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title(OpenApiDocs.TITLE)
                        .description(OpenApiDocs.DESCRIPTION)
                        .version(OpenApiDocs.VERSION));
    }

}
