package cn.pirate.aicodegen;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {RedisEmbeddingStoreAutoConfiguration.class})
@MapperScan("cn.pirate.aicodegen.mapper")
public class AiCodeGenApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiCodeGenApplication.class, args);
    }

}
