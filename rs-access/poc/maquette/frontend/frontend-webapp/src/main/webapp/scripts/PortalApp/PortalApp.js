import React from 'react';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import CSSModules from 'react-css-modules';
import { Rest } from 'grommet';

import RegardInstance from './Projects/InstanceComponent';
import RegardProjects from './Projects/ProjectsComponent';

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
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div styleName="main">
        <RegardInstance />
        Available projects on REGARDS instance :
        <RegardProjects projects={this.state.projects}/>
      </div>
    )
  }
  }
}

module.exports = CSSModules(PortalApp, styles);
