import { CALL_API } from 'redux-api-middleware'

const AUTHENTICATE_API='http://localhost:8080/oauth/token'
export const REQUEST_AUTHENTICATE = 'REQUEST_AUTHENTICATE'
export const RECEIVE_AUTHENTICATE = 'RECEIVE_AUTHENTICATE'
export const FAILED_AUTHENTICATE = 'FAILED_AUTHENTICATE'

export const fetchAuthenticate = (username:String, password:String) => ({
  [CALL_API]: {
    types: [
      REQUEST_AUTHENTICATE,
      {
        type: RECEIVE_AUTHENTICATE,
        meta: { authenticateDate: Date.now() }
      },
      FAILED_AUTHENTICATE
    ],
    endpoint: AUTHENTICATE_API + "?grant_type=password&username=" + username + "&password=" + password,
    method: 'POST'
  }
})

export const LOGOUT = 'LOGOUT'
export function logout() {
  return {
    type : LOGOUT
  }
}
