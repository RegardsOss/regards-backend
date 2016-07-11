import React from 'react'
import { connect } from 'react-redux'
import Project from '../components/ProjectComponent'
import { fetchProjects } from '../actions/ProjectsActions'

// Export class itself without connect to be able to use it in test without store connection.
export class ProjectsContainer extends React.Component {

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
          <p>Available projects on REGARDS instance :</p>
          <ul>
            {projects.items.map(project =>
              <Project key={project.name} project={project} styles={styles}/>
            )}
          </ul>
        </div>
      )
    }
  }
}

// Container props
ProjectsContainer.propTypes = {
  styles: React.PropTypes.object.isRequired
}

// Add projects from store to the container props
const mapStateToProps = (state) => {
  return {
    projects: state.portalApp.projects
  }
}

// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch) => {
  return {
    onLoad: () => dispatch(fetchProjects())
  }
}
export default connect(mapStateToProps,mapDispatchToProps)(ProjectsContainer)
