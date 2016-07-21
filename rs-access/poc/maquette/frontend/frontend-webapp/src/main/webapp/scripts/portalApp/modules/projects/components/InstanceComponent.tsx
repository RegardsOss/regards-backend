/** @module PortalProjects */
import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'
import { FormattedMessage } from 'react-intl'

interface InstacenProps {
  styles: any
}


/**
 * React component to display the Link to the Instance admin application
 */
class InstanceComponent extends React.Component<InstacenProps, any> {

  render(){
    // styles props is passed throught the react component creation
    const { styles } = this.props
    return (
      <div className={styles["instance-link"]}>
        <FormattedMessage id="instance.access.label" />
        <Link to={"/admin/instance"}><FormattedMessage id="instance.access.link"/></Link><br/>
      </div>
    )
  }
}

export default InstanceComponent
