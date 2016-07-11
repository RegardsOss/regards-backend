"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const ThemeUtils_1 = require('../../../common/theme/ThemeUtils');
const react_router_1 = require('react-router');
class Test extends React.Component {
    constructor() {
        super();
        this.state = {
            inputValue: ''
        };
        this.onChange = this.onChange.bind(this);
        this.onClick = this.onClick.bind(this);
    }
    componentWillMount() {
        // Read query parameters
        const { value } = this.props.location.query;
        this.setState({
            inputValue: value
        });
    }
    onChange(e) {
        let { value } = this.props.location.query;
        value = e.target.value;
        this.setState({
            inputValue: e.target.value
        });
    }
    onClick() {
        // Update query parameters of the current url
        const { location } = this.props;
        const route = location.pathname + "?value=" + this.state.inputValue;
        react_router_1.browserHistory.push(route);
    }
    render() {
        const styles = ThemeUtils_1.getThemeStyles(this.props.theme, 'adminApp/styles');
        return (React.createElement("div", {className: styles["grid-basics-example"]}, React.createElement("input", {type: 'text', value: this.state.inputValue, onChange: this.onChange}), React.createElement("input", {type: "button", onClick: this.onClick, value: "update"}), React.createElement("div", {className: styles.row + " " + styles.display}, React.createElement("div", {className: styles["small-2"] + " " + styles["large-4"]}, "4 columns"), React.createElement("div", {className: styles["small-4"] + " " + styles["large-4"]}, "4 columns"), React.createElement("div", {className: styles["small-6"] + " " + styles["large-4"]}, "4 columns")), React.createElement("div", {className: styles.row + " " + styles.display}, React.createElement("div", {className: styles["large-3"]}, "3 columns"), React.createElement("div", {className: styles["large-6"]}, "6 columns"), React.createElement("div", {className: styles["large-3"]}, "3 columns")), React.createElement("div", {className: styles.row + " " + styles.display}, React.createElement("div", {className: styles["small-6"] + " " + styles["large-2"]}, "2 columns"), React.createElement("div", {className: styles["small-6"] + " " + styles["large-8"]}, "8 columns"), React.createElement("div", {className: styles["small-12"] + " " + styles["large-2"]}, "2 columns"))));
    }
}
const mapStateToProps = (state) => {
    return {
        theme: state.theme
    };
};
module.exports = react_redux_1.connect(mapStateToProps)(Test);
//# sourceMappingURL=Test.js.map