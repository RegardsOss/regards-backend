import configureStore = require('redux-mock-store');
var {apiMiddleware} = require ('redux-api-middleware')
import thunk from "redux-thunk";
import * as nock from "nock";
import { expect } from "chai";
import * as actions from "../../../../scripts/adminApp/modules/projectAdmins/actions";
import { Action, AnyMeta } from "flux-standard-action";
import { FsaErrorAction, FsaErrorDefault } from "../../../../scripts/common/api/types"; // You can use any testing library
const middlewares = [thunk, apiMiddleware]

const mockStore = configureStore (middlewares)
describe ('[ADMIN APP] Testing project admins actions', () => {

  afterEach (() => {
    nock.cleanAll ()
  })

  // Test dégradé dans le cas ou le serveur renvoie un erreur
  it ('creates PROJECT_ADMIN_FAILURE action when fetching project admins returning error', () => {
    nock (actions.PROJECT_ADMINS_API)
    .get ('')
    .reply (500, 'Oops');
    const store = mockStore ({projectAdmins: []});

    const requestAction: Action<any> & AnyMeta = {
      type: 'PROJECT_ADMIN_REQUEST',
      payload: undefined,
      meta: undefined
    }
    const failureAction: FsaErrorAction & AnyMeta = {
      type: 'PROJECT_ADMIN_FAILURE',
      error: true,
      meta: undefined,
      payload: FsaErrorDefault
    }
    const expectedActions = [requestAction, failureAction]

    return store.dispatch (actions.fetchProjectAdmins ())
    .then (() => { // return of async actions
      expect (store.getActions ()).to.eql (expectedActions)
    })
  })

  // Test nominal
  it ('creates PROJECT_ADMIN_REQUEST and PROJECT_ADMIN_SUCESS actions when fetching project admins has been done', () => {
    nock (actions.PROJECT_ADMINS_API)
    .get ('')
    .reply (200, [
      {
        name: 'Alice',
        links: [{rel: 'self', href: 'fakeHref'}]
      },
      {
        name: 'Bob',
        links: [{rel: 'self', href: 'otherFakeHref'}]
      }
    ]);
    const store = mockStore ({projectAdmins: []});

    const requestAction: Action<any> & AnyMeta = {
      type: 'PROJECT_ADMIN_REQUEST',
      payload: undefined,
      meta: undefined
    }
    const successAction: Action<any> & AnyMeta = {
      type: 'PROJECT_ADMIN_SUCESS',
      meta: undefined,
      payload: {
        entities: {
          projectAdmins: {
            fakeHref: {
              name: 'Alice',
              links: [{rel: 'self', href: 'fakeHref'}]
            },
            otherFakeHref: {
              name: 'Bob',
              links: [{rel: 'self', href: 'otherFakeHref'}]
            }
          }
        },
        result: ['fakeHref', 'otherFakeHref']
      }
    }
    const expectedActions = [requestAction, successAction]

    return store.dispatch (actions.fetchProjectAdmins ())
    .then (() => { // return of async actions
      expect (store.getActions ()).to.eql (expectedActions)
    })
  })

  it ('should create an action to delete a project admin', () => {
    const expectedAction = {
      type: 'DELETE_PROJECT_ADMIN',
      id: 'toto'
    }
    expect (actions.deleteProjectAdmin ('toto')).to.eql (expectedAction)
  })

  it ('should create an action to update a project admin', () => {
    const expectedAction = {
      type: 'UPDATE_PROJECT_ADMIN',
      id: 'toto',
      payload: {
        name: 'Toto'
      }
    }
    expect (actions.updateProjectAdmin ('toto', {name: 'Toto'})).to.eql (expectedAction)
  })

  it ('should create an action to create a project admin', () => {
    const expectedAction = {
      type: 'CREATE_PROJECT_ADMIN',
      id: 'toto',
      payload: {
        name: 'Toto'
      }
    }
    expect (actions.createProjectAdmin ('toto', {name: 'Toto'})).to.eql (expectedAction)
  })

  it ('should create an action to update or create a project admin in single shot', () => {
    const expectedAction = {
      type: 'UPDATE_OR_CREATE_PROJECT_ADMIN',
      id: 'toto',
      payload: {
        name: 'Toto'
      }
    }
    expect (actions.updateOrCreateProjectAdmin ('toto', {name: 'Toto'})).to.eql (expectedAction)
  })

})
