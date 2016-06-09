import React from 'react';
import { connect } from 'react-redux';
import AccessRightsComponent from 'common/access-rights/AccessRightsComponent';

class Test extends AccessRightsComponent {

  getDependencies(){
    return {
      'GET' : ["dependencies"]
    }
  }

  render(){
    if (this.state.access == true){
      return (<div>This view shall not be displayed ! </div>);
    } else {
      return null;
    }
  }
}

module.exports = connect()(Test);
