import * as React from 'react'

import AccessRightsComponent from '../../../common/access-rights/AccessRightsComponent'
import { Dependencies } from '../../../common/access-rights/AccessRightsViewType'
import Test from './Test'

export class TestContainer extends React.Component<any, any> {
  render(){
    const dependencies:Dependencies = {
      "GET" : ["dependence"]
    }

    return (
      <AccessRightsComponent dependencies={dependencies} >
        <Test />
      </AccessRightsComponent>
    )
  }
}

export default TestContainer
