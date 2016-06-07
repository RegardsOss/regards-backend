import configureMockStore from 'redux-mock-store'
import thunk from 'redux-thunk'
import nock from 'nock'
import { expect } from 'chai' // You can use any testing library
import reducers from '../../../scripts/portalApp/projects/ProjectsReducers';
import {
    PROJECTS_API, REQUEST_PROJECTS,  RECEIVE_PROJECTS,
    FAILED_PROJECTS, fetchProjects, requestProjects, receiveProjects } from '../../../scripts/portalApp/projects/ProjectsActions';

const middlewares = [ thunk ]
const mockStore = configureMockStore(middlewares)

// Ce fichier permet de tester les actions et reducers liés aux projets

describe('Testing Projects reducers', () => {
  it('Should return the initial state', () => {
    expect(reducers.projects(undefined, {})).to.eql({
        isFetching : false,
        items: [],
        lastUpdate: ''
      });
  });

  it('Should set projects fetching true in state', () => {
    const initstate = {
        isFetching : false,
        items: [],
        lastUpdate: ''
    };

    const action = requestProjects();

    const expectedResult = {
        isFetching : true,
        items: [],
        lastUpdate: ''
    };

    const result = reducers.projects(initstate, action);
    expect(result).to.eql(expectedResult);
  });

  it('Should add projects to state', () => {
    const initstate = {
        isFetching : true,
        items: [],
        lastUpdate: ''
    };

    const action = receiveProjects([{"name":"cdpp"},{"name":"ssalto"}]);

    const expectedResult = {
        isFetching : false,
        items: [{"name":"cdpp"},{"name":"ssalto"}],
        lastUpdate: ''
    };

    const result = reducers.projects(initstate, action);
    expectedResult.lastUpdate = result.lastUpdate;
    expect(result).to.eql(expectedResult);
  });
});

describe('Testing projects async actions. (Retreive projects from backend)', () => {

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

    const expectedActions = [
      { type: REQUEST_PROJECTS },
      { type: RECEIVE_PROJECTS, projects : [{name: 'cdpp'}, {name: 'ssalto'}], receivedAt: '' }
    ]
    const store = mockStore({ projects: [] });

    return store.dispatch(fetchProjects())
      .then(() => { // return of async actions
        // There must be two dispatched actions from fetchProjects.
        expect(store.getActions().length).to.equal(2);
        // Check receivedAt time
        expect(store.getActions()[1].receivedAt).to.be.at.most(Date.now());
        // Add receivedAt time in expected action
        expectedActions[1].receivedAt = store.getActions()[1].receivedAt;
        // Check each dispatch action
        expect(store.getActions()).to.eql(expectedActions);
      })
  })
})
