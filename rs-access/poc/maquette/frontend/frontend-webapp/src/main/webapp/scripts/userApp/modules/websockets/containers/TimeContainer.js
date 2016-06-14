import React from 'react'
import { connect } from 'react-redux'
import { connectTime, disconnectTime } from '../actions/WSTimeActions'
import { startTime } from '../actions/TimeActions'
import { getThemeStyles } from 'common/theme/ThemeUtils'
import Time from '../components/TimeComponent'

class TimeContainer extends React.Component {

  componentWillMount(){
    // Action to connect to websocket server
    this.client = this.props.webSocketConnect()
    // Action to start the thread which send time by websocket
    this.props.startTime()
  }

  componentWillUnmount(){
    // Action to disconnect from web socket server
    this.props.webSocketDisconnect(this.client)
  }

  render(){
    // Render time
    const styles = getThemeStyles(this.props.theme,'userApp/base')
    if (this.props.started === true){
      return (
        <Time styles={styles}
          time={this.props.time}/>
      )
    }
    return null
  }
}

const mapDispatchToProps = (dispatch) => {
  return {
    webSocketConnect: () => dispatch(connectTime()),
    webSocketDisconnect: (sock) => dispatch(disconnectTime(sock)),
    startTime: () => dispatch(startTime())
  }
}
const mapStateToProps = (state) => {
  return {
    theme: state.theme,
    time: state.ws.time,
    started: state.ws.started
  }
}
module.exports = connect(mapStateToProps,mapDispatchToProps)(TimeContainer)
