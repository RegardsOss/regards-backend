import React from 'react';
import { connect } from 'react-redux';
import AccessRightsComponent from 'common/access-rights/AccessRightsComponent';

class TestView extends AccessRightsComponent {

  constructor(){
    super();
  }
  getDependencies(){
    return {
      "GET" : ["dependence"]
    }
  }

  renderView(){
    return (<div>This view shall not be displayed ! </div>);
  }
}

module.exports = TestView;
