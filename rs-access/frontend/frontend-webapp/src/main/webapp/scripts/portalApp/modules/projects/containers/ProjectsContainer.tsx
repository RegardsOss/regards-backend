/** @module PortalProjects */
import * as React from "react";
import { connect } from "react-redux";
import { FormattedMessage } from "react-intl";
import ProjectComponent from "../components/ProjectComponent";
import { fetchProjects } from "../actions/ProjectsActions";
import { ProjectsStore } from "../types/ProjectTypes";

// Container props
interface ProjectsProps {
  // Properties set by react redux connection
  onLoad?: () => void,
  projects?: ProjectsStore,
  i18n?: any
}

// Export class itself without connect to be able to use it in test without store connection.
/**
 * React container to manage projects in portal app
 */
export class ProjectsContainer extends React.Component<ProjectsProps, any> {

  componentWillMount(): any {
    // onLoad method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.onLoad ()
  }

  render(): any {
    // projects props is set to the container by tge react-redux connect.
    // See method mapStateToProps
    const {projects} = this.props

    // If projects are loading display a loading information message
    if (projects.isFetching === true || !projects.items) {
      return (<div>Loading projects ... </div>)
    } else {
      // Else display projects links
      return (
        <div>
          <p><FormattedMessage id="portalapp.projects.list.title"/></p>
          <ul>
            {projects.items.map (project =>
              <ProjectComponent key={project.name} project={project}/>
            )}
          </ul>
        </div>
      )
    }
  }
}

// Add projects from store to the container props
const mapStateToProps = (state: any) => ({
  projects: state.portalApp.projects,
  i18n: state.common.i18n
})

// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch: any) => ({
  onLoad: () => dispatch (fetchProjects ())
})
export default connect<{}, {}, ProjectsProps> (mapStateToProps, mapDispatchToProps) (ProjectsContainer)
