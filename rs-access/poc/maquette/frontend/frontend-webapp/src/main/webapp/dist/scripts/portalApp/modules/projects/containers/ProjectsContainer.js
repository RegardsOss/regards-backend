"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ProjectComponent_1 = require('../components/ProjectComponent');
const ProjectsActions_1 = require('../actions/ProjectsActions');
class ProjectsContainer extends React.Component {
    componentWillMount() {
        this.props.onLoad();
    }
    render() {
        const { styles, projects } = this.props;
        if (projects.isFetching === true || !projects.items) {
            return (React.createElement("div", null, "Loading projects ... "));
        }
        else {
            return (React.createElement("div", null, 
                React.createElement("p", null, "Available projects on REGARDS instance :"), 
                React.createElement("ul", null, projects.items.map(project => React.createElement(ProjectComponent_1.default, {key: project.name, project: project, styles: styles})))));
        }
    }
}
exports.ProjectsContainer = ProjectsContainer;
const mapStateToProps = (state) => {
    return {
        projects: state.portalApp.projects
    };
};
const mapDispatchToProps = (dispatch) => {
    return {
        onLoad: () => dispatch(ProjectsActions_1.fetchProjects())
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
//# sourceMappingURL=ProjectsContainer.js.map