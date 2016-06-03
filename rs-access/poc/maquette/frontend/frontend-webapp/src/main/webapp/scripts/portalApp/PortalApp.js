import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import { Rest } from 'grommet';

import InstanceComponent from './projects/InstanceComponent';
import ProjectsComponent from './projects/ProjectsComponent';
import { getThemeStyles } from 'common/ThemeUtils';
import { setTheme } from 'common/store/CommonActionCreators';

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

  componentWillMount(){
    const themeToSet = "";
    const { dispatch } = this.props;
    dispatch(setTheme(themeToSet));
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
    const styles = getThemeStyles(this.props.theme,'portalApp/base');
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div className={styles.main}>
        <InstanceComponent />
        Available projects on REGARDS instance :
        <ProjectsComponent projects={this.state.projects}/>
      </div>
    )
  }
  }
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
module.exports = connect(mapStateToProps)(PortalApp);
