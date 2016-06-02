import React from 'react';

import Project from './ProjectComponent';

class ProjectsComponent extends React.Component {
  render(){
    if (this.props.projects && this.props.projects.length > 0){
      return (
        <ul>
          {this.props.projects.map(project =>
            <Project key={project.name} project={project} />
          )}
        </ul>
      )
    } else {
      return (
        <div>Loading projects ... </div>
      );
    }
  }
}

ProjectsComponent.propTypes = {
  projects: React.PropTypes.array.isRequired
}

export default ProjectsComponent
