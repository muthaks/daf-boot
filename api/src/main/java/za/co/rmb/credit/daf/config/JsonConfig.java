package za.co.rmb.credit.daf.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import za.co.rmb.credit.daf.support.LegacyObjectMapper;

/** The JSON mapper Jersey uses: the legacy RESTEasy provider's (see {@link LegacyObjectMapper}). */
@Configuration(proxyBeanMethods = false)
public class JsonConfig {

    @Bean
    @Primary
    ObjectMapper legacyObjectMapper() {
        return LegacyObjectMapper.create();
    }
}
