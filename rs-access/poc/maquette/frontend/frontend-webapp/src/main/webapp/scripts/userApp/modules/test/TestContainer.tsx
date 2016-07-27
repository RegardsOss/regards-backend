import * as React from 'react'

import Test from './Test'

export class TestContainer extends React.Component<any, any> {
  render(){
    console.log("OYO")
    return (<Test />)
  }
}

export default TestContainer
