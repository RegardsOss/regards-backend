import * as React from 'react'
import AccessRightsComponent from '../../../common/access-rights/AccessRightsComponent'

import { Dependencies } from '../../../common/access-rights/AccessRightsViewType'

class Test extends React.Component<any, any> {

  getDependencies():Dependencies{
    return {
      'GET' : ["dependencies"]
    }
  }

  render(){
    return (<div>This view shall not be displayed ! </div>)
  }
}

export default Test
