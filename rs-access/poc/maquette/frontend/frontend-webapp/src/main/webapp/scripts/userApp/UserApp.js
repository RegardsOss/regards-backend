import React from 'react';
import { connect } from 'react-redux';
import scriptjs from 'scriptjs';
import { fetchPlugins } from 'common/plugins/PluginsActions';

import { setTheme } from 'common/theme/ThemeActions';
import { getThemeStyles } from 'common/utils/ThemeUtils';
import PluginLinksView from './modules/plugin/PluginLinksView';
import TestView from './modules/test/TestView';

class UserApp extends React.Component {

  constructor(){
    super();
  }

  componentWillMount(){
    const themeToSet = this.props.params.project;
    const { dispatch, plugins } = this.props;
    dispatch(setTheme(themeToSet));

    if (!plugins || !plugins.items || plugins.items.length === 0){
      dispatch(fetchPlugins());
    }
  }

  render(){
    const styles = getThemeStyles(this.props.theme, 'userApp/base');
    return (
      <div className="full-div">
        <div className="header">
          <h1> Test Application {this.props.params.project} </h1>
        </div>
        <div className="navigation">
          <PluginLinksView project={this.props.params.project} />
        </div>
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

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    plugins: state.plugins
  }
}
module.exports = connect(mapStateToProps)(UserApp);
