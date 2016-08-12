var configureMockStore = require('redux-mock-store')
var { apiMiddleware } = require('redux-api-middleware')
import thunk from 'redux-thunk'
import * as nock from 'nock'
import { expect } from 'chai' // You can use any testing library
import * as actions from '../../../../src/adminApp/modules/projects/actions';
import { Action, AnyMeta, TypedMeta, isFSA, isError } from 'flux-standard-action'
import { FsaErrorAction, FsaErrorDefault } from '../../../../src/common/api/types'
const middlewares = [ thunk, apiMiddleware ]
const mockStore = configureMockStore(middlewares)

describe('[ADMIN APP] Testing projects actions', () => {

  afterEach(() => {
    nock.cleanAll()
  })

  // Test dégradé dans le cas ou le serveur renvoie un erreur
  it('creates PROJECTS_FAILURE action when fetching projects returning error', () => {
    nock(actions.PROJECTS_API)
      .get('')
      .reply(500, 'Oops');
    const store = mockStore({ projects: [] });

    const requestAction: Action<any> & AnyMeta = {
      type: 'PROJECTS_REQUEST',
      payload: undefined,
      meta: undefined
    }
    const failureAction: FsaErrorAction & AnyMeta =  {
      type: 'PROJECTS_FAILURE',
      error: true,
      meta: undefined,
      payload: FsaErrorDefault
    }
    const expectedActions = [ requestAction, failureAction ]

    return store.dispatch(actions.fetchProjects())
      .then(() => { // return of async actions
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

  // Test nominal
  it('creates PROJECTS_REQUEST and PROJECTS_SUCCESS actions when fetching projects has been done', () => {
    nock(actions.PROJECTS_API)
      .get('')
      .reply(200, [
        {
          name: 'cdpp',
          links: [{ rel: 'self', href: 'fakeHref' }]
        },
        {
          name: 'ssalto',
          links: [{ rel: 'self', href: 'otherFakeHref' }]
        }
      ]);
    const store = mockStore({ projectAdmins: [] });

    const requestAction: Action<any> & AnyMeta = {
      type: 'PROJECTS_REQUEST',
      payload: undefined,
      meta: undefined
    }
    const successAction: Action<any> & AnyMeta = {
      type: 'PROJECTS_SUCCESS',
      meta: undefined,
      payload: {
        entities: {
          projects: {
            fakeHref:{
              name: 'cdpp',
              links: [{ rel: 'self', href: 'fakeHref' }]
            },
            otherFakeHref:{
              name: 'ssalto',
              links: [{ rel: 'self', href: 'otherFakeHref' }]
            }
          }
        },
        result: ['fakeHref', 'otherFakeHref']
      }
    }
    const expectedActions = [ requestAction, successAction ]

    return store.dispatch(actions.fetchProjects())
      .then(() => { // return of async actions
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

  it('should create an action to delete a project', () => {
    const expectedAction = {
      type: 'DELETE_PROJECT',
      id: 'cdpp'
    }
    expect(actions.deleteProject('cdpp')).to.eql(expectedAction)
  })

  it('should create an action to create a project', () => {
    const expectedAction = {
      type: 'ADD_PROJECT',
      id: 'toto',
      name: 'Toto'
    }
    expect(actions.addProject('toto', 'Toto')).to.eql(expectedAction)
  })

})
