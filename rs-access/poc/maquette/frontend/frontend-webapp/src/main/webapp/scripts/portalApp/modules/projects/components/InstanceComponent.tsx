import * as React from 'react'
import { connect } from 'react-redux'
import { Link } from 'react-router'

interface InstacenProps {
  styles: any
}

class InstanceComponent extends React.Component<InstacenProps, any> {

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

export default InstanceComponent
