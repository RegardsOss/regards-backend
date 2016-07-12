"use strict";
var configureMockStore = require('redux-mock-store');
const redux_thunk_1 = require('redux-thunk');
const nock = require('nock');
const chai_1 = require('chai'); // You can use any testing library
const ProjectsActions_1 = require('../../../scripts/portalApp/modules/projects/actions/ProjectsActions');
const middlewares = [redux_thunk_1.default];
const mockStore = configureMockStore(middlewares);
// Ce fichier permet de tester les actions liés aux projets
describe('Testing projects actions.', () => {
    afterEach(() => {
        nock.cleanAll();
    });
    // Test dégradé dans le cas ou le serveur renvoie un erreur
    it('creates FAILED_PROJECTS action when fetching projects returning error', () => {
        nock(ProjectsActions_1.PROJECTS_API)
            .get('')
            .reply(500, null);
        const expectedActions = [
            { type: ProjectsActions_1.REQUEST_PROJECTS },
            { type: ProjectsActions_1.FAILED_PROJECTS, error: "Internal Server Error" }
        ];
        const store = mockStore({ projects: [] });
        return store.dispatch(ProjectsActions_1.fetchProjects())
            .then(() => {
            // There must be two dispatched actions from fetchProjects.
            chai_1.expect(store.getActions().length).to.equal(2);
            // Check each dispatch action
            chai_1.expect(store.getActions()).to.eql(expectedActions);
        });
    });
    // Test nominal
    it('creates REQUEST_PROJECTS and RECEIVE_PROJECTS actions when fetching projects has been done', () => {
        nock(ProjectsActions_1.PROJECTS_API)
            .get('')
            .reply(200, [{ "name": "cdpp" }, { "name": "ssalto" }]);
        const expectedActions = [
            { type: ProjectsActions_1.REQUEST_PROJECTS },
            { type: ProjectsActions_1.RECEIVE_PROJECTS, projects: [{ name: 'cdpp' }, { name: 'ssalto' }], receivedAt: '' }
        ];
        const store = mockStore({ projects: [] });
        return store.dispatch(ProjectsActions_1.fetchProjects())
            .then(() => {
            // There must be two dispatched actions from fetchProjects.
            chai_1.expect(store.getActions().length).to.equal(2);
            // Check receivedAt time
            var action = store.getActions()[1];
            chai_1.expect(action.receivedAt).to.be.at.most(Date.now());
            // Add receivedAt time in expected action
            expectedActions[1].receivedAt = action.receivedAt;
            // Check each dispatch action
            chai_1.expect(store.getActions()).to.eql(expectedActions);
        });
    });
});
//# sourceMappingURL=ProjectsActionsTest.js.map