"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ThemeUtils_1 = require('../../../common/theme/ThemeUtils');
const ThemeActions_1 = require('../../../common/theme/actions/ThemeActions');
const SelectThemeComponent_1 = require('../../../common/theme/components/SelectThemeComponent');
const NavigationContainer_1 = require('./containers/NavigationContainer');
class Layout extends React.Component {
    render() {
        const { theme } = this.props;
        const styles = ThemeUtils_1.getThemeStyles(this.props.theme, 'userApp/base');
        const commonStyles = ThemeUtils_1.getThemeStyles(theme, 'common/common.scss');
        return (React.createElement("div", {className: "full-div"}, React.createElement("div", {className: "header"}, React.createElement("h1", null, " Test Application ", this.props.project, " ")), React.createElement(NavigationContainer_1.default, {project: this.props.project, location: this.props.location}), React.createElement("div", {className: styles.main}, this.props.children), React.createElement(SelectThemeComponent_1.default, {styles: commonStyles, themes: ["cdpp", "ssalto", "default"], curentTheme: theme, onThemeChange: this.props.setTheme})));
    }
}
// Add theme from store to the component props
const mapStateToProps = (state) => {
    return {
        theme: state.common.theme
    };
};
const mapDispatchToProps = (dispatch) => {
    return {
        setTheme: (theme) => dispatch(ThemeActions_1.setTheme(theme))
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(Layout);
//# sourceMappingURL=Layout.js.map