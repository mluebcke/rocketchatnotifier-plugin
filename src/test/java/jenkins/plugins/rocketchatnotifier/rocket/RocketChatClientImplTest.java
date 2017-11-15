package jenkins.plugins.rocketchatnotifier.rocket;

import jenkins.plugins.rocketchatnotifier.model.Info;
import jenkins.plugins.rocketchatnotifier.model.Response;
import jenkins.plugins.rocketchatnotifier.model.Room;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(PowerMockRunner.class)
@PrepareForTest({RocketChatClientCallBuilder.class, RocketChatClientImpl.class})
public class RocketChatClientImplTest {

  private RocketChatClientImpl rocketChatClient;

  @Mock
  private RocketChatClientCallBuilder callBuilder;

  @Before
  public void setup() throws Exception {
    callBuilder = mock(RocketChatClientCallBuilder.class);
    PowerMockito.whenNew(RocketChatClientCallBuilder.class).withArguments(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString()).thenReturn(callBuilder);
    rocketChatClient = new RocketChatClientImpl("", false, "", "");
  }

  @Test
  public void shouldSendMessageToRoom() throws Exception {
    // given
    final Room room = new Room();
    room.setName("room");
    mockSuccess();
    // when
    rocketChatClient.send(room, "message");
    // then no error
  }

  @Test
  public void shouldWorkWithEmojiAndNoAvatarEmptyAttachments() throws Exception {
    // given
    mockSuccess();
    // when
    rocketChatClient.send("room", "message", "test", null);
    // then no error
  }

  @Test
  public void shouldWorkWithEmojiAndAvatarEmptyAttachments() throws Exception {
    // given
    mockSuccess();
    // when
    rocketChatClient.send("room", "message", "test", "avatar");
    // then no error
  }

  @Test
  public void shouldWorkWithNoEmojiAndNoAvatarEmptyAttachments() throws Exception {
    // given
    mockSuccess();
    // when
    rocketChatClient.send("room", "message");
    // then no error
  }

  @Test
  public void shouldWorkWithEmojiAndAvatarAndAttachments() throws Exception {
    // given
    mockSuccess();
    final List<Map<String, Object>> attachements = new ArrayList<Map<String, Object>>();
    attachements.add(new HashMap<String, Object>());
    // when
    rocketChatClient.send("room", "message", "test", "avatar", attachements);
    // then no error
  }

  @Test(expected = IOException.class)
  public void shouldFailOnEmptyResponseOnVersionInfo() throws Exception {
    // given
    final Response errorResponse = new Response();
    errorResponse.setSuccess(false);
    when(callBuilder.buildCall(RocketChatRestApiV1.Info)).thenReturn(errorResponse);
    // when
    rocketChatClient.getInfo();
    // then error
  }

  @Test(expected = IOException.class)
  public void shouldFailOnEmptyResponseOnSendMessage() throws Exception {
    // given
    final Response infoResponse = new Response();
    infoResponse.setSuccess(true);
    final Info info = new Info();
    info.setVersion("0.52.1");
    infoResponse.setInfo(info);
    final Response errorResponse = new Response();
    errorResponse.setSuccess(false);
    when(callBuilder.buildCall(RocketChatRestApiV1.Info)).thenReturn(infoResponse);
    when(callBuilder.buildCall(any(RocketChatRestApiV1.class), any(RocketChatQueryParams.class), any(Map.class))).thenReturn(errorResponse);
    // when
    rocketChatClient.send("room", "message", null, null, null);
    // then error
  }

  private Response mockSuccess() throws Exception {
    final Response infoResponse = new Response();
    infoResponse.setSuccess(true);
    final Info info = new Info();
    info.setVersion("0.52.1");
    infoResponse.setInfo(info);
    final Response response = new Response();
    response.setSuccess(true);
    when(callBuilder.buildCall(RocketChatRestApiV1.Info)).thenReturn(infoResponse);
    when(callBuilder.buildCall(any(RocketChatRestApiV1.class), any(RocketChatQueryParams.class), any(Map.class))).thenReturn(response);

    return response;
  }


}
