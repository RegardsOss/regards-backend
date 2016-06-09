import fetch from 'isomorphic-fetch'
import scriptjs from 'scriptjs';

const ACCESS_RIGHTS_API='http://localhost:8080/api/access/rights';

export const REQUEST_ACCESSRIGHTS = 'REQUEST_ACCESSRIGHTS'
function requestAccessRights(view) {
  return {
    type: REQUEST_ACCESSRIGHTS,
    view: view
  }
}

export const RECEIVE_ACCESSRIGHTS = 'RECEIVE_ACCESSRIGHTS'
function receiveAccessRights(view, access) {
  return {
    type: RECEIVE_ACCESSRIGHTS,
    view: view,
    access: access
  }
}

export const FAILED_ACCESSRIGHTS = 'FAILED_ACCESSRIGHTS';
function failedAccessRights(view) {
  return {
    type : FAILED_ACCESSRIGHTS,
    view: view,
    access: false
  }
}

function checkResponseStatus(response){
  if (response.status === 200){
    return response;
  } else {
    throw new Error("Access denied to view : "+ view);
  }
}

// Meet our first thunk action creator!
// Though its insides are different, you would use it just like any other action creator:
// store.dispatch(fetchProjects())
export function fetchAccessRights(view, dependencies) {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.

  return function (dispatch, getState) {

    // First dispatch: the app state is updated to inform
    // that the API call is starting.
    dispatch(requestAccessRights(view))

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.

    let authorization = "Basic";
    if ( getState().authentication && getState().authentication.user && getState().authentication.user.access_token){
      authorization = "Bearer " + getState().authentication.user.access_token;
    }

    return fetch(ACCESS_RIGHTS_API, {
      headers: {
        'Accept': 'application/json',
        'Authorization': authorization
      },
      body: dependencies
    })
    .then(checkResponseStatus)
    .then(function(response) {
      return response.json()
    }).then(function(body) {
      dispatch(receiveAccessRights(view, true));
    }).catch(function(error) {
      dispatch(failedAccessRights(view));
    });
  }
}
