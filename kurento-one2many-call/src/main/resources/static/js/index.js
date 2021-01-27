/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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


var signalling_ws = new WebSocket('wss://' + location.host + '/call');
var chatting_ws;

var video;
var webRtcPeer;

var dataChannelSend;
var dataChannelReceive;
var sendButton;

window.onload = function () {
    console = new Console();
    console.log("Page loaded ....");
    video = document.getElementById('video');

    dataChannelSend = document.getElementById('dataChannelSend');
    dataChannelReceive = document.getElementById('dataChannelShow');
    sendButton = document.getElementById('send');

    disableStopButton();
}

window.onbeforeunload = function () {
    signalling_ws.close();
    chatting_ws.close();
}

signalling_ws.onmessage = function (message) {
    var parsedMessage = JSON.parse(message.data);
    console.info('Received message: ' + message.data);

    switch (parsedMessage.id) {
        case 'presenterResponse':
            presenterResponse(parsedMessage);
            break;
        case 'viewerResponse':
            viewerResponse(parsedMessage);
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
                if (error)
                    return console.error('Error adding candidate: ' + error);
            });
            break;
        case 'stopCommunication':
            dispose();
            break;
        /*case 'chatting_connected':
            chattingResponse(parsedMessage);
            break;*/
        case 'error':
            onError("Error message from server: " + parsedMessage.message);
            break;
        default:
            console.error('Unrecognized message', parsedMessage);
    }
};



function presenterResponse(message) {
    if (message.response != 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        console.log("presenterResponse");
        webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
            if (error)
                return console.error(error);
        });
    }
}

function viewerResponse(message) {
    if (message.response != 'accepted') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {

        console.log("viewerResponse");
        webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
            if (error)
                return console.error("viewer Response Error : " + error);
        });
    }
}
/*

function chattingResponse(message) {
    if (message.response != 'succeess') {
        var errorMsg = message.message ? message.message : 'Unknow error';
        console.info('Call not accepted for the following reason: ' + errorMsg);
        dispose();
    } else {
        console.log("chatting Response");
        //roomIdx 보내주기
        var message = {
            id: 'roomIdx',
            roomIdx: 1
        };
        sendMessageToChatting(message);
    }
}
*/

var chanId = 0;

function getChannelName() {
    return "TestChannel" + chanId++;
}

const configuration = {
    'iceServers': [
        {
            'urls': 'stun:stun.l.google.com:19302'
        },
        {
            'urls': 'turn:117.17.196.61:3478',
            'username': 'testuser',
            'credential': 'root',
        }
    ]
};

function presenter() {
    console.log("Starting Presenter ...");
    console.log("chatting websocket starting ....");
    chatting_ws = new WebSocket('ws://localhost:8002/chatting');

    if (!webRtcPeer) {
        showSpinner(video);

        sendButton.addEventListener("click", function () {
            var data = dataChannelSend.value;
            console.log("Send button pressed. Sending data " + data);

            // ## HTTP 통신 ( API Gateway로 가야 함 )
            onButtonClicked();
            console.log("[Presenter] message send to chatting server : " + data);
            function onButtonClicked() {
                axios({
                    method: "POST",
                    url: "http://localhost:8002/send/message",
                    data: {
                        roomIdx: 1,
                        userType: "presenter",
                        textMessage: data
                    }
                })
                    .then((res) => {
                        console.log(res);
                        if(res.data.code === 200) {
                            dataChannelReceive.value += data + "\n";
                        } else {
                            console.log("error : data not send");
                        }
                    })
                    .catch({
                        function (error) {
                            console.log("error : " + error);
                        }
                    })
            }

            //webRtcPeer.send(data);
            dataChannelSend.value = "";
        });

        chatting_ws.onopen = function (e) {
            console.log("chatting_ws open!!!");

            // ## 이 부분은 chatting Server
            //roomIdx 보내주기
            var message = {
                id: 'roomIdx',
                roomIdx: 1
            };

            function sendMessageToChatting(message) {
                var jsonMessage = JSON.stringify(message);
                console.log('Sending message: ' + jsonMessage);
                chatting_ws.send(jsonMessage);
            }

            sendMessageToChatting(message);
        };

        chatting_ws.onmessage = function (message) {
            var parsedMessage = JSON.parse(message.data);
            console.info('Chatting Received message: ' + message.data);

            switch (parsedMessage.id) {
                case 'sendChatting':
                    console.log("chatting ws message : " + parsedMessage);
                    console.log("chatting Detail Message : " + parsedMessage.from + " " + parsedMessage.message);

                    var data = parsedMessage.from + " : " + parsedMessage.message;
                    dataChannelReceive.value += data + "\n";
                    webRtcPeer.send(data);
                    break;
                case 'error':
                    onError("Error message from server: " + parsedMessage.message);
                    break;
                default:
                    console.error('Unrecognized message', parsedMessage);
            }
        };

        chatting_ws.onclose = function (e) {
            console.log("closing chatting ws : " );
        };

        function onOpen(event) {
            console.log("open");
            dataChannelSend.disabled = false;
            dataChannelSend.focus();

            dataChannelReceive.disabled = false;
            $('#send').attr('disabled', false);
        }

        function onMessage(event) {
            console.log("onMessage Received data : " + event["data"]);
            dataChannelReceive.focus();
            dataChannelReceive.value += event["data"] + "\n";
        }

        function onClosed(event) {
            console.log("onClosed Event");
            dataChannelReceive.disabled = true;
            dataChannelSend.disabled = true;
            dataChannelReceive.disabled = true;
            $('#send').attr('disabled', true);
        }

        function onError(event) {
            console.log("DataChannel Error")
        }

        const constraints = {
            audio: true,
            video: true
        };

        const options = {
            localVideo: video,
            dataChannelConfig: {
                id: getChannelName(),
                onopen: onOpen,
                onmessage: onMessage,
                onclose: onClosed,
                onerror: onError
            },
            dataChannels: true,
            onicecandidate: onIceCandidate,
            configuration: configuration,
            mediaConstraints: constraints
        };

        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
            function (error) {
                if (error) {
                    return console.error(error);
                }
                webRtcPeer.generateOffer(onOfferPresenter);
            });

        enableStopButton();
    }
}

function onOfferPresenter(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'presenter',
        sdpOffer: offerSdp
    };
    sendMessageToSignaling(message);
}

function viewer() {

    if (!webRtcPeer) {

        console.log("viewer starting ... ");
        showSpinner(video);

        sendButton.addEventListener("click", function () {
            var data = dataChannelSend.value;
            console.log("Send button pressed. Sending data " + data);

            onButtonClicked();

            console.log("[Viewer] message send to chatting server : " + data);

            function onButtonClicked() {
                axios({
                    method: "POST",
                    url: "http://localhost:8002/send/message",
                    data: {
                        roomIdx: 1,
                        textMessage: data
                    }
                })
                    .then((res) => {
                        console.log(res);
                        if(res.data.code === 200) {
                            console.log("success : data send Success!!!")
                        } else {
                            console.log("error : data not send");
                        }
                    })
                    .catch({
                        function (error) {
                            console.log("error : " + error);
                        }
                    })
            }

            //webRtcPeer.send(data);
            dataChannelSend.value = "";
        });

        function onOpen(event) {
            console.log("open");
            dataChannelSend.disabled = false;
            dataChannelSend.focus();
            dataChannelReceive.disabled = false;
            $('#send').attr('disabled', false);
        }

        function onMessage(event) {
            console.log("onMessage Received data : " + event["data"]);
            dataChannelReceive.focus();
            dataChannelReceive.value += event["data"] + "\n";
        }

        function onClosed(event) {
            console.log("onClosed Event");
            dataChannelReceive.disabled = true;
            dataChannelSend.disabled = true;
            dataChannelReceive.disabled = true;
            $('#send').attr('disabled', true);
        }

        console.log("Creating WebRtcPeer and generating local sdp offer ...");

        const constraints = {
            audio: true,
            video: true
        };

        const options = {
            remoteVideo: video,
            dataChannels: true,
            dataChannelConfig: {
                id: getChannelName(),
                onopen: onOpen,
                onmessage: onMessage,
                onclose: onClosed
            },
            onicecandidate: onIceCandidate,
            configuration: configuration,
            mediaConstraints: constraints
        };

        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
            function (error) {
                if (error) {
                    return console.error(error);
                }
                this.generateOffer(onOfferViewer);
            });

        console.log(webRtcPeer);
        enableStopButton();
    }
}

function onOfferViewer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id: 'viewer',
        sdpOffer: offerSdp
    }
    sendMessageToSignaling(message);
}

function onIceCandidate(candidate) {
    console.log("Local candidate" + JSON.stringify(candidate));

    const message = {
        id: 'onIceCandidate',
        candidate: candidate
    };
    sendMessageToSignaling(message);
}

function stop() {
    var message = {
        id: 'stop'
    }
    sendMessageToSignaling(message);
    dispose();
}

function dispose() {
    if (webRtcPeer) {
        webRtcPeer.dispose();
        webRtcPeer = null;
    }
    hideSpinner(video);

    disableStopButton();
}

function disableStopButton() {
    enableButton('#presenter', 'presenter()');
    enableButton('#viewer', 'viewer()');
    disableButton('#stop');
    disableButton('#send');
}

function enableStopButton() {
    disableButton('#presenter');
    disableButton('#viewer');
    enableButton('#send');
    enableButton('#stop', 'stop()');
}

function onError(error) {
    console.error(error);
}

function disableButton(id) {
    $(id).attr('disabled', true);
    $(id).removeAttr('onclick');
}

function enableButton(id, functionName) {
    $(id).attr('disabled', false);
    $(id).attr('onclick', functionName);
}

function sendMessageToSignaling(message) {
    var jsonMessage = JSON.stringify(message);
    console.log('Sending message: ' + jsonMessage);
    signalling_ws.send(jsonMessage);
}



function showSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].poster = './img/transparent-1px.png';
        arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
    }
}

function hideSpinner() {
    for (var i = 0; i < arguments.length; i++) {
        arguments[i].src = '';
        arguments[i].poster = './img/webrtc.png';
        arguments[i].style.background = '';
    }
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function (event) {
    event.preventDefault();
    $(this).ekkoLightbox();
});
