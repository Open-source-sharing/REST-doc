package smartdoc.dashboard.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Test;
import smartdoc.dashboard.model.doc.http.BodyFieldDescriptor;
import smartdoc.dashboard.projector.JsonDeProjector;

public class JsonDeProjectorTest {
  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testDeProject() throws IOException {

    JsonNode jsonNode =
        mapper.readValue(
            new File(
                "E:\\jw\\r"
                    + "est-doc\\r"
                    + "estdoc-web\\src\\test\\kotlin\\r"
                    + "estdoc\\web\\util\\deproject\\sample1.json"),
            JsonNode.class);

    List<BodyFieldDescriptor> descriptors = new JsonDeProjector(jsonNode).deProject();

    System.err.println(mapper.writeValueAsString(descriptors));
  }
}
