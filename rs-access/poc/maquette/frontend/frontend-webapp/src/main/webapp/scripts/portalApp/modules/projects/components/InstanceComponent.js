import React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

class InstanceComponent extends React.Component {

  render(){
    // styles props is passed throught the react component creation
    const { styles } = this.props
    return (
      <div className={styles["instance-link"]}>
        Accès direct à l'ihm d'administration de l'instance :
        <Link to={"/admin/instance"}>ihm admin instance</Link><br/>
      </div>
    )
  }
}

InstanceComponent.propTypes = {
  styles: React.PropTypes.object.isRequired
}

export default InstanceComponent
