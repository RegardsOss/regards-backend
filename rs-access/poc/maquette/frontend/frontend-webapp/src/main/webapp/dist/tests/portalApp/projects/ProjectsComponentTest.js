"use strict";
var __assign = (this && this.__assign) || Object.assign || function(t) {
    for (var s, i = 1, n = arguments.length; i < n; i++) {
        s = arguments[i];
        for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
            t[p] = s[p];
    }
    return t;
};
const react_1 = require('react');
const react_router_1 = require('react-router');
const enzyme_1 = require('enzyme');
const chai_1 = require('chai');
// Import unconnected version of ProjectsComponent. by using bracets {} around component.
// To get the react-redux connect component use "import ProjectsComponent" instead of "import { ProjectsComponent }"
const ProjectsContainer_1 = require('../../../scripts/portalApp/modules/projects/containers/ProjectsContainer');
const ProjectComponent_1 = require('../../../scripts/portalApp/modules/projects/components/ProjectComponent');
// Test a component rendering
describe('Testing projects components', () => {
    it('Should render correctly the loading projects message', () => {
        const dispatch = () => { };
        const onLoad = () => { };
        const projectsStyles = {
            link: 'link',
            projectlink: 'projectlink'
        };
        let props = {
            projects: {
                isFetching: true
            },
            styles: projectsStyles,
            dispatch: dispatch,
            onLoad: onLoad
        };
        const wrapper = enzyme_1.shallow(react_1.default.createElement(ProjectsContainer_1.ProjectsContainer, __assign({}, props)));
        chai_1.expect(wrapper.equals(react_1.default.createElement("div", null, "Loading projects ... "))).to.equal(true);
    });
    it('Should render correctly the projects list', () => {
        const dispatch = () => { };
        const onLoad = () => { };
        const projectsStyles = {
            link: 'link',
            projectlink: 'projectlink'
        };
        let props = {
            projects: {
                isFetching: false,
                items: [{ name: 'cdpp' }, { name: 'ssalto' }]
            },
            styles: projectsStyles,
            dispatch: dispatch,
            onLoad: onLoad
        };
        const result = (react_1.default.createElement("div", null, react_1.default.createElement("p", null, "Available projects on REGARDS instance :"), react_1.default.createElement("ul", null, react_1.default.createElement(ProjectComponent_1.default, {key: "cdpp", project: { name: 'cdpp' }, styles: projectsStyles}), react_1.default.createElement(ProjectComponent_1.default, {key: "ssalto", project: { name: 'ssalto' }, styles: projectsStyles}))));
        const wrapper = enzyme_1.shallow(react_1.default.createElement(ProjectsContainer_1.ProjectsContainer, __assign({}, props)));
        chai_1.expect(wrapper.contains(result)).to.equal(true);
    });
    it('Should render correctly a project link', () => {
        const projectsStyles = {
            link: 'link',
            "project-link": 'project-link'
        };
        let props = {
            styles: projectsStyles,
            project: { name: 'cdpp' }
        };
        const result = (react_1.default.createElement("li", {className: "link"}, react_1.default.createElement("p", null, "cdpp"), react_1.default.createElement(react_router_1.Link, {to: "/user/cdpp", className: "project-link"}, "ihm user"), react_1.default.createElement(react_router_1.Link, {to: "/admin/cdpp", className: "project-link"}, "ihm admin")));
        const wrapper = enzyme_1.shallow(react_1.default.createElement(ProjectComponent_1.default, __assign({}, props)));
        // To log result
        //console.log(wrapper.debug());
        chai_1.expect(wrapper.contains(result)).to.equal(true);
    });
});
//# sourceMappingURL=ProjectsComponentTest.js.map