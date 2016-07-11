"use strict";
const fetch = require('isomorphic-fetch');
exports.PROJECTS_API = 'http://localhost:8080/api/projects';
exports.REQUEST_PROJECTS = 'REQUEST_PROJECTS';
function requestProjects() {
    return {
        type: exports.REQUEST_PROJECTS,
    };
}
exports.requestProjects = requestProjects;
exports.RECEIVE_PROJECTS = 'RECEIVE_PROJECTS';
function receiveProjects(projects) {
    return {
        type: exports.RECEIVE_PROJECTS,
        projects: projects,
        receivedAt: Date.now()
    };
}
exports.receiveProjects = receiveProjects;
exports.FAILED_PROJECTS = 'FAILED_PROJECTS';
function failedProjects(error) {
    return {
        type: exports.FAILED_PROJECTS,
        error: error
    };
}
function checkStatus(response) {
    if (response.status === 200) {
        return response;
    }
    else {
        var error = new Error(response.statusText);
        throw error;
    }
}
function fetchProjects() {
    return function (dispatch, getState) {
        dispatch(requestProjects());
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