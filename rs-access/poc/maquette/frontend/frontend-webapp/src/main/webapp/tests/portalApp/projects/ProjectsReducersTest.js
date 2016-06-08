import { expect } from 'chai' // You can use any testing library
import reducers from '../../../scripts/portalApp/projects/ProjectsReducers';
import {
    PROJECTS_API, REQUEST_PROJECTS,  RECEIVE_PROJECTS,
    FAILED_PROJECTS, fetchProjects, requestProjects, receiveProjects } from '../../../scripts/portalApp/projects/ProjectsActions';

// Ce fichier permet de tester les reducers liés aux projets
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
