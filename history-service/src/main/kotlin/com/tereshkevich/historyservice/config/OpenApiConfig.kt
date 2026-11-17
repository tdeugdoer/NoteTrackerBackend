package com.tereshkevich.historyservice.config

import com.tereshkevich.historyservice.util.OpenApiDocs
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun historyOpenApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title(OpenApiDocs.TITLE)
                .description(OpenApiDocs.DESCRIPTION)
                .version(OpenApiDocs.VERSION),
        )
}
