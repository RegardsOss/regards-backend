import React from 'react';
import { connect } from 'react-redux';
import Project from './ProjectComponent';
import { fetchProjects } from './ProjectsActions';

class ProjectsComponent extends React.Component {

  componentWillMount(){
    const { dispatch } = this.props;
    dispatch(fetchProjects());
  }

  render(){
    if (this.props.projects.isFetching === true || !this.props.projects.items){
      return (<div>Loading projects ... </div>);
    } else {
      return (
        <div>
          <p>Available projects on REGARDS instance :</p>
          <ul>
            {this.props.projects.items.map(project =>
              <Project key={project.name} project={project} />
            )}
          </ul>
        </div>
      )
    }
  }
}

// Add projects from store to the component props
const mapStateToProps = (state) => {
  return {
    projects: state.projects
  }
}
export default connect(mapStateToProps)(ProjectsComponent);
