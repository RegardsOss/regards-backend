"use strict";
const React = require('react');
class SelectThemeComponent extends React.Component {
    constructor() {
        super();
        this.onChange = this.onChange.bind(this);
    }
    onChange(e) {
        this.props.onThemeChange(e.target.value);
    }
    componentWillMount() {
        this.setState({
            selectedValue: this.props.curentTheme
        });
    }
    render() {
        const { styles, themes, onThemeChange } = this.props;
        return (React.createElement("div", {className: styles["select-theme"]}, React.createElement("span", null, " Select your theme : "), React.createElement("select", {value: this.props.curentTheme, onChange: this.onChange}, themes.map((theme) => {
            return React.createElement("option", {key: theme, value: theme}, theme);
        }))));
    }
}
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = SelectThemeComponent;
//# sourceMappingURL=SelectThemeComponent.js.map