import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * A filter that dynamically adds the Authorization header.
 */
@Provider
public class OpenAIAuthFilter implements ClientRequestFilter {

    @ConfigProperty(name = "openai.api.key") // Property injection
    String apiKey;

    @Override
    public void filter(ClientRequestContext requestContext) {
        requestContext.getHeaders().add("Authorization", "Bearer " + apiKey);
    }
}