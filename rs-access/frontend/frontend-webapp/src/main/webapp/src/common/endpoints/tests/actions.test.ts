var configureMockStore = require('redux-mock-store')
var { apiMiddleware } = require('redux-api-middleware')
import thunk from 'redux-thunk'
import * as nock from 'nock'
import { expect } from 'chai' // You can use any testing library
import * as actions from '../actions'
import { Action, AnyMeta, TypedMeta, isFSA, isError } from 'flux-standard-action'
import { FsaErrorAction, FsaErrorDefault } from '../../api/types'
const middlewares = [thunk, apiMiddleware]
const mockStore = configureMockStore(middlewares)

describe('[COMMON] Testing endpoints actions', () => {

  afterEach(() => {
    nock.cleanAll()
  })

  // Test dégradé dans le cas ou le serveur renvoie un erreur
  it('creates ENDPOINT_FAILURE action when fetching endpoints returning error', () => {
    nock(actions.ENDPOINTS_API)
      .get('')
      .reply(500, 'Oops');
    const store = mockStore({
      endpoints: {
        isFetching: false,
        items: {},
        lastUpdate: ''
      }
    });

    const requestAction: Action<any> & AnyMeta = {
      type: 'ENDPOINTS_REQUEST',
      payload: undefined,
      meta: undefined
    }
    const failureAction: FsaErrorAction & AnyMeta = {
      type: 'ENDPOINTS_FAILURE',
      error: true,
      meta: undefined,
      payload: FsaErrorDefault
    }
    const expectedActions = [requestAction, failureAction]

    return store.dispatch(actions.fetchEndpoints())
      .then(() => { // return of async actions
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

  // Test nominal
  it('creates ENDPOINTS_REQUEST and ENDPOINTS_SUCCESS actions when fetching endpoints has been done', () => {
    nock(actions.ENDPOINTS_API)
      .get('')
      .reply(200, {
        "projects_users_url": "http://localhost:8080/api/users",
        "projects_url": "http://localhost:8080/api/projects"
      });
    const store = mockStore({
      endpoints: {
        isFetching: false,
        items: {},
        lastUpdate: ''
      }
    });

    const requestAction: Action<any> & AnyMeta = {
      type: 'ENDPOINTS_REQUEST',
      payload: undefined,
      meta: undefined
    }
    const successAction: Action<any> & AnyMeta = {
      type: 'ENDPOINTS_SUCCESS',
      meta: undefined,
      payload: {
        "projects_users_url": "http://localhost:8080/api/users",
        "projects_url": "http://localhost:8080/api/projects"
      }
    }
    const expectedActions = [requestAction, successAction]

    return store.dispatch(actions.fetchEndpoints())
      .then(() => { // return of async actions
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

  it('should create an action to delete an endpoint', () => {
    const expectedAction = {
      type: 'DELETE_ENDPOINT',
      id: 'projects_users_url'
    }
    expect(actions.deleteEndpoint('projects_users_url')).to.eql(expectedAction)
  })

})
