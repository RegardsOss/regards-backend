import Rest from 'grommet/utils/Rest';

const PROJECTS_API='http://localhost:8080/api/projects';

export const REQUEST_PROJECTS = 'REQUEST_PROJECTS'
function requestProjects() {
  return {
    type: REQUEST_PROJECTS,
  }
}

export const RECEIVE_PROJECTS = 'RECEIVE_PROJECTS'
function receiveProjects(projects) {
  return {
    type: RECEIVE_PROJECTS,
    projects: projects,
    receivedAt: Date.now()
  }
}

export const FAILED_PROJECTS = 'FAILED_PROJECTS';
function failedProjects(error) {
  return {
    type : FAILED_PROJECTS,
    error : error
  }
}

// Meet our first thunk action creator!
// Though its insides are different, you would use it just like any other action creator:
// store.dispatch(fetchProjects())
export function fetchProjects() {

  // Thunk middleware knows how to handle functions.
  // It passes the dispatch method as an argument to the function,
  // thus making it able to dispatch actions itself.

  return function (dispatch) {

    // First dispatch: the app state is updated to inform
    // that the API call is starting.

    dispatch(requestProjects())

    // The function called by the thunk middleware can return a value,
    // that is passed on as the return value of the dispatch method.

    // In this case, we return a promise to wait for.
    // This is not required by thunk middleware, but it is convenient for us.

    return Rest.get(PROJECTS_API)
      .end((error, response) => {
        if (response.status === 200){
          dispatch(receiveProjects(response.body));
        } else {
          dispatch(failedProjects(response));
        }
    });
  }
}
