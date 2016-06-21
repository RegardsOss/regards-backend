import React, { Component, PropTypes } from 'react';
import { bindActionCreators } from 'redux';
import { connect } from 'react-redux';
// Components
import ManageProjectsComponent from '../components/ManageProjectsComponent'
import ProjectAdministratorsComponent from '../components/ProjectAdministratorsComponent'
// import ProjectConfigurationComponent from '../components/ProjectConfigurationComponent'
// Actions
import { selectProject } from '../actions/ProjectsActions'

const ProjectsContainer = React.createClass({
  render () {
    const { projects } = this.props
    const selectedProject = projects.items.find(project => project.selected)

    return (
      <div>
        <ManageProjectsComponent projects={ projects } onSelect={this.props.onSelect} />
        <ProjectAdministratorsComponent project={ selectedProject } />
      </div>
    )
  }
})

ProjectsContainer.propTypes = {
  projects: PropTypes.object
};
const mapStateToProps = (state) => ({
  projects: state.adminApp.projects
})
const mapDispatchToProps = (dispatch) => ({
  onSelect: (e) => dispatch(selectProject(e.target.value))
})
module.exports = connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
