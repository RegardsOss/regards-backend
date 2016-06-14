import SockJS from 'sockjs-client'
import Stomp from 'stompjs'

// Backend websocket connection endpoint
export const TIME_WS_API='http://localhost:8080/wsconnect'

// Action to update time in store
export const SET_TIME = 'SET_TIME'
export function setTime(time) {
  return {
    type: SET_TIME,
    time: time
  }
}

// Asynchrone action to update time from websocket server
export function connectTime() {
  return function (dispatch, getState) {
    // Connect to websocket server
    const url = TIME_WS_API + "?access_token="+getState().authentication.user.access_token
    const socket = new SockJS(url)
    let stompClient = Stomp.over(socket)
    // Disable devbug log messages
    stompClient.debug = () => {}
    // Listen for the server messages
    stompClient.connect({}, function(frame) {
      console.log('Connected to websocket server')
      stompClient.subscribe('/topic/time', function(result){
        dispatch(setTime(result.body))
      })
    }, function(){
      console.log("Error connecting to stomp websocket server")
    })

    return stompClient
  }
}

// Dysconnect from the websocket server
export function disconnectTime(client) {
  return function (dispatch, getState) {
    client.disconnect( () => { console.log("Disconnected from websocket server")})
  }
}
