import React from 'react';
import { Rest } from 'grommet';

class AuthenticateView extends React.Component {

  constructor(){
    super();

    this.state = {
      username: "",
      password: "",
      error :""
    }

    this.onLogin = this.onLogin.bind(this);
    this.onResponse = this.onResponse.bind(this);
  }

  onLogin(){
    console.log(this.state);
    const params = {
      grant_type: "password",
      username: this.state.username,
      password: this.state.password
    }
    Rest.setHeaders({
      'Accept': 'application/json',
      'Authorization': "Basic " + btoa("acme:acmesecret")
    });
    const location = "http://localhost:8080/oauth/token?grant_type=password&username="
    + this.state.username + "&password=" + this.state.password;
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
      <div className={this.props.className}>
        <p className="login-error">{this.state.error}</p>
        <label for="username" >Username</label>
        <input id="username" onChange={(event) => {
             this.setState({username:event.target.value});
          }}/>
        <br/>
        <label for="password" >Password</label>
        <input type="password" id="password"
          onChange={(event) => {
               this.setState({password:event.target.value});
            }}/>
        <br/>
        <button onClick={this.onLogin}>Log in</button>
      </div>
    );
  }
}

export default AuthenticateView;
