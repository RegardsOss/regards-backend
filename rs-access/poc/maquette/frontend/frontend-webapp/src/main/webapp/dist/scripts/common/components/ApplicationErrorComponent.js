"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ThemeUtils_1 = require('../theme/ThemeUtils');
class ApplicationErrorComponent extends React.Component {
    render() {
        const styles = ThemeUtils_1.getThemeStyles(this.props.theme, 'common/common');
        return (React.createElement("div", {className: styles.errorApp}, "Application unavailable"));
    }
}
const mapStateToProps = (state) => {
    return {
        theme: state.theme
    };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = react_redux_1.connect(mapStateToProps)(ApplicationErrorComponent);
//# sourceMappingURL=ApplicationErrorComponent.js.map