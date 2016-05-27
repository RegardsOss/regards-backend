import React from 'react';
import ReactDOM from 'react-dom';
import { initRegardsPlugin } from './RegardsPlugin';
require('./css/plugin.css');

class HelloWorldPlugin extends React.Component {
  componentDidMount(){
    const { store, dispatch } = this.props;
    dispatch({
      type: "ADD_TODO",
      text: "Injecté par le plugin HelloWorldPlugin",
      completed: false
    });
  }
  render(){
    const { store, dispatch } = this.props;
    console.log("HelloWorld Store :",store);
    return <div className="plugin-bg"><h1 className="hello-style">NEW HelloWorld Plugin</h1></div>;
  }
}

initRegardsPlugin("HelloWorldPlugin",HelloWorldPlugin);
