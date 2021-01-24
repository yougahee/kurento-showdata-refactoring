package org.kurento.tutorial.showdatachannel.service;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.tutorial.showdatachannel.utils.SignalingListener;
import org.kurento.tutorial.showdatachannel.utils.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SendMessageService {

	private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

	@Autowired
	private KurentoClient kurento;

	@Autowired
	private UserSession userSession;
	private MediaPipeline pipeline;
	private WebRtcEndpoint webRtcEndpoint;
	private SignalingListener signalingListener;

	@PostConstruct
	protected void init() {
		pipeline = kurento.createMediaPipeline();
		webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).useDataChannels().build();
		signalingListener = new SignalingListener();
	}

	public void start(final WebSocketSession session, JsonObject jsonMessage) {
		try {
			userSession.setMediaPipeline(pipeline);
			userSession.setWebRtcEndpoint(webRtcEndpoint);

			// ## 추가
			webRtcEndpoint.connect(webRtcEndpoint);
			users.put(session.getId(), userSession);

			// ICE candidates
			webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
				@Override
				public void onEvent(IceCandidateFoundEvent iceCandidateFoundEvent) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "iceCandidate");
					response.add("candidate", JsonUtils.toJsonObject(iceCandidateFoundEvent.getCandidate()));
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
				}
			});

			// SDP negotiation (offer and answer)
			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			initWebRtcEndpoint(session, webRtcEndpoint, sdpOffer);

			String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "startResponse");
			response.addProperty("sdpAnswer", sdpAnswer);

			log.info("[Handler::re] SDP Offer from browser to KMS:\n{}", sdpOffer);

			synchronized (session) {
				session.sendMessage(new TextMessage(response.toString()));
			}

			webRtcEndpoint.gatherCandidates();

		} catch (Throwable t) {
			sendError(session, t.getMessage());
		}
	}

	public void receive(final WebSocketSession session, JsonObject jsonMessage) {
		try {
			//MediaPipeline pipeline = kurento.createMediaPipeline();
			//WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).useDataChannels().build();

			userSession.setMediaPipeline(pipeline);
			userSession.setWebRtcEndpoint(webRtcEndpoint);

			// ## 추가
			webRtcEndpoint.connect(webRtcEndpoint);
			users.put(session.getId(), userSession);

			// ICE candidates
			webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
				@Override
				public void onEvent(IceCandidateFoundEvent iceCandidateFoundEvent) {
					JsonObject response = new JsonObject();
					response.addProperty("id", "iceCandidate");
					response.add("candidate", JsonUtils.toJsonObject(iceCandidateFoundEvent.getCandidate()));
					try {
						synchronized (session) {
							session.sendMessage(new TextMessage(response.toString()));
						}
					} catch (IOException e) {
						log.debug(e.getMessage());
					}
				}
			});

			String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
			initWebRtcEndpoint(session, webRtcEndpoint, sdpOffer);

			String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "receiveResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);

			log.info("[Receive Handler::re] SDP Offer from browser to KMS:\n{}", sdpOffer);

			synchronized (session) {
				session.sendMessage(new TextMessage(response.toString()));
			}

			webRtcEndpoint.gatherCandidates();

		} catch (Throwable t) {
			sendError(session, t.getMessage());
		}
	}

	public void sendError(WebSocketSession session, String message) {
		try {
			JsonObject response = new JsonObject();
			response.addProperty("id", "error");
			response.addProperty("message", message);
			session.sendMessage(new TextMessage(response.toString()));
		} catch (IOException e) {
			log.error("Exception sending message", e);
		}
	}

	//stop
	public void stop(final WebSocketSession session) {
		// Remove the user session and release all resources
		userSession = users.remove(session.getId());
		if (userSession != null) {
			MediaPipeline mediaPipeline = userSession.getMediaPipeline();
			if (mediaPipeline != null) {
				log.info("[Handler::stop] Release the Media Pipeline");
				mediaPipeline.release();
			}
		}
	}

	public void stopUserSession(final WebSocketSession session) {
		UserSession user = users.remove(session.getId());
		if (user != null) {
			user.release();
		}
	}

	public void iceCandidate(final WebSocketSession session, JsonObject jsonMessage) {
		log.info("iceCandidate service 함수에 들어왔다.");
		JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

		UserSession user = users.get(session.getId());
		if (user != null) {
			IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
					jsonCandidate.get("sdpMid").getAsString(),
					jsonCandidate.get("sdpMLineIndex").getAsInt());
			user.addCandidate(candidate);
		}
	}

	private void initWebRtcEndpoint(final WebSocketSession session,
	                                final WebRtcEndpoint webRtcEp, String sdpOffer) {
		signalingListener.initBaseEventListeners(session, webRtcEp, "WebRtcEndpoint");
		signalingListener.initWebRtcEventListeners(session, webRtcEp);
	}
}
