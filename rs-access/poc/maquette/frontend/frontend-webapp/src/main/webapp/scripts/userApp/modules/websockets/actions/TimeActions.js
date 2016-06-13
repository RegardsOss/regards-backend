import fetch from 'isomorphic-fetch'

// Backend api adress
export const START_TIME_API='http://localhost:8080/api/time/start';

// Action when start time response is arrived.
export const RECEIVE_START_TIME = 'RECEIVE_START_TIME'
export function receiveStartTime() {
  return {
    type: RECEIVE_START_TIME
  }
}

// Check start timer response status
function checkStatus(response) {
  if (response.status === 200 ) {
    return response;
  } else {
    var error = new Error(response.statusText);
    throw error;
  }
}

// Rest request to start timer on server.
// The timer send time evey seconds by websocket
export function startTime() {
  return function (dispatch, getState) {

    // Init the authorization bearer of the fetch request
    let authorization = "Basic";
    if ( getState().authentication && getState().authentication.user && getState().authentication.user.access_token){
      authorization = "Bearer " + getState().authentication.user.access_token;
    }
    // Send REST Request
    return fetch(START_TIME_API, {
      headers: {
        'Accept': 'application/json',
        'Authorization': authorization
      }
    })
    .then(checkStatus)
    .then(function(response) {
      dispatch(receiveStartTime());
    });
  }
}
