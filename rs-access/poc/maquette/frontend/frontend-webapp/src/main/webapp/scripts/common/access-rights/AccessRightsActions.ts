import { CALL_API } from 'redux-api-middleware'

const ACCESS_RIGHTS_API='http://localhost:8080/api/access/rights'

export const REQUEST_ACCESSRIGHTS = 'REQUEST_ACCESSRIGHTS'
export const RECEIVE_ACCESSRIGHTS = 'RECEIVE_ACCESSRIGHTS'
export const FAILED_ACCESSRIGHTS = 'FAILED_ACCESSRIGHTS';

export const fetchAccessRights = () => ({
  [CALL_API]: {
    types: [
      REQUEST_ACCESSRIGHTS,
      RECEIVE_ACCESSRIGHTS,
      FAILED_ACCESSRIGHTS
    ],
    endpoint: ACCESS_RIGHTS_API,
    method: 'GET'
  }
})
