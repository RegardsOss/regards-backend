import Rest from 'grommet/utils/Rest';
import scriptjs from 'scriptjs';

const ACCESS_RIGHTS_API='http://localhost:8080/api/access/rights';

export const REQUEST_ACCESSRIGHTS = 'REQUEST_ACCESSRIGHTS'
function requestAccessRights() {
  return {
    type: REQUEST_ACCESSRIGHTS,
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

// Meet our first thunk action creator!
// Though its insides are different, you would use it just like any other action creator:
// store.dispatch(fetchProjects())
export function fetchAccessRights(view, dependencies) {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.

  return function (dispatch) {

    // First dispatch: the app state is updated to inform
    // that the API call is starting.

    dispatch(requestAccessRights())

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.

    return Rest.get(ACCESS_RIGHTS_API,dependencies)
      .end((error, response) => {
        if (response.status === 200){
          console.log("Access granted to view : "+ view);
          dispatch(receiveAccessRights(view, true));
        } else {
          console.log("Access denied to view : "+ view);
          dispatch(failedAccessRights(view));
        }
    });
  }
}
