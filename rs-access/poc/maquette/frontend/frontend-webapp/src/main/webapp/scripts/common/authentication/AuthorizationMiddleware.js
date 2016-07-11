import { CALL_API } from 'redux-api-middleware'

// Redux middleware provides a third-party extension point
// between dispatching an action, and the moment it reaches the reducer

// Intercept actions
// If the action is formated as [CALL_API]: {...}, inject the headers
export default store => next => action => {
  let callAPI = action[CALL_API]
  if (callAPI)
    callAPI["headers"] = (store) => ({ 'Accept': 'application/json', 'Authorization': getAuthorization(store) || '' })

  return next(action)
}

const getAuthorization = (state) => {
  // Init the authorization bearer of the fetch request
  const authentication = state.common.authentication
  // let authorization = "Basic "
  let authorization = "Basic " + btoa("acme:acmesecret")
  if ( authentication && authentication.user && authentication.user.access_token){
    authorization = "Bearer " + authentication.user.access_token
  }
  return authorization
}
