import React from 'react';
import { connect } from 'react-redux';
import { IndexLink, Link } from 'react-router';

import { logout } from 'common/authentication/AuthenticateActions';
import { getThemeStyles } from 'common/theme/ThemeUtils';

class Menu extends React.Component {

  render(){
    const { dispatch, theme, project } = this.props;
    const styles = getThemeStyles(theme, 'adminApp/menu');
    return (
        <div>
          <IndexLink to={"/admin/" + project}
             className={styles.unselected}
             activeClassName={styles.selected}>
             Home
          </IndexLink>
          <Link to={"/admin/"+project+"/test"}
            className={styles.unselected}
            activeClassName={styles.selected}>
            TestModule
          </Link>
          <span className={styles.unselected}
            onClick={() => {
              dispatch(logout());
            }}>Log out</span>
        </div>
      )
  }
}

const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
export default connect(mapStateToProps)(Menu);
