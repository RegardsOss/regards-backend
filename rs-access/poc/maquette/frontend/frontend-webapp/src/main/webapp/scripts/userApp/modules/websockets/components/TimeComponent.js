import React from 'react';

class Time extends React.Component {
  constructor(){
    super();
  }
  render(){
    return (
      <div class={this.props.styles.timer}>
        {this.props.time}
      </div>
    );
  }
}

export default Time
