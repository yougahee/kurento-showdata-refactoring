/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.tutorial.showdatachannel.handler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.module.datachannelexample.KmsShowData;
import org.kurento.tutorial.showdatachannel.service.SendMessageService;
import org.kurento.tutorial.showdatachannel.utils.SignalingListener;
import org.kurento.tutorial.showdatachannel.utils.UserSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import javax.annotation.PostConstruct;

/**
 * Show Data Channel handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @since 6.1.1
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class ShowDataChannelHandler extends TextWebSocketHandler {

	private static final Gson gson = new GsonBuilder().create();
	private final ConcurrentHashMap<String, UserSession> users = new ConcurrentHashMap<>();

	@Autowired
	private KurentoClient kurento;

	@Autowired
	private UserSession userSession;
	private MediaPipeline pipeline;
	private WebRtcEndpoint webRtcEndpoint;
	private SignalingListener signalingListener;

	private final SendMessageService sendMessageService;

	@PostConstruct
	protected void init() {
		log.info("init 들어오기 ");
		pipeline = kurento.createMediaPipeline();
		webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).useDataChannels().build();
		signalingListener = new SignalingListener();
		//sendMessageService = new SendMessageService();
	}

	//ws 연결 로그
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		log.info("[Handler::afterConnectionEstablished] New WebSocket connection, sessionId: {}", session.getId());
	}

	//ws 연결 끊겼을 경우 로그
	@Override
	public void afterConnectionClosed(final WebSocketSession session, CloseStatus status) throws Exception {
		if (!status.equalsCode(CloseStatus.NORMAL)) {
			log.warn("[Handler::afterConnectionClosed] status: {}, sessionId: {}", status, session.getId());
		}
		stop(session);
	}

	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

		log.info("Incoming message: {}", jsonMessage);
		log.debug("Incoming message: {}", jsonMessage);

		switch (jsonMessage.get("id").getAsString()) {
			case "start":
				start(session, jsonMessage);
				//sendMessageService.start(session, jsonMessage);
				break;
			case "stop": {
				UserSession user = users.remove(session.getId());
				if (user != null) {
					user.release();
				}
				//sendMessageService.stopUserSession(session);
				break;
			}
			case "receive":
				receive(session, jsonMessage);
				//sendMessageService.receive(session, jsonMessage);
				break;
			case "onIceCandidate": {
				//sendMessageService.iceCandidate(session, jsonMessage);
				JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

				UserSession user = users.get(session.getId());
				if (user != null) {
					IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
							jsonCandidate.get("sdpMid").getAsString(),
							jsonCandidate.get("sdpMLineIndex").getAsInt());
					user.addCandidate(candidate);
				}
				break;
			}
			default:
				sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
				break;
		}
	}

	private void start(final WebSocketSession session, JsonObject jsonMessage) {
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

			//Media Logic
			KmsShowData kmsShowData = new KmsShowData.Builder(pipeline).build();

			webRtcEndpoint.connect(kmsShowData);
			kmsShowData.connect(webRtcEndpoint);

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

	private void receive(final WebSocketSession session, JsonObject jsonMessage) {
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

	private void sendError(WebSocketSession session, String message) {
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
	private void stop(final WebSocketSession session) {
		// Remove the user session and release all resources
		userSession = users.remove(session.getId());
		if (userSession != null) {
			//MediaPipeline mediaPipeline = userSession.getMediaPipeline();
			if (pipeline != null) {
				log.info("[Handler::stop] Release the Media Pipeline");
				pipeline.release();
			}
		}
	}

	private void initWebRtcEndpoint(final WebSocketSession session,
	                                final WebRtcEndpoint webRtcEp, String sdpOffer) {
		signalingListener.initBaseEventListeners(session, webRtcEp, "WebRtcEndpoint");
		signalingListener.initWebRtcEventListeners(session, webRtcEp);
	}
}
