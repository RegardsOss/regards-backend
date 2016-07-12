"use strict";
const React = require('react');
const react_redux_1 = require('react-redux');
const WSTimeActions_1 = require('../actions/WSTimeActions');
const TimeActions_1 = require('../actions/TimeActions');
const ThemeUtils_1 = require('../../../../common/theme/ThemeUtils');
const TimeComponent_1 = require('../components/TimeComponent');
class TimeContainer extends React.Component {
    componentWillMount() {
        this.client = this.props.webSocketConnect();
        this.props.startTime();
    }
    componentWillUnmount() {
        this.props.webSocketDisconnect(this.client);
    }
    render() {
        const styles = ThemeUtils_1.getThemeStyles(this.props.theme, 'userApp/base');
        if (this.props.started === true) {
            return (React.createElement(TimeComponent_1.default, {styles: styles, time: this.props.time}));
        }
        return null;
    }
}
const mapDispatchToProps = (dispatch) => {
    return {
        webSocketConnect: () => dispatch(WSTimeActions_1.connectTime()),
        webSocketDisconnect: (sock) => dispatch(WSTimeActions_1.disconnectTime(sock)),
        startTime: () => dispatch(TimeActions_1.startTime())
    };
};
const mapStateToProps = (state) => {
    return {
        theme: state.theme,
        time: state.ws.time,
        started: state.ws.started
    };
};
const timeConnected = react_redux_1.connect(mapStateToProps, mapDispatchToProps)(TimeContainer);
Object.defineProperty(exports, "__esModule", { value: true });
exports.default = timeConnected;
//# sourceMappingURL=TimeContainer.js.map