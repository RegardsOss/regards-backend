import React from 'react';
import Project from './RegardProject'
import { Link } from 'react-router';

class RegardProject extends React.Component {
  render(){
    return (
      <li>
        {this.props.project.label}&nbsp;-&nbsp;
        <Link to={"/user/" +this.props.project.label}>ihm user</Link>&nbsp;/&nbsp;
        <Link to={"/admin/" +this.props.project.label}>ihm admin</Link>
      </li>
    )
  }
}

RegardProject.propTypes = {
  project: React.PropTypes.object.isRequired
}

export default RegardProject
