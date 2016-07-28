import * as React from 'react'
import { connectDependencies, DependencyAccessRight } from '../../../common/access-rights'

class Test extends React.Component<any, any> {

  render(){
    return (<div>This view shall not be displayed ! </div>)
  }
}

const dependencies:Array<DependencyAccessRight> = [{id:"/undefined@GET",verb:"GET",endpoint:"/undefined",access:false}]
export default connectDependencies(dependencies)(Test)
