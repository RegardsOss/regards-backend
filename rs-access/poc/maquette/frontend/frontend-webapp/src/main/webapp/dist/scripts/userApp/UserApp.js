"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const PluginsActions_1 = require('../common/plugins/PluginsActions');
const ThemeActions_1 = require('../common/theme/actions/ThemeActions');
const Layout_1 = require('./modules/layout/Layout');
const Test_1 = require('./modules/test/Test');
class UserApp extends React.Component {
    componentWillMount() {
        // Get project from params from react router. project param is the ":project" in userApp route
        // See routes.js
        const themeToSet = this.props.params.project;
        // Plugins are set to the container props by react-redux connect.
        // See method mapStateToProps of this container
        const { plugins } = this.props;
        // initTheme method is set to the container props by react-redux connect.
        // See method mapDispatchToProps of this container
        this.props.initTheme(themeToSet);
        if (!plugins || !plugins.items || plugins.items.length === 0) {
            // fetchPlugins method is set to the container props by react-redux connect.
            // See method mapDispatchToProps of this container
            this.props.fetchPlugins();
        }
    }
    render() {
        // Location ,params and content are set in this container props by react-router
        const { location, params, content } = this.props;
        const { project } = params;
        if (!content) {
            return (React.createElement(Layout_1.default, {location: location, project: project}, React.createElement(Test_1.default, null)));
        }
        else {
            return (React.createElement(Layout_1.default, {location: location, project: project}, this.props.content));
        }
    }
}
// Add functions dependending on store dispatch to container props.
const mapDispatchToProps = (dispatch) => {
    return {
        fetchPlugins: () => dispatch(PluginsActions_1.fetchPlugins()),
        initTheme: (theme) => dispatch(ThemeActions_1.setTheme(theme))
    };
};
const mapStateToProps = (state) => {
    return {
        plugins: state.plugins
    };
};
const userAppConnected = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(UserApp);
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = userAppConnected;
//# sourceMappingURL=UserApp.js.map