package smartdoc.dashboard.projector;

import java.util.List;
import smartdoc.dashboard.model.doc.http.BodyFieldDescriptor;

/** DeProjector */
public interface DeProjector {

  /** @return The flatten java pojo */
  List<BodyFieldDescriptor> deProject();
}
