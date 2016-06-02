import React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router';

import Project from './ProjectComponent'
import { getThemeStyles } from 'Common/ThemeUtils';

class InstanceComponent extends React.Component {

  render(){
    const styles = getThemeStyles(this.props.theme, 'PortalApp/project');
    return (
      <div className={styles.link}>
        Accès direct à l'ihm d'administration de l'instance :
        <Link to={"/admin/instance"}>ihm admin instance</Link><br/>
      </div>
    )
  }
}

const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
export default connect(mapStateToProps)(InstanceComponent);
