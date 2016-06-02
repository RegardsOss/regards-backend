
import React from 'react';
import ReactDOM from 'react-dom';
import { Rest } from 'grommet';

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
    if (this.props.params.project === "instance"){
      this.setState({instance: true});
    }
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
    let styles;
    let theme=this.props.params.project;
    if (this.state.instance === true){
      theme="";
      styles = getThemeStyles("", 'AdminApp/base');
    } else {
      styles = getThemeStyles(this.props.params.project, 'AdminApp/base');
    }
    if (!this.state.authenticated){
      return (
        <div className={styles.main}>
          <AuthenticateView
            theme={theme}
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

module.exports = AdminApp;
