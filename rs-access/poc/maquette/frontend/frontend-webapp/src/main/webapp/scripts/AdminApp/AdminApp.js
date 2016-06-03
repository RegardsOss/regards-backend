
import React from 'react';
import { connect } from 'react-redux';
import ReactDOM from 'react-dom';
import { Rest } from 'grommet';

import { setTheme } from 'Common/Store/CommonActionCreators';
import { getThemeStyles } from 'Common/ThemeUtils';
import AuthenticateView from './Authentication/AuthenticateView';

class AdminApp extends React.Component {
  constructor(){
    super();

    this.state = {
      authenticated: false,
      instance: false
    }
    this.onAuthenticate = this.onAuthenticate.bind(this);
  }

  componentWillMount(){

    let themeToSet = this.props.params.project;

    if (this.props.params.project === "instance"){
      this.setState({instance: true});
      themeToSet = "default";
    }

    const { dispatch } = this.props;
    dispatch(setTheme(themeToSet));
  }

  onAuthenticate(token){

    // Add token to rest requests
    Rest.setHeaders({
      'Authorization': "Bearer " + token
    });

    this.setState({
      authenticated: true
    });

    Rest.get("http://localhost:8080/controler").end((err,response) => {
      console.log(response);
    });
  }

  render(){
    const styles = getThemeStyles(this.props.theme, 'AdminApp/base');
    if (!this.state.authenticated){
      return (
        <div className={styles.main}>
          <AuthenticateView
            project={this.props.params.project}
            onAuthenticate={this.onAuthenticate}/>
        </div>
      );
    } else {
        if (this.state.instance){
          return (
            <div>Welcome to project : {this.props.params.project}</div>
          );
        } else {
          return (
            <div>Welcome to instance admin</div>
          );
        }
    }
  }
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
module.exports = connect(mapStateToProps)(AdminApp);
