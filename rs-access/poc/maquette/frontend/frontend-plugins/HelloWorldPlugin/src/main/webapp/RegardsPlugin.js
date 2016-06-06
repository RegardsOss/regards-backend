import { connect } from 'react-redux';

// Init the regards plugin in the application.
// The plugin is added to the application store to be used by the application.
const initPlugin = ( pluginName, pluginClass ) => {
  const Plugin = connect()(pluginClass)
  let event = new CustomEvent('plugin', {detail:{ name: pluginName, app: Plugin }});
  document.dispatchEvent(event);
}

export { initPlugin }
