import React, { Component, PropTypes } from 'react'
import { bindActionCreators } from 'redux'
import { connect } from 'react-redux'
import { getThemeStyles } from 'common/theme/ThemeUtils'
import classnames from 'classnames'
import { map } from 'lodash'
// Containers
import { ProjectAdminsContainer, UserFormContainer } from 'adminApp/modules/projectAdmins'
// Components
import ManageProjectsComponent from '../components/ManageProjectsComponent'
import ProjectConfigurationComponent from '../components/ProjectConfigurationComponent'
import AccessRightsComponent from 'common/access-rights/AccessRightsComponent'
import ModuleComponent from 'common/components/ModuleComponent'
// Actions
import {
  addProject,
  deleteProject,
  fetchProjects,
  deleteSelectedProject } from '../actions'
import {
  selectProject,
  showProjectConfiguration,
  hideProjectConfiguration,
  showProjectAdminConfiguration } from 'adminApp/modules/ui/actions'
// Selectors
import {
  getProjects,
  getSelectedProjectId,
  getProjectById } from 'adminApp/reducer'

class ProjectsContainer extends React.Component {
  componentWillMount(){
    // onLoad method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.onLoad()
  }

  render () {
    const className = classnames(
      this.props.styles['columns'],
      this.props.styles['small-4'],
      this.props.styles['callout'],
      this.props.styles['custom-callout']
    )

    return (
      <ModuleComponent>
        <fieldset className={className}>

          <legend>Projects</legend>

          <ProjectConfigurationComponent
            styles={this.props.styles}
            show={this.props.projectConfigurationIsShown}
            onSubmit={this.props.handleSubmit}
            onCancelClick={this.props.hideProjectConfiguration}
            styles={this.props.styles} />

          <ManageProjectsComponent
            styles={this.props.styles}
            projects={this.props.projects}
            selectedProjectId={this.props.selectedProjectId}
            onSelect={this.props.onSelect}
            onAddClick={this.props.showProjectConfiguration}
            onDeleteClick={this.props.deleteProject}
            styles={this.props.styles} />

          <AccessRightsComponent dependencies={null}>
            <ProjectAdminsContainer/>
          </AccessRightsComponent>

        </fieldset>
      </ModuleComponent>
    )
  }
}

ProjectsContainer.propTypes = {
  projects: PropTypes.array,
  selectedProject: PropTypes.object,
  projectConfigurationIsShown: PropTypes.bool
};
const mapStateToProps = (state) => ({
  projects: map(getProjects(state).items, (value, key) => ({id:key, name:value.name})),
  selectedProjectId: getSelectedProjectId(state),
  projectConfigurationIsShown: state.adminApp.ui.projectConfigurationIsShown,
  styles: getThemeStyles(state.common.theme, 'adminApp/styles')
})
const mapDispatchToProps = (dispatch) => ({
  onLoad:                   ()  => dispatch(fetchProjects()),
  onSelect:                 (e) => dispatch(selectProject(e.target.value)),
  deleteProject:            (id)=> dispatch(deleteProject(id)),
  showProjectConfiguration: ()  => dispatch(showProjectConfiguration()),
  hideProjectConfiguration: ()  => dispatch(hideProjectConfiguration()),
  handleSubmit:             (e) => {
    let idProject = "9999" // TODO
    dispatch(addProject(idProject, e.projectName))
    dispatch(hideProjectConfiguration())
    dispatch(selectProject(idProject))
  }
})
export default connect(mapStateToProps, mapDispatchToProps)(ProjectsContainer);
