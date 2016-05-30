import React from 'react';
import Project from './RegardProject'
import { Rest } from 'grommet';

class RegardProjects extends React.Component {

  constructor(){
    super();
      this.state = {
        projects : []
      };
  }

  componentDidMount(){
    this.loadProjects();
  }

  loadProjects() {
    const location = window.location.origin + '/json/projects.json';
    Rest.get(location)
      .end((error, response) => {
        console.log("Available projects : ",response.body.projects);
        if (response.status === 200){
          this.setState({
            projects : response.body.projects
          });
        } else {
          console.log(response);
        }
      });
  }

  render(){
    if (this.state.projects && this.state.projects.length > 0){
      return (
        <ul>
          {this.state.projects.map(project =>
            <Project key={project.label} project={project} />
          )}
        </ul>
      )
    } else {
      return <div>Loading projects ... </div>
    }
  }

}

export default RegardProjects
