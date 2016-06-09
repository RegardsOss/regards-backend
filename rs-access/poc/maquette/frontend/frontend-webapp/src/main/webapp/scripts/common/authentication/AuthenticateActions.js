import fetch from 'isomorphic-fetch'

const AUTHENTICATE_API='http://localhost:8080/oauth/token';

export const REQUEST_AUTHENTICATE = 'REQUEST_AUTHENTICATE'
function requestAuthenticate(user) {
  return {
    type: REQUEST_AUTHENTICATE,
    user: user
  }
}

export const RECEIVE_AUTHENTICATE = 'RECEIVE_AUTHENTICATE'
function receiveAuthenticate(user) {
  return {
    type: RECEIVE_AUTHENTICATE,
    user: user,
    authenticateDate: Date.now()
  }
}

export const FAILED_AUTHENTICATE = 'FAILED_AUTHENTICATE';
function failedAuthenticate(error) {
  return {
    type : FAILED_AUTHENTICATE,
    error : error
  }
}

export const LOGOUT = 'LOGOUT';
export function logout() {
  return {
    type : LOGOUT
  }
}

function checkResponseStatus (response) {
  if (!response){
    throw new Error("Service unavailable");
  } else if (response.status === 200) {
   return response;
  } else {
     throw new Error("Authentication error");
  }
}

// Meet our first thunk action creator!
// Though its insides are different, you would use it just like any other action creator:
// store.dispatch(fetchProjects())
export function fetchAuthenticate(username, password) {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.

  return function (dispatch) {

    // First dispatch: the app state is updated to inform
    // that the API call is starting.

    dispatch(requestAuthenticate(username))

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.
    const request = AUTHENTICATE_API + "?grant_type=password&username="
    + username + "&password=" +password;

    return fetch(request, {
      method: 'post',
      headers: {
        'Accept': 'application/json',
        'Authorization': "Basic " + btoa("acme:acmesecret")
      }
    })
    .then(checkResponseStatus)
    .then(function(response) {
      return response.json()
    }).then(function(body) {
      const user = Object.assign({}, body, {
        name: username
      });
      dispatch(receiveAuthenticate(user));
    }).catch(function(error) {
      dispatch(failedAuthenticate(error.message));
    });
  }
}
