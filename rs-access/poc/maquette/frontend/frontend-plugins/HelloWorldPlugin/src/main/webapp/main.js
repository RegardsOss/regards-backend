import React from 'react';
import ReactDOM from 'react-dom';
import { initPlugin } from './RegardsPlugin';
require('./css/plugin.css');

class HelloWorldPlugin extends React.Component {
  render(){
    return <div className="plugin-bg"><h1 className="hello-style">HelloWorld Plugin from frontend-plugins</h1></div>;
  }
}

initPlugin("HelloWorldPlugin",HelloWorldPlugin);
