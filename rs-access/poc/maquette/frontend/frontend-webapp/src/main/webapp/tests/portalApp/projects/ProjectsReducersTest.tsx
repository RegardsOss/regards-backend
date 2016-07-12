import { expect } from 'chai' // You can use any testing library
import projectsReducers from '../../../scripts/portalApp/modules/projects/reducers/ProjectsReducers'
import {
    PROJECTS_API, REQUEST_PROJECTS,  RECEIVE_PROJECTS,
    FAILED_PROJECTS, fetchProjects } from '../../../scripts/portalApp/modules/projects/actions/ProjectsActions';

// Ce fichier permet de tester les reducers liés aux projets
describe('Testing Projects reducers', () => {

  it('Should return the initial state', () => {
    expect(projectsReducers(undefined, {})).to.eql({
        isFetching : false,
        items: [],
        lastUpdate: ''
      });
  });

  it('Should set projects fetching true in state', () => {
    const initstate:ProjectsStore = {
        isFetching : false,
        items: [],
        lastUpdate: ''
    };

    const action = {
      type: REQUEST_PROJECTS
    }

    const expectedResult:ProjectsStore = {
        isFetching : true,
        items: [],
        lastUpdate: ''
    };

    const result = projectsReducers(initstate, action);
    expect(result).to.eql(expectedResult);
  });

  it('Should add projects to state', () => {
    const initstate:ProjectsStore = {
        isFetching : true,
        items: [],
        lastUpdate: ''
    };

    const action = {
      type: RECEIVE_PROJECTS,
      payload: [
        {"name":"cdpp"},
        {"name":"ssalto"}
      ],
      meta: {
        receivedAt: Date.now()
      }
    }

    const expectedResult = {
      isFetching : false,
      items: [
        {"name":"cdpp"},
        {"name":"ssalto"}
      ],
      lastUpdate: ''
    };

    const result = projectsReducers(initstate, action);
    expectedResult.lastUpdate = result.lastUpdate;
    expect(result).to.eql(expectedResult);
  });
});
