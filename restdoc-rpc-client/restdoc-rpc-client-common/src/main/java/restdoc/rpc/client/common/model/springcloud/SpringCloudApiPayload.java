package restdoc.rpc.client.common.model.springcloud;

import java.util.List;
import restdoc.rpc.client.common.model.AbstractApiPayload;
import restdoc.rpc.client.common.model.ApplicationType;

@Deprecated
public class SpringCloudApiPayload extends AbstractApiPayload {

  private List<SpringCloudApiDescriptor> apiList;

  public SpringCloudApiPayload() {
    super(ApplicationType.SPRINGCLOUD);
  }

  public List<SpringCloudApiDescriptor> getApiList() {
    return apiList;
  }

  public void setApiList(List<SpringCloudApiDescriptor> apiList) {
    this.apiList = apiList;
  }
}
