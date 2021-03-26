package smartdoc.remote;

public abstract class Client extends AbstractService {

  public abstract int serverPort();

  public abstract String serverHost();
}
