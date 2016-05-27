import React from 'react';
import ReactDOM from 'react-dom';

class AdminApp extends React.Component {
  render(){
    return <div>Admin APP : {this.props.params.project}</div>
  }
}

module.exports = AdminApp;
