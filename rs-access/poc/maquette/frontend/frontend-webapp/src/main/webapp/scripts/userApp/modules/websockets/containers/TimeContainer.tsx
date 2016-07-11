import * as React from 'react'
import { connect } from 'react-redux'
import { connectTime, disconnectTime } from '../actions/WSTimeActions'
import { startTime } from '../actions/TimeActions'
import { getThemeStyles } from '../../../../common/theme/ThemeUtils'
import Time from '../components/TimeComponent'

interface TimeProps {
  theme?: string,
  time?: any,
  started?: boolean,
  webSocketConnect?: any,
  webSocketDisconnect? : any,
  startTime? : any
}

class TimeContainer extends React.Component<TimeProps, any> {

  // Websocket client
  client:any

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

const mapDispatchToProps = (dispatch:any) => {
  return {
    webSocketConnect: () => dispatch(connectTime()),
    webSocketDisconnect: (sock:any) => dispatch(disconnectTime(sock)),
    startTime: () => dispatch(startTime())
  }
}
const mapStateToProps = (state:any) => {
  return {
    theme: state.theme,
    time: state.ws.time,
    started: state.ws.started
  }
}
const timeConnected = connect<{}, {}, TimeProps>(mapStateToProps,mapDispatchToProps)(TimeContainer)
export default timeConnected
module.exports = timeConnected
