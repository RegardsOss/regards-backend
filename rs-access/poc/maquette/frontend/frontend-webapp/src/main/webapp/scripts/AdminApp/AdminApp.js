
import React from 'react';
import ReactDOM from 'react-dom';
import AuthenticateView from './Authentication/AuthenticateView';
import CSSModules from 'react-css-modules';
import { Rest } from 'grommet';

import styles from 'AdminApp/base';

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
        <div styleName="main">
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

module.exports = CSSModules(AdminApp,styles);
