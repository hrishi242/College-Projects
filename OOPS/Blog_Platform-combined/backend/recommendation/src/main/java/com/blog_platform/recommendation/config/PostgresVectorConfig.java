package com.blog_platform.recommendation.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PostgresVectorConfig {

    @Bean
    public PostgreSQLDialect customPostgreSQLDialect() {
        return new CustomPostgreSQLDialect();
    }

    public static class CustomPostgreSQLDialect extends PostgreSQLDialect {
        @Override
        public void initializeFunctionRegistry(FunctionContributions functionContributions) {
            super.initializeFunctionRegistry(functionContributions);

            // Register vector similarity functions
            functionContributions.getFunctionRegistry().register(
                    "vector_cosine_distance",
                    new StandardSQLFunction("vector_cosine_distance", StandardBasicTypes.DOUBLE)
            );

            // Add other vector functions if needed
            functionContributions.getFunctionRegistry().register(
                    "vector_l2_distance",
                    new StandardSQLFunction("vector_l2_distance", StandardBasicTypes.DOUBLE)
            );
        }
    }
}