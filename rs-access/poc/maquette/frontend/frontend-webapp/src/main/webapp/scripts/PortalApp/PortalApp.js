import React from 'react';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import { Rest } from 'grommet';

import InstanceComponent from './Projects/InstanceComponent';
import ProjectsComponent from './Projects/ProjectsComponent';
import { getThemeStyles } from 'Common/ThemeUtils';

class PortalApp extends React.Component {

  constructor(){
    super();
      this.state = {
        projects : []
      };

      this.loadProjects = this.loadProjects.bind(this);
  }

  componentDidMount(){
    this.loadProjects();
  }

  loadProjects() {
    const location = 'http://localhost:8080/api/projects';
    Rest.get(location)
      .end((error, response) => {
        console.log("Available projects : ",response.body);
        if (response.status === 200){
          this.setState({
            projects : response.body
          });
        } else {
          console.log(response);
        }
      });
  }

  render(){
    const styles = getThemeStyles('','PortalApp/base');
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div className={styles.main}>
        <InstanceComponent theme=""/>
        Available projects on REGARDS instance :
        <ProjectsComponent theme="" projects={this.state.projects}/>
      </div>
    )
  }
  }
}

module.exports = PortalApp;
