"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const MenuComponent_1 = require('./components/MenuComponent');
const Home_1 = require('../home/Home');
// Styles
const ThemeUtils_1 = require('../../../common/theme/ThemeUtils');
var classnames = require('classnames');
class Layout extends React.Component {
    render() {
        // Add location to menu props in order to update view if location
        // change. This enable the activeClassName to update in the react
        // router links.
        const { theme, project, location, onLogout } = this.props;
        const styles = ThemeUtils_1.getThemeStyles(theme, 'adminApp/styles');
        const layoutClassName = classnames(styles['layout'], styles['row']);
        const contentClassName = classnames(styles['content'], styles['small-12'], styles['large-11'], styles['columns']);
        return (React.createElement("div", {className: layoutClassName}, React.createElement(MenuComponent_1.default, {theme: theme, onLogout: onLogout, project: project, location: location}), React.createElement("div", {className: contentClassName}, this.props.content || React.createElement(Home_1.default, null))));
    }
}
// Add theme from store to the component props
const mapStateToProps = (state) => ({
    theme: state.common.theme
});
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps)(Layout);
//# sourceMappingURL=Layout.js.map