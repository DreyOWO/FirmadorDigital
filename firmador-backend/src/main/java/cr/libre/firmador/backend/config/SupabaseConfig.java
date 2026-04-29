package cr.libre.firmador.backend.config;

import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.SupabaseClientBuilder;
import io.github.jan.supabase.storage.Storage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SupabaseConfig {
    
    @Value("${supabase.url}")
    private String supabaseUrl;
    
    @Value("${supabase.key}")
    private String supabaseKey;
    
    @Bean
    public SupabaseClient supabaseClient() {
        return new SupabaseClientBuilder()
                .supabaseUrl(supabaseUrl)
                .supabaseKey(supabaseKey)
                .build();
    }
}
