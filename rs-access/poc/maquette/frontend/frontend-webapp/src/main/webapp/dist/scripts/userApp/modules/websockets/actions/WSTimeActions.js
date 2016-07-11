"use strict";
const SockJS = require('sockjs-client');
// Backend websocket connection endpoint
exports.TIME_WS_API = 'http://localhost:8080/wsconnect';
// Action to update time in store
exports.SET_TIME = 'SET_TIME';
function setTime(time) {
    return {
        type: exports.SET_TIME,
        time: time
    };
}
exports.setTime = setTime;
// Asynchrone action to update time from websocket server
function connectTime() {
    return function (dispatch, getState) {
        // Connect to websocket server
        const url = exports.TIME_WS_API + "?access_token=" + getState().authentication.user.access_token;
        const socket = new SockJS(url);
        // TODO : Replace stompjs lib
        return socket;
    };
}
exports.connectTime = connectTime;
// Dysconnect from the websocket server
function disconnectTime(client) {
    return function (dispatch, getState) {
        client.disconnect(() => { console.log("Disconnected from websocket server"); });
    };
}
exports.disconnectTime = disconnectTime;
//# sourceMappingURL=WSTimeActions.js.map