import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router'
import ReactDOM from 'react-dom';
import Rest from 'grommet/utils/Rest';

import InstanceComponent from './projects/InstanceComponent';
import ProjectsComponent from './projects/ProjectsComponent';
import { getThemeStyles } from 'common/utils/ThemeUtils';
import { setTheme } from 'common/store/CommonActionCreators';

class PortalApp extends React.Component {

  componentWillMount(){
    // Init application theme
    const themeToSet = "";
    const { dispatch } = this.props;
    dispatch(setTheme(themeToSet));
  }

  render(){
    const styles = getThemeStyles(this.props.theme,'portalApp/base');
    if (this.props.children){
      return <div>{this.props.children}</div>
    } else {
    return (
      <div className={styles.main}>
        <InstanceComponent />
        <ProjectsComponent />
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
