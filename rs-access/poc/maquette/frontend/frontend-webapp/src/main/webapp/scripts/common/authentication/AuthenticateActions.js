import Rest from 'grommet/utils/Rest';

const AUTHENTICATE_API='http://localhost:8080/oauth/token';

export const REQUEST_AUTHENTICATE = 'REQUEST_AUTHENTICATE'
function requestAuthenticate() {
  return {
    type: REQUEST_AUTHENTICATE,
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

    dispatch(requestAuthenticate())

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.
    const request = AUTHENTICATE_API + "?grant_type=password&username="
    + username + "&password=" +password;

    // Init rest requests with client authentication
    Rest.setHeaders({
      'Accept': 'application/json',
      'Authorization': "Basic " + btoa("acme:acmesecret")
    });

    return Rest.post(request)
      .end((error, response) => {
        if (error && error.timeout > 1000) {
         dispatch(failedAuthenticate("Timeout"));
        } else if (!response) {
         dispatch(failedAuthenticate("Service unavailable"));
        } else if (response.status != 200){
         dispatch(failedAuthenticate("Authentication error"));
        } else if (response.status === 200){
         // Add token to rest requests
         Rest.setHeaders({
           'Authorization': "Bearer " + response.body.access_token
         });
         const user = Object.assign({}, response.body, {
           name: username
         });
         dispatch(receiveAuthenticate(user));
        }
    });
  }
}
