import React from 'react';
import { connect } from 'react-redux';
import Project from '../components/ProjectComponent';
import { fetchProjects } from '../actions/ProjectsActions';

// Export class itself without connect to be able to use it in test without store connection.
export class ProjectsContainer extends React.Component {

  componentWillMount(){
    this.props.onLoad();
  }

  render(){
    const { styles } = this.props;
    if (this.props.projects.isFetching === true || !this.props.projects.items){
      return (<div>Loading projects ... </div>);
    } else {
      return (
        <div>
          <p>Available projects on REGARDS instance :</p>
          <ul>
            {this.props.projects.items.map(project =>
              <Project key={project.name} project={project} styles={styles}/>
            )}
          </ul>
        </div>
      )
    }
  }
}

ProjectsContainer.propTypes = {
  styles: React.PropTypes.object.isRequired
}

// Add projects from store to the container props
const mapStateToProps = (state) => {
  return {
    projects: state.projects
  }
}

// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch) => {
  return {
    onLoad: () => dispatch(fetchProjects())
  }
}
export default connect(mapStateToProps,mapDispatchToProps)(ProjectsContainer);
