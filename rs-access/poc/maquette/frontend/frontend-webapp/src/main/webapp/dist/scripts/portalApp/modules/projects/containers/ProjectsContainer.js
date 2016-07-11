"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ProjectComponent_1 = require('../components/ProjectComponent');
const ProjectsActions_1 = require('../actions/ProjectsActions');
// Export class itself without connect to be able to use it in test without store connection.
class ProjectsContainer extends React.Component {
    componentWillMount() {
        // onLoad method is set to the container props by react-redux connect.
        // See method mapDispatchToProps of this container
        this.props.onLoad();
    }
    render() {
        // styles props is passed throught the react component creation
        // porjects props is set to the container by tge react-redux connect.
        // See method mapStateToProps
        const { styles, projects } = this.props;
        // If projects are loading display a loading information message
        if (projects.isFetching === true || !projects.items) {
            return (React.createElement("div", null, "Loading projects ... "));
        }
        else {
            // Else display projects links
            return (React.createElement("div", null, React.createElement("p", null, "Available projects on REGARDS instance :"), React.createElement("ul", null, projects.items.map(project => React.createElement(ProjectComponent_1.default, {key: project.name, project: project, styles: styles})))));
        }
    }
}
exports.ProjectsContainer = ProjectsContainer;
// Add projects from store to the container props
const mapStateToProps = (state) => {
    return {
        projects: state.portalApp.projects
    };
};
// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch) => {
    return {
        onLoad: () => dispatch(ProjectsActions_1.fetchProjects())
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
//# sourceMappingURL=ProjectsContainer.js.map