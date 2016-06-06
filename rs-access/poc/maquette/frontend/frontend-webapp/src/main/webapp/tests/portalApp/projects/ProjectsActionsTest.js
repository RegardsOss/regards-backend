import configureMockStore from 'redux-mock-store'
import thunk from 'redux-thunk'
import nock from 'nock'
import expect from 'expect' // You can use any testing library

import {
    PROJECTS_API, REQUEST_PROJECTS,  RECEIVE_PROJECTS,
    FAILED_PROJECTS, fetchProjects } from '../../../scripts/portalApp/projects/ProjectsActions';

const middlewares = [ thunk ]
const mockStore = configureMockStore(middlewares)

// Cette classe permet de tester l'action de récupération des projets depuis le backend
// et de vérifier les actions envoyées au Store.

describe('Testing projects async actions. (Retreive projects from backend)', () => {
  afterEach(() => {
    nock.cleanAll()
  })

  it('creates REQUEST_PROJECTS and RECEIVE_PROJECTS actions when fetching projects has been done', () => {

    nock(PROJECTS_API)
      .get('')
      .reply(200, [{"name":"cdpp"},{"name":"ssalto"}]);

    const expectedActions = [
      { type: REQUEST_PROJECTS },
      { type: RECEIVE_PROJECTS, projects : [{name: 'cdpp'}, {name: 'ssalto'}], receivedAt: '' }
    ]
    const store = mockStore({ projects: [] });

    return store.dispatch(fetchProjects())
      .then(() => { // return of async actions
        // There must be two dispatched actions from fetchProjects.
        expect(store.getActions().length).toEqual(2);
        // Check receivedAt time
        expect(store.getActions()[1].receivedAt).toBeLessThanOrEqualTo(Date.now());
        // Add receivedAt time in expected action
        expectedActions[1].receivedAt = store.getActions()[1].receivedAt;
        // Check each dispatch action
        expect(store.getActions()).toEqual(expectedActions)
      })
  })
})
