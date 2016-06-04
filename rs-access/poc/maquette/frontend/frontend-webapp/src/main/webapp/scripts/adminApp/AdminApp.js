
import React from 'react';
import { connect } from 'react-redux';
import ReactDOM from 'react-dom';
import Rest from 'grommet/utils/Rest';

import { setTheme, authenticated } from 'common/store/CommonActionCreators';
import { getThemeStyles } from 'common/utils/ThemeUtils';
import AuthenticateView from './containers/AuthenticateContainer';
import LayoutContainer from './containers/LayoutContainer';

class AdminApp extends React.Component {
  constructor(){
    super();
    this.state = {
      instance: false
    }
  }

  componentWillMount(){
    // Init admin theme
    let themeToSet = this.props.params.project;
    if (this.props.params.project === "instance"){
      this.setState({instance: true});
      themeToSet = "default";
    }
    const { dispatch } = this.props;
    dispatch(setTheme(themeToSet));
  }

  render(){
    const { theme, authenticated, content, params } = this.props;
    const styles = getThemeStyles(theme, 'adminApp/base');
    if (!authenticated){
      return (
        <div className={styles.main}>
          <AuthenticateView
            project={params.project}
            onAuthenticate={this.onAuthenticate}/>
        </div>
      );
    } else {
        return (
          <LayoutContainer
            content={content}
            project={params.project}
            instance={this.state.instance}/>
        );
    }
  }
}

AdminApp.contextTypes = {
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    authenticated: state.authenticated
  }
}
module.exports = connect(mapStateToProps)(AdminApp);
