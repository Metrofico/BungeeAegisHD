package xyz.yooniks.aegis.blacklist;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Proxies {

  private final List<Proxy> proxies = new ArrayList<>();
  private int index = 0;

  public void init(File file) throws IOException {
    if (!file.exists()) {
      file.createNewFile();
    }
    final Scanner scanner = new Scanner(file);

    while (scanner.hasNextLine()) {

      String line = scanner.nextLine();
      if (line.contains(" ")) {
        line = line.replace(" ", "");
      }

      if (line.isEmpty() || line.startsWith("##") || !line.contains(":")) {
        continue;
      }

      final String[] ip = line.split(":");
      if (ip.length < 1) {
        return;
      }
      final String hostname = ip[0];
      int port = 8080;
      try {
        port = Integer.parseInt(ip[1]);
      } catch (NumberFormatException ignored) {
      }

      final Proxy proxy = new Proxy(Type.HTTP, new InetSocketAddress(hostname, port));
      proxies.add(proxy);
    }
  }

  public int size() {
    return this.proxies.size();
  }

  public Proxy nextProxy() {
    if (this.proxies.size() == 0) {
      return Proxy.NO_PROXY;
    }
    if (this.index >= this.proxies.size()) {
      this.index = 0;
    }
    return Objects.requireNonNull(this.proxies.get(index++));
  }

  public void removeProxy(Proxy proxy) {
    this.proxies.remove(proxy);
  }

}
