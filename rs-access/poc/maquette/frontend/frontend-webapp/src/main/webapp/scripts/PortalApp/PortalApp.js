import React from 'react';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import CSSModules from 'react-css-modules';
import { Rest } from 'grommet';

import InstanceComponent from './Projects/InstanceComponent';
import ProjectsComponent from './Projects/ProjectsComponent';

import styles from 'PortalApp/base';


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
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div styleName="main">
        <InstanceComponent />
        Available projects on REGARDS instance :
        <ProjectsComponent projects={this.state.projects}/>
      </div>
    )
  }
  }
}

module.exports = CSSModules(PortalApp, styles);
