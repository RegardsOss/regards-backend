"use strict";
const chai_1 = require('chai'); // You can use any testing library
const ProjectsReducers_1 = require('../../../scripts/portalApp/modules/projects/reducers/ProjectsReducers');
const ProjectsActions_1 = require('../../../scripts/portalApp/modules/projects/actions/ProjectsActions');
// Ce fichier permet de tester les reducers liés aux projets
describe('Testing Projects reducers', () => {
    it('Should return the initial state', () => {
        chai_1.expect(ProjectsReducers_1.default.projects(undefined, {})).to.eql({
            isFetching: false,
            items: [],
            lastUpdate: ''
        });
    });
    it('Should set projects fetching true in state', () => {
        const initstate = {
            isFetching: false,
            items: [],
            lastUpdate: ''
        };
        const action = ProjectsActions_1.requestProjects();
        const expectedResult = {
            isFetching: true,
            items: [],
            lastUpdate: ''
        };
        const result = ProjectsReducers_1.default.projects(initstate, action);
        chai_1.expect(result).to.eql(expectedResult);
    });
    it('Should add projects to state', () => {
        const initstate = {
            isFetching: true,
            items: [],
            lastUpdate: ''
        };
        const action = ProjectsActions_1.receiveProjects([{ "name": "cdpp" }, { "name": "ssalto" }]);
        const expectedResult = {
            isFetching: false,
            items: [{ "name": "cdpp" }, { "name": "ssalto" }],
            lastUpdate: ''
        };
        const result = ProjectsReducers_1.default.projects(initstate, action);
        expectedResult.lastUpdate = result.lastUpdate;
        chai_1.expect(result).to.eql(expectedResult);
    });
});
//# sourceMappingURL=ProjectsReducersTest.js.map