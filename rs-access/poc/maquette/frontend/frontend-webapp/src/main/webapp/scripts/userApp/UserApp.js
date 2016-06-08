import React from 'react';
import { connect } from 'react-redux';

import { fetchPlugins } from 'common/plugins/PluginsActions';
import { setTheme } from 'common/theme/ThemeActions';
import Layout from './modules/layout/Layout';

class UserApp extends React.Component {

  componentWillMount(){
    const themeToSet = this.props.params.project;
    const { dispatch, plugins } = this.props;
    dispatch(setTheme(themeToSet));

    if (!plugins || !plugins.items || plugins.items.length === 0){
      dispatch(fetchPlugins());
    }
  }

  render(){
    const { location, content, params } = this.props;
    const { project } = params;
    return (<Layout location={location} content={content} project={project}/>);
  }
}

module.exports = connect()(UserApp);
