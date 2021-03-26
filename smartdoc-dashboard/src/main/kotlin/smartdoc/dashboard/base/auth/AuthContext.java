package smartdoc.dashboard.base.auth;

import javax.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthContext {

  public static final String CREDENTIAL_KEY = "auth_credential";

  public Credential getCredential(HttpServletRequest request) {
    return (Credential) request.getAttribute(CREDENTIAL_KEY);
  }

  public void setCredential(HttpServletRequest request, Credential credential) {
    request.setAttribute(CREDENTIAL_KEY, credential);
  }
}
