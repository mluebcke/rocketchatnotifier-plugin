package jenkins.plugins.rocketchatnotifier.utils;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class Environment {

  static final String JAVA_NET_PROXY_HOST_POSTFIX = ".proxyHost";
  static final String JAVA_NET_PROXY_PORT_POSTFIX = ".proxyPort";
  static final String JAVA_NET_NON_PROXY_HOSTS_POSTFIX = ".nonProxyHosts";
  static final String ENV_PROXY_POSTFIX = "_proxy";
  static final String ENV_NO_PROXY = "no_proxy";

  /**
   * <p>Determines the proxy to use for the given url.</p>
   * <p>With priority the system properties</p>
   * <ul>
   *   <li>http.proxyHost</li>
   *   <li>http.proxyPort</li>
   *   <li>https.nonProxyHosts</li>
   *   <li>https.proxyHost</li>
   *   <li>https.proxyPort</li>
   *   <li>https.nonProxyHosts</li>
   * </ul>
   * <p>will be read and handles theme as described in
   * <a href="https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html">JAVA network properties</a>,
   * except for nonProxyHosts will only used domain names.</p>
   *
   * <p>After this the system environment variables</p>
   * <ul>
   *   <li>http_proxy</li>
   *   <li>https_proxy</li>
   *   <li>no_proxy</li>
   * </ul>
   *
   * @param url an url
   * @return the proxy to use or an empty string
   */
  public String getProxy(String url) {
    URI serverUri = URI.create(url);
    String scheme = serverUri.getScheme();

    Set<String> nonProxies = getNoProxyForScheme(scheme);
    boolean resolveProxy = true;
    for (String nonProxy : nonProxies) {
      // following test is very simple. According to
      // https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html
      // we should check for IP ranges, too. But this is something for a future feature request ;)
      if (serverUri.getHost().equalsIgnoreCase(nonProxy)) {
        resolveProxy = false;
        break;
      }
    }

    if (resolveProxy) {
      return getProxyForScheme(scheme);
    }
    return "";
  }

  private String getProxyForScheme(String scheme) {
    // see https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html
    String host = System.getProperty(scheme + JAVA_NET_PROXY_HOST_POSTFIX);
    String port = System.getProperty(scheme + JAVA_NET_PROXY_PORT_POSTFIX);
    if (host != null && port != null) {
      return String.format("%s:%s", host, port);
    }

    String value = System.getenv(scheme + ENV_PROXY_POSTFIX);
    if (value != null) {
      return value;
    }

    return "";
  }

  private Set<String> getNoProxyForScheme(String scheme) {
    // see https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html
    String value = System.getProperty(scheme + JAVA_NET_NON_PROXY_HOSTS_POSTFIX);
    if (value != null) {
      return splitEnvValue(value, "\\|");
    }

    value = System.getenv(ENV_NO_PROXY);
    if (value != null) {
      return splitEnvValue(value, ",");
    }

    return Collections.emptySet();
  }

  private Set<String> splitEnvValue(String value, String delimiter) {
    Set<String> noProxies = new HashSet<>();
    for (String noProxy : value.split(delimiter)) {
      noProxies.add(noProxy.trim());
    }
    return noProxies;
  }
}
