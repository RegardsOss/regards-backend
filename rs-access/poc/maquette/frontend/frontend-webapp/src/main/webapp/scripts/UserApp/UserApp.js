import React from 'react';
import { connect } from 'react-redux';
import scriptjs from 'scriptjs';
import { loadPlugins } from 'Common/PluginsManager/PluginsControler';

import { setTheme } from 'Common/Store/CommonActionCreators';
import { getThemeStyles } from 'Common/ThemeUtils';
import PluginLinksView from './PluginModule/PluginLinksView';
import TestView from './TestModule/TestView';

class UserApp extends React.Component {

  constructor(){
    super();
  }

  componentWillMount(){
    this.state = {
      project : this.props.params.project,
      plugins : this.context.store.getState().plugins,
      pluginsLoaded : this.context.store.getState().pluginsLoaded
    }
    const themeToSet = this.props.params.project;
    const { dispatch } = this.props;
    dispatch(setTheme(themeToSet));
  }

  componentDidMount(){
    const { store } = this.context;
    this.unsubscribe = store.subscribe(()=> {
      this.setState({
        pluginsLoaded : store.getState().pluginsLoaded,
        plugins: store.getState().plugins
      });
    });

    if (!store.getState().pluginsLoaded){
      loadPlugins();
    }
  }

  componentWillUnmount(){
    this.unsubscribe();
  }

  render(){
    const styles = getThemeStyles(this.props.theme, 'UserApp/base');
    if (!this.state.pluginsLoaded ){
      return <div>Loading ... </div>
    } else {
      return (
        <div className="full-div">
          <div className="header">
            <h1> Test Application {this.state.project} </h1>
          </div>
          <div className="navigation">
            <PluginLinksView project={this.state.project} plugins={this.state.plugins}/>
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
}

UserApp.propTypes = {
  params: React.PropTypes.object.isRequired
}

UserApp.contextTypes = {
  store: React.PropTypes.object,
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

// Add theme from store to the component props
const mapStateToProps = (state) => {
  return {
    theme: state.theme
  }
}
module.exports = connect(mapStateToProps)(UserApp);
