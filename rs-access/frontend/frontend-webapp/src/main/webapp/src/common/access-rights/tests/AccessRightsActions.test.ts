var configureMockStore = require('redux-mock-store')
var { apiMiddleware } = require('redux-api-middleware')
import thunk from 'redux-thunk'
import * as nock from 'nock'
import { expect } from 'chai' // You can use any testing library
import * as actions from '../AccessRightsActions'
import { DependencyAccessRight } from '../index'
import { Action, AnyMeta, TypedMeta, isFSA, isError } from 'flux-standard-action'
import { FsaErrorAction, FsaErrorDefault } from '../../api/types'

const middlewares = [ thunk, apiMiddleware ]
const mockStore = configureMockStore(middlewares)

describe('[COMMON] Testing access rights actions', () => {

  afterEach(() => {
    nock.cleanAll()
  })

  // Test dégradé dans le cas ou le serveur renvoie un erreur
  it('creates FAILED_ACCESSRIGHTS action when fetching access rights returning error', () => {
    nock(actions.ACCESS_RIGHTS_API)
      .post('')
      .reply(500, 'Oops');
    const store = mockStore({ views: [] });

    const requestAction: Action<any> & AnyMeta = {
      type: 'REQUEST_ACCESSRIGHTS',
      payload: undefined,
      meta: undefined
    }
    const failureAction: FsaErrorAction & AnyMeta =  {
      type: 'FAILED_ACCESSRIGHTS',
      error: true,
      meta: undefined,
      payload: FsaErrorDefault
    }
    const expectedActions = [ requestAction, failureAction ]

    const dependencies:Array<DependencyAccessRight> = [
      {id:"aGetDependency",verb:"GET",endpoint:"/aGetDependency",access:false},
      {id:"anOtherGetDependency",verb:"GET",endpoint:"/anOtherGetDependency",access:false},
      {id:"aDeleteDependency",verb:"DELETE",endpoint:"/aDeleteDependency",access:false}]

    return store.dispatch(actions.fetchAccessRights(dependencies))
      .then(() => { // return of async actions
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

  // Test nominal
  it('creates PROJECTS_REQUEST and PROJECTS_SUCESS actions when fetching access rights has been done', () => {
    nock(actions.ACCESS_RIGHTS_API)
      .post('')
      .reply(200, [
        {id:"aGetDependency",verb:"GET",endpoint:"/aGetDependency",access:true},
        {id:"anOtherGetDependency",verb:"GET",endpoint:"/anOtherGetDependency",access:false},
        {id:"aDeleteDependency",verb:"DELETE",endpoint:"/aDeleteDependency",access:true}
      ]);
    const store = mockStore({ views: [] });

    const requestAction: Action<any> & AnyMeta = {
      type: 'REQUEST_ACCESSRIGHTS',
      payload: undefined,
      meta: undefined
    }
    const successAction: Action<any> & AnyMeta = {
      type: 'RECEIVE_ACCESSRIGHTS',
      meta: undefined,
      payload: [
        {id:"aGetDependency",verb:"GET",endpoint:"/aGetDependency",access:true},
        {id:"anOtherGetDependency",verb:"GET",endpoint:"/anOtherGetDependency",access:false},
        {id:"aDeleteDependency",verb:"DELETE",endpoint:"/aDeleteDependency",access:true}
      ]
    }
    const expectedActions = [ requestAction, successAction ]

    const dependencies:Array<DependencyAccessRight> = [
      {id:"aGetDependency",verb:"GET",endpoint:"/aGetDependency",access:false},
      {id:"anOtherGetDependency",verb:"GET",endpoint:"/anOtherGetDependency",access:false},
      {id:"aDeleteDependency",verb:"DELETE",endpoint:"/aDeleteDependency",access:false}]

    return store.dispatch(actions.fetchAccessRights(dependencies))
      .then(() => { // return of async actions
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

})
