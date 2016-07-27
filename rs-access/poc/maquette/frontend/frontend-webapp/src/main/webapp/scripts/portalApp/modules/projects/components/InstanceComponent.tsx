/** @module PortalProjects */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'
import { FormattedMessage } from 'react-intl'

interface InstanceProps {
}


/**
 * React component to display the Link to the Instance admin application
 */
class InstanceComponent extends React.Component<InstanceProps, any> {

  render(){
    return (
      <div>
        <FormattedMessage id="instance.access.label" />
        <Link to={"/admin/instance"}><FormattedMessage id="instance.access.link"/></Link><br/>
      </div>
    )
  }
}

export default InstanceComponent
