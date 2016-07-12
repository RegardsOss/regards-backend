"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ApplicationErrorComponent_1 = require('../common/components/ApplicationErrorComponent');
const SelectThemeComponent_1 = require('../common/theme/components/SelectThemeComponent');
const InstanceComponent_1 = require('./modules/projects/components/InstanceComponent');
const ProjectsContainer_1 = require('./modules/projects/containers/ProjectsContainer');
const ThemeUtils_1 = require('../common/theme/ThemeUtils');
const ThemeActions_1 = require('../common/theme/actions/ThemeActions');
const AuthenticateActions_1 = require('../common/authentication/AuthenticateActions');
class PortalApp extends React.Component {
    componentWillMount() {
        this.props.initTheme("");
        this.props.publicAuthenticate();
    }
    render() {
        const { authentication, theme } = this.props;
        const styles = ThemeUtils_1.getThemeStyles(theme, 'portalApp/styles');
        const commonStyles = ThemeUtils_1.getThemeStyles(theme, 'common/common.scss');
        if (!authentication || authentication.isFetching === true || !authentication.user || !authentication.user.access_token) {
            return React.createElement(ApplicationErrorComponent_1.default, null);
        }
        else if (this.props.children) {
            return (React.createElement("div", null, this.props.children));
        }
        else {
            return (React.createElement("div", {className: styles.main}, 
                React.createElement(InstanceComponent_1.default, {styles: styles}), 
                React.createElement(ProjectsContainer_1.default, {styles: styles}), 
                React.createElement(SelectThemeComponent_1.default, {styles: commonStyles, themes: ["cdpp", "ssalto", "default"], curentTheme: theme, onThemeChange: this.props.initTheme})));
        }
    }
}
const mapStateToProps = (state) => {
    return {
        theme: state.common.theme,
        authentication: state.common.authentication
    };
};
const mapDispatchToProps = (dispatch) => {
    return {
        publicAuthenticate: () => dispatch(AuthenticateActions_1.fetchAuthenticate("public", "public")),
        initTheme: (theme) => dispatch(ThemeActions_1.setTheme(theme))
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(PortalApp);
//# sourceMappingURL=PortalApp.js.map