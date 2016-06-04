import React from 'react';
import { connect } from 'react-redux';
import Rest from 'grommet/utils/Rest';

import { authenticated } from 'common/store/CommonActionCreators';
import LoginComponent from 'adminApp/components/LoginComponent';

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
   } else if (!response) {
     this.setState({error:  "Service unavailable"});
   } else if (response.status != 200){
     this.setState({error: "Authentication error"});
   } else if (response.status === 200){
     // Add token to rest requests
     Rest.setHeaders({
       'Authorization': "Bearer " + response.body.access_token
     });

     const { dispatch } = this.props;
     dispatch(authenticated(true));
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

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    authenticated: state.authenticated
  }
}
export default connect(mapStateToProps)(AuthenticateView);
