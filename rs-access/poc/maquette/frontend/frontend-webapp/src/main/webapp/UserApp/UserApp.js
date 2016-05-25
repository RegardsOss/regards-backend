import React from 'react';
import scriptjs from 'scriptjs';
import { loadPlugins } from 'Common/PluginsManager/PluginsControler';

import PluginLinks from './PluginLinks';

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
  }

  componentDidMount(){
    this.unsubscribe = this.context.store.subscribe(()=> {
      this.setState({
        pluginsLoaded : this.context.store.getState().pluginsLoaded,
        plugins: this.context.store.getState().plugins
      });
    });

    if (!this.context.store.getState().pluginsLoaded){
      loadPlugins( (plugin) => {
        console.log("Plugin loaded");
      });
    }
  }

  componentWillUnmount(){
    this.unsubscribe();
  }

  render(){
    if (!this.state.pluginsLoaded ){
      return <div>Loading ... </div>
    } else {
      return (
        <div className="full-div">
          <div className="header">
            <h1> Test Application {this.state.project} </h1>
          </div>
          <div className="navigation">
            Plugins : <PluginLinks plugins={this.state.plugins}/>
          </div>
          <div className="content full-div">
            {this.props.content}
          </div>
        </div>
      )
    }
  }
}

UserApp.contextTypes = {
  store: React.PropTypes.object,
  router: React.PropTypes.object,
  route : React.PropTypes.object
}

module.exports = UserApp
