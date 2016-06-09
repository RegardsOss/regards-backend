import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';

import ApplicationErrorComponent from 'common/components/ApplicationErrorComponent';
import InstanceComponent from './modules/projects/components/InstanceComponent';
import ProjectsComponent from './modules/projects/components/ProjectsComponent';
import { getThemeStyles } from 'common/theme/ThemeUtils';
import { setTheme } from 'common/theme/ThemeActions';

import { fetchAuthenticate } from 'common/authentication/AuthenticateActions';

class PortalApp extends React.Component {

  componentWillMount(){
    // Init application theme
    const themeToSet = "";
    const { dispatch } = this.props;
    dispatch(setTheme(themeToSet));

    // Connect as public in the first time
    dispatch(fetchAuthenticate("public","public"));
  }

  render(){
    const { authentication, theme } = this.props;
    const styles = getThemeStyles(theme,'portalApp/styles');

    if (!authentication.user){
      return <ApplicationErrorComponent />
    } else if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div className={styles.main}>
        <InstanceComponent />
        <ProjectsComponent styles={styles}/>
      </div>
    )
  }
  }
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    authentication: state.authentication
  }
}
module.exports = connect(mapStateToProps)(PortalApp);
