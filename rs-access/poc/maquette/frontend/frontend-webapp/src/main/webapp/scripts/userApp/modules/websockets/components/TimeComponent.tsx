import * as React from 'react'

class Time extends React.Component<any, any> {
  constructor(){
    super()
  }
  render(){
    return (
      <div>
        {this.props.time}
      </div>
    )
  }
}

export default Time
