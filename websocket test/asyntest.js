    var websocket;

    function getRootUri() {
//        return "ws://41.185.29.140:8080/lift_club";
 return "ws://41.185.29.140:8080/lift_club";
    }

  function init() {
       
        websocket = new WebSocket(getRootUri() + "/async/AsyncComm");
        websocket.onopen = function (evt) {
            onOpen(evt)
        };
        websocket.onmessage = function (evt) {
            onMessage(evt)
        };
        websocket.onerror = function (evt) {
            onError(evt)
        };
    }

    function onMessage(evt) {
        output = document.getElementById("output");
        output.innerHTML = evt.data;
    }

    function onOpen(evt) {
       output = document.getElementById("output");
        output.innerHTML = evt.data;
       websocket.send("uhybujniknmkmlk,p,lgvhj nk");
    }
    function onError(evt) {
        output = document.getElementById("output");
        output.innerHTML = evt.data;
    }

window.addEventListener("load", init, false);