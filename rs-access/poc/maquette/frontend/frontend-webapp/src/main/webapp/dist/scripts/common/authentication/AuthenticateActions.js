"use strict";
const fetch = require('isomorphic-fetch');
const AUTHENTICATE_API = 'http://localhost:8080/oauth/token';
exports.REQUEST_AUTHENTICATE = 'REQUEST_AUTHENTICATE';
function requestAuthenticate(user) {
    return {
        type: exports.REQUEST_AUTHENTICATE,
        user: user
    };
}
exports.RECEIVE_AUTHENTICATE = 'RECEIVE_AUTHENTICATE';
function receiveAuthenticate(user) {
    return {
        type: exports.RECEIVE_AUTHENTICATE,
        user: user,
        authenticateDate: Date.now()
    };
}
exports.FAILED_AUTHENTICATE = 'FAILED_AUTHENTICATE';
function failedAuthenticate(error) {
    return {
        type: exports.FAILED_AUTHENTICATE,
        error: error
    };
}
exports.LOGOUT = 'LOGOUT';
function logout() {
    return {
        type: exports.LOGOUT
    };
}
exports.logout = logout;
function checkResponseStatus(response) {
    if (!response) {
        throw new Error("Service unavailable");
    }
    else if (response.status === 200) {
        return response;
    }
    else {
        throw new Error("Authentication error");
    }
}
function fetchAuthenticate(username, password) {
    return function (dispatch) {
        dispatch(requestAuthenticate(username));
        const request = AUTHENTICATE_API + "?grant_type=password&username="
            + username + "&password=" + password;
        return fetch(request, {
            method: 'post',
            headers: {
                'Accept': 'application/json',
                'Authorization': "Basic " + btoa("acme:acmesecret")
            }
        })
            .then(checkResponseStatus)
            .then(function (response) {
            return response.json();
        }).then(function (body) {
            const user = Object.assign({}, body, {
                name: username
            });
            dispatch(receiveAuthenticate(user));
        }).catch(function (error) {
            dispatch(failedAuthenticate(error.message));
        });
    };
}
exports.fetchAuthenticate = fetchAuthenticate;
//# sourceMappingURL=AuthenticateActions.js.map