package com.africa.hr.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "springdoc")
public class SwaggerConfigProperties {
    private ApiDocs apiDocs;
    private SwaggerUi swaggerUi;

    public ApiDocs getApiDocs() {
        return apiDocs;
    }

    public void setApiDocs(ApiDocs apiDocs) {
        this.apiDocs = apiDocs;
    }

    public SwaggerUi getSwaggerUi() {
        return swaggerUi;
    }

    public void setSwaggerUi(SwaggerUi swaggerUi) {
        this.swaggerUi = swaggerUi;
    }

    public static class ApiDocs {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    public static class SwaggerUi {
        private String path;

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }
}
