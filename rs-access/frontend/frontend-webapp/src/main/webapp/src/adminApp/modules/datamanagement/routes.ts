import { PlainRoute } from "react-router"
import DatasetCreateContainer from "./dataset/containers/DatasetCreateContainer"


export const datamanagementRoute: PlainRoute = {
  path: 'dataset',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasetCreateContainer
      })
    })
  }
}

export const datasetCreateRoute: PlainRoute = {
  path: 'dataset/create',
  getComponents(nextState: any, cb: any): any {
    require.ensure([], (require: any) => {
      cb(null, {
        content: DatasetCreateContainer
      })
    })
  }
}
