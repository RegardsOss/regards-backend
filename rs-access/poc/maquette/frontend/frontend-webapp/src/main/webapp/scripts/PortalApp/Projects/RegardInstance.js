import React from 'react';
import CSSModules from 'react-css-modules';

import Project from './RegardProject'
import { Link } from 'react-router';

import styles from 'PortalApp/instance';

class RegardInstance extends React.Component {
  render(){
    return (
      <div styleName="link">
        Accès direct à l'ihm d'administration de l'instance :
        <Link to={"/admin/instance"}>ihm admin instance</Link><br/>
      </div>
    )
  }
}

export default CSSModules(RegardInstance, styles);
