import React from 'react';
import AccessRightsComponent from 'common/access-rights/AccessRightsComponent';

class Test extends AccessRightsComponent {

  getDependencies(){
    return {
      'GET' : ["dependencies"]
    }
  }

  render(){
    return (<div>This view shall not be displayed ! </div>);
  }
}

module.exports = Test;
