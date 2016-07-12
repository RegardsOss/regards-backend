var configureMockStore = require('redux-mock-store')
import thunk from 'redux-thunk'
import * as nock from 'nock'
import { expect } from 'chai' // You can use any testing library
import {
    PROJECTS_API, REQUEST_PROJECTS,  RECEIVE_PROJECTS,
    FAILED_PROJECTS, fetchProjects,
    requestProjects, receiveProjects } from '../../../scripts/portalApp/modules/projects/actions/ProjectsActions';

const middlewares = [ thunk ]
const mockStore = configureMockStore(middlewares)

// Ce fichier permet de tester les actions liés aux projets
describe('Testing projects actions.', () => {

  afterEach(() => {
    nock.cleanAll()
  })

  // Test dégradé dans le cas ou le serveur renvoie un erreur
  it('creates FAILED_PROJECTS action when fetching projects returning error', () => {
    nock(PROJECTS_API)
      .get('')
      .reply(500, null);

    const expectedActions = [
      { type: REQUEST_PROJECTS },
      { type: FAILED_PROJECTS, error: "Internal Server Error" }
    ]
    const store = mockStore({ projects: [] });

    return store.dispatch(fetchProjects())
      .then(() => { // return of async actions
        // There must be two dispatched actions from fetchProjects.
        expect(store.getActions().length).to.equal(2);
        // Check each dispatch action
        expect(store.getActions()).to.eql(expectedActions)
      })
  })

  // Test nominal
  it('creates REQUEST_PROJECTS and RECEIVE_PROJECTS actions when fetching projects has been done', () => {
    nock(PROJECTS_API)
      .get('')
      .reply(200, [{"name":"cdpp"},{"name":"ssalto"}]);

    const expectedActions:Array<any> = [
      { type: REQUEST_PROJECTS },
      { type: RECEIVE_PROJECTS, projects : [{name: 'cdpp'}, {name: 'ssalto'}], receivedAt: '' }
    ]
    const store = mockStore({ projects: [] });

    return store.dispatch(fetchProjects())
      .then(() => { // return of async actions
        // There must be two dispatched actions from fetchProjects.
        expect(store.getActions().length).to.equal(2);
        // Check receivedAt time
        var action:any = store.getActions()[1];
        expect(action.receivedAt).to.be.at.most(Date.now());
        // Add receivedAt time in expected action
        expectedActions[1].receivedAt = action.receivedAt;
        // Check each dispatch action
        expect(store.getActions()).to.eql(expectedActions);
      })
  })
})
