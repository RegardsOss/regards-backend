import React from 'react';
import { connect } from 'react-redux';
import { IndexLink, Link } from 'react-router';

import icons from 'stylesheets/foundation-icons/foundation-icons.scss';

class Menu extends React.Component {

  render(){
    const { onLogout, project, styles } = this.props;

    return (
        <div>
          <IndexLink to={"/admin/" + project}
             className={styles.item +" "+ styles.unselected}
             activeClassName={styles.selected}>
             Home
          </IndexLink>
          <Link to={"/admin/"+project+"/test"}
            className={styles.item +" "+ styles.unselected}
            activeClassName={styles.selected}>
            TestModule
          </Link>
          <span
            className={styles.item +" "+ styles.unselected}
            onClick={onLogout}>
            <i className={icons["fi-power"]} title="Logout"></i>
          </span>
        </div>
      )
  }
}

Menu.propTypes = {
  styles: React.PropTypes.object.isRequired,
  project: React.PropTypes.string.isRequired,
  onLogout: React.PropTypes.func.isRequired,
}

export default Menu;
