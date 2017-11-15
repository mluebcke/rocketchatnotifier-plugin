package jenkins.plugins.rocketchatnotifier.utils;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Because of the immutable nature of System.getenv() this, test will only check the properties that can be set
 * by System.setProperty()
 */
public class EnvironmentTest {

  private static final String SAMPLE_PROXY_HOST = "proxy.local.lan";
  private static final String SAMPLE_PROXY_PORT = "8888";
  private static final String SAMPLE_PROXY = SAMPLE_PROXY_HOST + ":" + SAMPLE_PROXY_PORT;

  private Environment env;

  @Before
  public void setup() {
    // we assume that the environment properties were not set, otherwise this will lead to an
    // unexpected behaviour
    assertNull(System.getenv("http" + Environment.ENV_PROXY_POSTFIX));
    assertNull(System.getenv("https" + Environment.ENV_PROXY_POSTFIX));
    assertNull(System.getenv(Environment.ENV_NO_PROXY));

    for (String scheme : Arrays.asList("http", "https")) {
      System.clearProperty(scheme + Environment.JAVA_NET_NON_PROXY_HOSTS_POSTFIX);
      System.clearProperty(scheme + Environment.JAVA_NET_PROXY_HOST_POSTFIX);
      System.clearProperty(scheme + Environment.JAVA_NET_PROXY_PORT_POSTFIX);
    }
    this.env = new Environment();
  }

  @Test
  public void noProxyConfigured() {
    assertTrue(isBlank(this.env.getProxy("http://www.google.de")));
    assertTrue(isBlank(this.env.getProxy("https://www.google.de")));
  }

  @Test
  public void httpPoxyConfigured() {
    // only http properties set
    System.setProperty("http" + Environment.JAVA_NET_PROXY_HOST_POSTFIX, SAMPLE_PROXY_HOST);
    System.setProperty("http" + Environment.JAVA_NET_PROXY_PORT_POSTFIX, SAMPLE_PROXY_PORT);
    assertEquals(this.env.getProxy("http://www.google.de"), SAMPLE_PROXY);
    assertTrue(isBlank(this.env.getProxy("https://www.google.de")));

    // no-https-property set
    System.setProperty("https" + Environment.JAVA_NET_NON_PROXY_HOSTS_POSTFIX, "www.google.de|www.apache.org");
    assertEquals(this.env.getProxy("http://www.google.de"), SAMPLE_PROXY);
    assertTrue(isBlank(this.env.getProxy("https://www.google.de")));
    assertEquals(this.env.getProxy("http://www.apache.org"), SAMPLE_PROXY);
    assertTrue(isBlank(this.env.getProxy("https://www.apache.org")));

    // no-http-property set, too
    System.setProperty("http" + Environment.JAVA_NET_NON_PROXY_HOSTS_POSTFIX, "www.google.de|www.apache.org");
    assertTrue(isBlank(this.env.getProxy("http://www.google.de")));
    assertTrue(isBlank(this.env.getProxy("https://www.google.de")));
    assertTrue(isBlank(this.env.getProxy("http://www.apache.org")));
    assertTrue(isBlank(this.env.getProxy("https://www.apache.org")));
  }

  @Test
  public void httpsPoxyConfigured() {
    // only https properties set
    System.setProperty("https" + Environment.JAVA_NET_PROXY_HOST_POSTFIX, SAMPLE_PROXY_HOST);
    System.setProperty("https" + Environment.JAVA_NET_PROXY_PORT_POSTFIX, SAMPLE_PROXY_PORT);
    assertEquals(this.env.getProxy("https://www.google.de"), SAMPLE_PROXY);
    assertTrue(isBlank(this.env.getProxy("http://www.google.de")));

    // no-http-property set
    System.setProperty("http" + Environment.JAVA_NET_NON_PROXY_HOSTS_POSTFIX, "www.google.de|www.apache.org");
    assertEquals(this.env.getProxy("https://www.google.de"), SAMPLE_PROXY);
    assertTrue(isBlank(this.env.getProxy("http://www.google.de")));
    assertEquals(this.env.getProxy("https://www.apache.org"), SAMPLE_PROXY);
    assertTrue(isBlank(this.env.getProxy("http://www.apache.org")));

    // no-https-property set, too
    System.setProperty("https" + Environment.JAVA_NET_NON_PROXY_HOSTS_POSTFIX, "www.google.de|www.apache.org");
    assertTrue(isBlank(this.env.getProxy("https://www.google.de")));
    assertTrue(isBlank(this.env.getProxy("http://www.google.de")));
    assertTrue(isBlank(this.env.getProxy("https://www.apache.org")));
    assertTrue(isBlank(this.env.getProxy("http://www.apache.org")));

  }
}
