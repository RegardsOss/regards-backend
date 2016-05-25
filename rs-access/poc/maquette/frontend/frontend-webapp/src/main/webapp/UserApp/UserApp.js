import React from 'react';
import scriptjs from 'scriptjs';
import { loadPlugins } from 'Common/PluginsManager/PluginsControler';

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
        pluginsLoaded : this.context.store.getState().pluginsLoaded
      });
    });

    if (!this.context.store.getState().pluginsLoaded){
      loadPlugins( (plugin) => {
        this.setState({
          plugins : [...this.state.plugins,{
            name: plugin.name,
            plugin: plugin.app}]
        });
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
            Menu
          </div>
          <div className="content full-div">
            Content
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
