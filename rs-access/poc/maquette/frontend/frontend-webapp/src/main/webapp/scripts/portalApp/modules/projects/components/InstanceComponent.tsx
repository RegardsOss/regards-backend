/** @module PortalProjects */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

interface InstanceProps {
}


/**
 * React component to display the Link to the Instance admin application
 */
class InstanceComponent extends React.Component<InstanceProps, any> {

  render(){
    return (
      <div>
        Accès direct à l'ihm d'administration de l'instance :
        <Link to={"/admin/instance"}>ihm admin instance</Link><br/>
      </div>
    )
  }
}

export default InstanceComponent
