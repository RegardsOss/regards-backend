/** @module PortalProjects */
import * as React from 'react'
import { connect } from 'react-redux'
import { FormattedMessage, intlShape } from 'react-intl'

import ProjectComponent from '../components/ProjectComponent'
import { fetchProjects } from '../actions/ProjectsActions'

import { Project, ProjectsStore } from '../types/ProjectTypes'

// Container props
interface ProjectsProps{
  styles: any,
  // Properties set by react redux connection
  onLoad?: ()=> void,
  projects?: ProjectsStore
}

// Export class itself without connect to be able to use it in test without store connection.
/**
 * React container to manage projects in portal app
 */
export class ProjectsContainer extends React.Component<ProjectsProps, any> {

  componentWillMount(){
    // onLoad method is set to the container props by react-redux connect.
    // See method mapDispatchToProps of this container
    this.props.onLoad()
  }

  render(){
    // styles props is passed throught the react component creation
    // porjects props is set to the container by tge react-redux connect.
    // See method mapStateToProps
    const { styles, projects } = this.props

    // If projects are loading display a loading information message
    if (projects.isFetching === true || !projects.items){
      return (<div>Loading projects ... </div>)
    } else {
      // Else display projects links
      return (
        <div>
          <p><FormattedMessage id="portalapp.projects.list.title" /></p>
          <ul>
            {projects.items.map(project =>
              <ProjectComponent key={project.name} project={project} styles={styles}/>
            )}
          </ul>
        </div>
      )
    }
  }
}

// Add projects from store to the container props
const mapStateToProps = (state:any) => {
  return {
    projects: state.portalApp.projects,
    locale: state.common.i18n.locale
  }
}

// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch:any) => {
  return {
    onLoad: () => dispatch(fetchProjects()),
  }
}
export default connect<{},{},ProjectsProps>(mapStateToProps,mapDispatchToProps)(ProjectsContainer)
