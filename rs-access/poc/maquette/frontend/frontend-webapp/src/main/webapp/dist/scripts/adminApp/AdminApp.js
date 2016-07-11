"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ThemeActions_1 = require('../common/theme/actions/ThemeActions');
const AuthenticateActions_1 = require('../common/authentication/AuthenticateActions');
const ThemeUtils_1 = require('../common/theme/ThemeUtils');
const Authentication_1 = require('./modules/authentication/Authentication');
const SelectThemeComponent_1 = require('../common/theme/components/SelectThemeComponent');
const Layout_1 = require('./modules/layout/Layout');
class AdminApp extends React.Component {
    constructor() {
        super();
        this.state = {
            instance: false
        };
        this.changeTheme = this.changeTheme.bind(this);
    }
    componentWillMount() {
        let themeToSet = this.props.params.project;
        if (this.props.params.project === "instance") {
            this.setState({ instance: true });
            themeToSet = "default";
        }
        this.props.setTheme(themeToSet);
    }
    changeTheme(themeToSet) {
        if (this.props.theme !== themeToSet) {
            this.props.setTheme(themeToSet);
        }
    }
    render() {
        const { theme, authentication, content, location, params, onLogout } = this.props;
        const styles = ThemeUtils_1.getThemeStyles(theme, 'adminApp/styles');
        const commonStyles = ThemeUtils_1.getThemeStyles(theme, 'common/common.scss');
        const authenticated = authentication.authenticateDate + authentication.user.expires_in > Date.now();
        if (!authenticated || authentication.user.name === 'public') {
            return (React.createElement("div", {className: styles.main}, 
                React.createElement(Authentication_1.default, {project: params.project, onAuthenticate: this.onAuthenticate}), 
                React.createElement(SelectThemeComponent_1.default, {styles: commonStyles, themes: ["cdpp", "ssalto", "default"], curentTheme: theme, onThemeChange: this.changeTheme})));
        }
        else {
            return (React.createElement("div", null, 
                React.createElement(Layout_1.default, {styles: styles, location: location, content: content, project: params.project, instance: this.state.instance, onLogout: onLogout}), 
                React.createElement(SelectThemeComponent_1.default, {styles: styles, themes: ["cdpp", "ssalto", "default"], curentTheme: theme, onThemeChange: this.changeTheme})));
        }
    }
}
const mapStateToProps = (state) => {
    return {
        theme: state.theme,
        authentication: state.authentication
    };
};
const mapDispatchToProps = (dispatch) => {
    return {
        setTheme: (theme) => { dispatch(ThemeActions_1.setTheme(theme)); },
        onLogout: () => { dispatch(AuthenticateActions_1.logout()); }
    };
};
const connectedAdminApp = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(AdminApp);
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = connectedAdminApp;
module.exports = connectedAdminApp;
//# sourceMappingURL=AdminApp.js.map