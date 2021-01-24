[![License badge](https://img.shields.io/badge/license-Apache2-orange.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![Documentation badge](https://readthedocs.org/projects/fiware-orion/badge/?version=latest)](http://doc-kurento.readthedocs.org/en/latest/)
[![Docker badge](https://img.shields.io/docker/pulls/fiware/orion.svg)](https://hub.docker.com/r/fiware/stream-oriented-kurento/)
[![Support badge]( https://img.shields.io/badge/support-sof-yellowgreen.svg)](http://stackoverflow.com/questions/tagged/kurento)

[![][KurentoImage]][Kurento]

Copyright © 2013-2016 [Kurento]. Licensed under [Apache 2.0 License].

kurento-show-data-channel
=========================

Kurento Java Tutorial: WebRTC in loopback with filter to show data received
through data channel.

This tutorial shows how media server can receive data sent by browser through
datachannels. Any message sent by browser will be displayed  in the loopback
video.

This tutorial requires media server module kms-datachannelexample. You can
install this module with command `sudo apt-get install kms-datachannelexample`.


Kurneto-show-data-channel Refactoring 
=====================================
by gahuiyu


FLOW
----

1. Web에 접속하자마자 클라이언트, 시그널링 서버 웹소켓 연결 
2. WebSocket이 연결되고 웹 페이지에서 start 버튼을 누른다.   
   2-1. start 버튼을 누르면 js파일의 start()로 들어가고 WebRtcPeer를 연결해달라는 {"id": 'start', "sdpOffer" : 'sdpOffer'} json을 서버에게 send 한다. 
3. 서버는 handlerTextMessage에서 jsonMessage를 받았다.  
    3-1. jsonMessage의 id를 통해 start 임을 알 수 있고 sdpOffer을 통해 어떤 방식으로 WebRTC를 연결할지(어떻게 미디어를 보낼지 sdp 프로토콜)확인하고 Answer을 보내준다. 
    3-3. jsonMessage의 sdpOffer을 사용하여 WebRtcEndpoint를 설정해준다. (어떤 방식으로 미디어를 전송할 것인지)
4. 클라이언트는 startResponse를 받았고, ICE Candidate를 찾아서 연결을 시켜준다. 
5. 연결할 수 있는 ICE Candidate를 찾으면 pipeLine에 연결이 되고 dataChannel 도 열린다. 
6. 


파이프라인이 삭제되면?
- 연결되어 있는 모든 피어들이 삭제되나?
- 
- 자동으로는 안되고 Redis에 집어넣어져 있으면 그 정보가 없어짐




TEST IMAGE
==========









Running this tutorial
---------------------

In order to run this tutorial, please read the following [instructions].

What is Kurento
---------------

Kurento is an open source software project providing a platform suitable
for creating modular applications with advanced real-time communication
capabilities. For knowing more about Kurento, please visit the Kurento
project website: http://www.kurento.org.

Kurento is part of [FIWARE]. For further information on the relationship of
FIWARE and Kurento check the [Kurento FIWARE Catalog Entry]

Kurento is part of the [NUBOMEDIA] research initiative.

Documentation
-------------

The Kurento project provides detailed [documentation] including tutorials,
installation and development guides. A simplified version of the documentation
can be found on [readthedocs.org]. The [Open API specification] a.k.a. Kurento
Protocol is also available on [apiary.io].

Source
------

Code for other Kurento projects can be found in the [GitHub Kurento Group].

News and Website
----------------

Check the [Kurento blog]
Follow us on Twitter @[kurentoms].

Issue tracker
-------------

Issues and bug reports should be posted to the [GitHub Kurento bugtracker]

Licensing and distribution
--------------------------

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Contribution policy
-------------------

You can contribute to the Kurento community through bug-reports, bug-fixes, new
code or new documentation. For contributing to the Kurento community, drop a
post to the [Kurento Public Mailing List] providing full information about your
contribution and its value. In your contributions, you must comply with the
following guidelines

* You must specify the specific contents of your contribution either through a
  detailed bug description, through a pull-request or through a patch.
* You must specify the licensing restrictions of the code you contribute.
* For newly created code to be incorporated in the Kurento code-base, you must
  accept Kurento to own the code copyright, so that its open source nature is
  guaranteed.
* You must justify appropriately the need and value of your contribution. The
  Kurento project has no obligations in relation to accepting contributions
  from third parties.
* The Kurento project leaders have the right of asking for further
  explanations, tests or validations of any code contributed to the community
  before it being incorporated into the Kurento code-base. You must be ready to
  addressing all these kind of concerns before having your code approved.

Support
-------

The Kurento project provides community support through the  [Kurento Public
Mailing List] and through [StackOverflow] using the tags *kurento* and
*fiware-kurento*.

Before asking for support, please read first the [Kurento Netiquette Guidelines]

[documentation]: http://www.kurento.org/documentation
[FIWARE]: http://www.fiware.org
[GitHub Kurento bugtracker]: https://github.com/Kurento/bugtracker/issues
[GitHub Kurento Group]: https://github.com/kurento
[kurentoms]: http://twitter.com/kurentoms
[Kurento]: http://kurento.org
[Kurento Blog]: http://www.kurento.org/blog
[Kurento FIWARE Catalog Entry]: http://catalogue.fiware.org/enablers/stream-oriented-kurento
[Kurento Netiquette Guidelines]: http://www.kurento.org/blog/kurento-netiquette-guidelines
[Kurento Public Mailing list]: https://groups.google.com/forum/#!forum/kurento
[KurentoImage]: https://secure.gravatar.com/avatar/21a2a12c56b2a91c8918d5779f1778bf?s=120
[Apache 2.0 License]: http://www.apache.org/licenses/LICENSE-2.0
[NUBOMEDIA]: http://www.nubomedia.eu
[StackOverflow]: http://stackoverflow.com/search?q=kurento
[Read-the-docs]: http://read-the-docs.readthedocs.org/
[readthedocs.org]: http://kurento.readthedocs.org/
[Open API specification]: http://kurento.github.io/doc-kurento/
[apiary.io]: http://docs.streamoriented.apiary.io/
[instructions]: http://www.kurento.org/docs/current/tutorials/java/tutorial-show-datachannel.html
