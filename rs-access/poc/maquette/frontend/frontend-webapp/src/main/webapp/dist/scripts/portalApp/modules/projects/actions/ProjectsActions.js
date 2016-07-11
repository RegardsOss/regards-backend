"use strict";
const fetch = require('isomorphic-fetch');
// Backend api adress
exports.PROJECTS_API = 'http://localhost:8080/api/projects';
// Action to inform store that te projects request is running
exports.REQUEST_PROJECTS = 'REQUEST_PROJECTS';
function requestProjects() {
    return {
        type: exports.REQUEST_PROJECTS,
    };
}
exports.requestProjects = requestProjects;
// Action to inform store that te projects are availables
exports.RECEIVE_PROJECTS = 'RECEIVE_PROJECTS';
function receiveProjects(projects) {
    return {
        type: exports.RECEIVE_PROJECTS,
        projects: projects,
        receivedAt: Date.now()
    };
}
exports.receiveProjects = receiveProjects;
// Action to inform store that the running projects request failed
exports.FAILED_PROJECTS = 'FAILED_PROJECTS';
function failedProjects(error) {
    return {
        type: exports.FAILED_PROJECTS,
        error: error
    };
}
// Function to check the projects request response status
function checkStatus(response) {
    if (response.status === 200) {
        return response;
    }
    else {
        var error = new Error(response.statusText);
        throw error;
    }
}
// Asynchrone action to fetch projects from backend
function fetchProjects() {
    // Thunk middleware knows how to handle functions.
    // It passes the dispatch method as an argument to the function,
    // thus making it able to dispatch actions itself.
    return function (dispatch, getState) {
        // First dispatch: the app state is updated to inform
        // that the API call is starting.
        dispatch(requestProjects());
        // The function called by the thunk middleware can return a value,
        // that is passed on as the return value of the dispatch method.
        // In this case, we return a promise to wait for.
        // This is not required by thunk middleware, but it is convenient for us.
        // Init the authorization bearer of the fetch request
        let authentication = getState().common.authentication;
        let authorization = "Basic";
        if (authentication && authentication.user && authentication.user.access_token) {
            authorization = "Bearer " + authentication.user.access_token;
        }
        return fetch(exports.PROJECTS_API, {
            headers: {
                'Accept': 'application/json',
                'Authorization': authorization
            }
        })
            .then(checkStatus)
            .then(function (response) {
            return response.json();
        }).then(function (body) {
            dispatch(receiveProjects(body));
        }).catch(function (error) {
            dispatch(failedProjects(error.message));
        });
    };
}
exports.fetchProjects = fetchProjects;
//# sourceMappingURL=ProjectsActions.js.map