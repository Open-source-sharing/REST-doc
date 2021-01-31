package smartdoc.oas;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class OpenAPITest {

  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testReadFile() throws IOException {
    JsonNode node = Yaml.mapper().readTree(new File("E:\\jw\\rest-doc\\smartdoc-oas\\src\\main\\resources\\openapi.yml"));
    OpenAPI openAPI = Json.mapper().convertValue(node, OpenAPI.class);
    Components components = openAPI.getComponents();

    if (components != null) {
      Map<String, Schema> schemas = components.getSchemas();
      System.err.println(schemas);
    }
  }
}
