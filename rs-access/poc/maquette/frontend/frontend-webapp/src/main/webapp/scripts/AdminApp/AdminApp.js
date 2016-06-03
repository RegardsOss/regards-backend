
import React from 'react';
import { connect } from 'react-redux';
import ReactDOM from 'react-dom';
import { Rest } from 'grommet';

import { setTheme, logout } from 'Common/Store/CommonActionCreators';
import { getThemeStyles } from 'Common/ThemeUtils';
import AuthenticateView from './Authentication/AuthenticateView';

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
    const { dispatch } = this.props;
    const styles = getThemeStyles(this.props.theme, 'AdminApp/base');
    if (!this.props.authenticated){
      return (
        <div className={styles.main}>
          <AuthenticateView
            project={this.props.params.project}
            onAuthenticate={this.onAuthenticate}/>
        </div>
      );
    } else {
      let message = "Welcome to project : " + this.props.params.project;
      if (this.state.instance){
        message = "Welcome to instance admin";
      }

      return (
        <div className={styles.main}>
          {message}
          <button onClick={() => {
              Rest.setHeaders({
                'Accept': 'application/json',
                'Authorization': "Basic " + btoa("acme:acmesecret")
              });
              dispatch(logout());
          }}>Log out</button>
        </div>
      );
    }
  }
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    authenticated: state.authenticated
  }
}
module.exports = connect(mapStateToProps)(AdminApp);
