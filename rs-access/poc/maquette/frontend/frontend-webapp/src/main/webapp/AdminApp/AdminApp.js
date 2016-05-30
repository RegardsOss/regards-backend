import React from 'react';
import ReactDOM from 'react-dom';
import AuthenticateView from './Authentication/AuthenticateView';

import { Rest } from 'grommet';

import './stylesheets/base';

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
    if (this.props.project === "instance"){
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
    if (!this.state.authenticated){
      return (
          <AuthenticateView
            className="login-modal"
            project={this.props.params.project}
            onAuthenticate={this.onAuthenticate}/>
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
