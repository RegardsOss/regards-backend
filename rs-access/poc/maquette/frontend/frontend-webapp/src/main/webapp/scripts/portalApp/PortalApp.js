import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';

import ApplicationErrorComponent from 'common/components/ApplicationErrorComponent';
import InstanceComponent from './modules/projects/components/InstanceComponent';
import ProjectsContainer from './modules/projects/containers/ProjectsContainer';
import { getThemeStyles } from 'common/theme/ThemeUtils';
import { setTheme } from 'common/theme/ThemeActions';

import { fetchAuthenticate } from 'common/authentication/AuthenticateActions';

class PortalApp extends React.Component {

  componentWillMount(){
    // Init application theme
    this.props.onInitTheme("");
    this.props.onPublicAuthenticate();
  }

  render(){
    const { authentication, theme } = this.props;
    const styles = getThemeStyles(theme,'portalApp/styles');

    if (authentication && !authentication.user){
      return <ApplicationErrorComponent />
    } else if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div className={styles.main}>
        <InstanceComponent />
        <ProjectsContainer styles={styles}/>
      </div>
    )
  }
  }
}

// Add props from store to the container props
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    authentication: state.authentication
  }
}
// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch) => {
  return {
    onPublicAuthenticate: () => dispatch(fetchAuthenticate("public","public")),
    onInitTheme: (theme) =>  dispatch(setTheme(theme))
  }
}
module.exports = connect(mapStateToProps,mapDispatchToProps)(PortalApp);
