package restdoc.web.projector;

import java.util.List;
import restdoc.web.model.doc.http.BodyFieldDescriptor;

/** DeProjector */
public interface DeProjector {

  /** @return The flatten java pojo */
  List<BodyFieldDescriptor> deProject();
}
