package im.bigs.pg.api.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Payment Gateway API")
                    .description("결제 게이트웨이 서비스 API 문서")
                    .version("v1.0.0")
                    .contact(
                        Contact()
                            .name("BIGS PG Team")
                            .email("pg-support@bigs.im"),
                    ),
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("Local Development"),
                ),
            )
    }
}
