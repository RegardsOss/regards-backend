import { connect } from 'react-redux';

// Init the regards plugin in the application.
// The plugin is added to the application store to be used by the application.
const initRegardsPlugin = ( pluginName, pluginClass ) => {
  const mapDispatchToProps = ( dispatch ) => {return { dispatch: dispatch }};
  const mapStateToProps = ( state ) => {return { store: state } };
  const Plugin = connect(mapStateToProps)(pluginClass)
  let event = new CustomEvent('plugin', {detail:{ name: pluginName, app: Plugin }});
  document.dispatchEvent(event);
}

export { initRegardsPlugin }
