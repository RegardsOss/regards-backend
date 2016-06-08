import React from 'react';
import { connect } from 'react-redux';

import { setTheme } from 'common/theme/ThemeActions';
import { getThemeStyles } from 'common/theme/ThemeUtils';
import NavigationContainer from './containers/NavigationContainer';
import TestView from '../test/Test';

class Layout extends React.Component {

  render(){
    const styles = getThemeStyles(this.props.theme, 'userApp/base');
    return (
      <div className="full-div">
        <div className="header">
          <h1> Test Application {this.props.project} </h1>
        </div>
        <NavigationContainer project={this.props.project} location={this.props.location}/>
        <div className={styles.main}>
          {this.props.content}
        </div>
        <div>
          <TestView />
        </div>
      </div>
    )
  }
}

Layout.propTypes = {
  project: React.PropTypes.string.isRequired,
  content: React.PropTypes.object.isRequired
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    plugins: state.plugins
  }
}
export default connect(mapStateToProps)(Layout);
