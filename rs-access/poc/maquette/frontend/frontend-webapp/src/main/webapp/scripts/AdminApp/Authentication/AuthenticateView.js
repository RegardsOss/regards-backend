import React from 'react';
import { Rest } from 'grommet';
import CSSModules from 'react-css-modules';

import LoginComponent from './LoginComponent';

import styles from 'AdminApp/base';



class AuthenticateView extends React.Component {

  constructor(){
    super();

    this.state = {
      error : ""
    }

    this.onLogin = this.onLogin.bind(this);
    this.onResponse = this.onResponse.bind(this);
  }

  onLogin(userName, password){
    Rest.setHeaders({
      'Accept': 'application/json',
      'Authorization': "Basic " + btoa("acme:acmesecret")
    });

    const location = "http://localhost:8080/oauth/token?grant_type=password&username="
    + userName + "&password=" +password;
    Rest.post(location).end(this.onResponse);
  }

  onResponse(err, response){
    console.log(err);
    if (err && err.timeout > 1000) {
     this.setState({error: 'Timeout'});
   } else if (err) {
     this.setState({error:  "Service unavailable"});
   } else if (response.status === 400) {
     this.setState({error: "Authentication error : " + response.body.error_description});
   } else if (response.status != 200){
     this.setState({error: "Authentication error"});
   } else {
     console.log("OK");
     this.props.onAuthenticate(response.body.access_token);
   }
  }

  render(){
    return (
      <LoginComponent
        onLogin={this.onLogin}
        errorMessage={this.state.error}/>
    );
  }
}

export default CSSModules(AuthenticateView,styles);
