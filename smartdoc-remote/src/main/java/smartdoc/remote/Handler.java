package smartdoc.remote;

public interface Handler {

  Command handle(Command cmd);
}
