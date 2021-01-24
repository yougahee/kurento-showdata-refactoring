package org.kurento.tutorial.showdatachannel.utils;

import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.tutorial.showdatachannel.service.SendMessageService;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

@Slf4j
public class SignalingListener {

	private SendMessageService sendMessageService = new SendMessageService();

	public void initBaseEventListeners(final WebSocketSession session,
	                                          BaseRtpEndpoint baseRtpEp, final String className) {
		log.info("[Handler::initBaseEventListeners] name: {}, class: {}, sessionId: {}",
				baseRtpEp.getName(), className, session.getId());

		// Event: Some error happened
		baseRtpEp.addErrorListener(new org.kurento.client.EventListener<ErrorEvent>() {
			@Override
			public void onEvent(ErrorEvent ev) {
				log.error("[{}::ErrorEvent] Error code {}: '{}', source: {}, timestamp: {}, tags: {}, description: {}",
						className, ev.getErrorCode(), ev.getType(), ev.getSource().getName(),
						ev.getTimestamp(), ev.getTags(), ev.getDescription());

				sendMessageService.sendError(session, "[Kurento] " + ev.getDescription());
				sendMessageService.stop(session);
			}
		});

		// Event: Media is flowing into this sink
		baseRtpEp.addMediaFlowInStateChangeListener(new org.kurento.client.EventListener<MediaFlowInStateChangeEvent>() {
			@Override
			public void onEvent(MediaFlowInStateChangeEvent ev) {
				log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
						className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
			}
		});

		// Event: Media is flowing out of this source
		baseRtpEp.addMediaFlowOutStateChangeListener(new org.kurento.client.EventListener<MediaFlowOutStateChangeEvent>() {
			@Override
			public void onEvent(MediaFlowOutStateChangeEvent ev) {
				log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, padName: {}, mediaType: {}",
						className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), ev.getState(), ev.getPadName(), ev.getMediaType());
			}
		});

		// Event: [TODO write meaning of this event]
		baseRtpEp.addConnectionStateChangedListener(new org.kurento.client.EventListener<ConnectionStateChangedEvent>() {
			@Override
			public void onEvent(ConnectionStateChangedEvent ev) {
				log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
						className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), ev.getOldState(), ev.getNewState());
			}
		});

		// Event: [TODO write meaning of this event]
		baseRtpEp.addMediaStateChangedListener(new org.kurento.client.EventListener<MediaStateChangedEvent>() {
			@Override
			public void onEvent(MediaStateChangedEvent ev) {
				log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, oldState: {}, newState: {}",
						className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), ev.getOldState(), ev.getNewState());
			}
		});

		// Event: This element will (or will not) perform media transcoding
		baseRtpEp.addMediaTranscodingStateChangeListener(new org.kurento.client.EventListener<MediaTranscodingStateChangeEvent>() {
			@Override
			public void onEvent(MediaTranscodingStateChangeEvent ev) {
				log.info("[{}::{}] source: {}, timestamp: {}, tags: {}, state: {}, binName: {}, mediaType: {}",
						className, ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), ev.getState(), ev.getBinName(), ev.getMediaType());
			}
		});
	}

	public void initWebRtcEventListeners(final WebSocketSession session,
	                                      final WebRtcEndpoint webRtcEp) {
		log.info("[Handler::initWebRtcEventListeners] name: {}, sessionId: {}",
				webRtcEp.getName(), session.getId());

		// Event: The ICE backend found a local candidate during Trickle ICE
		webRtcEp.addIceCandidateFoundListener(new org.kurento.client.EventListener<IceCandidateFoundEvent>() {
			@Override
			public void onEvent(IceCandidateFoundEvent ev) {
				log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, candidate: {}",
						ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), JsonUtils.toJson(ev.getCandidate()));

				JsonObject message = new JsonObject();
				message.addProperty("id", "iceCandidate");
				message.add("candidate", JsonUtils.toJsonObject(ev.getCandidate()));

				try {
					session.sendMessage(new TextMessage(message.toString()));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		// Event: The ICE backend changed state
		webRtcEp.addIceComponentStateChangeListener(new org.kurento.client.EventListener<IceComponentStateChangeEvent>() {
			@Override
			public void onEvent(IceComponentStateChangeEvent ev) {
				log.debug("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}, streamId: {}, componentId: {}, state: {}",
						ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), ev.getStreamId(), ev.getComponentId(), ev.getState());
			}
		});

		// Event: The ICE backend finished gathering ICE candidates
		webRtcEp.addIceGatheringDoneListener(new org.kurento.client.EventListener<IceGatheringDoneEvent>() {
			@Override
			public void onEvent(IceGatheringDoneEvent ev) {
				log.info("[WebRtcEndpoint::{}] source: {}, timestamp: {}, tags: {}",
						ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags());
			}
		});

		// Event: The ICE backend selected a new pair of ICE candidates for use
		webRtcEp.addNewCandidatePairSelectedListener(new org.kurento.client.EventListener<NewCandidatePairSelectedEvent>() {
			@Override
			public void onEvent(NewCandidatePairSelectedEvent ev) {
				log.info("[WebRtcEndpoint::{}] name: {}, timestamp: {}, tags: {}, streamId: {}, local: {}, remote: {}",
						ev.getType(), ev.getSource().getName(), ev.getTimestamp(),
						ev.getTags(), ev.getCandidatePair().getStreamID(),
						ev.getCandidatePair().getLocalCandidate(),
						ev.getCandidatePair().getRemoteCandidate());
			}
		});
	}
}
