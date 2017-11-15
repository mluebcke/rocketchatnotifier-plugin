package jenkins.plugins.rocketchatnotifier;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import jenkins.model.JenkinsLocationConfiguration;
import net.sf.json.JSONObject;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import sun.security.validator.ValidatorException;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RocketChatNotifier extends Notifier {

  private static final Logger LOGGER = Logger.getLogger(RocketChatNotifier.class.getName());
  private String rocketServerUrl;
  private boolean trustSSL;
  private String username;
  private String password;
  private String channel;
  private String buildServerUrl;
  private boolean startNotification;
  private boolean notifySuccess;
  private boolean notifyAborted;
  private boolean notifyNotBuilt;
  private boolean notifyUnstable;
  private boolean notifyFailure;
  private boolean notifyBackToNormal;
  private boolean notifyRepeatedFailure;
  private boolean includeTestSummary;
  private CommitInfoChoice commitInfoChoice;
  private boolean includeCustomMessage;
  private String customMessage;

  @Override
  public DescriptorImpl getDescriptor() {
    return (DescriptorImpl) super.getDescriptor();
  }

  public String getChannel() {
    return channel;
  }

  public String getBuildServerUrl() {
    LOGGER.log(Level.FINE, "Getting build server URL");
    if (buildServerUrl == null || buildServerUrl.equalsIgnoreCase("")) {
      JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
      return jenkinsConfig.getUrl();
    } else {
      return buildServerUrl;
    }
  }

  public boolean getStartNotification() {
    return startNotification;
  }

  public boolean getNotifySuccess() {
    return notifySuccess;
  }

  public CommitInfoChoice getCommitInfoChoice() {
    return commitInfoChoice;
  }

  public boolean getNotifyAborted() {
    return notifyAborted;
  }

  public boolean getNotifyFailure() {
    return notifyFailure;
  }

  public boolean getNotifyNotBuilt() {
    return notifyNotBuilt;
  }

  public boolean getNotifyUnstable() {
    return notifyUnstable;
  }

  public boolean getNotifyBackToNormal() {
    return notifyBackToNormal;
  }

  public boolean includeTestSummary() {
    return includeTestSummary;
  }

  public boolean getNotifyRepeatedFailure() {
    return notifyRepeatedFailure;
  }

  public boolean includeCustomMessage() {
    return includeCustomMessage;
  }

  public String getCustomMessage() {
    return customMessage;
  }

  @DataBoundConstructor
  public RocketChatNotifier(final String rocketServerUrl, final boolean trustSSL, final String username, final String password, final String channel, final String buildServerUrl,
                            final boolean startNotification, final boolean notifyAborted, final boolean notifyFailure,
                            final boolean notifyNotBuilt, final boolean notifySuccess, final boolean notifyUnstable, final boolean notifyBackToNormal,
                            final boolean notifyRepeatedFailure, final boolean includeTestSummary, CommitInfoChoice commitInfoChoice,
                            boolean includeCustomMessage, String customMessage) {
    super();
    this.rocketServerUrl = rocketServerUrl;
    this.trustSSL = trustSSL;
    this.username = username;
    this.password = password;
    this.buildServerUrl = buildServerUrl;
    this.channel = channel;
    this.startNotification = startNotification;
    this.notifyAborted = notifyAborted;
    this.notifyFailure = notifyFailure;
    this.notifyNotBuilt = notifyNotBuilt;
    this.notifySuccess = notifySuccess;
    this.notifyUnstable = notifyUnstable;
    this.notifyBackToNormal = notifyBackToNormal;
    this.notifyRepeatedFailure = notifyRepeatedFailure;
    this.includeTestSummary = includeTestSummary;
    this.commitInfoChoice = commitInfoChoice;
    this.includeCustomMessage = includeCustomMessage;
    this.customMessage = customMessage;
  }

  public BuildStepMonitor getRequiredMonitorService() {
    return BuildStepMonitor.NONE;
  }

  public RocketClient newRocketChatClient(AbstractBuild r, BuildListener listener) throws IOException {
    String serverUrl = this.rocketServerUrl;
    if (StringUtils.isEmpty(serverUrl)) {
      serverUrl = getDescriptor().getRocketServerUrl();
    }
    String username = this.username;
    if (StringUtils.isEmpty(username)) {
      username = getDescriptor().getUsername();
    }
    String password = this.password;
    if (StringUtils.isEmpty(password)) {
      password = getDescriptor().getPassword();
    }
    String channel = this.channel;
    if (StringUtils.isEmpty(channel)) {
      channel = getDescriptor().getChannel();
    }

    EnvVars env = null;
    try {
      env = r.getEnvironment(listener);
    } catch (Exception e) {
      listener.getLogger().println("Error retrieving environment vars: " + e.getMessage());
      env = new EnvVars();
    }
    serverUrl = env.expand(serverUrl);
    username = env.expand(username);
    password = env.expand(password);

    return new RocketClientImpl(serverUrl, trustSSL, username, password, channel);
  }

  @Override
  public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
    Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
    for (Publisher publisher : map.values()) {
      if (publisher instanceof RocketChatNotifier) {
        LOGGER.info("Invoking Completed...");
        new ActiveNotifier((RocketChatNotifier) publisher, listener).completed(build);
      }
    }
    return true;
  }

  @Override
  public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
    if (startNotification) {
      Map<Descriptor<Publisher>, Publisher> map = build.getProject().getPublishersList().toMap();
      for (Publisher publisher : map.values()) {
        if (publisher instanceof RocketChatNotifier) {
          LOGGER.info("Invoking Started...");
          new ActiveNotifier((RocketChatNotifier) publisher, listener).started(build);
        }
      }
    }
    return super.prebuild(build, listener);
  }

  @Extension
  public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

    private String rocketServerUrl;
    private boolean trustSSL;
    private String username;
    private String password;
    private String channel;
    private String buildServerUrl;

    public static final CommitInfoChoice[] COMMIT_INFO_CHOICES = CommitInfoChoice.values();

    public DescriptorImpl() {
      load();
    }

    public String getRocketServerUrl() {
      return rocketServerUrl;
    }

    public boolean isTrustSSL() {
      return trustSSL;
    }

    public String getUsername() {
      return username;
    }

    public String getPassword() {
      return password;
    }

    public String getChannel() {
      return channel;
    }

    public String getBuildServerUrl() {
      if (buildServerUrl == null || buildServerUrl.equalsIgnoreCase("")) {
        JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
        return jenkinsConfig.getUrl();
      } else {
        return buildServerUrl;
      }
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      return true;
    }

    @Override
    public RocketChatNotifier newInstance(StaplerRequest sr, JSONObject json) {
      if (sr != null && json != null) {
        String rocketServerUrl = sr.getParameter("rocketServerUrl");
        boolean trustSSL = BooleanUtils.toBoolean(sr.getParameter("trustSSL"));
        String username = sr.getParameter("rocketUsername");
        String password = sr.getParameter("rocketPassword");
        String channel = sr.getParameter("rocketChannel");
        boolean startNotification = "true".equals(sr.getParameter("rocketStartNotification"));
        boolean notifySuccess = "true".equals(sr.getParameter("rocketNotifySuccess"));
        boolean notifyAborted = "true".equals(sr.getParameter("rocketNotifyAborted"));
        boolean notifyNotBuilt = "true".equals(sr.getParameter("rocketNotifyNotBuilt"));
        boolean notifyUnstable = "true".equals(sr.getParameter("rocketNotifyUnstable"));
        boolean notifyFailure = "true".equals(sr.getParameter("rocketNotifyFailure"));
        boolean notifyBackToNormal = "true".equals(sr.getParameter("rocketNotifyBackToNormal"));
        boolean notifyRepeatedFailure = "true".equals(sr.getParameter("rocketNotifyRepeatedFailure"));
        boolean includeTestSummary = "true".equals(sr.getParameter("includeTestSummary"));
        CommitInfoChoice commitInfoChoice = CommitInfoChoice.forDisplayName(sr.getParameter("rocketCommitInfoChoice"));
        boolean includeCustomMessage = "on".equals(sr.getParameter("includeCustomMessage"));
        String customMessage = sr.getParameter("customMessage");
        return new RocketChatNotifier(rocketServerUrl, trustSSL, username, password, channel, buildServerUrl, startNotification, notifyAborted,
          notifyFailure, notifyNotBuilt, notifySuccess, notifyUnstable, notifyBackToNormal, notifyRepeatedFailure,
          includeTestSummary, commitInfoChoice, includeCustomMessage, customMessage);
      }
      return null;
    }

    @Override
    public boolean configure(StaplerRequest sr, JSONObject formData) throws FormException {
      rocketServerUrl = sr.getParameter("rocketServerUrl");
      trustSSL = BooleanUtils.toBoolean(sr.getParameter("trustSSL"));
      username = sr.getParameter("rocketUsername");
      password = sr.getParameter("rocketPassword");
      channel = sr.getParameter("rocketChannel");
      buildServerUrl = sr.getParameter("rocketBuildServerUrl");
      if (buildServerUrl == null || buildServerUrl.equalsIgnoreCase("")) {
        JenkinsLocationConfiguration jenkinsConfig = new JenkinsLocationConfiguration();
        buildServerUrl = jenkinsConfig.getUrl();
      }
      if (buildServerUrl != null && !buildServerUrl.endsWith("/")) {
        buildServerUrl = buildServerUrl + "/";
      }
      save();
      return super.configure(sr, formData);
    }

    @Override
    public String getDisplayName() {
      return "RocketChat Notifications";
    }

    public FormValidation doTestConnection(@QueryParameter("rocketServerUrl") final String rocketServerUrl,
                                           @QueryParameter("trustSSL") final String trustSSL,
                                           @QueryParameter("rocketUsername") final String username,
                                           @QueryParameter("rocketPassword") final String password,
                                           @QueryParameter("rocketChannel") final String channel,
                                           @QueryParameter("rocketBuildServerUrl") final String buildServerUrl) throws FormException {
      try {
        String targetServerUrl = rocketServerUrl + RocketClientImpl.API_PATH;
        if (StringUtils.isEmpty(rocketServerUrl)) {
          targetServerUrl = this.rocketServerUrl;
        }
        boolean targetTrustSSL = this.trustSSL;
        if (StringUtils.isNotEmpty(trustSSL)) {
          targetTrustSSL = BooleanUtils.toBoolean(trustSSL);
        }
        String targetUsername = username;
        if (StringUtils.isEmpty(targetUsername)) {
          targetUsername = this.username;
        }
        String targetPassword = password;
        if (StringUtils.isEmpty(targetPassword)) {
          targetPassword = this.password;
        }
        String targetChannel = channel;
        if (StringUtils.isEmpty(targetChannel)) {
          targetChannel = this.channel;
        }
        String targetBuildServerUrl = buildServerUrl;
        if (StringUtils.isEmpty(targetBuildServerUrl)) {
          targetBuildServerUrl = this.buildServerUrl;
        }
        RocketClient rocketChatClient = new RocketClientImpl(targetServerUrl, targetTrustSSL, targetUsername, targetPassword, targetChannel);
        String message = "RocketChat/Jenkins plugin: you're all set on " + targetBuildServerUrl;
        LOGGER.fine("Start validating config");
        rocketChatClient.validate();
        LOGGER.fine("Done validating config");
        LOGGER.fine("Start publishing message");
        rocketChatClient.publish(message);
        LOGGER.fine("Done publishing message");
        return FormValidation.ok("Success");
      } catch (ValidatorException e) {
        LOGGER.log(Level.SEVERE, "SSL error during trying to send rocket message", e);
        return FormValidation.error(e, "SSL error", e);
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, "Client error during trying to send rocket message", e);
        return FormValidation.error(e, "Client error - Could not send message");
      }
    }
  }

  @Deprecated
  public static class RocketJobProperty extends hudson.model.JobProperty<AbstractProject<?, ?>> {

    private String rocketServerUrl;
    private String username;
    private String password;
    private String channel;
    private boolean trustSSL;
    private boolean startNotification;
    private boolean notifySuccess;
    private boolean notifyAborted;
    private boolean notifyNotBuilt;
    private boolean notifyUnstable;
    private boolean notifyFailure;
    private boolean notifyBackToNormal;
    private boolean notifyRepeatedFailure;
    private boolean includeTestSummary;
    private boolean showCommitList;
    private boolean includeCustomMessage;
    private String customMessage;

    @DataBoundConstructor
    public RocketJobProperty(String rocketServerUrl,
                             boolean trustSSL,
                             String username,
                             String password,
                             String channel,
                             boolean startNotification,
                             boolean notifyAborted,
                             boolean notifyFailure,
                             boolean notifyNotBuilt,
                             boolean notifySuccess,
                             boolean notifyUnstable,
                             boolean notifyBackToNormal,
                             boolean notifyRepeatedFailure,
                             boolean includeTestSummary,
                             boolean showCommitList,
                             boolean includeCustomMessage,
                             String customMessage) {
      this.rocketServerUrl = rocketServerUrl;
      this.trustSSL = trustSSL;
      this.username = username;
      this.password = password;
      this.channel = channel;
      this.startNotification = startNotification;
      this.notifyAborted = notifyAborted;
      this.notifyFailure = notifyFailure;
      this.notifyNotBuilt = notifyNotBuilt;
      this.notifySuccess = notifySuccess;
      this.notifyUnstable = notifyUnstable;
      this.notifyBackToNormal = notifyBackToNormal;
      this.notifyRepeatedFailure = notifyRepeatedFailure;
      this.includeTestSummary = includeTestSummary;
      this.showCommitList = showCommitList;
      this.includeCustomMessage = includeCustomMessage;
      this.customMessage = customMessage;
    }

    @Exported
    public String getRocketServerUrl() {
      return rocketServerUrl;
    }

    @Exported
    public boolean isTrustSSL() {
      return trustSSL;
    }

    @Exported
    public String getUsername() {
      return username;
    }

    @Exported
    public String getPassword() {
      return password;
    }

    @Exported
    public String getChannel() {
      return channel;
    }

    @Exported
    public boolean getStartNotification() {
      return startNotification;
    }

    @Exported
    public boolean getNotifySuccess() {
      return notifySuccess;
    }

    @Exported
    public boolean getShowCommitList() {
      return showCommitList;
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> build, BuildListener listener) {
      return super.prebuild(build, listener);
    }

    @Exported
    public boolean getNotifyAborted() {
      return notifyAborted;
    }

    @Exported
    public boolean getNotifyFailure() {
      return notifyFailure;
    }

    @Exported
    public boolean getNotifyNotBuilt() {
      return notifyNotBuilt;
    }

    @Exported
    public boolean getNotifyUnstable() {
      return notifyUnstable;
    }

    @Exported
    public boolean getNotifyBackToNormal() {
      return notifyBackToNormal;
    }

    @Exported
    public boolean includeTestSummary() {
      return includeTestSummary;
    }

    @Exported
    public boolean getNotifyRepeatedFailure() {
      return notifyRepeatedFailure;
    }

    @Exported
    public boolean includeCustomMessage() {
      return includeCustomMessage;
    }

    @Exported
    public String getCustomMessage() {
      return customMessage;
    }

  }
}
